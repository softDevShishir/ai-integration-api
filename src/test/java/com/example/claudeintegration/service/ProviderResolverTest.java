package com.example.claudeintegration.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderResolverTest {

    @Test
    void fallsBackToDefaultWhenRequestHasNoProvider() {
        ProviderResolver claudeDefault = new ProviderResolver("claude");
        ProviderResolver geminiDefault = new ProviderResolver("gemini");
        ProviderResolver openAiDefault = new ProviderResolver("openai");

        assertThat(claudeDefault.resolve(null)).isEqualTo("claude");
        assertThat(claudeDefault.resolve("  ")).isEqualTo("claude");
        assertThat(geminiDefault.resolve(null)).isEqualTo("gemini");
        assertThat(openAiDefault.resolve(null)).isEqualTo("openai");
    }

    @Test
    void requestedProviderOverridesDefault() {
        ProviderResolver resolver = new ProviderResolver("gemini");

        assertThat(resolver.resolve("claude")).isEqualTo("claude");
        assertThat(resolver.resolve("CLAUDE")).isEqualTo("claude");
        assertThat(resolver.resolve("Gemini")).isEqualTo("gemini");
        assertThat(resolver.resolve("openai")).isEqualTo("openai");
        assertThat(resolver.resolve("OpenAI")).isEqualTo("openai");
    }

    @Test
    void acceptsDefaultInAnyCase() {
        assertThat(new ProviderResolver(" Gemini ").resolve(null)).isEqualTo("gemini");
        assertThat(new ProviderResolver("OPENAI").resolve(null)).isEqualTo("openai");
    }

    @Test
    void rejectsUnknownDefault() {
        assertThatThrownBy(() -> new ProviderResolver("mistral"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DEFAULT_PROVIDER")
                .hasMessageContaining("\"claude\", \"gemini\" or \"openai\"");
    }
}
