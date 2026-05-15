package com.notio.analytics.controller;

import com.notio.analytics.dto.AiUsageGranularity;
import com.notio.analytics.dto.AiUsageResponse;
import com.notio.analytics.dto.WeeklyAnalyticsResponse;
import com.notio.analytics.service.AiUsageLogService;
import com.notio.analytics.service.AnalyticsService;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AiUsageLogService aiUsageLogService;

    public AnalyticsController(
        final AnalyticsService analyticsService,
        final AiUsageLogService aiUsageLogService
    ) {
        this.analyticsService = analyticsService;
        this.aiUsageLogService = aiUsageLogService;
    }

    @GetMapping("/weekly")
    public ApiResponse<WeeklyAnalyticsResponse> weekly(final Authentication authentication) {
        return ApiResponse.success(analyticsService.getWeeklySummary(currentUserId(authentication)));
    }

    @GetMapping("/ai-usage")
    public ApiResponse<AiUsageResponse> aiUsage(
        @RequestParam(defaultValue = "DAILY") final String granularity,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) final LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) final LocalDate endDate,
        final Authentication authentication
    ) {
        return ApiResponse.success(
            aiUsageLogService.getAiUsage(
                currentUserId(authentication),
                AiUsageGranularity.from(granularity),
                startDate,
                endDate
            )
        );
    }

    private Long currentUserId(final Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new NotioException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (final NumberFormatException exception) {
            throw new NotioException(ErrorCode.UNAUTHORIZED);
        }
    }
}
