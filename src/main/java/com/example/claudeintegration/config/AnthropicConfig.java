package com.example.claudeintegration.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the {@link AnthropicClient} bean used to call the Claude API.
 *
 * <p>The API key is read from the {@code ANTHROPIC_API_KEY} environment
 * variable (bound via {@code anthropic.api-key} in application.properties).
 * It is intentionally never logged or exposed through any endpoint.
 */
@Configuration
public class AnthropicConfig {

    private static final Logger log = LoggerFactory.getLogger(AnthropicConfig.class);

    @Bean
    public AnthropicClient anthropicClient(@Value("${anthropic.api-key:}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY is not set. Requests to /api/ask will fail until it is configured.");
        }

        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey == null || apiKey.isBlank() ? "missing-anthropic-api-key" : apiKey)
                .build();
    }
}
