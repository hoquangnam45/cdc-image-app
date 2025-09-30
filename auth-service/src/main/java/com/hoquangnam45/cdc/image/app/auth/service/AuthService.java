package com.hoquangnam45.cdc.image.app.auth.service;

import com.hoquangnam45.cdc.image.app.auth.model.LoginRequest;
import com.hoquangnam45.cdc.image.app.auth.model.LoginResult;
import com.hoquangnam45.cdc.image.app.auth.model.RefreshTokenMdl;
import com.hoquangnam45.cdc.image.app.auth.model.RegisterRequest;
import com.hoquangnam45.cdc.image.app.common.model.UserMdl;
import com.hoquangnam45.cdc.image.app.auth.repository.AuthRepository;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.common.constant.CommonResponseCode;
import com.hoquangnam45.cdc.image.app.common.exception.ServiceException;
import com.hoquangnam45.cdc.image.app.common.service.TokenService;
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

    public AuthService(AuthRepository authRepository, TokenService tokenService, @Value("${jwt.access-token.expiration-min}") Integer accessTokenExpireDurationMin, @Value("${jwt.refresh-token.expiration-min}") Integer refreshTokenExpireDurationMin) {
        this.authRepository = authRepository;
        this.tokenService = tokenService;
        this.accessTokenExpireDuration = Duration.ofMinutes(accessTokenExpireDurationMin);
        this.refreshTokenExpireDuration = Duration.ofMinutes(refreshTokenExpireDurationMin);
    }

    public Mono<LoginResult> login(LoginRequest request) {
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
        return Mono.just(generateTokensAndSave(user));
    }

    public Mono<LoginResult> register(RegisterRequest request) {
        Throwable err = validateRegisterRequest(request);
        if (err != null) {
            return Mono.error(err);
        }
        String hashedPassword = BCrypt.hashpw(request.password(), BCrypt.gensalt());
        UserMdl user = new UserMdl(
                UUID.randomUUID(),
                request.username(),
                request.email(),
                request.phoneNumber(),
                hashedPassword,
                false,
                false,
                Instant.now()
        );
        authRepository.saveUser(user);
        return Mono.just(generateTokensAndSave(user));
    }

    public Mono<LoginResult> refresh(String refreshToken) {
        RefreshTokenMdl refreshTokenMdl;
        try {
            refreshTokenMdl = validateRefreshToken(refreshToken);
        } catch (ServiceException e) {
            return Mono.error(e);
        }
        authRepository.deleteRefreshToken(refreshTokenMdl.getRefreshToken());
        UserMdl userMdl = authRepository.getUser(refreshTokenMdl.getUserId());
        return Mono.just(generateTokensAndSave(userMdl));
    }

    public Mono<Boolean> logout(String refreshToken) {
        RefreshTokenMdl refreshTokenMdl;
        try {
            refreshTokenMdl = validateRefreshToken(refreshToken);
        } catch (ServiceException e) {
            refreshTokenMdl = null;
        }
        if (refreshTokenMdl != null) {
            authRepository.deleteRefreshToken(refreshTokenMdl.getRefreshToken());
        }
        return Mono.just(true);
    }

    private Throwable validateRegisterRequest(RegisterRequest request) {
        UserMdl userMdl = authRepository.findUser(request.username(), request.phoneNumber(), request.email());
        if (userMdl != null) {
            if (userMdl.getUsername().equals(request.username())) {
                return new ServiceException(409, CommonResponseCode.DUPLICATE, "Username already exists");
            } else if (userMdl.getEmail() != null && request.email() != null && userMdl.getEmail().equals(request.email())) {
                return new ServiceException(409, CommonResponseCode.DUPLICATE, "Email already exists");
            } else if (userMdl.getPhoneNumber() != null && request.phoneNumber() != null && userMdl.getPhoneNumber().equals(request.phoneNumber())) {
                return new ServiceException(409, CommonResponseCode.DUPLICATE, "Phone number already exists");
            }
        }
        return null;
    }

    private RefreshTokenMdl validateRefreshToken(String refreshToken) throws ServiceException {
        if (StringUtils.isBlank(refreshToken)) {
            throw new ServiceException(400, CommonResponseCode.REQUEST_VALIDATION_FAIL, "Refresh token is required");
        }
        String hashedRefreshToken = tokenService.hashToken(CommonConstant.HASH_ALGORITHM, refreshToken);
        RefreshTokenMdl refreshTokenMdl = authRepository.findRefreshToken(hashedRefreshToken);
        if (refreshTokenMdl == null) {
            throw new ServiceException(404, CommonResponseCode.NOT_FOUND, "Refresh token not found or may expired");
        }
        boolean isExpired = Instant.now().isAfter(refreshTokenMdl.getExpiredAt());
        if (isExpired) {
            throw new ServiceException(401, CommonResponseCode.UNAUTHENTICATED, "Refresh token expired");
        }
        return refreshTokenMdl;
    }

    private static boolean validPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

    private LoginResult generateTokensAndSave(UserMdl user) {
        Instant now = Instant.now();
        String accessToken = tokenService.generateJwtToken(now, accessTokenExpireDuration, user);
        String refreshToken = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        Instant refreshTokenExpireAt = now.plus(refreshTokenExpireDuration);
        Instant accessTokenExpireAt = now.plus(accessTokenExpireDuration);
        String hashedRefreshToken = tokenService.hashToken(CommonConstant.HASH_ALGORITHM, refreshToken);
        String hashedAccessToken = tokenService.hashToken(CommonConstant.HASH_ALGORITHM, accessToken);
        RefreshTokenMdl refreshTokenMdl = new RefreshTokenMdl(UUID.randomUUID(), user.getId(), hashedRefreshToken, hashedAccessToken, (int) refreshTokenExpireDuration.toSeconds(), now, refreshTokenExpireAt);
        authRepository.saveRefreshToken(refreshTokenMdl);
        return new LoginResult(accessToken, refreshToken, accessTokenExpireDuration, refreshTokenExpireDuration, accessTokenExpireAt, refreshTokenExpireAt);
    }
}
