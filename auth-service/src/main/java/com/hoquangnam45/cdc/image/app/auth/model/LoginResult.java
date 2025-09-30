package com.hoquangnam45.cdc.image.app.auth.model;

import java.time.Duration;
import java.time.Instant;

public record LoginResult(
        String accessToken,
        String refreshToken,
        Duration accessTokenExpireDuration,
        Duration refreshTokenExpireDuration,
        Instant accessTokenExpireAt,
        Instant refreshTokenExpireAt) {
}
