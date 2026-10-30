package com.example.claudeintegration.dto;

/**
 * Response body for POST /api/ask.
 *
 * @param response     the model's text answer
 * @param provider     which provider answered ({@code "claude"}, {@code "gemini"} or {@code "openai"})
 * @param model        the model that produced the answer
 * @param inputTokens  tokens in the prompt, as reported by the provider
 * @param outputTokens tokens generated (for Gemini and OpenAI reasoning models this includes
 *                     thinking/reasoning tokens)
 * @param durationMs   time spent waiting for the provider, in milliseconds
 */
public record AskResponse(
        String response,
        String provider,
        String model,
        long inputTokens,
        long outputTokens,
        long durationMs
) {
}
