package com.example.claudeintegration.controller;

import com.example.claudeintegration.dto.AskRequest;
import com.example.claudeintegration.dto.AskResponse;
import com.example.claudeintegration.service.AiResponse;
import com.example.claudeintegration.service.ClaudeService;
import com.example.claudeintegration.service.GeminiService;
import com.example.claudeintegration.service.ProviderResolver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the single {@code POST /api/ask} endpoint, which forwards the prompt
 * to Claude or Gemini depending on the request's {@code provider}, falling back
 * to {@code DEFAULT_PROVIDER} (Claude unless configured otherwise).
 * Requires a valid JWT Bearer token (enforced by Spring Security — see
 * {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api")
public class AskController {

    private final ClaudeService claudeService;
    private final GeminiService geminiService;
    private final ProviderResolver providerResolver;

    public AskController(ClaudeService claudeService, GeminiService geminiService,
                         ProviderResolver providerResolver) {
        this.claudeService = claudeService;
        this.geminiService = geminiService;
        this.providerResolver = providerResolver;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        String provider = providerResolver.resolve(request.provider());
        boolean gemini = ProviderResolver.GEMINI.equals(provider);

        long start = System.nanoTime();
        AiResponse answer = gemini
                ? geminiService.ask(request.prompt())
                : claudeService.ask(request.prompt());
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        return ResponseEntity.ok(new AskResponse(
                answer.text(),
                provider,
                answer.model(),
                answer.inputTokens(),
                answer.outputTokens(),
                durationMs));
    }
}
