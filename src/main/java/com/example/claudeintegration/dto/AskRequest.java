package com.example.claudeintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/ask.
 */
public record AskRequest(
        @NotBlank(message = "prompt must not be blank")
        @Size(max = 8000, message = "prompt must not exceed 8000 characters")
        String prompt
) {
}
