package com.hoquangnam45.cdc.image.app.common.exception;

import lombok.Getter;

@Getter
public class ServiceException extends Exception {
    private final int statusCode;
    private final String errorCode;
    private final String displayMessage;
    private final Object result;

    public ServiceException(int statusCode, Object result, String errorCode, String message, String debugMessage, Exception cause) {
        super(debugMessage == null ? message : debugMessage, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.displayMessage = message;
        this.result = result;
    }

    public ServiceException(int statusCode, String errorCode, String message, String debugMessage, Exception cause) {
        this(statusCode, null, errorCode, message, debugMessage, cause);
    }

    public ServiceException(int statusCode, String errorCode, String message, Exception cause) {
        this(statusCode, null, errorCode, message, null, cause);
    }

    public ServiceException(int statusCode, String errorCode, String message) {
        this(statusCode, null, errorCode, message, null, null);
    }

    public ServiceException(int statusCode, String errorCode, Exception cause) {
        this(statusCode, null, errorCode, cause.getMessage(), cause.getMessage(), cause);
    }

    public ServiceException(int statusCode, Object result, String errorCode, String message, Exception cause) {
        this(statusCode, result, errorCode, message, cause == null ? message : cause.getMessage(), cause);
    }

    public ServiceException(int statusCode, Object result, String errorCode, Exception cause) {
        this(statusCode, result, errorCode, cause.getMessage(), cause.getMessage(), cause);
    }
}
