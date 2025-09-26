package com.hoquangnam45.cdc.image.app.security;

import com.hoquangnam45.cdc.image.app.common.filter.JwtFilter;
import com.hoquangnam45.cdc.image.app.common.service.TokenService;
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
                .build();
    }

    @Bean
    @Order(3)
    public SecurityWebFilterChain userSecurityFilterChain(ServerHttpSecurity http, TokenService tokenService) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .addFilterAt(new JwtFilter(tokenService), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public SecurityWebFilterChain defaultSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .build();
    }
}
