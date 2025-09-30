package com.hoquangnam45.cdc.image.app.image.model;

import com.hoquangnam45.cdc.image.app.common.enums.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record UserGeneratedImageResponse(
        UUID id,
        UUID userId,
        UUID imageId,
        UUID configurationId,
        Integer width,
        Integer height,
        Integer fileSize,
        String downloadUrl,
        String fileType,
        String fileHash,
        Instant createdAt,
        JobStatus status
) {
}
