package com.hoquangnam45.cdc.image.app.image.repository;

import com.hoquangnam45.cdc.image.app.common.model.UserGeneratedImageMdl;
import com.hoquangnam45.cdc.image.app.common.model.UserImageMdl;
import com.hoquangnam45.cdc.image.app.common.model.UserUploadedImageMdl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Mapper
@Repository
public interface ImageRepository {
    List<UserUploadedImageMdl> getUserUploadedImages(@Param("userId") UUID userId);

    List<UserGeneratedImageMdl> getUserGeneratedImages(@Param("userId") UUID userId);

    void saveUserImage(UserImageMdl request);

    boolean deleteUserImage(@Param("userId") UUID userId, @Param("imageId") UUID imageId);
}
