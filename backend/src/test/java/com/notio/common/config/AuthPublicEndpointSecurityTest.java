package com.notio.common.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.auth.controller.AuthController;
import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.dto.FindIdResponse;
import com.notio.auth.dto.OAuthCallbackResponse;
import com.notio.auth.dto.OAuthExchangeResponse;
import com.notio.auth.dto.OAuthStartResponse;
import com.notio.auth.dto.PasswordResetConfirmResponse;
import com.notio.auth.dto.PasswordResetRequestResponse;
import com.notio.auth.dto.SignupResponse;
import com.notio.auth.filter.JwtAuthenticationFilter;
import com.notio.auth.service.AuthService;
import com.notio.auth.service.LocalAuthService;
import com.notio.auth.service.OAuthAuthService;
import com.notio.auth.util.JwtTokenProvider;
import com.notio.common.exception.GlobalExceptionHandler;
import com.notio.common.ratelimit.RateLimitEvaluation;
import com.notio.common.ratelimit.RateLimitFilter;
import com.notio.common.ratelimit.RateLimitProperties;
import com.notio.common.ratelimit.RateLimitService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AuthPublicEndpointSecurityTest.TestConfig.class)
class AuthPublicEndpointSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LocalAuthService localAuthService;

    @Autowired
    private OAuthAuthService oAuthAuthService;

    @Autowired
    private RateLimitService rateLimitService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(rateLimitService.evaluate(any())).thenReturn(RateLimitEvaluation.allow());
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void publicAuthEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        when(localAuthService.signup(any()))
                .thenReturn(SignupResponse.builder()
                        .userId("1")
                        .email("user@example.com")
                        .displayName("Notio")
                        .build());
        when(localAuthService.findId(any()))
                .thenReturn(FindIdResponse.builder()
                        .message("ok")
                        .build());
        when(localAuthService.requestPasswordReset(any()))
                .thenReturn(PasswordResetRequestResponse.builder()
                        .message("requested")
                        .build());
        when(localAuthService.confirmPasswordReset(any()))
                .thenReturn(PasswordResetConfirmResponse.builder()
                        .message("confirmed")
                        .build());
        when(oAuthAuthService.start(any()))
                .thenReturn(OAuthStartResponse.builder()
                        .provider(AuthProvider.GOOGLE)
                        .platform(AuthPlatform.WEB)
                        .state("state-1")
                        .authorizationUrl("https://example.com/oauth")
                        .expiresAt(OffsetDateTime.now().plusMinutes(5))
                        .build());
        when(oAuthAuthService.exchange(any()))
                .thenReturn(OAuthExchangeResponse.builder()
                        .provider(AuthProvider.GOOGLE)
                        .state("state-1")
                        .message("exchange completed")
                        .build());
        when(oAuthAuthService.callback("google", "state-1", "code-1", null))
                .thenReturn(OAuthCallbackResponse.builder()
                        .provider(AuthProvider.GOOGLE)
                        .platform(AuthPlatform.WEB)
                        .state("state-1")
                        .redirectUri("https://app.notio.dev/callback")
                        .message("callback accepted")
                        .build());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123",
                                  "displayName": "Notio"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/find-id")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "valid-token",
                                  "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/oauth/start")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GOOGLE",
                                  "platform": "WEB",
                                  "redirectUri": "https://app.notio.dev/callback"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/oauth/exchange")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GOOGLE",
                                  "state": "state-1",
                                  "code": "code-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/auth/oauth/callback/google")
                        .param("state", "state-1")
                        .param("code", "code-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, AuthController.class, GlobalExceptionHandler.class})
    static class TestConfig {

        @Bean
        CorsProperties corsProperties() {
            return new CorsProperties(List.of("http://localhost:[*]"));
        }

        @Bean
        AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        LocalAuthService localAuthService() {
            return mock(LocalAuthService.class);
        }

        @Bean
        OAuthAuthService oAuthAuthService() {
            return mock(OAuthAuthService.class);
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(final JwtTokenProvider jwtTokenProvider) {
            return new JwtAuthenticationFilter(jwtTokenProvider);
        }

        @Bean
        RateLimitService rateLimitService() {
            return mock(RateLimitService.class);
        }

        @Bean
        RateLimitProperties rateLimitProperties() {
            return new RateLimitProperties();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        RateLimitFilter rateLimitFilter(
                final RateLimitService rateLimitService,
                final RateLimitProperties rateLimitProperties,
                final ObjectMapper objectMapper
        ) {
            return new RateLimitFilter(rateLimitService, rateLimitProperties, objectMapper);
        }
    }
}
