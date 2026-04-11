package com.notio.chat.application;

import com.notio.chat.api.DailySummaryResponse;
import com.notio.notification.api.NotificationFilterRequest;
import com.notio.notification.application.NotificationService;
import com.notio.notification.domain.Notification;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class DailySummaryService {

    private final NotificationService notificationService;

    public DailySummaryService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public DailySummaryResponse getSummary() {
        final List<Notification> notifications = notificationService.findAll(
                new NotificationFilterRequest(null, null, null, null),
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final List<Notification> todaysNotifications = notifications.stream()
                .filter(notification -> notification.getCreatedAt().toLocalDate().isEqual(today))
                .toList();
        final List<String> topics = todaysNotifications.stream()
                .map(notification -> notification.getSource().name())
                .distinct()
                .collect(Collectors.toList());
        final String summary = todaysNotifications.isEmpty()
                ? "오늘 수집된 알림이 없습니다."
                : "오늘 알림 " + todaysNotifications.size() + "건이 수집되었습니다.";
        return new DailySummaryResponse(summary, today, todaysNotifications.size(), topics);
    }
}

