package com.notio.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.analytics.dto.WeeklyAnalyticsResponse;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.service.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void getWeeklySummaryAggregatesOnlyRequestUserNotificationsWithinSevenDays() {
        final AnalyticsService analyticsService = new AnalyticsService(notificationService);
        final Long userId = 10L;
        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final List<Notification> notifications = List.of(
                notification(userId, NotificationSource.GITHUB, NotificationPriority.HIGH, false,
                        dayAtUtc(today.minusDays(1), 8)),
                notification(userId, NotificationSource.SLACK, NotificationPriority.MEDIUM, true,
                        dayAtUtc(today.minusDays(3), 9)),
                notification(userId, NotificationSource.GITHUB, NotificationPriority.HIGH, false,
                        dayAtUtc(today, 10)),
                notification(userId, NotificationSource.GMAIL, NotificationPriority.LOW, false,
                        dayAtUtc(today.minusDays(7), 11)),
                notification(11L, NotificationSource.CLAUDE, NotificationPriority.URGENT, false,
                        dayAtUtc(today.minusDays(2), 12))
        );
        when(notificationService.findAll(eq(userId), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(notifications, PageRequest.of(0, 500), notifications.size()));

        final WeeklyAnalyticsResponse response = analyticsService.getWeeklySummary(userId);

        assertThat(response.totalNotifications()).isEqualTo(3);
        assertThat(response.unreadNotifications()).isEqualTo(2);
        assertThat(response.sourceDistribution()).hasSize(2);
        assertThat(response.sourceDistribution()).containsEntry(NotificationSource.GITHUB.name(), 2L);
        assertThat(response.sourceDistribution()).containsEntry(NotificationSource.SLACK.name(), 1L);
        assertThat(response.priorityDistribution()).hasSize(2);
        assertThat(response.priorityDistribution()).containsEntry(NotificationPriority.HIGH.name(), 2L);
        assertThat(response.priorityDistribution()).containsEntry(NotificationPriority.MEDIUM.name(), 1L);
        assertThat(response.dailyTrend()).hasSize(3);
        assertThat(response.dailyTrend()).containsEntry(today.minusDays(1).toString(), 1L);
        assertThat(response.dailyTrend()).containsEntry(today.minusDays(3).toString(), 1L);
        assertThat(response.dailyTrend()).containsEntry(today.toString(), 1L);
        assertThat(response.insight()).isEqualTo("가장 많은 알림은 GITHUB 소스에서 발생했습니다.");
        verify(notificationService).findAll(eq(userId), eq(null), eq(null), any());
    }

    @Test
    void getWeeklySummaryReturnsEmptyResponseWhenThereAreNoRecentNotifications() {
        final AnalyticsService analyticsService = new AnalyticsService(notificationService);
        final Long userId = 10L;
        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final List<Notification> notifications = List.of(
                notification(userId, NotificationSource.GITHUB, NotificationPriority.HIGH, false,
                        dayAtUtc(today.minusDays(10), 8))
        );
        when(notificationService.findAll(eq(userId), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(notifications, PageRequest.of(0, 500), notifications.size()));

        final WeeklyAnalyticsResponse response = analyticsService.getWeeklySummary(userId);

        assertThat(response.totalNotifications()).isZero();
        assertThat(response.unreadNotifications()).isZero();
        assertThat(response.sourceDistribution()).isEmpty();
        assertThat(response.priorityDistribution()).isEmpty();
        assertThat(response.dailyTrend()).isEmpty();
        assertThat(response.insight()).isEqualTo("이번 주에는 아직 수집된 알림이 없습니다.");
    }

    private Notification notification(
            final Long userId,
            final NotificationSource source,
            final NotificationPriority priority,
            final boolean isRead,
            final Instant createdAt
    ) {
        return Notification.builder()
                .userId(userId)
                .source(source)
                .title("title")
                .body("body")
                .priority(priority)
                .read(isRead)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private Instant dayAtUtc(final LocalDate date, final int hour) {
        return date.atTime(hour, 0).toInstant(ZoneOffset.UTC);
    }
}
