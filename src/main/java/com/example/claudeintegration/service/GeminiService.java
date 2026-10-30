package com.example.claudeintegration.service;

import com.example.claudeintegration.exception.GeminiApiException;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around the Google Gen AI Java SDK that turns a user prompt
 * into a plain-text Gemini response.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final Client client;
    private final String model;
    private final int maxTokens;

    public GeminiService(Client client,
                         @Value("${gemini.model}") String model,
                         @Value("${gemini.max-tokens}") int maxTokens) {
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /**
     * Sends the given prompt to Gemini and returns the text of the response
     * together with the model and token usage. Thinking tokens are counted as
     * output tokens, since Gemini bills them that way.
     *
     * @throws GeminiApiException if the call to Gemini fails for any reason
     */
    public AiResponse ask(String prompt) {
        return ask(null, prompt);
    }

    /**
     * Same as {@link #ask(String)}, but also sends {@code systemPrompt} as the
     * Gemini system instruction (ignored when null or blank).
     *
     * @throws GeminiApiException if the call to Gemini fails for any reason
     */
    public AiResponse ask(String systemPrompt, String prompt) {
        GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
                .maxOutputTokens(maxTokens);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.systemInstruction(Content.fromParts(Part.fromText(systemPrompt)));
        }

        GenerateContentConfig config = builder.build();

        try {
            GenerateContentResponse response = client.models.generateContent(model, prompt, config);

            String text = response.text();

            if (text == null || text.isBlank()) {
                throw new GeminiApiException("Gemini returned an empty response");
            }

            GenerateContentResponseUsageMetadata usage = response.usageMetadata().orElse(null);
            long inputTokens = usage == null ? 0 : usage.promptTokenCount().orElse(0);
            long outputTokens = usage == null ? 0
                    : usage.candidatesTokenCount().orElse(0) + usage.thoughtsTokenCount().orElse(0);

            return new AiResponse(text, response.modelVersion().orElse(model), inputTokens, outputTokens);
        } catch (ApiException e) {
            log.error("Gemini API returned an error status", e);
            throw new GeminiApiException("Gemini API request failed: " + e.getMessage(), e);
        } catch (GenAiIOException e) {
            log.error("Failed to call Gemini API", e);
            throw new GeminiApiException("Unable to reach the Gemini API", e);
        }
    }
}
