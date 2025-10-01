package com.hoquangnam45.cdc.image.app.event.repository;

import com.hoquangnam45.cdc.image.app.common.enums.ImageStatus;
import com.hoquangnam45.cdc.image.app.common.enums.JobStatus;
import com.hoquangnam45.cdc.image.app.common.model.GeneratedImageMdl;
import com.hoquangnam45.cdc.image.app.common.model.ProcessJobConfigurationMdl;
import com.hoquangnam45.cdc.image.app.common.model.ProcessingJobMdl;
import com.hoquangnam45.cdc.image.app.common.model.UploadedImageMdl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
@Repository
public interface EventRepository {
    Boolean isUserImageExpired(@Param("userImageId") UUID userImageId, @Param("now") Instant now);

    void saveUploadedImage(UploadedImageMdl request);

    void saveGeneratedImage(GeneratedImageMdl request);

    Boolean hasImageThumbnailGenerated(@Param("md5Hash") String md5Hash);

    UUID getUploadedImageId(@Param("md5Hash") String md5Hash);

    List<ProcessJobConfigurationMdl> getAllUnprocessingJobConfigurations(@Param("imageId") UUID imageId);

    void updateProcessingJob(@Param("jobId") UUID jobId, @Param("remark") String remark, @Param("jobStatus") JobStatus jobStatus, @Param("endedAt") Instant endedAt);

    void saveProcessingJob(ProcessingJobMdl processingJobMdl);

    void removeLastProcessingJob(@Param("configurationId") UUID configurationId, @Param("uploadedImageId") UUID uploadedImageId);

    void updateUserImageStatus(@Param("id") UUID id, @Param("status") ImageStatus status, @Param("uploadedImageId") UUID uploadedImageId);

    int updateUserImageStatusRunning(@Param("id") UUID id);

    ImageStatus getUploadedImageStatus(@Param("md5Hash") String md5Hash);

    JobStatus getJobStatus(@Param("configurationId") UUID configurationId, @Param("uploadedImageId") UUID uploadedImageId);
}
