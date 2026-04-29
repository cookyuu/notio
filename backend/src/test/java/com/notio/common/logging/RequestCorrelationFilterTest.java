package com.notio.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestCorrelationFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void reusesIncomingCorrelationIdAndLogsStartAndCompletionWithSameMdc() throws ServletException, IOException {
        final RequestCorrelationFilter filter = new RequestCorrelationFilter(new CorrelationIdGenerator());
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER, "corr-123");
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("127.0.0.1");

        final Logger logger = (Logger) LoggerFactory.getLogger(RequestCorrelationFilter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            filter.doFilter(request, response, authenticatedChain());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            SecurityContextHolder.clearContext();
        }

        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER)).isEqualTo("corr-123");
        assertThat(appender.list).hasSize(2);

        final ILoggingEvent startedEvent = appender.list.get(0);
        final ILoggingEvent completedEvent = appender.list.get(1);

        assertThat(startedEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(startedEvent.getFormattedMessage()).contains("event=request_started");
        assertThat(startedEvent.getMDCPropertyMap()).containsEntry(RequestCorrelationFilter.CORRELATION_ID_KEY, "corr-123");
        assertThat(startedEvent.getMDCPropertyMap()).containsEntry(RequestCorrelationFilter.ROUTE_KEY, "/api/v1/notifications");
        assertThat(startedEvent.getMDCPropertyMap()).containsEntry(RequestCorrelationFilter.HTTP_METHOD_KEY, "GET");

        assertThat(completedEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(completedEvent.getFormattedMessage()).contains("event=request_completed");
        assertThat(completedEvent.getFormattedMessage()).contains("status=200");
        assertThat(completedEvent.getFormattedMessage()).contains("authenticated=true");
        assertThat(completedEvent.getMDCPropertyMap()).containsEntry(RequestCorrelationFilter.CORRELATION_ID_KEY, "corr-123");
        assertThat(completedEvent.getMDCPropertyMap()).containsEntry(RequestCorrelationFilter.ROUTE_KEY, "/api/v1/notifications");
        assertThat(completedEvent.getMDCPropertyMap()).containsEntry(RequestCorrelationFilter.HTTP_METHOD_KEY, "GET");
        assertThat(completedEvent.getMDCPropertyMap()).isEqualTo(startedEvent.getMDCPropertyMap());
    }

    @Test
    void generatesCorrelationIdAndClearsMdcAfterRequestCompletion() throws ServletException, IOException {
        final RequestCorrelationFilter filter = new RequestCorrelationFilter(fixedGenerator("generated-corr-id"));
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("127.0.0.1");

        final FilterChain chain = (req, res) -> {
            final Map<String, String> mdc = MDC.getCopyOfContextMap();
            assertThat(mdc).containsEntry(RequestCorrelationFilter.CORRELATION_ID_KEY, "generated-corr-id");
            assertThat(mdc).containsEntry(RequestCorrelationFilter.ROUTE_KEY, "/api/v1/chat");
            assertThat(mdc).containsEntry(RequestCorrelationFilter.HTTP_METHOD_KEY, "POST");
            ((MockHttpServletResponse) res).setStatus(201);
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER)).isEqualTo("generated-corr-id");
        assertThat(MDC.getCopyOfContextMap()).isIn(null, Map.of());
    }

    @Test
    void skipsNonApiV1Requests() throws ServletException, IOException {
        final RequestCorrelationFilter filter = new RequestCorrelationFilter(fixedGenerator("unused"));
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(204));

        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER)).isNull();
        assertThat(MDC.getCopyOfContextMap()).isIn(null, Map.of());
    }

    private FilterChain authenticatedChain() {
        return (request, response) -> SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user-1", null, null)
        );
    }

    private CorrelationIdGenerator fixedGenerator(final String correlationId) {
        return new CorrelationIdGenerator() {
            @Override
            public String generate() {
                return correlationId;
            }
        };
    }
}
