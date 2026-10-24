package com.example.claudeintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/ask. {@code provider} is optional and defaults
 * to {@code "claude"}.
 */
public record AskRequest(
        @NotBlank(message = "prompt must not be blank")
        @Size(max = 8000, message = "prompt must not exceed 8000 characters")
        String prompt,

        @Pattern(regexp = "(?i)claude|gemini", message = "provider must be either \"claude\" or \"gemini\"")
        String provider
) {
}
