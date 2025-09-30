package com.hoquangnam45.cdc.image.app.image.model;

import java.time.Instant;
import java.util.UUID;

public record UploadImageResponse(UUID id, String fileName, Instant createdAt, Instant expiredAt, String uploadUrl) {
}
