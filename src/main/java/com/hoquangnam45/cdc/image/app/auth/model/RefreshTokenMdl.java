package com.hoquangnam45.cdc.image.app.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenMdl {
    private UUID id;
    private UUID userId;
    private String refreshToken;
    private String accessToken;
    private Integer ttlSec;
    private Instant createdAt;
    private Instant expiredAt;
}
