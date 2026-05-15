package com.notio.analytics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.analytics.domain.AiUsageDataPoint;
import com.notio.analytics.domain.AiUsageLog;
import com.notio.analytics.domain.ModelUsageDataPoint;
import com.notio.analytics.dto.AiUsageGranularity;
import com.notio.analytics.dto.AiUsageResponse;
import com.notio.analytics.dto.AiUsageResponse.AiUsageModelShare;
import com.notio.analytics.dto.AiUsageResponse.AiUsagePeriodPoint;
import com.notio.analytics.repository.AiUsageLogRepository;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiUsageLogService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final AiUsageLogRepository aiUsageLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Notification 저장 후 AI 토큰 사용량을 ai_usage_logs에 기록한다.
     *
     * <p>다음 케이스에서는 아무것도 기록하지 않고 즉시 반환한다:
     * <ul>
     *   <li>source가 CLAUDE가 아닌 경우</li>
     *   <li>동일 notificationId가 이미 존재하는 경우 (idempotency)</li>
     *   <li>input_tokens + output_tokens 모두 0인 경우 (fallback 페이로드)</li>
     *   <li>metadata JSON 파싱 실패</li>
     * </ul>
     *
     * @param saved 저장된 Notification 엔티티
     */
    @Transactional
    public void logFromNotification(final Notification saved) {
        if (saved.getSource() != NotificationSource.CLAUDE) {
            log.debug(
                    "event=ai_usage_log_skip reason=non_claude_source notification_id={} source={}",
                    saved.getId(), saved.getSource()
            );
            return;
        }

        if (aiUsageLogRepository.existsByNotificationId(saved.getId())) {
            log.info(
                    "event=ai_usage_log_skip reason=duplicate_notification_id notification_id={} user_id={}",
                    saved.getId(), saved.getUserId()
            );
            return;
        }

        final Map<String, Object> metadata;
        try {
            metadata = parseMetadata(saved.getMetadata());
        } catch (final Exception e) {
            log.warn(
                    "event=ai_usage_log_skip reason=metadata_parse_failed notification_id={} user_id={} exception_type={}",
                    saved.getId(), saved.getUserId(), e.getClass().getSimpleName()
            );
            return;
        }

        if (metadata == null) {
            log.info(
                    "event=ai_usage_log_skip reason=empty_metadata notification_id={} user_id={}",
                    saved.getId(), saved.getUserId()
            );
            return;
        }

        final long inputTokens = extractLong(metadata, "usage", "input_tokens");
        final long outputTokens = extractLong(metadata, "usage", "output_tokens");
        final String model = extractModel(metadata);

        if (inputTokens == 0 && outputTokens == 0) {
            log.info(
                    "event=ai_usage_log_skip reason=zero_tokens notification_id={} user_id={}",
                    saved.getId(), saved.getUserId()
            );
            return;
        }

        final Instant sessionAt = resolveSessionAt(metadata, saved);

        final AiUsageLog aiUsageLog = AiUsageLog.builder()
                .userId(saved.getUserId())
                .notificationId(saved.getId())
                .model(model != null ? model : "unknown")
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .sessionAt(sessionAt)
                .build();

        aiUsageLogRepository.save(aiUsageLog);

        log.info(
                "event=ai_usage_log_created notification_id={} user_id={} model={} input_tokens={} output_tokens={} session_at={}",
                saved.getId(), saved.getUserId(), aiUsageLog.getModel(),
                inputTokens, outputTokens, sessionAt
        );
    }

    /**
     * 사용자의 AI 토큰 사용량 통계를 조회한다.
     *
     * @param userId      조회 대상 사용자 ID
     * @param granularity 집계 단위 (DAILY / WEEKLY / MONTHLY)
     * @param startDate   조회 시작일 (null이면 granularity.defaultDays() 기준 자동 설정)
     * @param endDate     조회 종료일 (null이면 오늘 기준 자동 설정)
     * @return AI 사용량 응답 DTO
     */
    public AiUsageResponse getAiUsage(
            final Long userId,
            final AiUsageGranularity granularity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        final LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (endDate == null) {
            endDate = today;
        }
        if (startDate == null) {
            startDate = endDate.minusDays(granularity.defaultDays() - 1L);
        }

        if (startDate.isAfter(endDate)) {
            throw new NotioException(
                    ErrorCode.INVALID_REQUEST,
                    "startDate는 endDate보다 이전이어야 합니다."
            );
        }

        final long daysBetween = endDate.toEpochDay() - startDate.toEpochDay();
        if (daysBetween > granularity.maxDays()) {
            throw new NotioException(
                    ErrorCode.INVALID_REQUEST,
                    "조회 범위가 " + granularity.name() + " 단위 최대값(" + granularity.maxDays() + "일)을 초과했습니다."
            );
        }

        final Instant since = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant until = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        final long totalInputTokens = aiUsageLogRepository.sumInputTokensInRange(userId, since, until);
        final long totalOutputTokens = aiUsageLogRepository.sumOutputTokensInRange(userId, since, until);
        final long totalSessions = aiUsageLogRepository.countSessionsInRange(userId, since, until);

        final List<AiUsagePeriodPoint> trend = buildTrend(userId, granularity, since, until);

        final List<ModelUsageDataPoint> modelDistributionRaw =
                aiUsageLogRepository.findModelDistributionInRange(userId, since, until);

        final List<AiUsageModelShare> modelDistribution = modelDistributionRaw.stream()
                .map(p -> new AiUsageModelShare(p.getModel(), p.getTotalTokens(), p.getSessionCount()))
                .toList();

        final String mostUsedModel = modelDistributionRaw.isEmpty()
                ? null
                : modelDistributionRaw.get(0).getModel();

        return new AiUsageResponse(
                granularity,
                startDate,
                endDate,
                totalInputTokens,
                totalOutputTokens,
                totalSessions,
                mostUsedModel,
                trend,
                modelDistribution
        );
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    private List<AiUsagePeriodPoint> buildTrend(
            final Long userId,
            final AiUsageGranularity granularity,
            final Instant since,
            final Instant until
    ) {
        final List<AiUsageDataPoint> raw = switch (granularity) {
            case DAILY -> aiUsageLogRepository.findDailyInRange(userId, since, until);
            case WEEKLY -> aiUsageLogRepository.findWeeklyInRange(userId, since, until);
            case MONTHLY -> aiUsageLogRepository.findMonthlyInRange(userId, since, until);
        };

        return raw.stream()
                .map(p -> new AiUsagePeriodPoint(
                        p.getPeriodLabel(),
                        p.getTotalInput(),
                        p.getTotalOutput(),
                        p.getSessionCount()
                ))
                .toList();
    }

    private Map<String, Object> parseMetadata(final String json) throws Exception {
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readValue(json, MAP_TYPE);
    }

    /**
     * metadata → usage 맵에서 숫자 값을 추출한다.
     * 키가 없거나 타입 불일치면 0을 반환한다.
     */
    @SuppressWarnings("unchecked")
    private long extractLong(final Map<String, Object> metadata, final String sectionKey, final String fieldKey) {
        final Object section = metadata.get(sectionKey);
        if (!(section instanceof Map<?, ?> sectionMap)) {
            return 0L;
        }
        final Object value = sectionMap.get(fieldKey);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    /**
     * metadata → model 필드 추출.
     * 최상위 "model" 키를 우선 조회하며, 없으면 null을 반환한다.
     */
    private String extractModel(final Map<String, Object> metadata) {
        final Object value = metadata.get("model");
        return value instanceof String s ? s : null;
    }

    /**
     * session_at 결정 로직.
     * metadata의 "timestamp" 필드를 ISO-8601로 파싱하며, 실패 시 notification.createdAt으로 fallback한다.
     */
    private Instant resolveSessionAt(final Map<String, Object> metadata, final Notification notification) {
        final Object timestampValue = metadata.get("timestamp");
        if (timestampValue instanceof String timestampStr) {
            try {
                return Instant.parse(timestampStr);
            } catch (final DateTimeParseException e) {
                log.debug(
                        "event=session_at_parse_failed notification_id={} timestamp_value={} fallback=created_at",
                        notification.getId(), timestampStr
                );
            }
        }
        return notification.getCreatedAt();
    }
}
