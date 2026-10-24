package com.example.claudeintegration.exception;

/**
 * Thrown when the call to the Gemini API fails (network error, non-2xx
 * response, malformed response, etc).
 */
public class GeminiApiException extends RuntimeException {

    public GeminiApiException(String message) {
        super(message);
    }

    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
