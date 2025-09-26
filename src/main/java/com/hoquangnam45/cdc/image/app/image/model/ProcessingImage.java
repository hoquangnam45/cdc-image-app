package com.hoquangnam45.cdc.image.app.image.model;

import java.awt.image.BufferedImage;

public record ProcessingImage(String fileName, Integer fileSize, Integer width, Integer height, String fileType,
                              String ext, BufferedImage bufferedImage) {
}
