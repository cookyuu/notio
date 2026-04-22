package com.notio.chat.service;

import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.chat.dto.DailySummaryResponse;
import com.notio.common.exception.AiExceptionTranslator;
import com.notio.notification.domain.Notification;
import com.notio.notification.service.NotificationService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class DailySummaryService {

    private static final Logger logger = LoggerFactory.getLogger(DailySummaryService.class);
    private static final Long DEFAULT_PHASE0_USER_ID = 1L;
    private static final String CACHE_NAME = "dailySummary";

    private final NotificationService notificationService;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final CacheManager cacheManager;
    private final AiExceptionTranslator aiExceptionTranslator;

    public DailySummaryService(
            final NotificationService notificationService,
            final PromptBuilder promptBuilder,
            final LlmProvider llmProvider,
            final CacheManager cacheManager,
            final AiExceptionTranslator aiExceptionTranslator
    ) {
        this.notificationService = notificationService;
        this.promptBuilder = promptBuilder;
        this.llmProvider = llmProvider;
        this.cacheManager = cacheManager;
        this.aiExceptionTranslator = aiExceptionTranslator;
    }

    public DailySummaryResponse getSummary() {
        final Long userId = DEFAULT_PHASE0_USER_ID;
        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final String cacheKey = buildCacheKey(userId, today);

        // 1. 캐시 확인
        final Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            final DailySummaryResponse cached = cache.get(cacheKey, DailySummaryResponse.class);
            if (cached != null) {
                logger.debug("Cache hit for daily summary: userId={}, date={}", userId, today);
                return cached;
            }
        }

        // 2. 오늘 알림 조회
        final List<Notification> notifications = notificationService.findAll(
                null, // source
                null, // isRead
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        final OffsetDateTime startOfDay = today.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        final List<Notification> todaysNotifications = notifications.stream()
                .filter(notification -> notification.getCreatedAt().atZone(ZoneOffset.UTC).toOffsetDateTime().isAfter(startOfDay))
                .toList();

        // 3. topics 추출 (기존 규칙 기반 유지)
        final List<String> topics = extractTopics(todaysNotifications);

        // 4. LLM으로 summary 생성
        final String summary = generateSummaryWithLlm(today, todaysNotifications);

        // 5. 응답 생성
        final DailySummaryResponse response = new DailySummaryResponse(
                summary,
                today.toString(),
                todaysNotifications.size(),
                topics
        );

        // 6. 캐시 저장
        if (cache != null) {
            cache.put(cacheKey, response);
            logger.debug("Cache stored for daily summary: userId={}, date={}", userId, today);
        }

        return response;
    }

    private String buildCacheKey(final Long userId, final LocalDate date) {
        return userId + ":" + date;
    }

    private List<String> extractTopics(final List<Notification> notifications) {
        final Map<String, Long> sourceFrequency = notifications.stream()
                .collect(Collectors.groupingBy(
                        notification -> notification.getSource().name(),
                        Collectors.counting()
                ));

        return sourceFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String generateSummaryWithLlm(final LocalDate date, final List<Notification> todaysNotifications) {
        if (todaysNotifications.isEmpty()) {
            return "오늘은 아직 수집된 알림이 없습니다. 조용한 하루를 보내고 계시네요!";
        }

        try {
            final LlmPrompt prompt = promptBuilder.buildDailySummaryPrompt(date, todaysNotifications);
            final String summary = llmProvider.chat(prompt);

            if (summary == null || summary.isBlank()) {
                logger.warn("LLM returned empty summary for date={}, using fallback", date);
                throw aiExceptionTranslator.llmUnavailable(new IllegalStateException("Empty LLM response"));
            }

            return summary;
        } catch (final Exception e) {
            logger.error("Failed to generate daily summary with LLM for date={}", date, e);
            throw aiExceptionTranslator.llmUnavailable(e);
        }
    }
}

