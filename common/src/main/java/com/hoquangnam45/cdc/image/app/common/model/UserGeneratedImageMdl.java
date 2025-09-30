package com.hoquangnam45.cdc.image.app.common.model;

import com.hoquangnam45.cdc.image.app.common.enums.JobStatus;
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
public class UserGeneratedImageMdl {
    private UUID id;
    private UUID userId;
    private UUID imageId;
    private UUID configurationId;
    private Integer width;
    private Integer height;
    private Integer fileSize;
    private String filePath;
    private String fileType;
    private String fileHash;
    private Instant createdAt;
    private JobStatus status;
}
