package com.notio.notification.domain;

import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;

public enum NotificationSource {
    CLAUDE,
    CODEX,
    SLACK,
    GITHUB,
    GMAIL,
    INTERNAL;

    public static NotificationSource from(final String value) {
        try {
            return NotificationSource.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new NotioException(
                    ErrorCode.INVALID_REQUEST,
                    "지원하지 않는 source 입니다."
            );
        }
    }
}
