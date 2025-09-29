package com.hoquangnam45.cdc.image.app.image.model;

import com.hoquangnam45.cdc.image.app.image.enums.ImageStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUploadedImageMdl {
    private UUID id;
    private UUID imageId;
    private UUID userId;
    private Integer width;
    private Integer height;
    private Integer fileSize;
    private String filePath;
    private String fileType;
    private String fileHash;
    private String fileName;
    private Instant createdAt;
    private Instant updatedAt;

}
