package com.notio.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.analytics.domain.AiUsageLog;
import com.notio.analytics.dto.AiUsageGranularity;
import com.notio.analytics.dto.AiUsageResponse;
import com.notio.analytics.repository.AiUsageLogRepository;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiUsageLogServiceTest {

    @Mock
    private AiUsageLogRepository aiUsageLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiUsageLogService createService() {
        return new AiUsageLogService(aiUsageLogRepository, objectMapper);
    }

    // -------------------------------------------------------------------------
    // logFromNotification
    // -------------------------------------------------------------------------

    @Test
    void logFromNotificationSavesAiUsageLogWhenClaudeSourceWithValidMetadata() throws Exception {
        final AiUsageLogService service = createService();
        final String metadata = """
                {"model":"claude-sonnet-4-6","usage":{"input_tokens":100,"output_tokens":50},"timestamp":"2026-05-15T12:00:00Z"}
                """.trim();
        final Notification notification = notification(1L, NotificationSource.CLAUDE, metadata);
        when(aiUsageLogRepository.existsByNotificationId(notification.getId())).thenReturn(false);

        service.logFromNotification(notification);

        final ArgumentCaptor<AiUsageLog> captor = ArgumentCaptor.forClass(AiUsageLog.class);
        verify(aiUsageLogRepository).save(captor.capture());

        final AiUsageLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getNotificationId()).isNull();
        assertThat(saved.getModel()).isEqualTo("claude-sonnet-4-6");
        assertThat(saved.getInputTokens()).isEqualTo(100L);
        assertThat(saved.getOutputTokens()).isEqualTo(50L);
    }

    @Test
    void logFromNotificationSkipsWhenSourceIsNotClaude() {
        final AiUsageLogService service = createService();
        final Notification notification = notification(1L, NotificationSource.GITHUB, "{}");

        service.logFromNotification(notification);

        verify(aiUsageLogRepository, never()).save(any());
    }

    @Test
    void logFromNotificationSkipsWhenNotificationIdAlreadyExists() {
        final AiUsageLogService service = createService();
        final String metadata = """
                {"model":"claude-sonnet-4-6","usage":{"input_tokens":100,"output_tokens":50}}
                """.trim();
        final Notification notification = notification(1L, NotificationSource.CLAUDE, metadata);
        when(aiUsageLogRepository.existsByNotificationId(notification.getId())).thenReturn(true);

        service.logFromNotification(notification);

        verify(aiUsageLogRepository, never()).save(any());
    }

    @Test
    void logFromNotificationSkipsWhenBothTokensAreZero() {
        final AiUsageLogService service = createService();
        final String metadata = """
                {"model":"claude-sonnet-4-6","usage":{"input_tokens":0,"output_tokens":0}}
                """.trim();
        final Notification notification = notification(1L, NotificationSource.CLAUDE, metadata);
        when(aiUsageLogRepository.existsByNotificationId(notification.getId())).thenReturn(false);

        service.logFromNotification(notification);

        verify(aiUsageLogRepository, never()).save(any());
    }

    @Test
    void logFromNotificationSkipsWhenMetadataParsingFails() throws Exception {
        final ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        final AiUsageLogService service = new AiUsageLogService(aiUsageLogRepository, failingMapper);

        final Notification notification = notification(1L, NotificationSource.CLAUDE, "{invalid-json}");
        when(aiUsageLogRepository.existsByNotificationId(notification.getId())).thenReturn(false);
        when(failingMapper.readValue(anyString(), any(TypeReference.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "parse error"));

        service.logFromNotification(notification);

        verify(aiUsageLogRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getAiUsage
    // -------------------------------------------------------------------------

    @Test
    void getAiUsageThrowsInvalidRequestWhenDateRangeExceedsDailyMax() {
        final AiUsageLogService service = createService();
        final LocalDate startDate = LocalDate.of(2026, 1, 1);
        final LocalDate endDate = LocalDate.of(2026, 12, 31);

        assertThatThrownBy(() -> service.getAiUsage(1L, AiUsageGranularity.DAILY, startDate, endDate))
                .isInstanceOf(NotioException.class)
                .satisfies(ex -> assertThat(((NotioException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void getAiUsageThrowsInvalidRequestWhenStartDateIsAfterEndDate() {
        final AiUsageLogService service = createService();
        final LocalDate startDate = LocalDate.of(2026, 5, 10);
        final LocalDate endDate = LocalDate.of(2026, 5, 1);

        assertThatThrownBy(() -> service.getAiUsage(1L, AiUsageGranularity.DAILY, startDate, endDate))
                .isInstanceOf(NotioException.class)
                .satisfies(ex -> assertThat(((NotioException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void getAiUsageAppliesDefaultDatesWhenBothParamsAreNull() {
        final AiUsageLogService service = createService();
        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final Instant expectedSince = today.minusDays(6L).atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant expectedUntil = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        when(aiUsageLogRepository.sumInputTokensInRange(any(), any(), any())).thenReturn(0L);
        when(aiUsageLogRepository.sumOutputTokensInRange(any(), any(), any())).thenReturn(0L);
        when(aiUsageLogRepository.countSessionsInRange(any(), any(), any())).thenReturn(0L);
        when(aiUsageLogRepository.findDailyInRange(any(), any(), any())).thenReturn(List.of());
        when(aiUsageLogRepository.findModelDistributionInRange(any(), any(), any())).thenReturn(List.of());

        final AiUsageResponse response = service.getAiUsage(1L, AiUsageGranularity.DAILY, null, null);

        assertThat(response.startDate()).isEqualTo(today.minusDays(6L));
        assertThat(response.endDate()).isEqualTo(today);

        verify(aiUsageLogRepository).sumInputTokensInRange(1L, expectedSince, expectedUntil);
        verify(aiUsageLogRepository).sumOutputTokensInRange(1L, expectedSince, expectedUntil);
        verify(aiUsageLogRepository).countSessionsInRange(1L, expectedSince, expectedUntil);
        verify(aiUsageLogRepository).findDailyInRange(1L, expectedSince, expectedUntil);
        verify(aiUsageLogRepository).findModelDistributionInRange(1L, expectedSince, expectedUntil);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Notification notification(
            final Long userId,
            final NotificationSource source,
            final String metadata
    ) {
        return Notification.builder()
                .userId(userId)
                .source(source)
                .title("title")
                .body("body")
                .priority(NotificationPriority.MEDIUM)
                .read(false)
                .metadata(metadata)
                .createdAt(Instant.parse("2026-05-15T12:00:00Z"))
                .updatedAt(Instant.parse("2026-05-15T12:00:00Z"))
                .build();
    }
}
