package com.hoquangnam45.cdc.image.app.image.util;

import com.google.cloud.storage.BlobId;

public class GcsUtil {
    public static BlobId parseToBlobId(String gcsPath) {
        if (gcsPath == null || !gcsPath.startsWith("gs://")) {
            return null;
        }
        gcsPath = gcsPath.substring("gs://".length());
        String[] parts = gcsPath.split("/", 2);
        if (parts.length != 2) {
            return null;
        }
        return BlobId.of(parts[0], parts[1]);
    }
}
