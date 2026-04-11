package com.notio.analytics.api;

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

