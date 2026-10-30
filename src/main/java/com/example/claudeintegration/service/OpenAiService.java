package com.example.claudeintegration.service;

import com.example.claudeintegration.exception.OpenAiApiException;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.completions.CompletionUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around the OpenAI Java SDK (Chat Completions API) that turns a
 * user prompt into a plain-text OpenAI response.
 */
@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

    private final OpenAIClient client;
    private final String model;
    private final long maxTokens;

    public OpenAiService(OpenAIClient client,
                         @Value("${openai.model}") String model,
                         @Value("${openai.max-tokens}") long maxTokens) {
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /**
     * Sends the given prompt to OpenAI and returns the text of the response
     * together with the model and token usage. For reasoning models the
     * output tokens include hidden reasoning tokens, since OpenAI bills them
     * that way.
     *
     * @throws OpenAiApiException if the call to OpenAI fails for any reason
     */
    public AiResponse ask(String prompt) {
        return ask(null, prompt);
    }

    /**
     * Same as {@link #ask(String)}, but also sends {@code systemPrompt} as the
     * OpenAI system message (ignored when null or blank).
     *
     * @throws OpenAiApiException if the call to OpenAI fails for any reason
     */
    public AiResponse ask(String systemPrompt, String prompt) {
        // max_completion_tokens (not the deprecated max_tokens, which reasoning models reject)
        // caps visible output plus any hidden reasoning tokens.
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(model)
                .maxCompletionTokens(maxTokens);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.addSystemMessage(systemPrompt);
        }

        ChatCompletionCreateParams params = builder.addUserMessage(prompt).build();

        try {
            ChatCompletion response = client.chat().completions().create(params);

            ChatCompletion.Choice choice = response.choices().isEmpty() ? null : response.choices().get(0);
            String text = choice == null ? null : choice.message().content().orElse(null);

            if (text == null || text.isBlank()) {
                String finishReason = choice == null ? "none" : choice.finishReason().asString();
                throw new OpenAiApiException("OpenAI returned an empty response (finish reason: " + finishReason
                        + "). If it is \"length\", increase OPENAI_MAX_TOKENS");
            }

            CompletionUsage usage = response.usage().orElse(null);
            long inputTokens = usage == null ? 0 : usage.promptTokens();
            long outputTokens = usage == null ? 0 : usage.completionTokens();

            return new AiResponse(text, response.model(), inputTokens, outputTokens);
        } catch (OpenAIServiceException e) {
            log.error("OpenAI API returned an error status", e);
            throw new OpenAiApiException("OpenAI API request failed: " + e.getMessage(), e);
        } catch (OpenAIException e) {
            log.error("Failed to call OpenAI API", e);
            throw new OpenAiApiException("Unable to reach the OpenAI API", e);
        }
    }
}
