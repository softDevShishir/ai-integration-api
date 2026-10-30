package com.example.claudeintegration.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Decides which provider answers a request: the request's own
 * {@code provider} when given, otherwise the configured default
 * ({@code DEFAULT_PROVIDER}, bound via {@code app.default-provider}).
 */
@Component
public class ProviderResolver {

    public static final String CLAUDE = "claude";
    public static final String GEMINI = "gemini";
    public static final String OPENAI = "openai";

    private static final Set<String> SUPPORTED = Set.of(CLAUDE, GEMINI, OPENAI);

    private final String defaultProvider;

    public ProviderResolver(@Value("${app.default-provider:claude}") String defaultProvider) {
        String normalized = defaultProvider == null ? "" : defaultProvider.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED.contains(normalized)) {
            throw new IllegalArgumentException(
                    "DEFAULT_PROVIDER must be \"claude\", \"gemini\" or \"openai\", but was \"" + defaultProvider + "\"");
        }
        this.defaultProvider = normalized;
    }

    /**
     * Returns {@value #CLAUDE}, {@value #GEMINI} or {@value #OPENAI}. {@code requested} is assumed
     * to be already validated by {@code AskRequest} (or null/blank).
     */
    public String resolve(String requested) {
        if (requested == null || requested.isBlank()) {
            return defaultProvider;
        }
        return requested.trim().toLowerCase(Locale.ROOT);
    }
}
