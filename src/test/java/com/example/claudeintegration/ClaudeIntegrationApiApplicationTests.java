package com.example.claudeintegration;

import com.example.claudeintegration.service.RetrievalService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Smoke test: verifies the full application context (security config,
 * controllers, Anthropic/Gemini client beans, DataSource) wires up correctly
 * without requiring a real ANTHROPIC_API_KEY or a running PostgreSQL.
 *
 * <p>SQL init is disabled so startup never opens a database connection (the
 * connection pool is lazy), and RetrievalService is mocked so nothing queries it.
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
class ClaudeIntegrationApiApplicationTests {

    @MockBean
    private RetrievalService retrievalService;

    @Test
    void contextLoads() {
    }
}
