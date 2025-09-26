package com.hoquangnam45.cdc.image.app.image.model;

import com.hoquangnam45.cdc.image.app.image.enums.JobStatus;
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
public class ProcessingJobMdl {
    private UUID id;
    private UUID imageId;
    private UUID configurationId;
    private JobStatus jobStatus;
    private Instant startedAt;
    private Instant endedAt;
    private String remark;
}
