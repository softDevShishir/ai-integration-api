package com.example.claudeintegration.config;

import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the Google Gen AI {@link Client} bean used to call the Gemini API.
 *
 * <p>The API key is read from the {@code GEMINI_API_KEY} environment
 * variable (bound via {@code gemini.api-key} in application.properties).
 * It is intentionally never logged or exposed through any endpoint.
 */
@Configuration
public class GeminiConfig {

    private static final Logger log = LoggerFactory.getLogger(GeminiConfig.class);

    @Bean(destroyMethod = "close")
    public Client geminiClient(@Value("${gemini.api-key:}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not set. Requests to /api/ask with provider \"gemini\" will fail until it is configured.");
        }

        return Client.builder()
                .apiKey(apiKey == null || apiKey.isBlank() ? "missing-gemini-api-key" : apiKey)
                .build();
    }
}
