package com.notio.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 400 Bad Request
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    UNSUPPORTED_SOURCE(HttpStatus.BAD_REQUEST, "UNSUPPORTED_SOURCE", "지원하지 않는 알림 소스입니다."),
    CONNECTION_PROVIDER_UNSUPPORTED(HttpStatus.BAD_REQUEST, "CONNECTION_PROVIDER_UNSUPPORTED", "지원하지 않는 연결 제공자입니다."),
    CONNECTION_AUTH_TYPE_UNSUPPORTED(HttpStatus.BAD_REQUEST, "CONNECTION_AUTH_TYPE_UNSUPPORTED", "지원하지 않는 연결 인증 방식입니다."),

    // 413 Payload Too Large
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "요청 본문 크기가 제한을 초과했습니다."),

    // 401 Unauthorized
    WEBHOOK_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "WEBHOOK_VERIFICATION_FAILED", "Webhook 서명 검증에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),

    // 404 Not Found
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "할일을 찾을 수 없습니다."),
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "디바이스를 찾을 수 없습니다."),
    CONNECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CONNECTION_NOT_FOUND", "연결을 찾을 수 없습니다."),

    // 429 Too Many Requests
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "요청 횟수 제한을 초과했습니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    EMBEDDING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMBEDDING_FAILED", "임베딩 생성에 실패했습니다."),

    // 502 Bad Gateway
    EXTERNAL_PUSH_FAILED(HttpStatus.BAD_GATEWAY, "EXTERNAL_PUSH_FAILED", "외부 푸시 연동에 실패했습니다."),

    // 503 Service Unavailable
    LLM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "LLM_UNAVAILABLE", "LLM 서비스를 사용할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(final HttpStatus httpStatus, final String code, final String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
