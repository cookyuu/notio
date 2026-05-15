package com.notio.analytics.repository;

import com.notio.analytics.domain.AiUsageDataPoint;
import com.notio.analytics.domain.AiUsageLog;
import com.notio.analytics.domain.ModelUsageDataPoint;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    // -------------------------------------------------------------------------
    // Native SQL — 기간별 트렌드 (daily / weekly / monthly)
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT
                to_char(session_at AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS periodLabel,
                COALESCE(SUM(input_tokens), 0)                        AS totalInput,
                COALESCE(SUM(output_tokens), 0)                       AS totalOutput,
                COUNT(*)                                               AS sessionCount
            FROM ai_usage_logs
            WHERE user_id = :userId
              AND session_at >= :since
              AND session_at  < :until
              AND deleted_at IS NULL
            GROUP BY to_char(session_at AT TIME ZONE 'UTC', 'YYYY-MM-DD')
            ORDER BY periodLabel
            """,
            nativeQuery = true)
    List<AiUsageDataPoint> findDailyInRange(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            @Param("until") Instant until
    );

    @Query(value = """
            SELECT
                to_char(session_at AT TIME ZONE 'UTC', 'IYYY-"W"IW') AS periodLabel,
                COALESCE(SUM(input_tokens), 0)                        AS totalInput,
                COALESCE(SUM(output_tokens), 0)                       AS totalOutput,
                COUNT(*)                                               AS sessionCount
            FROM ai_usage_logs
            WHERE user_id = :userId
              AND session_at >= :since
              AND session_at  < :until
              AND deleted_at IS NULL
            GROUP BY to_char(session_at AT TIME ZONE 'UTC', 'IYYY-"W"IW')
            ORDER BY periodLabel
            """,
            nativeQuery = true)
    List<AiUsageDataPoint> findWeeklyInRange(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            @Param("until") Instant until
    );

    @Query(value = """
            SELECT
                to_char(session_at AT TIME ZONE 'UTC', 'YYYY-MM') AS periodLabel,
                COALESCE(SUM(input_tokens), 0)                    AS totalInput,
                COALESCE(SUM(output_tokens), 0)                   AS totalOutput,
                COUNT(*)                                           AS sessionCount
            FROM ai_usage_logs
            WHERE user_id = :userId
              AND session_at >= :since
              AND session_at  < :until
              AND deleted_at IS NULL
            GROUP BY to_char(session_at AT TIME ZONE 'UTC', 'YYYY-MM')
            ORDER BY periodLabel
            """,
            nativeQuery = true)
    List<AiUsageDataPoint> findMonthlyInRange(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            @Param("until") Instant until
    );

    @Query(value = """
            SELECT
                model                              AS model,
                COALESCE(SUM(total_tokens), 0)     AS totalTokens,
                COUNT(*)                           AS sessionCount
            FROM ai_usage_logs
            WHERE user_id = :userId
              AND session_at >= :since
              AND session_at  < :until
              AND deleted_at IS NULL
            GROUP BY model
            ORDER BY totalTokens DESC
            """,
            nativeQuery = true)
    List<ModelUsageDataPoint> findModelDistributionInRange(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            @Param("until") Instant until
    );

    // -------------------------------------------------------------------------
    // JPQL — 집계 쿼리
    // -------------------------------------------------------------------------

    @Query("""
            SELECT COALESCE(SUM(a.inputTokens), 0)
            FROM AiUsageLog a
            WHERE a.userId = :userId
              AND a.sessionAt >= :since
              AND a.sessionAt < :until
              AND a.deletedAt IS NULL
            """)
    long sumInputTokensInRange(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            @Param("until") Instant until
    );

    @Query("""
            SELECT COALESCE(SUM(a.outputTokens), 0)
            FROM AiUsageLog a
            WHERE a.userId = :userId
              AND a.sessionAt >= :since
              AND a.sessionAt < :until
              AND a.deletedAt IS NULL
            """)
    long sumOutputTokensInRange(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            @Param("until") Instant until
    );

    @Query("""
            SELECT COUNT(a)
            FROM AiUsageLog a
            WHERE a.userId = :userId
              AND a.sessionAt >= :since
              AND a.sessionAt < :until
              AND a.deletedAt IS NULL
            """)
    long countSessionsInRange(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            @Param("until") Instant until
    );

    // -------------------------------------------------------------------------
    // 파생 메서드 — idempotency 중복 체크
    // -------------------------------------------------------------------------

    boolean existsByNotificationId(Long notificationId);
}
