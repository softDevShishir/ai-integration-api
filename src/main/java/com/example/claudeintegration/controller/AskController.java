package com.example.claudeintegration.controller;

import com.example.claudeintegration.dto.AskRequest;
import com.example.claudeintegration.dto.AskResponse;
import com.example.claudeintegration.service.ClaudeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the single {@code POST /api/ask} endpoint. Requires a valid JWT
 * Bearer token (enforced by Spring Security — see {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api")
public class AskController {

    private final ClaudeService claudeService;

    public AskController(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        String answer = claudeService.ask(request.prompt());
        return ResponseEntity.ok(new AskResponse(answer));
    }
}
