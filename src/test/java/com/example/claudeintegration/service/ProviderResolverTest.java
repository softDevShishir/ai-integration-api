package com.example.claudeintegration.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderResolverTest {

    @Test
    void fallsBackToDefaultWhenRequestHasNoProvider() {
        ProviderResolver claudeDefault = new ProviderResolver("claude");
        ProviderResolver geminiDefault = new ProviderResolver("gemini");

        assertThat(claudeDefault.resolve(null)).isEqualTo("claude");
        assertThat(claudeDefault.resolve("  ")).isEqualTo("claude");
        assertThat(geminiDefault.resolve(null)).isEqualTo("gemini");
    }

    @Test
    void requestedProviderOverridesDefault() {
        ProviderResolver resolver = new ProviderResolver("gemini");

        assertThat(resolver.resolve("claude")).isEqualTo("claude");
        assertThat(resolver.resolve("CLAUDE")).isEqualTo("claude");
        assertThat(resolver.resolve("Gemini")).isEqualTo("gemini");
    }

    @Test
    void acceptsDefaultInAnyCase() {
        assertThat(new ProviderResolver(" Gemini ").resolve(null)).isEqualTo("gemini");
    }

    @Test
    void rejectsUnknownDefault() {
        assertThatThrownBy(() -> new ProviderResolver("openai"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DEFAULT_PROVIDER");
    }
}
