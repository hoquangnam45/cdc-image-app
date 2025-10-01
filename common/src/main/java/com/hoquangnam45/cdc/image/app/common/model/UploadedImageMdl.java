package com.hoquangnam45.cdc.image.app.common.model;

import com.hoquangnam45.cdc.image.app.common.enums.ImageStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UploadedImageMdl {
    private UUID id;
    private Integer width;
    private Integer height;
    private Integer fileSize;
    private String filePath;
    private String fileType;
    private String fileHash;
    private ImageStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
