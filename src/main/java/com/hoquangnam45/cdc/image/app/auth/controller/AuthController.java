package com.hoquangnam45.cdc.image.app.auth.controller;

import com.hoquangnam45.cdc.image.app.auth.model.LoginRequest;
import com.hoquangnam45.cdc.image.app.auth.model.LoginResponse;
import com.hoquangnam45.cdc.image.app.auth.model.LoginResult;
import com.hoquangnam45.cdc.image.app.auth.model.RegisterRequest;
import com.hoquangnam45.cdc.image.app.auth.service.AuthService;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.common.constant.CommonResponseCode;
import com.hoquangnam45.cdc.image.app.common.exception.ServiceException;
import com.hoquangnam45.cdc.image.app.common.model.ServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<ServiceResponse<LoginResponse>>> login(@RequestBody LoginRequest request) {
        return authService.login(request)
                .map(AuthController::buildSuccessLoginResponse);
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<ServiceResponse<LoginResponse>>> register(@RequestBody RegisterRequest request) {
        return authService.register(request)
                .map(AuthController::buildSuccessLoginResponse);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ServiceResponse<LoginResponse>>> refresh(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(CommonConstant.REFRESH_COOKIE_NAME);
        if (cookie == null) {
            return Mono.error(new ServiceException(401, CommonResponseCode.UNAUTHENTICATED, "Refresh token is missing"));
        }
        String refreshToken = cookie.getValue();
        return authService.refresh(refreshToken)
                .map(AuthController::buildSuccessLoginResponse);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<ServiceResponse<Boolean>>> logout(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(CommonConstant.REFRESH_COOKIE_NAME);
        if (cookie == null) {
            return Mono.just(ResponseEntity.ok(ServiceResponse.success(true)));
        }
        String refreshToken = cookie.getValue();
        return authService.logout(refreshToken)
                .map(_x -> AuthController.buildSuccessLogoutResponse());
    }

    private static ResponseEntity<ServiceResponse<LoginResponse>> buildSuccessLoginResponse(LoginResult result) {
        ResponseCookie authCookie = ResponseCookie.from(CommonConstant.AUTH_COOKIE_NAME, result.accessToken())
                .httpOnly(true)
                .secure(true)
                .path("/api")
                .maxAge(result.accessTokenExpireDuration())
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from(CommonConstant.REFRESH_COOKIE_NAME, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(result.refreshTokenExpireDuration())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ServiceResponse.success(new LoginResponse(result.accessToken(), result.refreshToken(), result.accessTokenExpireAt(), result.refreshTokenExpireAt())));
    }

    private static ResponseEntity<ServiceResponse<Boolean>> buildSuccessLogoutResponse() {
        ResponseCookie authCookie = ResponseCookie.from(CommonConstant.AUTH_COOKIE_NAME, "")
                .maxAge(0)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from(CommonConstant.REFRESH_COOKIE_NAME, "")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ServiceResponse.success(true));
    }
}
