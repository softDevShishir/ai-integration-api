package com.example.claudeintegration.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlock;
import com.example.claudeintegration.exception.ClaudeApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Thin wrapper around the Anthropic Java SDK that turns a user prompt into a
 * plain-text Claude response.
 */
@Service
public class ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeService.class);

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;

    public ClaudeService(AnthropicClient client,
                          @Value("${anthropic.model}") String model,
                          @Value("${anthropic.max-tokens}") long maxTokens) {
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /**
     * Sends the given prompt to Claude and returns the concatenated text of
     * the response together with the model and token usage.
     *
     * @throws ClaudeApiException if the call to Claude fails for any reason
     */
    public AiResponse ask(String prompt) {
        return ask(null, prompt);
    }

    /**
     * Same as {@link #ask(String)}, but also sends {@code systemPrompt} as the
     * Claude system prompt (ignored when null or blank).
     *
     * @throws ClaudeApiException if the call to Claude fails for any reason
     */
    public AiResponse ask(String systemPrompt, String prompt) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(Model.of(model))
                .maxTokens(maxTokens)
                .addUserMessage(prompt);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.system(systemPrompt);
        }

        MessageCreateParams params = builder.build();

        try {
            Message response = client.messages().create(params);

            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining());

            if (text.isBlank()) {
                throw new ClaudeApiException("Claude returned an empty response");
            }

            return new AiResponse(text, response.model().asString(),
                    response.usage().inputTokens(), response.usage().outputTokens());
        } catch (AnthropicServiceException e) {
            log.error("Claude API returned an error status", e);
            throw new ClaudeApiException("Claude API request failed: " + e.getMessage(), e);
        } catch (AnthropicException e) {
            log.error("Failed to call Claude API", e);
            throw new ClaudeApiException("Unable to reach the Claude API", e);
        }
    }
}
