package com.notio.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.connection.domain.Connection;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.dto.NotificationSummaryResponse;
import com.notio.notification.repository.NotificationRepository;
import com.notio.push.service.PushService;
import com.notio.webhook.dto.NotificationEvent;
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

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final Long DEFAULT_PHASE0_USER_ID = 1L;

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final PushService pushService;
    private final CacheManager cacheManager;

    /**
     * Webhook 이벤트로부터 알림 생성.
     * Phase 0부터 사용자 식별 없는 저장은 금지한다.
     */
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

    /**
     * Connection으로 식별된 webhook 이벤트로부터 알림 생성
     */
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
        log.info("Notification created: id={}, userId={}, connectionId={}, source={}, title={}",
            saved.getId(), saved.getUserId(), saved.getConnectionId(), saved.getSource(), saved.getTitle());

        // 푸시 알림 발송 (동기 - Phase 0)
        try {
            pushService.sendPush(saved.getId(), saved.getUserId());
        } catch (Exception e) {
            // 푸시 발송 실패해도 알림 생성은 성공으로 처리
            log.error("Failed to send push notification: notificationId={}, error={}",
                saved.getId(), e.getMessage(), e);
        }

        return saved;
    }

    /**
     * 알림 목록 조회 (필터링 지원)
     */
    public Page<Notification> findAll(Long userId, NotificationSource source, Boolean isRead, Pageable pageable) {
        return notificationRepository.findAllWithFilter(userId, source, isRead, pageable);
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

    /**
     * Phase 0 legacy internal callers. User-facing Notification API must use the user-scoped overload.
     */
    @Deprecated
    public Page<Notification> findAll(NotificationSource source, Boolean isRead, Pageable pageable) {
        return findAll(DEFAULT_PHASE0_USER_ID, source, isRead, pageable);
    }

    public Notification findById(Long userId, Long id) {
        return notificationRepository.findByIdAndUserIdAndNotDeleted(userId, id)
            .orElseThrow(() -> new NotioException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    /**
     * Phase 0 legacy internal callers. User-facing Notification API must use the user-scoped overload.
     */
    @Deprecated
    public Notification findById(Long id) {
        return findById(DEFAULT_PHASE0_USER_ID, id);
    }

    /**
     * 알림 상세 조회. 미읽음 상태면 읽음 처리 후 반환한다.
     */
    @Transactional
    public Notification getDetail(Long userId, Long id) {
        return getDetailAndMarkReadIfUnread(userId, id);
    }

    /**
     * 알림을 읽음 상태로 변경
     */
    @Transactional
    public Notification markRead(Long userId, Long id) {
        return markReadIfUnread(userId, id);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    @CacheEvict(value = "unreadCount", key = "#userId")
    public int markAllRead(Long userId) {
        int count = notificationRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read: userId={}", count, userId);
        return count;
    }

    /**
     * 알림 삭제 (Soft Delete)
     */
    @Transactional
    @CacheEvict(value = "unreadCount", key = "#userId")
    public void delete(Long userId, Long id) {
        Notification notification = findById(userId, id);
        notification.softDelete();
        log.info("Notification soft deleted: id={}, userId={}", id, userId);
    }

    /**
     * 미읽음 알림 개수 조회 (Redis 캐싱)
     */
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

    /**
     * Metadata Map을 JSON 문자열로 변환
     */
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

    /**
     * JSON 문자열을 Map으로 변환
     */
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
