package com.example.claudeintegration.controller;

import com.example.claudeintegration.dto.AskRequest;
import com.example.claudeintegration.dto.AskResponse;
import com.example.claudeintegration.service.AiResponse;
import com.example.claudeintegration.service.ClaudeService;
import com.example.claudeintegration.service.GeminiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the single {@code POST /api/ask} endpoint, which forwards the prompt
 * to Claude (default) or Gemini depending on the request's {@code provider}.
 * Requires a valid JWT Bearer token (enforced by Spring Security — see
 * {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api")
public class AskController {

    private final ClaudeService claudeService;
    private final GeminiService geminiService;

    public AskController(ClaudeService claudeService, GeminiService geminiService) {
        this.claudeService = claudeService;
        this.geminiService = geminiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        boolean gemini = "gemini".equalsIgnoreCase(request.provider());

        long start = System.nanoTime();
        AiResponse answer = gemini
                ? geminiService.ask(request.prompt())
                : claudeService.ask(request.prompt());
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        return ResponseEntity.ok(new AskResponse(
                answer.text(),
                gemini ? "gemini" : "claude",
                answer.model(),
                answer.inputTokens(),
                answer.outputTokens(),
                durationMs));
    }
}
