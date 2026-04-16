package com.notio.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.connection.domain.Connection;
import com.notio.common.exception.NotioException;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.repository.NotificationRepository;
import com.notio.push.service.PushService;
import com.notio.webhook.dto.NotificationEvent;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PushService pushService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, new ObjectMapper(), pushService);
    }

    @Test
    void saveFromEventSavesNotificationAndTriggersPush() {
        final NotificationEvent event = new NotificationEvent(
                NotificationSource.SLACK,
                "title",
                "body",
                NotificationPriority.HIGH,
                "ext-1",
                "https://notio.dev",
                Map.of("channel", "dev"),
                10L,
                20L
        );
        final Notification saved = Notification.builder()
                .id(1L)
                .userId(event.userId())
                .connectionId(event.connectionId())
                .source(event.source())
                .title(event.title())
                .body(event.body())
                .priority(event.priority())
                .externalId(event.externalId())
                .externalUrl(event.externalUrl())
                .metadata("{\"channel\":\"dev\"}")
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        final Notification savedNotification = notificationService.saveFromEvent(event);

        assertThat(savedNotification.getId()).isEqualTo(saved.getId());
        verify(notificationRepository).save(any(Notification.class));
        verify(pushService).sendPush(saved.getId(), saved.getUserId());
    }

    @Test
    void countUnreadDelegatesToRepository() {
        when(notificationRepository.countUnread(10L)).thenReturn(7L);

        final long unreadCount = notificationService.countUnread(10L);

        assertThat(unreadCount).isEqualTo(7);
        verify(notificationRepository).countUnread(10L);
    }

    @Test
    void countUnreadSeparatesUsers() {
        when(notificationRepository.countUnread(10L)).thenReturn(3L);
        when(notificationRepository.countUnread(11L)).thenReturn(5L);

        assertThat(notificationService.countUnread(10L)).isEqualTo(3L);
        assertThat(notificationService.countUnread(11L)).isEqualTo(5L);
        verify(notificationRepository).countUnread(10L);
        verify(notificationRepository).countUnread(11L);
    }

    @Test
    void findByIdThrowsWhenNotificationDoesNotExist() {
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.findById(10L, 999L))
                .isInstanceOf(NotioException.class)
                .hasMessage("알림을 찾을 수 없습니다.");
    }

    @Test
    void saveFromEventRejectsMissingUserId() {
        final NotificationEvent event = new NotificationEvent(
                NotificationSource.SLACK,
                "title",
                "body",
                NotificationPriority.HIGH,
                "ext-1",
                "https://notio.dev",
                Map.of("channel", "dev")
        );

        assertThatThrownBy(() -> notificationService.saveFromEvent(event))
                .isInstanceOf(NotioException.class)
                .hasMessage("인증에 실패했습니다.");
    }

    @Test
    void markReadThrowsWhenNotificationBelongsToAnotherUser() {
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(10L, 999L))
            .isInstanceOf(NotioException.class)
            .hasMessage("알림을 찾을 수 없습니다.");
    }

    @Test
    void markAllReadUsesRequestUserOnly() {
        when(notificationRepository.markAllAsRead(10L)).thenReturn(4);

        final int count = notificationService.markAllRead(10L);

        assertThat(count).isEqualTo(4);
        verify(notificationRepository).markAllAsRead(10L);
    }

    @Test
    void saveFromConnectionUsesConnectionUserAndId() {
        final NotificationEvent event = new NotificationEvent(
                NotificationSource.CLAUDE,
                "title",
                "body",
                NotificationPriority.MEDIUM,
                "ext-1",
                "https://notio.dev",
                Map.of("session", "abc")
        );
        final Connection connection = Connection.builder()
                .id(20L)
                .userId(10L)
                .build();
        final Notification saved = Notification.builder()
                .id(1L)
                .userId(10L)
                .connectionId(20L)
                .source(event.source())
                .title(event.title())
                .body(event.body())
                .priority(event.priority())
                .externalId(event.externalId())
                .externalUrl(event.externalUrl())
                .metadata("{\"session\":\"abc\"}")
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        final Notification savedNotification = notificationService.saveFromConnection(event, connection);

        assertThat(savedNotification.getUserId()).isEqualTo(10L);
        assertThat(savedNotification.getConnectionId()).isEqualTo(20L);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void unreadCountCacheUsesUserScopedKey() throws Exception {
        final Method method = NotificationService.class.getMethod("countUnread", Long.class);
        final Cacheable cacheable = method.getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.key()).isEqualTo("#userId");
    }

    @Test
    void writeOperationsEvictUnreadCountByUserScopedKey() throws Exception {
        final Method saveFromEvent = NotificationService.class.getMethod("saveFromEvent", NotificationEvent.class);
        final CacheEvict saveFromEventEvict = saveFromEvent.getAnnotation(CacheEvict.class);
        final Method saveFromConnection = NotificationService.class.getMethod("saveFromConnection", NotificationEvent.class, Connection.class);
        final CacheEvict saveFromConnectionEvict = saveFromConnection.getAnnotation(CacheEvict.class);
        final Method markRead = NotificationService.class.getMethod("markRead", Long.class, Long.class);
        final CacheEvict markReadEvict = markRead.getAnnotation(CacheEvict.class);
        final Method markAllRead = NotificationService.class.getMethod("markAllRead", Long.class);
        final CacheEvict markAllReadEvict = markAllRead.getAnnotation(CacheEvict.class);

        assertThat(saveFromEventEvict.key()).isEqualTo("#event.userId()");
        assertThat(saveFromConnectionEvict.key()).isEqualTo("#connection.userId");
        assertThat(markReadEvict.key()).isEqualTo("#userId");
        assertThat(markAllReadEvict.key()).isEqualTo("#userId");
    }
}
