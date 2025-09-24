package com.hoquangnam45.cdc.image.app.auth.model;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpireAt,
        Instant refreshTokenExpireAt) {
}
