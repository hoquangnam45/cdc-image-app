package com.hoquangnam45.cdc.image.app.image.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.image.model.UploadImageResponse;
import com.hoquangnam45.cdc.image.app.image.model.UserGeneratedImageMdl;
import com.hoquangnam45.cdc.image.app.image.model.UserGeneratedImageResponse;
import com.hoquangnam45.cdc.image.app.image.model.UserUploadedImageMdl;
import com.hoquangnam45.cdc.image.app.image.model.UserUploadedImageResponse;
import com.hoquangnam45.cdc.image.app.image.repository.ImageRepository;
import com.hoquangnam45.cdc.image.app.image.util.GcsUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ImageService {
    private final ImageRepository imageRepository;
    private final Storage storage;
    private final String bucketName;
    private final Duration presignedUrlDuration;

    public ImageService(
            ImageRepository imageRepository, Storage storage,
            @Value("${gcp.storage.bucket-name}") String bucketName,
            @Value("${gcp.storage.presigned-url.duration-minute}") long presignedUrlDurationMinutes) {
        this.imageRepository = imageRepository;
        this.storage = storage;
        this.bucketName = bucketName;
        this.presignedUrlDuration = Duration.ofMinutes(presignedUrlDurationMinutes);
    }

    public Mono<List<UploadImageResponse>> createUploadPresignedUrls(UUID userId, List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }
        return Flux.fromIterable(fileNames)
                .map(fileName -> generatePresignedUrl(userId, bucketName, fileName, presignedUrlDuration))
                .collectList();
    }

    private UploadImageResponse generatePresignedUrl(UUID userId, String bucketName, String fileName, Duration presignedUrlDuration) {
        UUID fileId = UUID.randomUUID();
        String objectName = "uploads/" + userId + "/" + fileId;
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName).build();

        URL signedUrl = storage.signUrl(blobInfo, presignedUrlDuration.toMinutes(), TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withExtHeaders(Map.of(
                        "x-goog-if-generation-match", "0",
                        "x-goog-meta-" + CommonConstant.FILE_NAME_METADATA, fileName
                )),
                Storage.SignUrlOption.withV4Signature());

        Instant now = Instant.now();
        Instant expiredAt = now.plus(presignedUrlDuration);
        return new UploadImageResponse(fileId, fileName, now, expiredAt, signedUrl.toString());
    }

    public Mono<List<UserUploadedImageResponse>> listUploadedUserImages(UUID userId) {
        List<UserUploadedImageMdl> userUploadedImages = imageRepository.getUserUploadedImages(userId);
        List<UserGeneratedImageMdl> userGeneratedImages = imageRepository.getUserGeneratedImages(userId);
        Map<UUID, List<UserGeneratedImageMdl>> generatedImageMap = new HashMap<>();
        for (UserGeneratedImageMdl userGeneratedImage : userGeneratedImages) {
            generatedImageMap.computeIfAbsent(userGeneratedImage.getImageId(), _k -> new ArrayList<>())
                    .add(userGeneratedImage);
        }
        List<UserUploadedImageResponse> response = new ArrayList<>();
        for (UserUploadedImageMdl userUploadedImage : userUploadedImages) {
            BlobInfo uploadedImageBlobInfo = BlobInfo.newBuilder(GcsUtil.parseToBlobId(userUploadedImage.getFilePath()))
                    .setContentType(userUploadedImage.getFileType())
                    .setCacheControl("public, max-age=86400")
                    .build();
            String uploadedImageDownloadUrl = storage.signUrl(uploadedImageBlobInfo, 1, TimeUnit.DAYS,
                            Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                            Storage.SignUrlOption.withV4Signature())
                    .toString();
            List<UserGeneratedImageResponse> thumbnails = new ArrayList<>();
            for (UserGeneratedImageMdl userGeneratedImageMdl : generatedImageMap.get(userUploadedImage.getImageId())) {
                BlobInfo thumbnailImageBlobInfo = BlobInfo.newBuilder(GcsUtil.parseToBlobId(userUploadedImage.getFilePath()))
                        .setContentType(userUploadedImage.getFileType())
                        .setCacheControl("public, max-age=86400")
                        .build();
                String thumbnailImageDownloadUrl = storage.signUrl(thumbnailImageBlobInfo, 1, TimeUnit.DAYS,
                                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                                Storage.SignUrlOption.withV4Signature())
                        .toString();
                thumbnails.add(new UserGeneratedImageResponse(
                        userGeneratedImageMdl.getId(),
                        userId,
                        userUploadedImage.getId(),
                        userGeneratedImageMdl.getConfigurationId(),
                        userGeneratedImageMdl.getWidth(),
                        userGeneratedImageMdl.getHeight(),
                        userGeneratedImageMdl.getFileSize(),
                        thumbnailImageDownloadUrl,
                        userGeneratedImageMdl.getFileType(),
                        userGeneratedImageMdl.getFileHash(),
                        userGeneratedImageMdl.getCreatedAt(),
                        userGeneratedImageMdl.getStatus()));
            }
            response.add(new UserUploadedImageResponse(
                    userUploadedImage.getId(),
                    userId,
                    userUploadedImage.getWidth(),
                    userUploadedImage.getHeight(),
                    userUploadedImage.getFileSize(),
                    uploadedImageDownloadUrl,
                    userUploadedImage.getFileType(),
                    userUploadedImage.getFileHash(),
                    userUploadedImage.getFileName(),
                    userUploadedImage.getCreatedAt(),
                    userUploadedImage.getUpdatedAt(),
                    thumbnails
            ));
        }
        return Mono.just(response);
    }
}
