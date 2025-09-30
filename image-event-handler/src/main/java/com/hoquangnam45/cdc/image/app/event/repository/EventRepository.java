package com.hoquangnam45.cdc.image.app.event.repository;

import com.hoquangnam45.cdc.image.app.common.enums.JobStatus;
import com.hoquangnam45.cdc.image.app.common.model.GeneratedImageMdl;
import com.hoquangnam45.cdc.image.app.common.model.ProcessJobConfigurationMdl;
import com.hoquangnam45.cdc.image.app.common.model.ProcessingJobMdl;
import com.hoquangnam45.cdc.image.app.common.model.UploadedImageMdl;
import com.hoquangnam45.cdc.image.app.common.model.UserImageMdl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
@Repository
public interface EventRepository {
    void saveUserImage(UserImageMdl request);

    Boolean isUserImageExist(@Param("userId") UUID userId, @Param("uploadedImageId") UUID uploadedImageId);

    void saveUploadedImage(UploadedImageMdl request);

    void saveGeneratedImage(GeneratedImageMdl request);

    Boolean hasImageThumbnailGenerated(@Param("md5Hash") String md5Hash);

    UUID getUploadedImageId(@Param("md5Hash") String md5Hash);

    List<ProcessJobConfigurationMdl> getAllUnprocessingJobConfigurations(@Param("imageId") UUID imageId);

    void updateProcessingJob(@Param("jobId") UUID jobId, @Param("remark") String remark, @Param("jobStatus") JobStatus jobStatus, @Param("endedAt") Instant endedAt);

    void saveProcessingJob(ProcessingJobMdl processingJobMdl);

    void removeLastProcessingJob(@Param("configurationId") UUID configurationId, @Param("uploadedImageId") UUID uploadedImageId);
}
