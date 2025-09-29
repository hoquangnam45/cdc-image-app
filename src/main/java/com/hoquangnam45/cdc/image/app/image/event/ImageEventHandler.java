package com.hoquangnam45.cdc.image.app.image.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.image.enums.ImageStatus;
import com.hoquangnam45.cdc.image.app.image.enums.JobStatus;
import com.hoquangnam45.cdc.image.app.image.model.GeneratedImageMdl;
import com.hoquangnam45.cdc.image.app.image.model.ProcessJobConfigurationMdl;
import com.hoquangnam45.cdc.image.app.image.model.ProcessingImage;
import com.hoquangnam45.cdc.image.app.image.model.ProcessingJobMdl;
import com.hoquangnam45.cdc.image.app.image.model.UploadedImageMdl;
import com.hoquangnam45.cdc.image.app.image.model.UserImageMdl;
import com.hoquangnam45.cdc.image.app.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.Channels;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class ImageEventHandler {
    private final Storage storageClient;
    private final ObjectMapper objectMapper;
    private final ImageRepository imageRepository;

    // Example:
    // {
    //  "@type" : "type.googleapis.com/google.events.cloud.audit.v1.LogEntryData",
    //  "protoPayload" : {
    //    "status" : { },
    //    "authenticationInfo" : {
    //      "principalEmail" : "hoquangnam69@gmail.com"
    //    },
    //    "requestMetadata" : {
    //      "callerIp" : "113.185.83.126",
    //      "callerSuppliedUserAgent" : "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0,gzip(gfe)",
    //      "requestAttributes" : {
    //        "time" : "2025-09-25T08:11:08.629926672Z",
    //        "auth" : { }
    //      },
    //      "destinationAttributes" : { }
    //    },
    //    "serviceName" : "storage.googleapis.com",
    //    "methodName" : "storage.objects.create",
    //    "authorizationInfo" : [ {
    //      "resource" : "projects/_/buckets/cdc-image-bucket/objects/uploads/cdc_image (9).sql",
    //      "permission" : "storage.objects.create",
    //      "granted" : true,
    //      "resourceAttributes" : { }
    //    }, {
    //      "resource" : "projects/_/buckets/cdc-image-bucket/objects/uploads/cdc_image (9).sql",
    //      "permission" : "storage.objects.delete",
    //      "granted" : true,
    //      "resourceAttributes" : { }
    //    } ],
    //    "resourceName" : "projects/_/buckets/cdc-image-bucket/objects/uploads/cdc_image (9).sql",
    //    "serviceData" : {
    //      "policyDelta" : { },
    //      "@type" : "type.googleapis.com/google.iam.v1.logging.AuditData"
    //    },
    //    "resourceLocation" : {
    //      "currentLocations" : [ "asia-southeast1" ]
    //    }
    //  },
    //  "insertId" : "1wjhakmdczw8",
    //  "resource" : {
    //    "type" : "gcs_bucket",
    //    "labels" : {
    //      "project_id" : "cdc-image-service",
    //      "location" : "asia-southeast1",
    //      "bucket_name" : "cdc-image-bucket"
    //    }
    //  },
    //  "timestamp" : "2025-09-25T08:11:08.621833623Z",
    //  "severity" : "INFO",
    //  "logName" : "projects/cdc-image-service/logs/cloudaudit.googleapis.com%2Fdata_access",
    //  "receiveTimestamp" : "2025-09-25T08:11:08.901662409Z"
    //}
    @ServiceActivator(inputChannel = "uploadedImageChannel")
    public void handleImageUploadedEvent(String payloadString, @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage originalMessage) {
        try {
            Map<String, Object> payload;
            try {
                payload = objectMapper.readValue(payloadString, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                originalMessage.nack();
                log.error("ERROR: Payload for uploaded image event cannot be parsed: [Payload = {}]. Reason: {}", payloadString, e.getMessage(), e);
                return;
            }
            String resourceName;
            try {
                Object resourceNameObj = ((Map<String, Object>) payload.get("protoPayload")).get("resourceName");
                if (!(resourceNameObj instanceof String)) {
                    originalMessage.nack();
                    log.error("ERROR: Resource name need to be presence and need to be a string");
                    return;
                }
                resourceName = (String) resourceNameObj;
            } catch (Throwable e) {
                originalMessage.nack();
                log.error("ERROR: Unrecognized payload. Reason: {}", e.getMessage(), e);
                return;
            }
            BlobId sourceBlobId;
            try {
                sourceBlobId = parseGcsAuditResourceName(resourceName);
            } catch (Exception e) {
                originalMessage.nack();
                log.error("ERROR: Fail to parse resource name into bucket name and object name. Reason: {}", e.getMessage(), e);
                return;
            }
            String bucketName = sourceBlobId.getBucket();
            Blob uploadFileBlob = storageClient.get(sourceBlobId);
            if (uploadFileBlob == null || uploadFileBlob.getDeleteTimeOffsetDateTime() != null) {
                originalMessage.ack();
                return;
            }

            String md5Hash = uploadFileBlob.getMd5();
            UUID uploadedImageId = imageRepository.getUploadedImageId(md5Hash);

            // Retrieve stored user metadata
            String fileName = uploadFileBlob.getMetadata().get(CommonConstant.FILE_NAME_METADATA.toLowerCase());
            if (fileName == null) {
                originalMessage.ack();
                storageClient.delete(uploadFileBlob.getBlobId());
                return;
            }
            String objectName = uploadFileBlob.getName();
            String[] parts = objectName.split("/", 3);
            UUID userId = UUID.fromString(parts[1]);
            UUID userImageId = UUID.fromString(parts[2]);
            UserImageMdl userImageMdl = new UserImageMdl(userImageId, userId, uploadedImageId, fileName, uploadFileBlob.getCreateTimeOffsetDateTime().toInstant(), null, null);

            boolean hasFileBeenUploaded = checkIfFileHasBeenUploaded(uploadedImageId, bucketName);
            if (!hasFileBeenUploaded) {
                ProcessingImage processingImage = loadingUploadImage(uploadFileBlob);
                if (processingImage == null) {
                    originalMessage.ack();
                    storageClient.delete(uploadFileBlob.getBlobId());
                    return;
                }
                UUID newUploadedImageId;
                if (uploadedImageId == null) {
                    newUploadedImageId = UUID.randomUUID();
                } else {
                    newUploadedImageId = uploadedImageId;
                }
                String newDestObjectName = "uploaded/" + newUploadedImageId;
                BlobInfo destBlobInfo = BlobInfo.newBuilder(bucketName, newDestObjectName)
                        .setContentType(processingImage.fileType())
                        .setMetadata(Map.of(
                                CommonConstant.WIDTH_METADATA, processingImage.width().toString(),
                                CommonConstant.HEIGHT_METADATA, processingImage.height().toString(),
                                CommonConstant.EXTENSION_METADATA, processingImage.ext(),
                                CommonConstant.MIMETYPE_METADATA, processingImage.fileType(),
                                CommonConstant.FILE_ID_METADATA, newUploadedImageId.toString()
                        )).build();
                Storage.CopyRequest copyRequest = Storage.CopyRequest.newBuilder().setTarget(destBlobInfo).setSource(sourceBlobId).build();
                storageClient.copy(copyRequest);
                storageClient.delete(sourceBlobId);
                if (uploadedImageId == null) {
                    createNewUploadedImage(destBlobInfo.getBlobId());
                }
                uploadedImageId = newUploadedImageId;
                userImageMdl.setUploadedImageId(newUploadedImageId);
            }

            Boolean isUserImageExist = imageRepository.isUserImageExist(userId, uploadedImageId);
            if (isUserImageExist == null || !isUserImageExist) {
                try {
                    imageRepository.saveUserImage(userImageMdl);
                } catch (Exception e) {
                    log.error("Error saving user image for uploaded image event. Reason: {}", e.getMessage(), e);
                    originalMessage.nack();
                    return;
                }
            }
            List<ProcessJobConfigurationMdl> unprocessedJobConfigurations = imageRepository.getAllUnprocessingJobConfigurations(uploadedImageId);
            if (!unprocessedJobConfigurations.isEmpty()) {
                String destObjectName = "uploaded/" + uploadedImageId;
                BlobId destBlobId = BlobId.of(bucketName, destObjectName);
                Blob uploadedFileBlob = storageClient.get(destBlobId);
                ProcessingImage processingImage = loadingUploadImage(uploadedFileBlob);
                for (ProcessJobConfigurationMdl unprocessJobConfiguration : unprocessedJobConfigurations) {
                    UUID jobId = null;
                    try {
                        jobId = startProcessingJob(unprocessJobConfiguration.getId(), uploadedImageId);
                        ProcessingImage thumbnailProcessingImage = processConfiguration(unprocessJobConfiguration, processingImage);
                        String destThumbnailName = "thumbnails/" + uploadedImageId + "/" + unprocessJobConfiguration.getId();
                        BlobInfo destThumbBlobInfo = BlobInfo.newBuilder(bucketName, destThumbnailName)
                                .setContentType(thumbnailProcessingImage.fileType())
                                .setMetadata(Map.of(
                                        CommonConstant.ORIGINAL_FILE_ID_METADATA, uploadedImageId.toString(),
                                        CommonConstant.WIDTH_METADATA, thumbnailProcessingImage.width().toString(),
                                        CommonConstant.HEIGHT_METADATA, thumbnailProcessingImage.height().toString(),
                                        CommonConstant.EXTENSION_METADATA, thumbnailProcessingImage.ext(),
                                        CommonConstant.MIMETYPE_METADATA, thumbnailProcessingImage.fileType(),
                                        CommonConstant.CONFIGURATION_ID_METADATA, unprocessJobConfiguration.getId().toString()
                                )).build();
                        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                            ImageIO.write(thumbnailProcessingImage.bufferedImage(), thumbnailProcessingImage.ext(), byteArrayOutputStream);
                            storageClient.create(destThumbBlobInfo, byteArrayOutputStream.toByteArray());
                            createNewGeneratedImage(destThumbBlobInfo.getBlobId());
                        }
                        imageRepository.updateProcessingJob(jobId, null, JobStatus.COMPLETED, Instant.now());
                    } catch (Throwable e) {
                        log.error("Failed processing job[id = {}]. Reason: {}", jobId, e.getMessage(), e);
                        if (jobId != null) {
                            imageRepository.updateProcessingJob(jobId, MessageFormat.format("Failed processing job[id = {0}]. Reason: {1}", jobId, e.getMessage()), JobStatus.FAILED, Instant.now());
                        }
                    }
                }
            }
            originalMessage.ack();
        } catch (Throwable e) {
            log.error("ERROR: Unknown error. Reason: {}", e.getMessage(), e);
            originalMessage.nack();
        }
    }

    private UUID startProcessingJob(UUID configurationId, UUID uploadedImageId) {
        UUID jobId = UUID.randomUUID();
        ProcessingJobMdl processingJobMdl = new ProcessingJobMdl(jobId, uploadedImageId, configurationId, JobStatus.RUNNING, Instant.now(), null, null);
        imageRepository.saveProcessingJob(processingJobMdl);
        return jobId;
    }

    private void createNewUploadedImage(BlobId blobId) {
        Blob blob = storageClient.get(blobId);
        Map<String, String> metadata = blob.getMetadata();
        UUID fileId = UUID.fromString(metadata.get(CommonConstant.FILE_ID_METADATA));
        Integer width = Integer.valueOf(metadata.get(CommonConstant.WIDTH_METADATA));
        Integer height = Integer.valueOf(metadata.get(CommonConstant.HEIGHT_METADATA));
        String filePath = "gs://" + blob.getBucket() + "/" + blob.getName();
        String fileType = metadata.get(CommonConstant.MIMETYPE_METADATA);
        UploadedImageMdl uploadedImageMdl = new UploadedImageMdl(fileId, width, height, blob.getSize().intValue(), filePath, fileType, blob.getMd5(), blob.getCreateTimeOffsetDateTime().toInstant(), blob.getUpdateTimeOffsetDateTime().toInstant());
        imageRepository.saveUploadedImage(uploadedImageMdl);
    }

    private void createNewGeneratedImage(BlobId blobId) {
        Blob blob = storageClient.get(blobId);
        Map<String, String> metadata = blob.getMetadata();
        UUID originalFileId = UUID.fromString(metadata.get(CommonConstant.ORIGINAL_FILE_ID_METADATA));
        UUID configurationId = UUID.fromString(metadata.get(CommonConstant.CONFIGURATION_ID_METADATA));
        Integer width = Integer.valueOf(metadata.get(CommonConstant.WIDTH_METADATA));
        Integer height = Integer.valueOf(metadata.get(CommonConstant.HEIGHT_METADATA));
        String filePath = "gs://" + blob.getBucket() + "/" + blob.getName();
        String fileType = metadata.get(CommonConstant.MIMETYPE_METADATA);
        GeneratedImageMdl generatedImageMdl = new GeneratedImageMdl(UUID.randomUUID(), originalFileId, configurationId, width, height, blob.getSize().intValue(), filePath, fileType, blob.getMd5(), blob.getCreateTimeOffsetDateTime().toInstant());
        imageRepository.saveGeneratedImage(generatedImageMdl);
    }

    private ProcessingImage processConfiguration(ProcessJobConfigurationMdl processJobConfiguration, ProcessingImage processingImage) {
        Integer newWidth = processJobConfiguration.getWidth();
        Integer newHeight = processJobConfiguration.getHeight();
        if (newHeight != null && newWidth == null && processJobConfiguration.getKeepRatio()) {
            newWidth = BigDecimal.valueOf(newHeight).divide(BigDecimal.valueOf(processingImage.height()), 12, RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(processingImage.width())).intValue();
        } else if (newHeight == null && newWidth != null && processJobConfiguration.getKeepRatio()) {
            newHeight = BigDecimal.valueOf(newWidth).divide(BigDecimal.valueOf(processingImage.width()), 12, RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(processingImage.height())).intValue();
        } else if (newHeight == null && newWidth == null && processJobConfiguration.getScale() != null) {
            newHeight = BigDecimal.valueOf(processingImage.height()).multiply(processJobConfiguration.getScale()).intValue();
            newWidth = BigDecimal.valueOf(processingImage.width()).multiply(processJobConfiguration.getScale()).intValue();
        } else {
            throw new IllegalStateException("Can't determine width / height for this configuration " + processJobConfiguration.getId());
        }
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, processingImage.bufferedImage().getType());
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(processingImage.bufferedImage(), 0, 0, newWidth, newHeight, null);
        String mimeType = processJobConfiguration.getFileType() != null ? processJobConfiguration.getFileType() : processingImage.fileType();
        String outputFileType = processJobConfiguration.getOutputFileType() != null ? processJobConfiguration.getOutputFileType() : processingImage.ext();
        return new ProcessingImage(processingImage.fileName(), null, newWidth, newHeight, mimeType, outputFileType, resizedImage);
    }

    // Example: projects/_/buckets/<bucketName>/objects/<objectName>
    private static BlobId parseGcsAuditResourceName(String resourceName) {
        if (resourceName == null || resourceName.isEmpty()) {
            throw new IllegalStateException("Resource name cannot be null or empty");
        }
        String[] parts = resourceName.split("/", 6);
        if (parts.length != 6 || !"buckets".equals(parts[2])) {
            throw new IllegalStateException("Invalid resource name " + resourceName);
        }
        String bucketName = parts[3];
        String objectName = parts[5];
        return BlobId.of(bucketName, objectName);
    }

    private boolean checkIfFileHasBeenUploaded(UUID uploadedImageId, String bucketName) {
        if (uploadedImageId == null) {
            return false;
        }
        String processedObjectName = "uploaded/" + uploadedImageId;
        Blob blob = storageClient.get(bucketName, processedObjectName);
        return blob != null;
    }

    private boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    private ProcessingImage loadingUploadImage(Blob blob) throws IOException {
        String mimeType;
        int width;
        int height;
        BufferedImage bufferedImage;
        try (ReadChannel readChannel = storageClient.reader(blob.getBlobId());
             InputStream inputStream = Channels.newInputStream(readChannel);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
            Tika tika = new Tika();
            mimeType = tika.detect(bufferedInputStream);
            if (!isImage(mimeType)) {
                return null;
            }
            bufferedImage = ImageIO.read(bufferedInputStream);
            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();
        }
        String fileName = blob.getName();
        Integer fileSize = blob.getSize().intValue();
        String ext = mimeType.split("/")[1];
        return new ProcessingImage(fileName, fileSize, width, height, mimeType, ext, bufferedImage);
    }
}
