package com.notio.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.analytics.service.AiUsageLogService;
import com.notio.channel.ChannelRouter;
import com.notio.connection.domain.Connection;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.dto.NotificationSummaryResponse;
import com.notio.notification.embedding.NotificationEmbeddingService;
import com.notio.notification.metrics.NotificationFlowMetrics;
import com.notio.notification.repository.NotificationRepository;
import com.notio.webhook.dto.NotificationEvent;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final Long DEFAULT_PHASE0_USER_ID = 1L;

    private static final Executor VIRTUAL_THREAD_EXECUTOR =
        Executors.newVirtualThreadPerTaskExecutor();

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final NotificationEmbeddingService notificationEmbeddingService;
    private final NotificationFlowMetrics notificationFlowMetrics;
    private final NotificationSummaryService notificationSummaryService;
    private final ChannelRouter channelRouter;
    private final AiUsageLogService aiUsageLogService;

    @Transactional
    @CacheEvict(value = "unreadCount", key = "#event.userId()")
    public Notification saveFromEvent(NotificationEvent event) {
        if (event.userId() == null) {
            throw new NotioException(ErrorCode.UNAUTHORIZED);
        }

        return saveNotification(
            event,
            event.userId(),
            event.connectionId()
        );
    }

    @Transactional
    @CacheEvict(value = "unreadCount", key = "#connection.userId")
    public Notification saveFromConnection(NotificationEvent event, Connection connection) {
        return saveNotification(
            event,
            connection.getUserId(),
            connection.getId()
        );
    }

    private Notification saveNotification(
        NotificationEvent event,
        Long userId,
        Long connectionId
    ) {
        Notification notification = Notification.builder()
            .userId(userId)
            .connectionId(connectionId)
            .source(event.source())
            .title(event.title())
            .body(event.body())
            .priority(event.priority())
            .externalId(event.externalId())
            .externalUrl(event.externalUrl())
            .metadata(convertMetadataToJson(event.metadata()))
            .build();

        Notification saved = notificationRepository.save(notification);
        log.info(
            "event=notification_created notification_id={} user_id={} connection_id={} source={} outcome=success",
            saved.getId(),
            saved.getUserId(),
            saved.getConnectionId(),
            saved.getSource().name().toLowerCase()
        );
        notificationFlowMetrics.recordNotificationCreated(saved.getSource().name().toLowerCase());
        evictUnreadCountCache(saved.getUserId());

        // Branch A: pgvector 임베딩 (비동기)
        CompletableFuture.runAsync(() -> {
            try {
                notificationEmbeddingService.embedNotification(saved);
                notificationFlowMetrics.recordNotificationEmbedding("success");
            } catch (Exception e) {
                notificationFlowMetrics.recordNotificationEmbedding("failure");
                log.warn(
                    "event=notification_embedding_failed notification_id={} user_id={} source={} exception_type={}",
                    saved.getId(),
                    saved.getUserId(),
                    saved.getSource().name().toLowerCase(),
                    e.getClass().getSimpleName()
                );
            }
        }, VIRTUAL_THREAD_EXECUTOR);

        // Branch B: 채널 라우팅 (비동기, Branch A와 병렬)
        CompletableFuture.runAsync(() -> {
            try {
                channelRouter.route(saved);
            } catch (Exception e) {
                log.error("event=channel_routing_failed notification_id={} user_id={}",
                    saved.getId(), saved.getUserId(), e);
            }
        }, VIRTUAL_THREAD_EXECUTOR);

        // Branch C: LLM 요약 (비동기, Branch A·B와 병렬)
        CompletableFuture.runAsync(() -> {
            try {
                notificationSummaryService.summarize(saved);
            } catch (Exception e) {
                log.error("event=notification_summarize_failed notification_id={} user_id={}",
                    saved.getId(), saved.getUserId(), e);
            }
        }, VIRTUAL_THREAD_EXECUTOR);

        // Branch D: AI 토큰 사용량 기록 (비동기, Branch A·B·C와 병렬)
        CompletableFuture.runAsync(
            () -> aiUsageLogService.logFromNotification(saved),
            VIRTUAL_THREAD_EXECUTOR
        ).exceptionally(e -> {
            log.warn(
                "event=ai_usage_log_failed notification_id={} user_id={} exception_type={}",
                saved.getId(),
                saved.getUserId(),
                e.getClass().getSimpleName()
            );
            return null;
        });

        return saved;
    }

    public Page<Notification> findAll(Long userId, NotificationSource source, Boolean isRead, Pageable pageable) {
        return notificationRepository.findAllWithFilter(userId, source, isRead, pageable);
    }

    public Page<Notification> findAllCreatedInRange(
        Long userId,
        Instant startInclusive,
        Instant endExclusive,
        Pageable pageable
    ) {
        return notificationRepository.findAllByUserIdAndCreatedAtRange(
            userId,
            startInclusive,
            endExclusive,
            pageable
        );
    }

    public Page<NotificationSummaryResponse> findAllSummaries(
        Long userId,
        NotificationSource source,
        Boolean isRead,
        Pageable pageable
    ) {
        return notificationRepository.findAllSummariesWithFilter(userId, source, isRead, pageable)
            .map(NotificationSummaryResponse::from);
    }

    @Deprecated
    public Page<Notification> findAll(NotificationSource source, Boolean isRead, Pageable pageable) {
        return findAll(DEFAULT_PHASE0_USER_ID, source, isRead, pageable);
    }

    public Notification findById(Long userId, Long id) {
        return notificationRepository.findByIdAndUserIdAndNotDeleted(userId, id)
            .orElseThrow(() -> new NotioException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Deprecated
    public Notification findById(Long id) {
        return findById(DEFAULT_PHASE0_USER_ID, id);
    }

    @Transactional
    public Notification getDetail(Long userId, Long id) {
        return getDetailAndMarkReadIfUnread(userId, id);
    }

    @Transactional
    public Notification markRead(Long userId, Long id) {
        return markReadIfUnread(userId, id);
    }

    @Transactional
    @CacheEvict(value = "unreadCount", key = "#userId")
    public int markAllRead(Long userId) {
        int count = notificationRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read: userId={}", count, userId);
        return count;
    }

    @Transactional
    @CacheEvict(value = "unreadCount", key = "#userId")
    public void delete(Long userId, Long id) {
        Notification notification = findById(userId, id);
        notification.softDelete();
        log.info("Notification soft deleted: id={}, userId={}", id, userId);
    }

    @Cacheable(value = "unreadCount", key = "#userId")
    public long countUnread(Long userId) {
        return notificationRepository.countUnread(userId);
    }

    private Notification getDetailAndMarkReadIfUnread(Long userId, Long id) {
        Notification notification = findById(userId, id);
        return applyReadTransitionIfNeeded(userId, notification);
    }

    private Notification markReadIfUnread(Long userId, Long id) {
        Notification notification = findById(userId, id);
        return applyReadTransitionIfNeeded(userId, notification);
    }

    private Notification applyReadTransitionIfNeeded(Long userId, Notification notification) {
        if (notification.isRead()) {
            return notification;
        }

        notification.markAsRead();
        evictUnreadCountCache(userId);
        return notification;
    }

    private void evictUnreadCountCache(Long userId) {
        Cache cache = cacheManager.getCache("unreadCount");
        if (cache == null) {
            log.warn("Unread count cache is not configured; skip eviction for userId={}", userId);
            return;
        }

        cache.evict(userId);
    }

    private String convertMetadataToJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert metadata to JSON: {}", metadata, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseMetadataFromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON metadata: {}", json, e);
            return null;
        }
    }
}
