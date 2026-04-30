package com.notio.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilterReturns429WithApiErrorAndHeaders() throws Exception {
        final RateLimitService rateLimitService = mock(RateLimitService.class);
        final RateLimitProperties properties = new RateLimitProperties();
        final RateLimitFilter filter = new RateLimitFilter(rateLimitService, properties, new ObjectMapper());
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setContent("{\"email\":\"a\",\"password\":\"b\"}".getBytes());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("correlation_id", "corr-rate-limit");

        when(rateLimitService.evaluate(org.mockito.ArgumentMatchers.any(CachedBodyHttpServletRequest.class)))
            .thenReturn(RateLimitEvaluation.deny(new RateLimitDecision(5, 0, Instant.ofEpochSecond(1_900_000_000L))));

        final Logger logger = (Logger) LoggerFactory.getLogger(RateLimitFilter.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);

        try {
            filter.doFilter(request, response, new MockFilterChain());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("event=rate_limit_blocked");
        assertThat(event.getFormattedMessage()).contains("limit=5");
        assertThat(event.getFormattedMessage()).contains("remaining=0");
        assertThat(event.getFormattedMessage()).contains("route=/api/v1/auth/login");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "rate_limit_blocked");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "failure");
        assertThat(event.getMDCPropertyMap()).containsEntry("correlation_id", "corr-rate-limit");
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

    @Test
    void doFilterReturns503AndLogsErrorWhenRateLimitStoreFailsClosed() throws Exception {
        final RateLimitService rateLimitService = mock(RateLimitService.class);
        final RateLimitProperties properties = new RateLimitProperties();
        final RateLimitFilter filter = new RateLimitFilter(rateLimitService, properties, new ObjectMapper());
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/webhook/github");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("correlation_id", "corr-store-failure");

        when(rateLimitService.evaluate(org.mockito.ArgumentMatchers.any(CachedBodyHttpServletRequest.class)))
            .thenThrow(new RateLimitStoreException("redis down", new IllegalStateException("redis down")));

        final Logger logger = (Logger) LoggerFactory.getLogger(RateLimitFilter.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.ERROR);
        logger.addAppender(appender);

        try {
            filter.doFilter(request, response, new MockFilterChain());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("INTERNAL_SERVER_ERROR");
        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("event=rate_limit_store_failed");
        assertThat(event.getFormattedMessage()).contains("route=/api/v1/webhook/github");
        assertThat(event.getFormattedMessage()).contains("correlation_id=corr-store-failure");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "rate_limit_store_failed");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "error");
    }
}
