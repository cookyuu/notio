package com.notio.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void doFilterReturns429WithApiErrorAndHeaders() throws Exception {
        final RateLimitService rateLimitService = mock(RateLimitService.class);
        final RateLimitProperties properties = new RateLimitProperties();
        final RateLimitFilter filter = new RateLimitFilter(rateLimitService, properties, new ObjectMapper());
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setContent("{\"email\":\"a\",\"password\":\"b\"}".getBytes());
        final MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitService.evaluate(org.mockito.ArgumentMatchers.any(CachedBodyHttpServletRequest.class)))
            .thenReturn(RateLimitEvaluation.deny(new RateLimitDecision(5, 0, Instant.ofEpochSecond(1_900_000_000L))));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void doFilterReturns413WhenWebhookPayloadIsTooLarge() throws Exception {
        final RateLimitService rateLimitService = mock(RateLimitService.class);
        final RateLimitProperties properties = new RateLimitProperties();
        properties.setWebhookBodyMaxBytes(32);
        final RateLimitFilter filter = new RateLimitFilter(rateLimitService, properties, new ObjectMapper());
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/webhook/claude");
        request.setContentType("application/json");
        request.setContent(new byte[64]);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("PAYLOAD_TOO_LARGE");
    }

    @Test
    void doFilterReturns413WhenChatInputIsTooLong() throws Exception {
        final RateLimitService rateLimitService = mock(RateLimitService.class);
        when(rateLimitService.evaluate(org.mockito.ArgumentMatchers.any(CachedBodyHttpServletRequest.class)))
            .thenReturn(RateLimitEvaluation.allow());
        final RateLimitProperties properties = new RateLimitProperties();
        properties.setChatMaxChars(5);
        final RateLimitFilter filter = new RateLimitFilter(rateLimitService, properties, new ObjectMapper());
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat");
        request.setContentType("application/json");
        request.setContent("{\"content\":\"123456\"}".getBytes());
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("PAYLOAD_TOO_LARGE");
    }
}
