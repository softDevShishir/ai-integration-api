package com.example.claudeintegration.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    @Test
    void generatesTokenThatRoundTripsToTheSameUsername() {
        String token = jwtService.generateToken("alice");

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        JwtService otherService = new JwtService(
                "a-completely-different-secret-key-that-is-also-32-bytes", 3_600_000L);
        String token = otherService.generateToken("bob");

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLivedService = new JwtService(SECRET, -1_000L);
        String expiredToken = shortLivedService.generateToken("carol");

        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-real-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
