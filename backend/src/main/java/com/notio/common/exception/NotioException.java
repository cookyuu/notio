package com.notio.common.exception;

import java.util.Map;

public class NotioException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public NotioException(final ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), Map.of());
    }

    public NotioException(final ErrorCode errorCode, final String message) {
        this(errorCode, message, Map.of());
    }

    public NotioException(final ErrorCode errorCode, final String message, final Map<String, Object> details) {
        this(errorCode, message, details, null);
    }

    public NotioException(
            final ErrorCode errorCode,
            final String message,
            final Map<String, Object> details,
            final Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
