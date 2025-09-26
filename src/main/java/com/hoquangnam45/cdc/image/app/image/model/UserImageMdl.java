package com.hoquangnam45.cdc.image.app.image.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserImageMdl {
    private UUID id;
    private UUID userId;
    private UUID uploadedImageId;
    private String fileName;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
