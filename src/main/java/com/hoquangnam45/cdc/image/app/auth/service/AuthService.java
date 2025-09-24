package com.hoquangnam45.cdc.image.app.auth.service;

import com.hoquangnam45.cdc.image.app.auth.model.LoginRequest;
import com.hoquangnam45.cdc.image.app.auth.model.LoginResponse;
import com.hoquangnam45.cdc.image.app.auth.model.RefreshTokenMdl;
import com.hoquangnam45.cdc.image.app.auth.model.UserMdl;
import com.hoquangnam45.cdc.image.app.auth.repository.AuthRepository;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.common.constant.CommonResponseCode;
import com.hoquangnam45.cdc.image.app.common.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthRepository authRepository;
    private final TokenService tokenService;
    private final Duration accessTokenExpireDuration;
    private final Duration refreshTokenExpireDuration;

    public AuthService(AuthRepository authRepository, TokenService tokenService, @Value("${jwt.access-token.expiration-ms}") Integer accessTokenExpireDurationMs, @Value("${jwt.refresh-token.expiration-ms}") Integer refreshTokenExpireDurationMs) {
        this.authRepository = authRepository;
        this.tokenService = tokenService;
        this.accessTokenExpireDuration = Duration.ofMillis(accessTokenExpireDurationMs);
        this.refreshTokenExpireDuration = Duration.ofMillis(refreshTokenExpireDurationMs);
    }

    public Mono<LoginResponse> login(LoginRequest request) {
        if (StringUtils.isAllBlank(request.username(), request.email(), request.phoneNumber())) {
            return Mono.error(new ServiceException(400, CommonResponseCode.REQUEST_VALIDATION_FAIL, "Username, email, or phone number is required."));
        }

        if (StringUtils.isBlank(request.password())) {
            return Mono.error(new ServiceException(400, CommonResponseCode.REQUEST_VALIDATION_FAIL, "Password is required."));
        }

        UserMdl user = authRepository.findUser(request.username(), request.phoneNumber(), request.email());
        if (user == null) {
            return Mono.error(new ServiceException(404, CommonResponseCode.NOT_FOUND, "User not found"));
        }
        boolean validPassword = validPassword(request.password(), user.getPasswordHash());
        if (!validPassword) {
            return Mono.error(new ServiceException(401, CommonResponseCode.UNAUTHENTICATED, "Invalid credentials."));
        }
        Instant now = Instant.now();
        String accessToken = tokenService.generateJwtToken(now, accessTokenExpireDuration, user);
        String refreshToken = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        Instant refreshTokenExpireAt = now.plus(refreshTokenExpireDuration);
        Instant accessTokenExpireAt = now.plus(accessTokenExpireDuration);
        String hashedRefreshToken = tokenService.hashToken(CommonConstant.HASH_ALGORITHM, refreshToken);
        String hashedAccessToken = tokenService.hashToken(CommonConstant.HASH_ALGORITHM, accessToken);
        RefreshTokenMdl refreshTokenMdl = new RefreshTokenMdl(UUID.randomUUID(), user.getId(), hashedRefreshToken, hashedAccessToken, (int) refreshTokenExpireDuration.toSeconds(), now, refreshTokenExpireAt);
        authRepository.saveRefreshToken(refreshTokenMdl);
        return Mono.just(new LoginResponse(accessToken, refreshToken, accessTokenExpireAt, refreshTokenExpireAt));
    }

    private static boolean validPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
