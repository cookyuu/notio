package com.notio.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.repository.NotificationRepository;
import com.notio.push.service.PushService;
import com.notio.webhook.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final PushService pushService;

    /**
     * Webhook 이벤트로부터 알림 생성
     */
    @Transactional
    @CacheEvict(value = "unreadCount", allEntries = true)
    public Notification saveFromEvent(NotificationEvent event) {
        Notification notification = Notification.builder()
            .source(event.source())
            .title(event.title())
            .body(event.body())
            .priority(event.priority())
            .externalId(event.externalId())
            .externalUrl(event.externalUrl())
            .metadata(convertMetadataToJson(event.metadata()))
            .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: id={}, source={}, title={}",
            saved.getId(), saved.getSource(), saved.getTitle());

        // 푸시 알림 발송 (동기 - Phase 0)
        try {
            pushService.sendPush(saved.getId());
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
    public Page<Notification> findAll(NotificationSource source, Boolean isRead, Pageable pageable) {
        return notificationRepository.findAllWithFilter(source, isRead, pageable);
    }

    /**
     * 알림 상세 조회
     */
    public Notification findById(Long id) {
        return notificationRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new NotioException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    /**
     * 알림을 읽음 상태로 변경
     */
    @Transactional
    @CacheEvict(value = "unreadCount", allEntries = true)
    public Notification markRead(Long id) {
        Notification notification = findById(id);
        notification.markAsRead();
        return notification;
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    @CacheEvict(value = "unreadCount", allEntries = true)
    public int markAllRead() {
        int count = notificationRepository.markAllAsRead();
        log.info("Marked {} notifications as read", count);
        return count;
    }

    /**
     * 알림 삭제 (Soft Delete)
     */
    @Transactional
    @CacheEvict(value = "unreadCount", allEntries = true)
    public void delete(Long id) {
        Notification notification = findById(id);
        notification.softDelete();
        log.info("Notification soft deleted: id={}", id);
    }

    /**
     * 미읽음 알림 개수 조회 (Redis 캐싱)
     */
    @Cacheable(value = "unreadCount", key = "'all'")
    public long countUnread() {
        return notificationRepository.countUnread();
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
