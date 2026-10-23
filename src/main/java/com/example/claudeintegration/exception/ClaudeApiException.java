package com.example.claudeintegration.exception;

/**
 * Thrown when the call to the Claude API fails (network error, non-2xx
 * response, malformed response, etc).
 */
public class ClaudeApiException extends RuntimeException {

    public ClaudeApiException(String message) {
        super(message);
    }

    public ClaudeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
