package com.booking.common.error;

import java.util.Map;

/** 400 with per-field messages, for validation done in the service layer. */
public class FieldValidationException extends BadRequestException {

    private final Map<String, String> errors;

    public FieldValidationException(Map<String, String> errors) {
        super("VALIDATION_FAILED", "Request validation failed");
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
