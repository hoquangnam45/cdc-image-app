package com.hoquangnam45.cdc.image.app.common.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.hoquangnam45.cdc.image.app.common.constant.CommonClaims;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.common.model.JwtUser;
import com.hoquangnam45.cdc.image.app.common.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtFilter implements WebFilter {
    private final TokenService tokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .switchIfEmpty(createNewContextFromCookie(exchange))
                .flatMap(securityContext -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<SecurityContext> createNewContextFromCookie(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            HttpCookie authCookie = exchange.getRequest().getCookies().getFirst(CommonConstant.AUTH_COOKIE_NAME);
            if (authCookie == null) {
                return Mono.empty();
            }
            SecurityContextImpl securityContext = new SecurityContextImpl();
            String accessTokenString = authCookie.getValue();
            DecodedJWT accessToken = tokenService.validateJwtToken(accessTokenString);
            UUID userId = UUID.fromString(accessToken.getSubject());
            String username = accessToken.getClaim(CommonClaims.USERNAME).asString();
            String email = accessToken.getClaim(CommonClaims.EMAIL).asString();
            boolean emailConfirmed = accessToken.getClaim(CommonClaims.EMAIL_CONFIRMED).asBoolean();
            String phoneNumber = accessToken.getClaim(CommonClaims.PHONE_NUMBER).asString();
            boolean phoneNumberConfirmed = accessToken.getClaim(CommonClaims.PHONE_NUMBER_CONFIRMED).asBoolean();
            JwtUser jwtUser = new JwtUser(userId, username, email, phoneNumber, emailConfirmed, phoneNumberConfirmed);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(jwtUser, accessToken, Collections.emptyList());
            securityContext.setAuthentication(authenticationToken);
            return Mono.just(securityContext);
        });
    }
}
