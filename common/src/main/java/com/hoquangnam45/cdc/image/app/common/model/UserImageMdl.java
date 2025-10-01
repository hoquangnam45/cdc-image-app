package com.hoquangnam45.cdc.image.app.common.model;

import com.hoquangnam45.cdc.image.app.common.enums.ImageStatus;
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
    private Instant expiredAt;
    private ImageStatus status;
}
