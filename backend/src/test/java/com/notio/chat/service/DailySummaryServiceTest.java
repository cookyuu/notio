package com.notio.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.chat.dto.DailySummaryResponse;
import com.notio.common.exception.AiExceptionTranslator;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.service.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class DailySummaryServiceTest {

    @Test
    void getSummaryReturnsCachedResponseWhenCacheHit() {
        final NotificationService notificationService = mock(NotificationService.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final CacheManager cacheManager = mock(CacheManager.class);
        final AiExceptionTranslator aiExceptionTranslator = mock(AiExceptionTranslator.class);
        final Cache cache = mock(Cache.class);

        final DailySummaryService dailySummaryService = new DailySummaryService(
                notificationService,
                promptBuilder,
                llmProvider,
                cacheManager,
                aiExceptionTranslator
        );

        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final String cacheKey = "1:" + today;
        final DailySummaryResponse cachedResponse = new DailySummaryResponse(
                "캐시된 요약입니다.",
                today.toString(),
                5,
                List.of("GITHUB", "SLACK")
        );

        when(cacheManager.getCache("dailySummary")).thenReturn(cache);
        when(cache.get(cacheKey, DailySummaryResponse.class)).thenReturn(cachedResponse);

        final DailySummaryResponse response = dailySummaryService.getSummary();

        assertThat(response).isEqualTo(cachedResponse);
        verify(llmProvider, never()).chat(any(LlmPrompt.class));
        verify(notificationService, never()).findAll(any(), any(), any());
    }

    @Test
    void getSummaryGeneratesWithLlmAndCachesWhenCacheMiss() {
        final NotificationService notificationService = mock(NotificationService.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final CacheManager cacheManager = mock(CacheManager.class);
        final AiExceptionTranslator aiExceptionTranslator = new AiExceptionTranslator();
        final Cache cache = mock(Cache.class);

        final DailySummaryService dailySummaryService = new DailySummaryService(
                notificationService,
                promptBuilder,
                llmProvider,
                cacheManager,
                aiExceptionTranslator
        );

        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final String cacheKey = "1:" + today;
        final Instant todayInstant = today.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3600);

        final Notification notification1 = createNotification(
                1L,
                NotificationSource.GITHUB,
                "PR review requested",
                "PR 리뷰 요청",
                NotificationPriority.HIGH,
                todayInstant
        );
        final Notification notification2 = createNotification(
                2L,
                NotificationSource.SLACK,
                "New message",
                "새 메시지",
                NotificationPriority.MEDIUM,
                todayInstant.plusSeconds(1800)
        );

        final List<Notification> notifications = List.of(notification1, notification2);
        final Page<Notification> notificationPage = new PageImpl<>(notifications);
        final LlmPrompt prompt = new LlmPrompt("system", "user");
        final String llmSummary = "오늘은 GitHub PR 리뷰 요청과 Slack 메시지가 주요 이슈입니다.";

        when(cacheManager.getCache("dailySummary")).thenReturn(cache);
        when(cache.get(cacheKey, DailySummaryResponse.class)).thenReturn(null);
        when(notificationService.findAll(any(), any(), any(PageRequest.class))).thenReturn(notificationPage);
        when(promptBuilder.buildDailySummaryPrompt(any(LocalDate.class), any(List.class))).thenReturn(prompt);
        when(llmProvider.chat(prompt)).thenReturn(llmSummary);

        final DailySummaryResponse response = dailySummaryService.getSummary();

        assertThat(response.summary()).isEqualTo(llmSummary);
        assertThat(response.date()).isEqualTo(today.toString());
        assertThat(response.totalMessages()).isEqualTo(2);
        assertThat(response.topics()).containsExactlyInAnyOrder("GITHUB", "SLACK");

        verify(llmProvider).chat(prompt);
        verify(promptBuilder).buildDailySummaryPrompt(any(LocalDate.class), any(List.class));
        verify(cache).put(cacheKey, response);
    }

    @Test
    void getSummaryReturnsFallbackMessageWhenNoNotifications() {
        final NotificationService notificationService = mock(NotificationService.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final CacheManager cacheManager = mock(CacheManager.class);
        final AiExceptionTranslator aiExceptionTranslator = new AiExceptionTranslator();
        final Cache cache = mock(Cache.class);

        final DailySummaryService dailySummaryService = new DailySummaryService(
                notificationService,
                promptBuilder,
                llmProvider,
                cacheManager,
                aiExceptionTranslator
        );

        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final String cacheKey = "1:" + today;
        final Page<Notification> emptyPage = new PageImpl<>(List.of());

        when(cacheManager.getCache("dailySummary")).thenReturn(cache);
        when(cache.get(cacheKey, DailySummaryResponse.class)).thenReturn(null);
        when(notificationService.findAll(any(), any(), any(PageRequest.class))).thenReturn(emptyPage);

        final DailySummaryResponse response = dailySummaryService.getSummary();

        assertThat(response.summary()).isEqualTo("오늘은 아직 수집된 알림이 없습니다. 조용한 하루를 보내고 계시네요!");
        assertThat(response.date()).isEqualTo(today.toString());
        assertThat(response.totalMessages()).isZero();
        assertThat(response.topics()).isEmpty();

        verify(llmProvider, never()).chat(any(LlmPrompt.class));
        verify(cache).put(cacheKey, response);
    }

    @Test
    void getSummaryThrowsLlmUnavailableWhenLlmFails() {
        final NotificationService notificationService = mock(NotificationService.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final CacheManager cacheManager = mock(CacheManager.class);
        final AiExceptionTranslator aiExceptionTranslator = new AiExceptionTranslator();
        final Cache cache = mock(Cache.class);

        final DailySummaryService dailySummaryService = new DailySummaryService(
                notificationService,
                promptBuilder,
                llmProvider,
                cacheManager,
                aiExceptionTranslator
        );

        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final String cacheKey = "1:" + today;
        final Instant todayInstant = today.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3600);

        final Notification notification = createNotification(
                1L,
                NotificationSource.GITHUB,
                "PR review requested",
                "PR 리뷰 요청",
                NotificationPriority.HIGH,
                todayInstant
        );

        final List<Notification> notifications = List.of(notification);
        final Page<Notification> notificationPage = new PageImpl<>(notifications);
        final LlmPrompt prompt = new LlmPrompt("system", "user");

        when(cacheManager.getCache("dailySummary")).thenReturn(cache);
        when(cache.get(cacheKey, DailySummaryResponse.class)).thenReturn(null);
        when(notificationService.findAll(any(), any(), any(PageRequest.class))).thenReturn(notificationPage);
        when(promptBuilder.buildDailySummaryPrompt(any(LocalDate.class), any(List.class))).thenReturn(prompt);
        when(llmProvider.chat(prompt)).thenThrow(new RuntimeException("Ollama connection failed"));

        assertThatThrownBy(() -> dailySummaryService.getSummary())
                .isInstanceOf(NotioException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LLM_UNAVAILABLE);

        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void getSummaryThrowsLlmUnavailableWhenLlmReturnsEmptyResponse() {
        final NotificationService notificationService = mock(NotificationService.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final CacheManager cacheManager = mock(CacheManager.class);
        final AiExceptionTranslator aiExceptionTranslator = new AiExceptionTranslator();
        final Cache cache = mock(Cache.class);

        final DailySummaryService dailySummaryService = new DailySummaryService(
                notificationService,
                promptBuilder,
                llmProvider,
                cacheManager,
                aiExceptionTranslator
        );

        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final String cacheKey = "1:" + today;
        final Instant todayInstant = today.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3600);

        final Notification notification = createNotification(
                1L,
                NotificationSource.GITHUB,
                "PR review requested",
                "PR 리뷰 요청",
                NotificationPriority.HIGH,
                todayInstant
        );

        final List<Notification> notifications = List.of(notification);
        final Page<Notification> notificationPage = new PageImpl<>(notifications);
        final LlmPrompt prompt = new LlmPrompt("system", "user");

        when(cacheManager.getCache("dailySummary")).thenReturn(cache);
        when(cache.get(cacheKey, DailySummaryResponse.class)).thenReturn(null);
        when(notificationService.findAll(any(), any(), any(PageRequest.class))).thenReturn(notificationPage);
        when(promptBuilder.buildDailySummaryPrompt(any(LocalDate.class), any(List.class))).thenReturn(prompt);
        when(llmProvider.chat(prompt)).thenReturn("");

        assertThatThrownBy(() -> dailySummaryService.getSummary())
                .isInstanceOf(NotioException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LLM_UNAVAILABLE);

        verify(cache, never()).put(anyString(), any());
    }

    private Notification createNotification(
            final Long id,
            final NotificationSource source,
            final String title,
            final String body,
            final NotificationPriority priority,
            final Instant createdAt
    ) {
        final Notification notification = Notification.builder()
                .userId(1L)
                .source(source)
                .externalId("ext-" + id)
                .title(title)
                .body(body)
                .priority(priority)
                .externalUrl("https://example.com/" + id)
                .metadata("{}")
                .read(false)
                .build();

        // Set id and timestamps using reflection for testing
        org.springframework.test.util.ReflectionTestUtils.setField(notification, "id", id);
        org.springframework.test.util.ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        org.springframework.test.util.ReflectionTestUtils.setField(notification, "updatedAt", createdAt);

        return notification;
    }
}
