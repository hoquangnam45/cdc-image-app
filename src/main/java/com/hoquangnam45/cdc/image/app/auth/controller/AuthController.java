package com.hoquangnam45.cdc.image.app.auth.controller;

import com.hoquangnam45.cdc.image.app.auth.model.LoginRequest;
import com.hoquangnam45.cdc.image.app.auth.model.LoginResponse;
import com.hoquangnam45.cdc.image.app.auth.model.RegisterRequest;
import com.hoquangnam45.cdc.image.app.auth.service.AuthService;
import com.hoquangnam45.cdc.image.app.common.model.ServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<ServiceResponse<LoginResponse>>> login(LoginRequest request) {
        return authService.login(request)
                .map(loginResponse -> ResponseEntity.ok(ServiceResponse.success(loginResponse)));
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<ServiceResponse<LoginResponse>>> register(RegisterRequest request) {
        return Mono.empty(); // TODO: Implement registration logic
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ServiceResponse<LoginResponse>>> refresh() { // TODO: Implement refresh logic
        return Mono.empty();
    }
}
