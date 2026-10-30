package com.example.claudeintegration.exception;

/**
 * Thrown when the call to the OpenAI API fails (network error, non-2xx
 * response, malformed response, etc).
 */
public class OpenAiApiException extends RuntimeException {

    public OpenAiApiException(String message) {
        super(message);
    }

    public OpenAiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
