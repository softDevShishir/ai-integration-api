package com.example.claudeintegration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: verifies the full application context (security config,
 * controllers, Anthropic client bean) wires up correctly without requiring
 * a real ANTHROPIC_API_KEY.
 */
@SpringBootTest
class ClaudeIntegrationApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
