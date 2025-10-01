package com.hoquangnam45.cdc.image.app.common.filter;

import com.hoquangnam45.cdc.image.app.common.constant.CommonClaims;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.common.model.JwtUser;
import com.hoquangnam45.cdc.image.app.common.service.TokenService;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
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
            JWTClaimsSet accessToken = null;
            try {
                accessToken = tokenService.validateJwtToken(accessTokenString);
            } catch (Exception e) {
                log.error("Failed jwt validation. Reason: {}", e.getMessage(), e);
                return Mono.empty();
            }
            try {
                UUID userId = UUID.fromString(accessToken.getSubject());
                String username = accessToken.getStringClaim(CommonClaims.USERNAME);
                String email = accessToken.getStringClaim(CommonClaims.EMAIL);
                boolean emailConfirmed = accessToken.getBooleanClaim(CommonClaims.EMAIL_CONFIRMED);
                String phoneNumber = accessToken.getStringClaim(CommonClaims.PHONE_NUMBER);
                boolean phoneNumberConfirmed = accessToken.getBooleanClaim(CommonClaims.PHONE_NUMBER_CONFIRMED);
                JwtUser jwtUser = new JwtUser(userId, username, email, phoneNumber, emailConfirmed, phoneNumberConfirmed);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(jwtUser, accessToken, Collections.emptyList());
                securityContext.setAuthentication(authenticationToken);
                return Mono.just(securityContext);
            } catch (ParseException e) {
                log.error("Bad jwt format. Reason: {}", e.getMessage(), e);
                return Mono.empty();
            }
        });
    }
}
