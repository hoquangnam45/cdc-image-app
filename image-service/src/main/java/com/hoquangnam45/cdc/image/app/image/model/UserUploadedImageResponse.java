package com.hoquangnam45.cdc.image.app.image.model;

import com.hoquangnam45.cdc.image.app.common.enums.ImageStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserUploadedImageResponse(
        UUID id,
        UUID userId,
        Integer width,
        Integer height,
        Integer fileSize,
        String downloadUrl,
        String uploadUrl,
        String fileType,
        String fileHash,
        ImageStatus status,
        String fileName,
        Instant createdAt,
        Instant updatedAt,
        Instant expiredAt,
        List<UserGeneratedImageResponse> thumbnails) {
}
