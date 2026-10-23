package com.example.claudeintegration.controller;

import com.example.claudeintegration.dto.TokenRequest;
import com.example.claudeintegration.dto.TokenResponse;
import com.example.claudeintegration.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues test JWTs signed with the app's hardcoded secret, so the API can be
 * exercised end-to-end without a separate identity provider.
 *
 * <p><strong>For local testing/demo purposes only.</strong> Replace with a
 * real authentication flow (OAuth2/OIDC, a user store, etc.) before
 * deploying this service anywhere it matters.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@Valid @RequestBody TokenRequest request) {
        String token = jwtService.generateToken(request.username());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
