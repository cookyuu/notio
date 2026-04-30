package com.notio.auth.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.notio.auth.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsDebugOnJwtAuthenticationSuccess() throws Exception {
        final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        request.addHeader("Authorization", "Bearer valid-token");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("correlation_id", "corr-jwt-success");

        when(jwtTokenProvider.validateTokenWithReason("valid-token"))
            .thenReturn(JwtTokenProvider.JwtValidationResult.success());
        when(jwtTokenProvider.getUserId("valid-token")).thenReturn("user-1");
        when(jwtTokenProvider.getEmail("valid-token")).thenReturn("masked@example.com");

        final Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);

        try {
            filter.doFilter(request, response, noopChain());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(event.getFormattedMessage()).contains("event=jwt_authentication_succeeded");
        assertThat(event.getFormattedMessage()).contains("user_id=user-1");
        assertThat(event.getFormattedMessage()).contains("route=/api/v1/notifications");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "jwt_authentication_succeeded");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "success");
        assertThat(event.getMDCPropertyMap()).containsEntry("correlation_id", "corr-jwt-success");
    }

    @Test
    void logsWarnOnJwtAuthenticationFailure() throws Exception {
        final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        request.addHeader("Authorization", "Bearer expired-token");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("correlation_id", "corr-jwt-failure");

        when(jwtTokenProvider.validateTokenWithReason("expired-token"))
            .thenReturn(JwtTokenProvider.JwtValidationResult.invalid("expired_token"));

        final Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);

        try {
            filter.doFilter(request, response, noopChain());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("event=jwt_authentication_failed");
        assertThat(event.getFormattedMessage()).contains("reason=expired_token");
        assertThat(event.getFormattedMessage()).contains("route=/api/v1/notifications");
        assertThat(event.getFormattedMessage()).contains("correlation_id=corr-jwt-failure");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "jwt_authentication_failed");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "failure");
    }

    private FilterChain noopChain() {
        return (request, response) -> {
        };
    }
}
