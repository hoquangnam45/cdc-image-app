package com.hoquangnam45.cdc.image.app.common.config;

import com.hoquangnam45.cdc.image.app.common.constant.CommonResponseCode;
import com.hoquangnam45.cdc.image.app.common.exception.ServiceException;
import com.hoquangnam45.cdc.image.app.common.model.ServiceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ServiceResponse<Void>>> defaultException(ServerWebExchange exchange, Exception exception) {
        return Mono.just(ResponseEntity.internalServerError().body(ServiceResponse.failEmpty(exchange.getRequest().getPath().value(), CommonResponseCode.INTERNAL_SERVER_ERROR, "Internal server error", exception.getMessage())));
    }

    @ExceptionHandler(ServiceException.class)
    public Mono<ResponseEntity<ServiceResponse<?>>> serviceException(ServerWebExchange exchange, ServiceException exception) {
        return Mono.just(ResponseEntity.status(exception.getStatusCode()).body(ServiceResponse.failWithResult(exception.getResult(), exchange.getRequest().getPath().value(), exception.getErrorCode(), exception.getDisplayMessage(), exception.getMessage())));
    }
}
