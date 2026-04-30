package com.notio.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void logsWarnForNotioExceptionWithStandardFields() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications/1");
        MDC.put("correlation_id", "corr-notio");

        final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);

        try {
            handler.handleNotioException(new NotioException(ErrorCode.NOTIFICATION_NOT_FOUND), request);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("event=request_failed");
        assertThat(event.getFormattedMessage()).contains("correlation_id=corr-notio");
        assertThat(event.getFormattedMessage()).contains("route=/api/v1/notifications/1");
        assertThat(event.getFormattedMessage()).contains("error_code=NOTIFICATION_NOT_FOUND");
        assertThat(event.getFormattedMessage()).contains("http_status=404");
        assertThat(event.getFormattedMessage()).contains("exception_type=NotioException");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "request_failed");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "failure");
    }

    @Test
    void logsWarnForValidationException() throws Exception {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MDC.put("correlation_id", "corr-validation");

        final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "loginRequest");
        bindingResult.addError(new FieldError("loginRequest", "email", "must not be blank"));
        final Method method = SampleController.class.getDeclaredMethod("sample", String.class);
        final MethodArgumentNotValidException exception =
            new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);

        try {
            handler.handleMethodArgumentNotValidException(exception, request);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("event=request_failed");
        assertThat(event.getFormattedMessage()).contains("error_code=INVALID_INPUT_VALUE");
        assertThat(event.getFormattedMessage()).contains("http_status=400");
        assertThat(event.getFormattedMessage()).contains("exception_type=MethodArgumentNotValidException");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "failure");
    }

    @Test
    void logsErrorForUnexpectedExceptionWithStackTrace() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/chat");
        MDC.put("correlation_id", "corr-error");
        final RuntimeException exception = new RuntimeException("chat failed", new IllegalStateException("redis down"));

        final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.ERROR);
        logger.addAppender(appender);

        try {
            handler.handleException(exception, request);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("event=request_failed");
        assertThat(event.getFormattedMessage()).contains("correlation_id=corr-error");
        assertThat(event.getFormattedMessage()).contains("route=/api/v1/chat");
        assertThat(event.getFormattedMessage()).contains("error_code=INTERNAL_SERVER_ERROR");
        assertThat(event.getFormattedMessage()).contains("http_status=500");
        assertThat(event.getFormattedMessage()).contains("exception_type=RuntimeException");
        assertThat(event.getFormattedMessage()).contains("root_cause=redis down");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "error");
    }

    static class SampleController {
        void sample(final String body) {
        }
    }
}
