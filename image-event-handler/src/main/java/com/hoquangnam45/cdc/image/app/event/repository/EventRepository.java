package com.hoquangnam45.cdc.image.app.event.repository;

import com.hoquangnam45.cdc.image.app.common.enums.JobStatus;
import com.hoquangnam45.cdc.image.app.common.model.GeneratedImageMdl;
import com.hoquangnam45.cdc.image.app.common.model.ProcessJobConfigurationMdl;
import com.hoquangnam45.cdc.image.app.common.model.ProcessingJobMdl;
import com.hoquangnam45.cdc.image.app.common.model.UploadedImageMdl;
import com.hoquangnam45.cdc.image.app.common.model.UserImageMdl;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
@Repository
public interface EventRepository {
    void saveUserImage(UserImageMdl request);

    Boolean isUserImageExist(UUID userId, UUID uploadedImageId);

    void saveUploadedImage(UploadedImageMdl request);

    void saveGeneratedImage(GeneratedImageMdl request);

    Boolean hasImageThumbnailGenerated(String md5Hash);

    UUID getUploadedImageId(String md5Hash);

    List<ProcessJobConfigurationMdl> getAllUnprocessingJobConfigurations(UUID imageId);

    void updateProcessingJob(UUID jobId, String remark, JobStatus jobStatus, Instant endedAt);

    void saveProcessingJob(ProcessingJobMdl processingJobMdl);

    void removeLastProcessingJob(UUID configurationId, UUID uploadedImageId);
}
