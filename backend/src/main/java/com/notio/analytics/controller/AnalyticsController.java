package com.notio.analytics.controller;

import com.notio.analytics.dto.WeeklyAnalyticsResponse;
import com.notio.analytics.service.AnalyticsService;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(final AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/weekly")
    public ApiResponse<WeeklyAnalyticsResponse> weekly(final Authentication authentication) {
        return ApiResponse.success(analyticsService.getWeeklySummary(currentUserId(authentication)));
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
