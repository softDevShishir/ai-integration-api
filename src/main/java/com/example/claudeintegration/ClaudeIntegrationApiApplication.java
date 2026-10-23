package com.example.claudeintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * This API authenticates purely via JWT Bearer tokens (see
 * {@code SecurityConfig} / {@code JwtAuthenticationFilter}) and has no use
 * for Spring Security's default in-memory user store, so that
 * auto-configuration is excluded to avoid the misleading
 * "Using generated security password" startup log.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ClaudeIntegrationApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaudeIntegrationApiApplication.class, args);
    }
}
