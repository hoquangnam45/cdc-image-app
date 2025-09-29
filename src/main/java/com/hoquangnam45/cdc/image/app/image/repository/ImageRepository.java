package com.hoquangnam45.cdc.image.app.image.repository;

import com.hoquangnam45.cdc.image.app.image.enums.ImageStatus;
import com.hoquangnam45.cdc.image.app.image.enums.JobStatus;
import com.hoquangnam45.cdc.image.app.image.model.GeneratedImageMdl;
import com.hoquangnam45.cdc.image.app.image.model.ProcessJobConfigurationMdl;
import com.hoquangnam45.cdc.image.app.image.model.ProcessingJobMdl;
import com.hoquangnam45.cdc.image.app.image.model.UploadedImageMdl;
import com.hoquangnam45.cdc.image.app.image.model.UserGeneratedImageMdl;
import com.hoquangnam45.cdc.image.app.image.model.UserImageMdl;
import com.hoquangnam45.cdc.image.app.image.model.UserUploadedImageMdl;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
@Repository
public interface ImageRepository {
    void saveUserImage(UserImageMdl request);

    Boolean isUserImageExist(UUID userId, UUID uploadedImageId);

    void updateUserImageUploadedImageId(UUID userImageId, UUID uploadedImageId);

    void saveUploadedImage(UploadedImageMdl request);

    void saveGeneratedImage(GeneratedImageMdl request);

//    List<UploadedImageMdl> listUploadedUserImage(UUID userId);

    Boolean hasImageThumbnailGenerated(String md5Hash);

    UUID getUploadedImageId(String md5Hash);

    List<ProcessJobConfigurationMdl> getAllUnprocessingJobConfigurations(UUID imageId);

    void updateProcessingJob(UUID jobId, String remark, JobStatus jobStatus, Instant endedAt);

    void saveProcessingJob(ProcessingJobMdl processingJobMdl);

    List<UserUploadedImageMdl> getUserUploadedImages(UUID userId);

    List<UserGeneratedImageMdl> getUserGeneratedImages(UUID userId);

    void removeLastProcessingJob(UUID configurationId, UUID uploadedImageId);
}
