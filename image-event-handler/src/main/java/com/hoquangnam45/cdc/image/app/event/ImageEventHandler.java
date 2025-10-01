package com.hoquangnam45.cdc.image.app.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.ReadChannel;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.hoquangnam45.cdc.image.app.common.constant.CommonConstant;
import com.hoquangnam45.cdc.image.app.common.enums.ImageStatus;
import com.hoquangnam45.cdc.image.app.common.enums.JobStatus;
import com.hoquangnam45.cdc.image.app.common.model.GeneratedImageMdl;
import com.hoquangnam45.cdc.image.app.common.model.ProcessJobConfigurationMdl;
import com.hoquangnam45.cdc.image.app.common.model.ProcessingImage;
import com.hoquangnam45.cdc.image.app.common.model.ProcessingJobMdl;
import com.hoquangnam45.cdc.image.app.common.model.UploadedImageMdl;
import com.hoquangnam45.cdc.image.app.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final EventRepository eventRepository;

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
        UUID processingId = UUID.randomUUID();
        try {
            Map<String, Object> payload;
            try {
                payload = objectMapper.readValue(payloadString, new TypeReference<Map<String, Object>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("[processingId = {}] ERROR: Payload for uploaded image event cannot be parsed: [Payload = {}]. Reason: {}", payloadString, processingId, e.getMessage(), e);
                originalMessage.ack();
                return;
            }
            String resourceName;
            try {
                Object resourceNameObj = ((Map<String, Object>) payload.get("protoPayload")).get("resourceName");
                if (!(resourceNameObj instanceof String)) {
                    log.error("[processingId = {}] ERROR: Resource name need to be presence and need to be a string", processingId);
                    originalMessage.ack();
                    return;
                }
                resourceName = (String) resourceNameObj;
            } catch (Throwable e) {
                log.error("[processingId = {}] ERROR: Unrecognized payload. Reason: {}", processingId, e.getMessage(), e);
                originalMessage.ack();
                return;
            }
            log.info("[processingId = {}] Start processing for object {}", processingId, resourceName);
            BlobId sourceBlobId;
            try {
                sourceBlobId = parseGcsAuditResourceName(resourceName);
            } catch (Exception e) {
                log.error("[processingId = {}] ERROR: Fail to parse resource name into bucket name and object name. Reason: {}", processingId, e.getMessage(), e);
                originalMessage.ack();
                return;
            }
            String bucketName = sourceBlobId.getBucket();
            String objectName = sourceBlobId.getName();
            String[] parts = objectName.split("/", 3);
            UUID userId = UUID.fromString(parts[1]);
            UUID userImageId = UUID.fromString(parts[2]);
//            int affectedRow = eventRepository.updateUserImageStatusRunning(userImageId);
//            if (affectedRow == 0) {
//                originalMessage.nack();
//            }
            Blob uploadFileBlob = storageClient.get(sourceBlobId);
            if (uploadFileBlob == null || uploadFileBlob.getDeleteTimeOffsetDateTime() != null) {
                log.info("[processingId = {}] {} has been deleted", processingId, resourceName);
                originalMessage.ack();
                eventRepository.updateUserImageStatus(userImageId, ImageStatus.EXPIRED, null);
                return;
            }

            String md5Hash = uploadFileBlob.getMd5();
            ImageStatus uploadedImageStatus = eventRepository.getUploadedImageStatus(md5Hash);
            if (uploadedImageStatus == ImageStatus.INVALID) {
                log.info("[processingId = {}] {} has been deleted because invalid image", processingId, resourceName);
                storageClient.delete(sourceBlobId);
                eventRepository.updateUserImageStatus(userImageId, ImageStatus.INVALID, null);
                originalMessage.ack();
                return;
            }
            UUID uploadedImageId = eventRepository.getUploadedImageId(md5Hash);

            // Retrieve stored user metadata
            String fileName = uploadFileBlob.getMetadata().get(CommonConstant.FILE_NAME_METADATA.toLowerCase());
            if (fileName == null) {
                originalMessage.ack();
                storageClient.delete(uploadFileBlob.getBlobId());
                return;
            }
            Boolean isExpired = eventRepository.isUserImageExpired(userImageId, Instant.now());
            if (isExpired) {
                log.info("[processingId = {}] {} has been deleted because reaching expire time", processingId, resourceName);
                storageClient.delete(sourceBlobId);
                eventRepository.updateUserImageStatus(userImageId, ImageStatus.EXPIRED, null);
                originalMessage.ack();
                return;
            }

            boolean hasFileBeenUploaded = checkIfFileHasBeenUploaded(uploadedImageId, bucketName);
            if (!hasFileBeenUploaded) {
                ProcessingImage processingImage = loadingUploadImage(uploadFileBlob);
                if (!processingImage.isImage()) {
                    originalMessage.ack();
                    storageClient.delete(uploadFileBlob.getBlobId());
                    eventRepository.updateUserImageStatus(userImageId, ImageStatus.INVALID, null);
                    if (uploadedImageId == null) {
                        eventRepository.saveUploadedImage(new UploadedImageMdl(
                                UUID.randomUUID(),
                                null,
                                null,
                                processingImage.fileSize(),
                                null,
                                processingImage.fileType(),
                                processingImage.fileHash(),
                                ImageStatus.INVALID,
                                Instant.now(),
                                null
                        ));
                    }
                    log.info("[processingId = {}] Object {} is not image", processingId, objectName);
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
                try {
                    storageClient.copy(copyRequest);
                    storageClient.delete(sourceBlobId);
                    if (uploadedImageId == null) {
                        createNewUploadedImage(destBlobInfo.getBlobId());
                    }
                } catch (Exception e) {
                    log.warn("[processingId = {}] {} has been processed by other job", processingId, resourceName);
                }
                uploadedImageId = newUploadedImageId;
            }

            try {
                eventRepository.updateUserImageStatus(userImageId, ImageStatus.UPLOADED, uploadedImageId);
                log.info("[processingId = {}] Finished uploading user image {} -> {}", processingId, objectName, uploadedImageId);
            } catch (Exception e) {
                log.error("[processingId = {}] Error saving user image for uploaded image event. Reason: {}", processingId, e.getMessage(), e);
                originalMessage.nack();
                return;
            }

            List<ProcessJobConfigurationMdl> unprocessedJobConfigurations = eventRepository.getAllUnprocessingJobConfigurations(uploadedImageId);
            if (!unprocessedJobConfigurations.isEmpty()) {
                String destObjectName = "uploaded/" + uploadedImageId;
                BlobId destBlobId = BlobId.of(bucketName, destObjectName);
                Blob uploadedFileBlob = storageClient.get(destBlobId);
                ProcessingImage processingImage = loadingUploadImage(uploadedFileBlob);
                for (ProcessJobConfigurationMdl unprocessJobConfiguration : unprocessedJobConfigurations) {
                    UUID jobId = null;
                    try {
                        JobStatus jobStatus = eventRepository.getJobStatus(unprocessJobConfiguration.getId(), uploadedImageId);
                        if (jobStatus == JobStatus.RUNNING || jobStatus == JobStatus.COMPLETED) {
                            continue;
                        }
                        jobId = startProcessingJob(unprocessJobConfiguration.getId(), uploadedImageId);
                        log.info("[processingId = {}] Start processing thumbnail image generation [jobId = {}, configurationId = {}, imageId = {}]", processingId, jobId, unprocessJobConfiguration.getId(), uploadedImageId);
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
                        log.info("[processingId = {}] Finished processing thumbnail image generation [jobId = {}, configurationId = {}, imageId = {}]", processingId, jobId, unprocessJobConfiguration.getId(), uploadedImageId);
                        eventRepository.updateProcessingJob(jobId, null, JobStatus.COMPLETED, Instant.now());
                    } catch (Throwable e) {
                        log.info("[processingId = {}] Failed processing thumbnail image generation [jobId = {}, configurationId = {}, imageId = {}]", processingId, jobId, unprocessJobConfiguration.getId(), uploadedImageId);
                        if (jobId != null) {
                            eventRepository.updateProcessingJob(jobId, MessageFormat.format("[processingId = {0}] Failed processing job[id = {1}]. Reason: {2}", processingId, jobId, e.getMessage()), JobStatus.FAILED, Instant.now());
                        }
                    }
                }
            }
            log.info("[processingId = {}] Finished all thumbnail generation for image {}", processingId, uploadedImageId);
            originalMessage.ack();
        } catch (Throwable e) {
            log.error("[processingId = {}] ERROR: Unknown error. Reason: {}", processingId, e.getMessage(), e);
            originalMessage.nack();
        }
    }

    private UUID startProcessingJob(UUID configurationId, UUID uploadedImageId) {
        eventRepository.removeLastProcessingJob(configurationId, uploadedImageId);
        UUID jobId = UUID.randomUUID();
        ProcessingJobMdl processingJobMdl = new ProcessingJobMdl(jobId, uploadedImageId, configurationId, JobStatus.RUNNING, Instant.now(), null, null);
        eventRepository.saveProcessingJob(processingJobMdl);
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
        UploadedImageMdl uploadedImageMdl = new UploadedImageMdl(fileId, width, height, blob.getSize().intValue(), filePath, fileType, blob.getMd5(), ImageStatus.UPLOADED, blob.getCreateTimeOffsetDateTime().toInstant(), blob.getUpdateTimeOffsetDateTime().toInstant());
        eventRepository.saveUploadedImage(uploadedImageMdl);
    }

    private void createNewGeneratedImage(BlobId blobId) {
        Blob blob = storageClient.get(blobId);
        String md5Hash = blob.getMd5();
        Boolean hasImageThumbnailBeenGenerated = eventRepository.hasImageThumbnailGenerated(md5Hash);
        if (hasImageThumbnailBeenGenerated != null && hasImageThumbnailBeenGenerated) {
            return;
        }
        Map<String, String> metadata = blob.getMetadata();
        UUID originalFileId = UUID.fromString(metadata.get(CommonConstant.ORIGINAL_FILE_ID_METADATA));
        UUID configurationId = UUID.fromString(metadata.get(CommonConstant.CONFIGURATION_ID_METADATA));
        Integer width = Integer.valueOf(metadata.get(CommonConstant.WIDTH_METADATA));
        Integer height = Integer.valueOf(metadata.get(CommonConstant.HEIGHT_METADATA));
        String filePath = "gs://" + blob.getBucket() + "/" + blob.getName();
        String fileType = metadata.get(CommonConstant.MIMETYPE_METADATA);
        GeneratedImageMdl generatedImageMdl = new GeneratedImageMdl(UUID.randomUUID(), originalFileId, configurationId, width, height, blob.getSize().intValue(), filePath, fileType, blob.getMd5(), blob.getCreateTimeOffsetDateTime().toInstant());
        eventRepository.saveGeneratedImage(generatedImageMdl);
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
        return new ProcessingImage(true, processingImage.fileName(), null, newWidth, newHeight, mimeType, outputFileType, null, resizedImage);
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
        String fileName = blob.getName();
        Integer fileSize = blob.getSize().intValue();
        String fileHash = blob.getMd5();
        String ext;
        try (ReadChannel readChannel = storageClient.reader(blob.getBlobId());
             InputStream inputStream = Channels.newInputStream(readChannel);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
            Tika tika = new Tika();
            mimeType = tika.detect(bufferedInputStream);
            ext = mimeType.split("/")[1];
            if (!isImage(mimeType)) {
                return new ProcessingImage(false, fileName, fileSize, null, null, mimeType, ext, fileHash, null);
            }
            bufferedImage = ImageIO.read(bufferedInputStream);
            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();
        }
        return new ProcessingImage(true, fileName, fileSize, width, height, mimeType, ext, fileHash, bufferedImage);
    }
}
