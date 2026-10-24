package com.example.claudeintegration.service;

/**
 * Provider-neutral result of a single model call: the response text plus the
 * model that produced it and the token usage reported by the provider.
 */
public record AiResponse(String text, String model, long inputTokens, long outputTokens) {
}
