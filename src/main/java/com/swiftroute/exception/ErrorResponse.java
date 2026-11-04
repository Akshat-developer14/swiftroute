package com.swiftroute.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> fieldErrors // Used for validation error
) {
    // Helper constructor for errors without field validation details
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, Instant.now(), null);
    }

    // Helper constructor for validation errors
    public ErrorResponse(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        this(status, error, message, path, Instant.now(), fieldErrors);
    }
}
