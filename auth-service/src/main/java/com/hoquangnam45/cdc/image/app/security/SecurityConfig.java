package com.hoquangnam45.cdc.image.app.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoquangnam45.cdc.image.app.common.constant.CommonResponseCode;
import com.hoquangnam45.cdc.image.app.common.filter.JwtFilter;
import com.hoquangnam45.cdc.image.app.common.model.ServiceResponse;
import com.hoquangnam45.cdc.image.app.common.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
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
    public SecurityWebFilterChain userSecurityFilterChain(ServerHttpSecurity http, TokenService tokenService, ObjectMapper objectMapper) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .addFilterAt(new JwtFilter(tokenService), SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint((exchange, authenticationException) -> {
                    exchange.getResponse().setRawStatusCode(401);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    log.error("Unauthenticated. Reason: {}", authenticationException.getMessage(), authenticationException);
                    try {
                        String unauthenticatedResponse = objectMapper.writeValueAsString(ServiceResponse.failEmpty(exchange.getRequest().getPath().value(), CommonResponseCode.UNAUTHENTICATED, "Authentication fail", "Authentication fail. Reason: " + authenticationException.getMessage()));
                        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(unauthenticatedResponse.getBytes(StandardCharsets.UTF_8))));
                    } catch (JsonProcessingException e) {
                        log.error("Unexpected error when return authenticate response. Reason: {}", authenticationException.getMessage(), authenticationException);
                        exchange.getResponse().setRawStatusCode(500);
                        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(("Authentication fail. Unknown reason. Reason: " + e.getMessage()).getBytes(StandardCharsets.UTF_8))));
                    }
                }))
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
