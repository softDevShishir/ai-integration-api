package com.example.claudeintegration.controller;

import com.example.claudeintegration.dto.AskRequest;
import com.example.claudeintegration.dto.AskResponse;
import com.example.claudeintegration.service.AiResponse;
import com.example.claudeintegration.service.ProviderResolver;
import com.example.claudeintegration.service.RagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes {@code POST /api/ask/rag}, which answers customer questions from the
 * product database via {@link RagService}. Answered by the request's
 * {@code provider}, falling back to {@code DEFAULT_PROVIDER} (Claude unless
 * configured otherwise). Requires a valid JWT Bearer
 * token (enforced by Spring Security — see {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api")
public class RagController {

    private final RagService ragService;
    private final ProviderResolver providerResolver;

    public RagController(RagService ragService, ProviderResolver providerResolver) {
        this.ragService = ragService;
        this.providerResolver = providerResolver;
    }

    @PostMapping("/ask/rag")
    public ResponseEntity<AskResponse> askRag(@Valid @RequestBody AskRequest request) {
        String provider = providerResolver.resolve(request.provider());
        boolean claude = ProviderResolver.CLAUDE.equals(provider);

        long start = System.nanoTime();
        AiResponse answer = ragService.answer(request.prompt(), claude);
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
