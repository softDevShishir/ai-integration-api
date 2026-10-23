package com.example.claudeintegration.dto;

import java.time.Instant;

/**
 * Uniform error body returned by the API for 4xx/5xx responses.
 */
public record ErrorResponse(
        String error,
        String message,
        int status,
        Instant timestamp
) {
    public ErrorResponse(String error, String message, int status) {
        this(error, message, status, Instant.now());
    }
}
