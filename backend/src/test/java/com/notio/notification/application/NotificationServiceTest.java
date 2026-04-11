package com.notio.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.common.error.NotioException;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.infrastructure.NotificationRepository;
import com.notio.push.application.PushService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PushService pushService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, stringRedisTemplate, pushService);
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
                Map.of("channel", "dev")
        );
        final Notification saved = new Notification(
                event.source(),
                event.title(),
                event.body(),
                event.priority(),
                event.externalId(),
                event.externalUrl(),
                event.metadata()
        );
        ReflectionTestUtils.setField(saved, "id", 1L);
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        final long savedId = notificationService.saveFromEvent(event);

        assertThat(savedId).isEqualTo(saved.getId());
        verify(notificationRepository).save(any(Notification.class));
        verify(pushService).sendPush(saved.getId());
    }

    @Test
    void countUnreadUsesRedisCacheWhenAvailable() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("notifications:unread-count")).thenReturn("7");

        final long unreadCount = notificationService.countUnread();

        assertThat(unreadCount).isEqualTo(7);
        verify(notificationRepository, never()).countByIsReadFalseAndDeletedAtIsNull();
    }

    @Test
    void findByIdThrowsWhenNotificationDoesNotExist() {
        when(notificationRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.findById(999L))
                .isInstanceOf(NotioException.class)
                .hasMessage("알림을 찾을 수 없습니다.");
    }
}
