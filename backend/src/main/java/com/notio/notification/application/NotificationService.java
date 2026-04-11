package com.notio.notification.application;

import com.notio.common.error.ErrorCode;
import com.notio.common.error.NotioException;
import com.notio.notification.api.NotificationFilterRequest;
import com.notio.notification.domain.Notification;
import com.notio.notification.infrastructure.NotificationRepository;
import com.notio.push.application.PushService;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class NotificationService implements NotificationIngestionService {

    private static final String UNREAD_COUNT_CACHE_KEY = "notifications:unread-count";

    private final NotificationRepository notificationRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final PushService pushService;

    public NotificationService(
            final NotificationRepository notificationRepository,
            final StringRedisTemplate stringRedisTemplate,
            final PushService pushService
    ) {
        this.notificationRepository = notificationRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.pushService = pushService;
    }

    @Override
    public long saveFromEvent(final NotificationEvent event) {
        final Notification notification = new Notification(
                event.source(),
                event.title(),
                event.body(),
                event.priority(),
                event.externalId(),
                event.externalUrl(),
                event.metadata()
        );
        final Notification savedNotification = notificationRepository.save(notification);
        evictUnreadCountCache();
        pushService.sendPush(savedNotification.getId());
        return savedNotification.getId();
    }

    @Transactional
    public Page<Notification> findAll(final NotificationFilterRequest filter, final Pageable pageable) {
        return notificationRepository.findAll(buildSpecification(filter), pageable);
    }

    public Notification getDetailAndMarkRead(final long id) {
        final Notification notification = findById(id);
        if (!notification.isRead()) {
            notification.markRead();
            evictUnreadCountCache();
        }
        return notification;
    }

    public Notification markRead(final long id) {
        final Notification notification = findById(id);
        if (!notification.isRead()) {
            notification.markRead();
            evictUnreadCountCache();
        }
        return notification;
    }

    public long markAllRead() {
        final List<Notification> unreadNotifications = notificationRepository.findAll(
                Specification.where(notDeleted())
                        .and((root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("isRead")))
        );

        unreadNotifications.forEach(Notification::markRead);
        if (!unreadNotifications.isEmpty()) {
            evictUnreadCountCache();
        }
        return unreadNotifications.size();
    }

    public void delete(final long id) {
        final Notification notification = findById(id);
        notification.markDeleted();
        evictUnreadCountCache();
    }

    @Transactional
    public long countUnread() {
        try {
            final String cachedCount = stringRedisTemplate.opsForValue().get(UNREAD_COUNT_CACHE_KEY);
            if (cachedCount != null) {
                return Long.parseLong(cachedCount);
            }
        } catch (RuntimeException ignored) {
            return notificationRepository.countByIsReadFalseAndDeletedAtIsNull();
        }

        final long unreadCount = notificationRepository.countByIsReadFalseAndDeletedAtIsNull();
        try {
            stringRedisTemplate.opsForValue().set(
                    UNREAD_COUNT_CACHE_KEY,
                    Long.toString(unreadCount),
                    Duration.ofMinutes(10)
            );
        } catch (RuntimeException ignored) {
            return unreadCount;
        }
        return unreadCount;
    }

    @Transactional
    public Notification findById(final long id) {
        return notificationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotioException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    private Specification<Notification> buildSpecification(final NotificationFilterRequest filter) {
        Specification<Notification> specification = Specification.allOf(notDeleted());
        if (filter == null) {
            return specification;
        }
        if (filter.source() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("source"), filter.source()));
        }
        if (filter.priority() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("priority"), filter.priority()));
        }
        if (filter.isRead() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("isRead"), filter.isRead()));
        }
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            final String keywordPattern = "%" + filter.keyword().trim().toLowerCase() + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("title").as(String.class)), keywordPattern),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("body").as(String.class)), keywordPattern),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("externalId").as(String.class)), keywordPattern)
                    ));
        }
        return specification;
    }

    private Specification<Notification> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    private void evictUnreadCountCache() {
        try {
            stringRedisTemplate.delete(UNREAD_COUNT_CACHE_KEY);
        } catch (RuntimeException ignored) {
        }
    }
}
