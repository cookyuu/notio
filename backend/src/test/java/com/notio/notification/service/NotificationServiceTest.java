package com.notio.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.connection.domain.Connection;
import com.notio.common.exception.NotioException;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.dto.NotificationSummaryResponse;
import com.notio.notification.embedding.NotificationEmbeddingService;
import com.notio.notification.repository.NotificationRepository;
import com.notio.notification.repository.NotificationSummaryProjection;
import com.notio.push.service.PushService;
import com.notio.webhook.dto.NotificationEvent;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PushService pushService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache unreadCountCache;

    @Mock
    private Cache dailySummaryCache;

    @Mock
    private NotificationEmbeddingService notificationEmbeddingService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                new ObjectMapper(),
                pushService,
                cacheManager,
                notificationEmbeddingService
        );
        lenient().when(cacheManager.getCache("unreadCount")).thenReturn(unreadCountCache);
        lenient().when(cacheManager.getCache("dailySummary")).thenReturn(dailySummaryCache);
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
        verify(notificationEmbeddingService).embedNotification(saved);
        verify(pushService).sendPush(saved.getId(), saved.getUserId());
        verify(dailySummaryCache).evict(event.userId() + ":" + LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    @Test
    void saveFromEventKeepsNotificationWhenEmbeddingFails() {
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
        org.mockito.Mockito.doThrow(new IllegalStateException("embedding down"))
                .when(notificationEmbeddingService).embedNotification(saved);

        final Notification savedNotification = notificationService.saveFromEvent(event);

        assertThat(savedNotification).isSameAs(saved);
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationEmbeddingService).embedNotification(saved);
        verify(dailySummaryCache).evict(event.userId() + ":" + LocalDate.now(ZoneId.of("Asia/Seoul")));
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
    void findAllSummariesUsesLightweightProjectionAndKeepsPagination() {
        final NotificationSummaryProjection summary = new NotificationSummaryProjection() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public NotificationSource getSource() {
                return NotificationSource.GITHUB;
            }

            @Override
            public String getTitle() {
                return "PR opened";
            }

            @Override
            public NotificationPriority getPriority() {
                return NotificationPriority.HIGH;
            }

            @Override
            public boolean isRead() {
                return false;
            }

            @Override
            public Instant getCreatedAt() {
                return Instant.parse("2026-04-17T12:30:00Z");
            }

            @Override
            public String getBody() {
                return "A new PR is ready with tests and deployment notes.";
            }
        };
        final PageRequest pageable = PageRequest.of(0, 20);
        when(notificationRepository.findAllSummariesWithFilter(10L, null, null, pageable))
            .thenReturn(new PageImpl<>(java.util.List.of(summary), pageable, 1));

        final var result = notificationService.findAllSummaries(10L, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getContent())
            .extracting(NotificationSummaryResponse::id, NotificationSummaryResponse::source, NotificationSummaryResponse::bodyPreview)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(
                1L,
                NotificationSource.GITHUB.name(),
                "A new PR is ready with tests and deployment notes."
            ));
        verify(notificationRepository).findAllSummariesWithFilter(10L, null, null, pageable);
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
    void getDetailMarksUnreadNotificationAsReadAndEvictsUnreadCountCache() {
        final Notification notification = Notification.builder()
                .id(99L)
                .userId(10L)
                .source(NotificationSource.GITHUB)
                .title("PR opened")
                .body("A new PR is ready")
                .priority(NotificationPriority.HIGH)
                .read(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 99L)).thenReturn(Optional.of(notification));

        final Notification result = notificationService.getDetail(10L, 99L);

        assertThat(result.isRead()).isTrue();
        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).findByIdAndUserIdAndNotDeleted(10L, 99L);
        verify(unreadCountCache).evict(10L);
    }

    @Test
    void getDetailReturnsAlreadyReadNotificationWithoutStateChange() {
        final Notification notification = Notification.builder()
                .id(100L)
                .userId(10L)
                .source(NotificationSource.SLACK)
                .title("Mention")
                .body("Already read")
                .priority(NotificationPriority.MEDIUM)
                .read(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 100L)).thenReturn(Optional.of(notification));

        final Notification result = notificationService.getDetail(10L, 100L);

        assertThat(result.isRead()).isTrue();
        assertThat(result).isSameAs(notification);
        verify(notificationRepository).findByIdAndUserIdAndNotDeleted(10L, 100L);
        verify(unreadCountCache, org.mockito.Mockito.never()).evict(ArgumentMatchers.any());
    }

    @Test
    void markReadEvictsUnreadCountOnlyWhenReadStateChanges() {
        final Notification notification = Notification.builder()
                .id(101L)
                .userId(10L)
                .source(NotificationSource.GITHUB)
                .title("Issue assigned")
                .body("Unread issue assignment")
                .priority(NotificationPriority.HIGH)
                .read(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 101L)).thenReturn(Optional.of(notification));

        final Notification result = notificationService.markRead(10L, 101L);

        assertThat(result.isRead()).isTrue();
        verify(unreadCountCache).evict(10L);
    }

    @Test
    void markReadDoesNotEvictUnreadCountWhenNotificationAlreadyRead() {
        final Notification notification = Notification.builder()
                .id(102L)
                .userId(10L)
                .source(NotificationSource.SLACK)
                .title("Already read")
                .body("No state change")
                .priority(NotificationPriority.LOW)
                .read(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 102L)).thenReturn(Optional.of(notification));

        final Notification result = notificationService.markRead(10L, 102L);

        assertThat(result.isRead()).isTrue();
        verify(unreadCountCache, org.mockito.Mockito.never()).evict(ArgumentMatchers.any());
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
        verify(dailySummaryCache).evict("10:" + LocalDate.now(ZoneId.of("Asia/Seoul")));
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
        final Method markAllRead = NotificationService.class.getMethod("markAllRead", Long.class);
        final CacheEvict markAllReadEvict = markAllRead.getAnnotation(CacheEvict.class);
        final Method delete = NotificationService.class.getMethod("delete", Long.class, Long.class);
        final CacheEvict deleteEvict = delete.getAnnotation(CacheEvict.class);

        assertThat(saveFromEventEvict.key()).isEqualTo("#event.userId()");
        assertThat(saveFromConnectionEvict.key()).isEqualTo("#connection.userId");
        assertThat(markAllReadEvict.key()).isEqualTo("#userId");
        assertThat(deleteEvict.key()).isEqualTo("#userId");
    }
}
