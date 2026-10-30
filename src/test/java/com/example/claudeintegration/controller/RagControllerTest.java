package com.example.claudeintegration.controller;

import com.example.claudeintegration.config.SecurityConfig;
import com.example.claudeintegration.security.JwtService;
import com.example.claudeintegration.security.RestAuthenticationEntryPoint;
import com.example.claudeintegration.service.AiResponse;
import com.example.claudeintegration.service.ProviderResolver;
import com.example.claudeintegration.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, JwtService.class, ProviderResolver.class})
// Pin the default so a DEFAULT_PROVIDER in a developer's local .env can't change the outcome.
@TestPropertySource(properties = "app.default-provider=claude")
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private RagService ragService;

    @Test
    void returns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/ask/rag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"Do you have running shoes?\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verifyNoInteractions(ragService);
    }

    @Test
    void returns400ForBlankPrompt() throws Exception {
        mockMvc.perform(post("/api/ask/rag")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("prompt: prompt must not be blank"));

        verifyNoInteractions(ragService);
    }

    @Test
    void returns200UsingDefaultProviderWhenNoneGiven() throws Exception {
        when(ragService.answer(anyString(), eq(true)))
                .thenReturn(new AiResponse("Yes, the Bata Comfit Running Shoes cost 3499 BDT.",
                        "claude-test", 250, 20));

        mockMvc.perform(post("/api/ask/rag")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"Do you have running shoes?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Yes, the Bata Comfit Running Shoes cost 3499 BDT."))
                .andExpect(jsonPath("$.provider").value("claude"))
                .andExpect(jsonPath("$.model").value("claude-test"))
                .andExpect(jsonPath("$.inputTokens").value(250))
                .andExpect(jsonPath("$.outputTokens").value(20))
                .andExpect(jsonPath("$.durationMs").isNumber());
    }

    @Test
    void usesGeminiWhenProviderIsGemini() throws Exception {
        when(ragService.answer(anyString(), eq(false)))
                .thenReturn(new AiResponse("Yes, we have running shoes.", "gemini-test", 250, 12));

        mockMvc.perform(post("/api/ask/rag")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"Do you have running shoes?\", \"provider\": \"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("gemini"))
                .andExpect(jsonPath("$.model").value("gemini-test"));
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateToken("test-user");
    }
}
