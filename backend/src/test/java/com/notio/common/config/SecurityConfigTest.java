package com.notio.common.config;

import com.notio.auth.filter.JwtAuthenticationFilter;
import com.notio.common.ratelimit.RateLimitFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void corsConfigurationSourceShouldAllowLocalDevelopmentOrigins() {
        final CorsProperties corsProperties =
                new CorsProperties(List.of("http://localhost:[*]", "http://127.0.0.1:[*]"));
        final SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(RateLimitFilter.class),
                corsProperties
        );

        final CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        final CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOriginPatterns())
                .containsExactly("http://localhost:[*]", "http://127.0.0.1:[*]");
        assertThat(configuration.getAllowedMethods())
                .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).containsExactly("*");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}
