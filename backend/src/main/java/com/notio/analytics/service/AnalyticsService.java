package com.notio.analytics.service;

import com.notio.analytics.dto.WeeklyAnalyticsResponse;
import com.notio.notification.dto.NotificationFilterRequest;
import com.notio.notification.service.NotificationService;
import com.notio.notification.domain.Notification;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final NotificationService notificationService;

    public AnalyticsService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public WeeklyAnalyticsResponse getWeeklySummary() {
        final List<Notification> notifications = notificationService.findAll(
                null, // source
                null, // isRead
                PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        final LocalDate threshold = LocalDate.now(ZoneOffset.UTC).minusDays(6);
        final List<Notification> weeklyNotifications = notifications.stream()
                .filter(notification -> !notification.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().isBefore(threshold))
                .toList();
        final Map<String, Long> sourceDistribution = weeklyNotifications.stream()
                .collect(Collectors.groupingBy(
                        notification -> notification.getSource().name(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        final Map<String, Long> priorityDistribution = weeklyNotifications.stream()
                .collect(Collectors.groupingBy(
                        notification -> notification.getPriority().name(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        final Map<String, Long> dailyTrend = weeklyNotifications.stream()
                .collect(Collectors.groupingBy(
                        notification -> notification.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().toString(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        return new WeeklyAnalyticsResponse(
                weeklyNotifications.size(),
                weeklyNotifications.stream().filter(notification -> !notification.isRead()).count(),
                sourceDistribution,
                priorityDistribution,
                dailyTrend,
                weeklyNotifications.isEmpty()
                        ? "이번 주에는 아직 수집된 알림이 없습니다."
                        : "가장 많은 알림은 "
                        + sourceDistribution.entrySet().stream().max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey).orElse("UNKNOWN")
                        + " 소스에서 발생했습니다."
        );
    }
}

