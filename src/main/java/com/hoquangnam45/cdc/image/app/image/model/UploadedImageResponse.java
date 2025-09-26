package com.hoquangnam45.cdc.image.app.image.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UploadedImageResponse(UUID id, String fileName, String downloadUrl, List<String> tags, Integer width, Integer height, String fileType, Integer fileSize, Instant createdAt, Instant updatedAt) {
}
