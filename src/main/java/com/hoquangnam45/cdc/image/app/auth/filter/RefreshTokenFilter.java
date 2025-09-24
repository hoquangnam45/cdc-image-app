package com.hoquangnam45.cdc.image.app.auth.filter;

import com.hoquangnam45.cdc.image.app.auth.model.RefreshTokenSession;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import org.springframework.http.HttpCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class RefreshTokenFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Mono<SecurityContext> contextMono = ReactiveSecurityContextHolder.getContext()
            .flatMap(context -> {
                Authentication auth = context.getAuthentication();
                if (auth != null && auth.isAuthenticated()) {
                    return Mono.just(context);
                }
                return createNewContextFromCookie(exchange);
            })
            .switchIfEmpty(createNewContextFromCookie(exchange));
        return chain.filter(exchange)
                    .contextWrite(ctx -> ReactiveSecurityContextHolder.withSecurityContext(contextMono));
    }

    private Mono<SecurityContext> createNewContextFromCookie(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            HttpCookie refreshCookie = exchange.getRequest().getCookies().getFirst(CommonConstant.REFRESH_COOKIE_NAME);
            if (refreshCookie == null) {
                return Mono.empty();
            }
            RefreshTokenSession session = new RefreshTokenSession(refreshCookie.getValue());
            SecurityContextImpl securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(session);
            return Mono.just(securityContext);
        });
    }
}
