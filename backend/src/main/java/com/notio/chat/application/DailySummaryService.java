package com.notio.chat.application;

import com.notio.chat.api.DailySummaryResponse;
import com.notio.notification.api.NotificationFilterRequest;
import com.notio.notification.application.NotificationService;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class DailySummaryService {

    private final NotificationService notificationService;

    public DailySummaryService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Cacheable(value = "dailySummary", key = "#root.methodName + '-' + T(java.time.LocalDate).now()")
    public DailySummaryResponse getSummary() {
        final List<Notification> notifications = notificationService.findAll(
                new NotificationFilterRequest(null, null, null, null),
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final OffsetDateTime startOfDay = today.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        final List<Notification> todaysNotifications = notifications.stream()
                .filter(notification -> notification.getCreatedAt().isAfter(startOfDay))
                .toList();

        final List<String> topics = extractTopics(todaysNotifications);
        final String summary = generateSummary(todaysNotifications);

        return new DailySummaryResponse(summary, today, todaysNotifications.size(), topics);
    }

    private List<String> extractTopics(final List<Notification> notifications) {
        final Map<String, Long> sourceFrequency = notifications.stream()
                .collect(Collectors.groupingBy(
                        notification -> notification.getSource().name(),
                        Collectors.counting()
                ));

        return sourceFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String generateSummary(final List<Notification> todaysNotifications) {
        if (todaysNotifications.isEmpty()) {
            return "오늘은 아직 수집된 알림이 없습니다. 조용한 하루를 보내고 계시네요!";
        }

        final long highPriorityCount = todaysNotifications.stream()
                .filter(notification -> notification.getPriority() == NotificationPriority.HIGH)
                .count();

        final long unreadCount = todaysNotifications.stream()
                .filter(notification -> !notification.isRead())
                .count();

        final Map<String, Long> sourceCount = todaysNotifications.stream()
                .collect(Collectors.groupingBy(
                        notification -> notification.getSource().name(),
                        Collectors.counting()
                ));

        final String topSource = sourceCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("INTERNAL");

        final StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append(String.format("오늘 총 %d건의 알림이 수집되었습니다. ", todaysNotifications.size()));

        if (highPriorityCount > 0) {
            summaryBuilder.append(String.format("그 중 %d건이 높은 우선순위 알림입니다. ", highPriorityCount));
        }

        summaryBuilder.append(String.format("%s 소스에서 가장 많은 알림(%d건)이 도착했습니다. ",
                topSource, sourceCount.get(topSource)));

        if (unreadCount > 0) {
            summaryBuilder.append(String.format("미확인 알림이 %d건 남아있습니다.", unreadCount));
        } else {
            summaryBuilder.append("모든 알림을 확인하셨습니다!");
        }

        return summaryBuilder.toString();
    }
}

