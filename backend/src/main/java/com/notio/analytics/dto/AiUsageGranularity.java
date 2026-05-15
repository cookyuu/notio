package com.notio.analytics.dto;

import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;

public enum AiUsageGranularity {

    DAILY,
    WEEKLY,
    MONTHLY;

    /**
     * 문자열 → AiUsageGranularity 변환.
     * 대소문자 무시. 파싱 실패 시 INVALID_REQUEST 예외.
     */
    public static AiUsageGranularity from(final String value) {
        if (value == null) {
            throw new NotioException(
                    ErrorCode.INVALID_REQUEST,
                    "granularity 값이 누락되었습니다."
            );
        }
        try {
            return AiUsageGranularity.valueOf(value.trim().toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new NotioException(
                    ErrorCode.INVALID_REQUEST,
                    "지원하지 않는 granularity 값입니다: " + value
            );
        }
    }

    /**
     * 허용 최대 날짜 범위 (startDate ~ endDate 포함).
     * 초과 시 INVALID_REQUEST 반환.
     */
    public int maxDays() {
        return switch (this) {
            case DAILY -> 90;
            case WEEKLY -> 365;
            case MONTHLY -> 730;
        };
    }

    /**
     * startDate/endDate 미지정 시 자동 적용하는 기본 조회 일수.
     */
    public int defaultDays() {
        return switch (this) {
            case DAILY -> 7;
            case WEEKLY -> 56;
            case MONTHLY -> 365;
        };
    }
}
