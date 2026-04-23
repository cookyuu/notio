package com.notio.notification.repository;

import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Soft Delete를 고려한 ID 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.deletedAt IS NULL AND n.userId = :userId AND n.id = :id")
    Optional<Notification> findByIdAndUserIdAndNotDeleted(
        @Param("userId") Long userId,
        @Param("id") Long id
    );

    /**
     * 필터링 조회 (source, isRead 필터 지원)
     * QueryDSL이 비활성화되어 있으므로 JPQL 사용
     */
    @Query("SELECT n FROM Notification n WHERE n.deletedAt IS NULL " +
           "AND n.userId = :userId " +
           "AND (:source IS NULL OR n.source = :source) " +
           "AND (:isRead IS NULL OR n.read = :isRead) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findAllWithFilter(
        @Param("userId") Long userId,
        @Param("source") NotificationSource source,
        @Param("isRead") Boolean isRead,
        Pageable pageable
    );

    @Query("SELECT n FROM Notification n WHERE n.deletedAt IS NULL " +
           "AND n.userId = :userId " +
           "AND n.createdAt >= :startInclusive " +
           "AND n.createdAt < :endExclusive " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findAllByUserIdAndCreatedAtRange(
        @Param("userId") Long userId,
        @Param("startInclusive") Instant startInclusive,
        @Param("endExclusive") Instant endExclusive,
        Pageable pageable
    );

    @Query("SELECT " +
           "n.id AS id, " +
           "n.source AS source, " +
           "n.title AS title, " +
           "n.priority AS priority, " +
           "n.read AS read, " +
           "n.createdAt AS createdAt, " +
           "n.body AS body " +
           "FROM Notification n WHERE n.deletedAt IS NULL " +
           "AND n.userId = :userId " +
           "AND (:source IS NULL OR n.source = :source) " +
           "AND (:isRead IS NULL OR n.read = :isRead) " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationSummaryProjection> findAllSummariesWithFilter(
        @Param("userId") Long userId,
        @Param("source") NotificationSource source,
        @Param("isRead") Boolean isRead,
        Pageable pageable
    );

    /**
     * 미읽음 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.deletedAt IS NULL AND n.userId = :userId AND n.read = false")
    long countUnread(@Param("userId") Long userId);

    /**
     * 전체 알림 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE n.deletedAt IS NULL AND n.userId = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId);

}
