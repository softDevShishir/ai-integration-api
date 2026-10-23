package com.example.claudeintegration.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/auth/token.
 *
 * <p>This endpoint exists so the API is self-contained for local testing/demo
 * purposes. It issues a JWT signed with the hardcoded test secret configured
 * via {@code jwt.secret}. In a production deployment this should be replaced
 * by a real identity provider / authentication flow.
 */
public record TokenRequest(
        @NotBlank(message = "username must not be blank")
        String username
) {
}
