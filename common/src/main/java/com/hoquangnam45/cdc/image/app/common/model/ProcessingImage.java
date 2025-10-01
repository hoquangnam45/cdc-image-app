package com.hoquangnam45.cdc.image.app.common.model;

import java.awt.image.BufferedImage;

public record ProcessingImage(boolean isImage, String fileName, Integer fileSize, Integer width, Integer height, String fileType,
                              String ext, String fileHash, BufferedImage bufferedImage) {
}
