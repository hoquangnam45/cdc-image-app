package com.hoquangnam45.cdc.image.app.image.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.image.model.UploadImageResponse;
import com.hoquangnam45.cdc.image.app.image.model.UploadedImageResponse;
import com.hoquangnam45.cdc.image.app.image.model.UserImageMdl;
import com.hoquangnam45.cdc.image.app.image.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
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

    public Mono<List<UploadImageResponse>> createUploadSession(UUID userId, List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }
        return Flux.fromIterable(fileNames)
                .map(fileName -> generatePresignedUrl(userId, fileName))
                .doOnNext(uploadImageResponse -> {
                    UserImageMdl userImageMdl = new UserImageMdl(uploadImageResponse.id(), userId, null, uploadImageResponse.fileName(), uploadImageResponse.createdAt(), null, null);
                    imageRepository.saveUserImage(userImageMdl);
                })
                .collectList();
    }

    private UploadImageResponse generatePresignedUrl(UUID userId, String fileName) {
        UUID fileId = UUID.randomUUID();
        String objectName = "uploads/" + fileId;
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setMetadata(Map.of(
                        CommonConstant.FILE_NAME_METADATA, fileName,
                        CommonConstant.USER_ID_METADATA, userId.toString(),
                        CommonConstant.FILE_ID_METADATA, fileId.toString())
                )
                .build();

        URL signedUrl = storage.signUrl(blobInfo, presignedUrlDuration.toMinutes(), TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withExtHeaders(Map.of(
                        "x-goog-if-generation-match", "0"
                )),
                Storage.SignUrlOption.withV4Signature());

        Instant now = Instant.now();
        Instant expiredAt = now.plus(presignedUrlDuration);
        return new UploadImageResponse(fileId, fileName, now, expiredAt, signedUrl.toString());
    }

    public Mono<List<UploadedImageResponse>> listUploadedUserImages(UUID userId) {
        return Mono.empty();
//        return imageRepository.listUploadedUserImage(userId);
    }
}
