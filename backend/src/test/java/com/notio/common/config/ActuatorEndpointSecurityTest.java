package com.notio.common.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.auth.filter.JwtAuthenticationFilter;
import com.notio.auth.util.JwtTokenProvider;
import com.notio.common.ratelimit.RateLimitEvaluation;
import com.notio.common.ratelimit.RateLimitFilter;
import com.notio.common.ratelimit.RateLimitProperties;
import com.notio.common.ratelimit.RateLimitService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = ActuatorEndpointSecurityTest.TestConfig.class)
class ActuatorEndpointSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

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
    void actuatorHealthAndPrometheusEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string("metric 1"));
    }

    @Test
    void nonPublicActuatorAndProtectedApiEndpointsStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/webhook/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("accepted"));

        mockMvc.perform(get("/api/v1/notifications/test"))
                .andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, TestEndpoints.class})
    static class TestConfig {

        @Bean
        CorsProperties corsProperties() {
            return new CorsProperties(List.of("http://localhost:[*]"));
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
        RateLimitFilter rateLimitFilter(
                final RateLimitService rateLimitService,
                final RateLimitProperties rateLimitProperties
        ) {
            return new RateLimitFilter(rateLimitService, rateLimitProperties, new ObjectMapper());
        }
    }

    @RestController
    static class TestEndpoints {

        @GetMapping(path = "/actuator/health", produces = MediaType.APPLICATION_JSON_VALUE)
        String health() {
            return "{\"status\":\"UP\"}";
        }

        @GetMapping(path = "/actuator/prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
        String prometheus() {
            return "metric 1";
        }

        @GetMapping(path = "/actuator/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
        String metrics() {
            return "{\"name\":\"jvm.memory.used\"}";
        }

        @PostMapping(path = "/api/v1/webhook/test", produces = MediaType.TEXT_PLAIN_VALUE)
        String webhook() {
            return "accepted";
        }

        @GetMapping(path = "/api/v1/notifications/test", produces = MediaType.TEXT_PLAIN_VALUE)
        String notifications() {
            return "protected";
        }
    }
}
