package com.hoquangnam45.cdc.image.app.security;

import com.hoquangnam45.cdc.image.app.auth.filter.RefreshTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    @Order(1)
    public SecurityWebFilterChain authSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/auth/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .addFilterBefore(new RefreshTokenFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    @Order(2)
    public SecurityWebFilterChain userSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .build();
    }

    @Bean
    @Order(3) // It's good practice to give the last filter an explicit order
    public SecurityWebFilterChain defaultSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .build();
    }
}
