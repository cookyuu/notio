package com.notio.common.exception;

import com.notio.common.response.ApiError;
import com.notio.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotioException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotioException(final NotioException exception) {
        final ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(new ApiError(
                        errorCode.getCode(),
                        exception.getMessage()
                )));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException exception
    ) {
        final Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildErrorResponse(ErrorCode.INVALID_REQUEST, details);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(final Exception exception) {
        return buildErrorResponse(ErrorCode.INVALID_REQUEST, Map.of("reason", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(final Exception exception) {
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, Map.of("reason", exception.getMessage()));
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
}
