package com.notio.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record AiUsageResponse(
        AiUsageGranularity granularity,
        LocalDate startDate,
        LocalDate endDate,
        long totalInputTokens,
        long totalOutputTokens,
        long totalSessions,
        String mostUsedModel,
        List<AiUsagePeriodPoint> trend,
        List<AiUsageModelShare> modelDistribution
) {

    /**
     * 기간별 토큰 사용량 포인트.
     *
     * @param label        기간 레이블 (DAILY: "YYYY-MM-DD", WEEKLY: "IYYY-WIW", MONTHLY: "YYYY-MM")
     * @param inputTokens  해당 기간 입력 토큰 합계
     * @param outputTokens 해당 기간 출력 토큰 합계
     * @param sessions     해당 기간 세션(호출) 수
     */
    public record AiUsagePeriodPoint(
            String label,
            long inputTokens,
            long outputTokens,
            long sessions
    ) {}

    /**
     * 모델별 토큰 사용량 분포.
     *
     * @param model       모델명 (예: "claude-opus-4-5", "claude-sonnet-4-5")
     * @param totalTokens 총 토큰 수 (input + output)
     * @param sessions    세션(호출) 수
     */
    public record AiUsageModelShare(
            String model,
            long totalTokens,
            long sessions
    ) {}
}
