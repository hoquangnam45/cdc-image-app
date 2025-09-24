package com.hoquangnam45.cdc.image.app.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hoquangnam45.cdc.image.app.common.constant.CommonResponseCode;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceResponse<T>(
        T data,
        boolean success,
        String code,
        String path,
        Instant timestamp,
        String message,
        String debugMessage) {
    public static <T> ServiceResponse<T> success(T data) {
        return new ServiceResponse<>(data, true, CommonResponseCode.OK, null, null, null, null);
    }

    public static ServiceResponse<Void> failEmpty(String path, String code, String displayMessage, String debugMessage) {
        return new ServiceResponse<>(null, false, code, path, Instant.now(), displayMessage, debugMessage);
    }

    public static <T> ServiceResponse<T> failWithResult(T result, String path, String code, String displayMessage, String debugMessage) {
        return new ServiceResponse<>(result, false, code, path, Instant.now(), displayMessage, debugMessage);
    }
}
