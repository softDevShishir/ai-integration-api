package com.example.claudeintegration.controller;

import com.example.claudeintegration.config.SecurityConfig;
import com.example.claudeintegration.exception.OpenAiApiException;
import com.example.claudeintegration.security.JwtService;
import com.example.claudeintegration.security.RestAuthenticationEntryPoint;
import com.example.claudeintegration.service.AiResponse;
import com.example.claudeintegration.service.ClaudeService;
import com.example.claudeintegration.service.GeminiService;
import com.example.claudeintegration.service.OpenAiService;
import com.example.claudeintegration.service.ProviderResolver;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AskController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, JwtService.class, ProviderResolver.class})
// Pin the default so a DEFAULT_PROVIDER in a developer's local .env can't change the outcome.
@TestPropertySource(properties = "app.default-provider=claude")
class AskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ClaudeService claudeService;

    @MockBean
    private GeminiService geminiService;

    @MockBean
    private OpenAiService openAiService;

    @Test
    void usesOpenAiWhenProviderIsOpenAi() throws Exception {
        when(openAiService.ask(anyString()))
                .thenReturn(new AiResponse("A REST API exposes resources over HTTP.", "openai-test", 18, 40));

        mockMvc.perform(post("/api/ask")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"What is a REST API?\", \"provider\": \"OPENAI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("A REST API exposes resources over HTTP."))
                .andExpect(jsonPath("$.provider").value("openai"))
                .andExpect(jsonPath("$.model").value("openai-test"))
                .andExpect(jsonPath("$.inputTokens").value(18))
                .andExpect(jsonPath("$.outputTokens").value(40))
                .andExpect(jsonPath("$.durationMs").isNumber());

        verifyNoInteractions(claudeService, geminiService);
    }

    @Test
    void usesDefaultProviderWhenNoneGiven() throws Exception {
        when(claudeService.ask(anyString()))
                .thenReturn(new AiResponse("Hello!", "claude-test", 10, 3));

        mockMvc.perform(post("/api/ask")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"Say hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("claude"));

        verifyNoInteractions(geminiService, openAiService);
    }

    @Test
    void returns400ListingValidProvidersForUnknownProvider() throws Exception {
        mockMvc.perform(post("/api/ask")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"Hello\", \"provider\": \"unknown\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("provider: provider must be one of \"claude\", \"gemini\" or \"openai\""));

        verifyNoInteractions(claudeService, geminiService, openAiService);
    }

    @Test
    void returns502WhenOpenAiFails() throws Exception {
        when(openAiService.ask(anyString()))
                .thenThrow(new OpenAiApiException("Unable to reach the OpenAI API"));

        mockMvc.perform(post("/api/ask")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"Hello\", \"provider\": \"openai\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Unable to reach the OpenAI API"))
                .andExpect(jsonPath("$.status").value(502));
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateToken("test-user");
    }
}
