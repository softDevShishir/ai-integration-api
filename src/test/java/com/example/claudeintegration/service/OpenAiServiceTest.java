package com.example.claudeintegration.service;

import com.example.claudeintegration.exception.OpenAiApiException;
import com.openai.client.OpenAIClient;
import com.openai.core.http.Headers;
import com.openai.errors.InternalServerException;
import com.openai.errors.OpenAIIoException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.completions.CompletionUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OpenAiService} with a mocked {@link OpenAIClient}, so
 * no API key or network access is needed.
 */
class OpenAiServiceTest {

    private static final String MODEL = "openai-test-model";

    private OpenAIClient client;
    private OpenAiService openAiService;

    @BeforeEach
    void setUp() {
        client = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
        openAiService = new OpenAiService(client, MODEL, 1024);
    }

    @Test
    void returnsTextModelAndTokenUsage() {
        when(client.chat().completions().create(any(ChatCompletionCreateParams.class)))
                .thenReturn(completion("Hello there!", ChatCompletion.Choice.FinishReason.STOP));

        AiResponse response = openAiService.ask("Say hello");

        assertThat(response).isEqualTo(new AiResponse("Hello there!", MODEL + "-2026", 12, 34));

        ChatCompletionCreateParams params = captureParams();
        assertThat(params.model().asString()).isEqualTo(MODEL);
        assertThat(params.maxCompletionTokens()).contains(1024L);
        assertThat(params.maxTokens()).isEmpty();
        assertThat(params.messages()).hasSize(1);
        assertThat(params.messages().get(0).isUser()).isTrue();
    }

    @Test
    void sendsSystemPromptAsSystemMessageWhenGiven() {
        when(client.chat().completions().create(any(ChatCompletionCreateParams.class)))
                .thenReturn(completion("Answer", ChatCompletion.Choice.FinishReason.STOP));

        openAiService.ask("Only use the product list.", "Do you have shoes?");

        ChatCompletionCreateParams params = captureParams();
        assertThat(params.messages()).hasSize(2);
        assertThat(params.messages().get(0).isSystem()).isTrue();
        assertThat(params.messages().get(1).isUser()).isTrue();
    }

    @Test
    void throwsClearErrorOnEmptyResponse() {
        when(client.chat().completions().create(any(ChatCompletionCreateParams.class)))
                .thenReturn(completion(null, ChatCompletion.Choice.FinishReason.LENGTH));

        assertThatThrownBy(() -> openAiService.ask("Say hello"))
                .isInstanceOf(OpenAiApiException.class)
                .hasMessageContaining("OpenAI returned an empty response")
                .hasMessageContaining("length")
                .hasMessageContaining("OPENAI_MAX_TOKENS");
    }

    @Test
    void throwsClearErrorWhenThereAreNoChoices() {
        when(client.chat().completions().create(any(ChatCompletionCreateParams.class)))
                .thenReturn(baseCompletion().choices(List.of()).build());

        assertThatThrownBy(() -> openAiService.ask("Say hello"))
                .isInstanceOf(OpenAiApiException.class)
                .hasMessageContaining("OpenAI returned an empty response");
    }

    @Test
    void wrapsServiceErrorStatus() {
        InternalServerException error = InternalServerException.builder()
                .statusCode(500)
                .headers(Headers.builder().build())
                .build();
        when(client.chat().completions().create(any(ChatCompletionCreateParams.class))).thenThrow(error);

        assertThatThrownBy(() -> openAiService.ask("Say hello"))
                .isInstanceOf(OpenAiApiException.class)
                .hasMessageStartingWith("OpenAI API request failed")
                .hasCause(error);
    }

    @Test
    void wrapsNetworkError() {
        OpenAIIoException error = new OpenAIIoException("connection reset");
        when(client.chat().completions().create(any(ChatCompletionCreateParams.class))).thenThrow(error);

        assertThatThrownBy(() -> openAiService.ask("Say hello"))
                .isInstanceOf(OpenAiApiException.class)
                .hasMessage("Unable to reach the OpenAI API")
                .hasCause(error);
    }

    private ChatCompletionCreateParams captureParams() {
        ArgumentCaptor<ChatCompletionCreateParams> captor = ArgumentCaptor.forClass(ChatCompletionCreateParams.class);
        verify(client.chat().completions()).create(captor.capture());
        return captor.getValue();
    }

    private static ChatCompletion completion(String content, ChatCompletion.Choice.FinishReason finishReason) {
        ChatCompletionMessage message = ChatCompletionMessage.builder()
                .content(content)
                .refusal((String) null)
                .build();
        ChatCompletion.Choice choice = ChatCompletion.Choice.builder()
                .index(0)
                .finishReason(finishReason)
                .logprobs((ChatCompletion.Choice.Logprobs) null)
                .message(message)
                .build();
        return baseCompletion().choices(List.of(choice)).build();
    }

    private static ChatCompletion.Builder baseCompletion() {
        return ChatCompletion.builder()
                .id("chatcmpl-test")
                .created(0)
                .model(MODEL + "-2026")
                .usage(CompletionUsage.builder()
                        .promptTokens(12)
                        .completionTokens(34)
                        .totalTokens(46)
                        .build());
    }
}
