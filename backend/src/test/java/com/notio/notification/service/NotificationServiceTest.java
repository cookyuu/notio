package com.notio.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.analytics.service.AiUsageLogService;
import com.notio.channel.ChannelRouter;
import com.notio.connection.domain.Connection;
import com.notio.common.exception.NotioException;
import com.notio.common.metrics.NotioMetrics;
import com.notio.common.metrics.NotioMetricsTagPolicy;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.dto.NotificationSummaryResponse;
import com.notio.notification.embedding.NotificationEmbeddingService;
import com.notio.notification.metrics.NotificationFlowMetrics;
import com.notio.notification.repository.NotificationRepository;
import com.notio.notification.repository.NotificationSummaryProjection;
import com.notio.webhook.dto.NotificationEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private CacheManager cacheManager;

    @Mock
    private Cache unreadCountCache;

    @Mock
    private NotificationEmbeddingService notificationEmbeddingService;

    @Mock
    private NotificationSummaryService notificationSummaryService;

    @Mock
    private ChannelRouter channelRouter;

    @Mock
    private AiUsageLogService aiUsageLogService;

    private SimpleMeterRegistry meterRegistry;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        notificationService = new NotificationService(
            notificationRepository,
            new ObjectMapper(),
            cacheManager,
            notificationEmbeddingService,
            new NotificationFlowMetrics(new NotioMetrics(meterRegistry, new NotioMetricsTagPolicy())),
            notificationSummaryService,
            channelRouter,
            aiUsageLogService
        );
        lenient().when(cacheManager.getCache("unreadCount")).thenReturn(unreadCountCache);
    }

    @Test
    void saveFromEventSavesNotificationAndRecordsMetric() {
        final NotificationEvent event = new NotificationEvent(
            NotificationSource.SLACK, "title", "body", NotificationPriority.HIGH,
            "ext-1", "https://notio.dev", Map.of("channel", "dev"), 10L, 20L
        );
        final Notification saved = Notification.builder()
            .id(1L).userId(event.userId()).connectionId(event.connectionId())
            .source(event.source()).title(event.title()).body(event.body())
            .priority(event.priority()).externalId(event.externalId())
            .externalUrl(event.externalUrl()).metadata("{\"channel\":\"dev\"}").build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        final Notification savedNotification = notificationService.saveFromEvent(event);

        assertThat(savedNotification.getId()).isEqualTo(saved.getId());
        verify(notificationRepository).save(any(Notification.class));
        assertThat(meterRegistry.get("notio_notifications_created_total")
            .tag("source", "slack")
            .counter().count()).isEqualTo(1.0d);
    }

    @Test
    void saveFromEventEvictsUnreadCountCache() {
        final NotificationEvent event = new NotificationEvent(
            NotificationSource.GITHUB, "title", "body", NotificationPriority.HIGH,
            "ext-1", null, null, 10L, null
        );
        final Notification saved = Notification.builder()
            .id(1L).userId(10L).source(NotificationSource.GITHUB)
            .title("title").body("body").priority(NotificationPriority.HIGH).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        notificationService.saveFromEvent(event);

        verify(unreadCountCache).evict(10L);
    }

    @Test
    void saveFromEventRejectsMissingUserId() {
        final NotificationEvent event = new NotificationEvent(
            NotificationSource.SLACK, "title", "body", NotificationPriority.HIGH,
            "ext-1", "https://notio.dev", Map.of("channel", "dev")
        );

        assertThatThrownBy(() -> notificationService.saveFromEvent(event))
            .isInstanceOf(NotioException.class)
            .hasMessage("인증에 실패했습니다.");
    }

    @Test
    void saveFromConnectionUsesConnectionUserAndId() {
        final NotificationEvent event = new NotificationEvent(
            NotificationSource.CLAUDE, "title", "body", NotificationPriority.MEDIUM,
            "ext-1", "https://notio.dev", Map.of("session", "abc")
        );
        final Connection connection = Connection.builder().id(20L).userId(10L).build();
        final Notification saved = Notification.builder()
            .id(1L).userId(10L).connectionId(20L).source(event.source())
            .title(event.title()).body(event.body()).priority(event.priority())
            .externalId(event.externalId()).externalUrl(event.externalUrl())
            .metadata("{\"session\":\"abc\"}").build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        final Notification savedNotification = notificationService.saveFromConnection(event, connection);

        assertThat(savedNotification.getUserId()).isEqualTo(10L);
        assertThat(savedNotification.getConnectionId()).isEqualTo(20L);
        verify(notificationRepository).save(any(Notification.class));
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
    }

    @Test
    void findAllSummariesUsesLightweightProjectionAndKeepsPagination() {
        final NotificationSummaryProjection summary = new NotificationSummaryProjection() {
            @Override public Long getId() { return 1L; }
            @Override public NotificationSource getSource() { return NotificationSource.GITHUB; }
            @Override public String getTitle() { return "PR opened"; }
            @Override public NotificationPriority getPriority() { return NotificationPriority.HIGH; }
            @Override public boolean isRead() { return false; }
            @Override public Instant getCreatedAt() { return Instant.parse("2026-04-17T12:30:00Z"); }
            @Override public String getBody() { return "A new PR is ready with tests and deployment notes."; }
        };
        final PageRequest pageable = PageRequest.of(0, 20);
        when(notificationRepository.findAllSummariesWithFilter(10L, null, null, pageable))
            .thenReturn(new PageImpl<>(List.of(summary), pageable, 1));

        final var result = notificationService.findAllSummaries(10L, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
            .extracting(NotificationSummaryResponse::id, NotificationSummaryResponse::source, NotificationSummaryResponse::bodyPreview)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(
                1L, NotificationSource.GITHUB.name(),
                "A new PR is ready with tests and deployment notes."
            ));
    }

    @Test
    void findByIdThrowsWhenNotificationDoesNotExist() {
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.findById(10L, 999L))
            .isInstanceOf(NotioException.class)
            .hasMessage("알림을 찾을 수 없습니다.");
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
            .id(99L).userId(10L).source(NotificationSource.GITHUB)
            .title("PR opened").body("A new PR is ready").priority(NotificationPriority.HIGH)
            .read(false).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 99L)).thenReturn(Optional.of(notification));

        final Notification result = notificationService.getDetail(10L, 99L);

        assertThat(result.isRead()).isTrue();
        verify(unreadCountCache).evict(10L);
    }

    @Test
    void getDetailReturnsAlreadyReadNotificationWithoutStateChange() {
        final Notification notification = Notification.builder()
            .id(100L).userId(10L).source(NotificationSource.SLACK)
            .title("Mention").body("Already read").priority(NotificationPriority.MEDIUM)
            .read(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 100L)).thenReturn(Optional.of(notification));

        final Notification result = notificationService.getDetail(10L, 100L);

        assertThat(result.isRead()).isTrue();
        verify(unreadCountCache, org.mockito.Mockito.never()).evict(ArgumentMatchers.any());
    }

    @Test
    void markReadEvictsUnreadCountOnlyWhenReadStateChanges() {
        final Notification notification = Notification.builder()
            .id(101L).userId(10L).source(NotificationSource.GITHUB)
            .title("Issue assigned").body("Unread issue assignment").priority(NotificationPriority.HIGH)
            .read(false).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(notificationRepository.findByIdAndUserIdAndNotDeleted(10L, 101L)).thenReturn(Optional.of(notification));

        final Notification result = notificationService.markRead(10L, 101L);

        assertThat(result.isRead()).isTrue();
        verify(unreadCountCache).evict(10L);
    }

    @Test
    void markReadDoesNotEvictUnreadCountWhenNotificationAlreadyRead() {
        final Notification notification = Notification.builder()
            .id(102L).userId(10L).source(NotificationSource.SLACK)
            .title("Already read").body("No state change").priority(NotificationPriority.LOW)
            .read(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();
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

    // -----------------------------------------------------------------------
    // Phase 6 — IMMEDIATE 지연 개선: channelRouter.route()가 summarize() 완료를
    // 기다리지 않고 즉시 실행되는지 검증
    // -----------------------------------------------------------------------

    @Test
    void channelRoutingDoesNotBlockOnSummarize() throws Exception {
        final NotificationEvent event = new NotificationEvent(
            NotificationSource.GITHUB, "title", "body", NotificationPriority.HIGH,
            "ext-1", null, null, 10L, null
        );
        final Notification saved = Notification.builder()
            .id(1L).userId(10L).source(NotificationSource.GITHUB)
            .title("title").body("body").priority(NotificationPriority.HIGH).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        // summarize()는 300ms 동안 블로킹하는 느린 작업으로 설정
        final CountDownLatch routeCalled = new CountDownLatch(1);
        final AtomicBoolean summarizeFinished = new AtomicBoolean(false);

        lenient().doAnswer(invocation -> {
            Thread.sleep(300);
            summarizeFinished.set(true);
            return null;
        }).when(notificationSummaryService).summarize(any(Notification.class));

        lenient().doAnswer(invocation -> {
            routeCalled.countDown();
            return null;
        }).when(channelRouter).route(any(Notification.class));

        notificationService.saveFromEvent(event);

        // route()는 summarize()가 끝나기 전에 호출되어야 한다 (최대 1초 대기)
        final boolean routeCalledBeforeTimeout = routeCalled.await(1, TimeUnit.SECONDS);
        assertThat(routeCalledBeforeTimeout)
            .as("channelRouter.route()는 summarize() 완료를 기다리지 않고 즉시 실행되어야 한다")
            .isTrue();
        assertThat(summarizeFinished.get())
            .as("route()가 먼저 호출된 시점에 summarize()는 아직 완료되지 않아야 한다")
            .isFalse();
    }

    // -----------------------------------------------------------------------
    // Phase 6 — IMMEDIATE 원본 body: route()에 전달되는 ChannelMessage.body가
    // notification.getBody()와 동일한지는 ChannelRouter 단위에서 검증되므로,
    // 여기서는 saveNotification()이 원본 body를 그대로 유지하는지 확인한다
    // -----------------------------------------------------------------------

    @Test
    void savedNotificationBodyEqualsOriginalEventBody() {
        final String originalBody = "PR #42 opened: feat/new-api";
        final NotificationEvent event = new NotificationEvent(
            NotificationSource.GITHUB, "PR opened", originalBody, NotificationPriority.HIGH,
            "ext-1", null, null, 10L, null
        );
        final Notification saved = Notification.builder()
            .id(1L).userId(10L).source(NotificationSource.GITHUB)
            .title("PR opened").body(originalBody).priority(NotificationPriority.HIGH).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        final Notification result = notificationService.saveFromEvent(event);

        assertThat(result.getBody())
            .as("저장된 알림의 body는 이벤트 원본 body와 동일해야 한다")
            .isEqualTo(originalBody);
        assertThat(result.getAiSummary())
            .as("saveFromEvent 직후에는 aiSummary가 설정되지 않아야 한다")
            .isNull();
    }

    // -----------------------------------------------------------------------
    // Phase 6 — 로그 독립성: Branch B(routing)와 Branch C(summarize)가 독립
    // CompletableFuture로 실행되어 서로 블로킹하지 않는지 검증
    // -----------------------------------------------------------------------

    @Test
    void routingAndSummarizeRunAsIndependentAsyncBranches() throws Exception {
        final NotificationEvent event = new NotificationEvent(
            NotificationSource.SLACK, "title", "body", NotificationPriority.MEDIUM,
            "ext-1", null, null, 10L, null
        );
        final Notification saved = Notification.builder()
            .id(1L).userId(10L).source(NotificationSource.SLACK)
            .title("title").body("body").priority(NotificationPriority.MEDIUM).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        final CountDownLatch bothBranchesStarted = new CountDownLatch(2);
        final AtomicBoolean routeStarted = new AtomicBoolean(false);
        final AtomicBoolean summarizeStarted = new AtomicBoolean(false);

        lenient().doAnswer(invocation -> {
            routeStarted.set(true);
            bothBranchesStarted.countDown();
            return null;
        }).when(channelRouter).route(any(Notification.class));

        lenient().doAnswer(invocation -> {
            summarizeStarted.set(true);
            bothBranchesStarted.countDown();
            return null;
        }).when(notificationSummaryService).summarize(any(Notification.class));

        notificationService.saveFromEvent(event);

        // 두 브랜치 모두 2초 이내에 독립적으로 시작되어야 한다
        final boolean bothStarted = bothBranchesStarted.await(2, TimeUnit.SECONDS);
        assertThat(bothStarted)
            .as("channelRouter.route()와 notificationSummaryService.summarize()가 모두 독립적으로 실행되어야 한다")
            .isTrue();
        assertThat(routeStarted.get()).isTrue();
        assertThat(summarizeStarted.get()).isTrue();
    }
}
