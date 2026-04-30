package com.notio.common.exception;

import com.notio.common.response.ApiError;
import com.notio.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotioException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotioException(
            final NotioException exception,
            final HttpServletRequest request
    ) {
        final ErrorCode errorCode = exception.getErrorCode();
        logRequestFailure("request_failed", request, errorCode, exception, false);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(new ApiError(
                        errorCode.getCode(),
                        exception.getMessage()
                )));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException exception,
            final HttpServletRequest request
    ) {
        final ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        logRequestFailure("request_failed", request, errorCode, exception, false);
        final Map<String, Object> details = new LinkedHashMap<>();
        for (final FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildErrorResponse(errorCode, details);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            final Exception exception,
            final HttpServletRequest request
    ) {
        final ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        logRequestFailure("request_failed", request, errorCode, exception, false);
        return buildErrorResponse(errorCode, Map.of("reason", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            final Exception exception,
            final HttpServletRequest request
    ) {
        final ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        logRequestFailure("request_failed", request, errorCode, exception, true);
        return buildErrorResponse(errorCode, Map.of("reason", exception.getMessage()));
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            final ErrorCode errorCode,
            final Map<String, Object> details
    ) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(new ApiError(
                        errorCode.getCode(),
                        errorCode.getMessage()
                )));
    }

    private String rootCauseMessage(final Throwable throwable) {
        Throwable current = throwable;
        Throwable root = throwable;
        while (current != null) {
            root = current;
            current = current.getCause();
        }
        if (root == null) {
            return "n/a";
        }
        if (root.getMessage() == null || root.getMessage().isBlank()) {
            return root.getClass().getSimpleName();
        }
        return root.getMessage();
    }

    private void logRequestFailure(
            final String event,
            final HttpServletRequest request,
            final ErrorCode errorCode,
            final Exception exception,
            final boolean unexpected
    ) {
        final HttpStatus httpStatus = errorCode.getHttpStatus();
        MDC.put("event", event);
        MDC.put("outcome", unexpected ? "error" : "failure");
        try {
            if (unexpected) {
                log.error(
                        "event={} correlation_id={} route={} error_code={} http_status={} exception_type={} root_cause={}",
                        event,
                        MDC.get("correlation_id"),
                        request.getRequestURI(),
                        errorCode.getCode(),
                        httpStatus.value(),
                        exception.getClass().getSimpleName(),
                        rootCauseMessage(exception),
                        exception
                );
                return;
            }

            log.warn(
                    "event={} correlation_id={} route={} error_code={} http_status={} exception_type={} root_cause={}",
                    event,
                    MDC.get("correlation_id"),
                    request.getRequestURI(),
                    errorCode.getCode(),
                    httpStatus.value(),
                    exception.getClass().getSimpleName(),
                    rootCauseMessage(exception)
            );
        } finally {
            MDC.remove("outcome");
            MDC.remove("event");
        }
    }
}
