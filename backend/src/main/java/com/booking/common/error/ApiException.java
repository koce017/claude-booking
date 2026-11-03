package com.booking.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected business errors. The handler maps each one to a
 * problem-detail response with its status, a stable machine-readable code and a
 * client-safe message.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
