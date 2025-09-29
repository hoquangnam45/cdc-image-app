package com.hoquangnam45.cdc.image.app.image.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UserUploadedImageResponse(
        UUID id,
        UUID userId,
        Integer width,
        Integer height,
        Integer fileSize,
        String downloadUrl,
        String fileType,
        String fileHash,
        String fileName,
        Instant createdAt,
        Instant updatedAt,
        List<UserGeneratedImageResponse> thumbnails) {
}
