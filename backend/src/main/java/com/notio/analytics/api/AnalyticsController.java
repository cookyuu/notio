package com.notio.analytics.api;

import com.notio.analytics.application.AnalyticsService;
import com.notio.common.api.ApiResponse;
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
    public ApiResponse<WeeklyAnalyticsResponse> weekly() {
        return ApiResponse.success(analyticsService.getWeeklySummary());
    }
}
