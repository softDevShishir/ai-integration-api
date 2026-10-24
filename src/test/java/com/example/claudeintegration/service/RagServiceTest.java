package com.example.claudeintegration.service;

import com.example.claudeintegration.dto.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RagServiceTest {

    private static final String QUESTION = "Do you have running shoes?";

    private RetrievalService retrievalService;
    private GeminiService geminiService;
    private ClaudeService claudeService;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        retrievalService = mock(RetrievalService.class);
        geminiService = mock(GeminiService.class);
        claudeService = mock(ClaudeService.class);
        ragService = new RagService(retrievalService, geminiService, claudeService);

        when(geminiService.ask(anyString(), anyString()))
                .thenReturn(new AiResponse("Yes, we do.", "gemini-test", 120, 10));
        when(claudeService.ask(anyString(), anyString()))
                .thenReturn(new AiResponse("Yes, we do (Claude).", "claude-test", 120, 10));
    }

    @Test
    void sendsRetrievedProductsAsSystemPromptAndQuestionAsUserPrompt() {
        when(retrievalService.search(QUESTION)).thenReturn(List.of(
                new Product(1, "Bata Comfit Running Shoes", "Lightweight mesh running shoes.",
                        "shoes", new BigDecimal("3499.00"), true),
                new Product(4, "Kids School Shoes", "Durable black school shoes.",
                        "shoes", new BigDecimal("1290.00"), false)));

        AiResponse response = ragService.answer(QUESTION, false);

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(geminiService).ask(systemPrompt.capture(), eq(QUESTION));
        verifyNoInteractions(claudeService);

        assertThat(systemPrompt.getValue())
                .startsWith(RagService.INSTRUCTIONS)
                .contains("- Bata Comfit Running Shoes", "Category: shoes", "Price: 3499.00 BDT",
                        "Availability: In stock", "Description: Lightweight mesh running shoes.")
                .contains("- Kids School Shoes", "Price: 1290.00 BDT", "Availability: Out of stock")
                .doesNotContain(RagService.NO_PRODUCTS);
        assertThat(systemPrompt.getValue().indexOf("Bata Comfit"))
                .as("products keep retrieval rank order")
                .isLessThan(systemPrompt.getValue().indexOf("Kids School Shoes"));
        assertThat(response.text()).isEqualTo("Yes, we do.");
    }

    @Test
    void tellsClaudeNothingWasFoundWhenRetrievalIsEmpty() {
        when(retrievalService.search(QUESTION)).thenReturn(List.of());

        ragService.answer(QUESTION, false);

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(geminiService).ask(systemPrompt.capture(), eq(QUESTION));

        assertThat(systemPrompt.getValue())
                .startsWith(RagService.INSTRUCTIONS)
                .endsWith(RagService.NO_PRODUCTS);
    }

    @Test
    void sendsSameSystemPromptToClaudeWhenRequested() {
        when(retrievalService.search(QUESTION)).thenReturn(List.of());

        AiResponse response = ragService.answer(QUESTION, true);

        verify(claudeService).ask(ragService.buildSystemPrompt(List.of()), QUESTION);
        verifyNoInteractions(geminiService);
        assertThat(response.model()).isEqualTo("claude-test");
    }
}
