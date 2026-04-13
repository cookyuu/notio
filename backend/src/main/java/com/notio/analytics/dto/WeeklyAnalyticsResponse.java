package com.notio.analytics.dto;

import java.util.Map;

public record WeeklyAnalyticsResponse(
        long totalNotifications,
        long unreadNotifications,
        Map<String, Long> sourceDistribution,
        Map<String, Long> priorityDistribution,
        Map<String, Long> dailyTrend,
        String insight
) {
}

