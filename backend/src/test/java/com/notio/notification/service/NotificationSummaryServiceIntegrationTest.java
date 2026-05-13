package com.notio.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.ai.rag.RagRetriever;
import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.metrics.NotioMetrics;
import com.notio.common.metrics.NotioMetricsTagPolicy;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.metrics.NotificationFlowMetrics;
import com.notio.notification.repository.NotificationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSummaryServiceIntegrationTest {

    @Mock private RagRetriever ragRetriever;
    @Mock private LlmProvider llmProvider;
    @Mock private NotificationRepository notificationRepository;

    private NotificationSummaryService service;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        NotificationFlowMetrics metrics = new NotificationFlowMetrics(
            new NotioMetrics(meterRegistry, new NotioMetricsTagPolicy())
        );
        NotioAiProperties aiProperties = new NotioAiProperties(
            "ollama", Duration.ofSeconds(30), Duration.ofSeconds(10),
            Duration.ofSeconds(30), List.of("CLAUDE", "CODEX")
        );

        service = new NotificationSummaryService(
            ragRetriever, new PromptBuilder(), llmProvider, notificationRepository, aiProperties, metrics
        );
    }

    @Test
    void claudeSourceNotificationSummarySavedToDatabase() {
        Notification notification = Notification.builder()
            .id(1L).userId(10L)
            .source(NotificationSource.CLAUDE)
            .title("Claude Code 배포 완료")
            .body("새 버전이 성공적으로 배포되었습니다.")
            .priority(NotificationPriority.MEDIUM)
            .build();

        when(ragRetriever.retrieve(anyLong(), anyString(), any())).thenReturn(List.of());
        when(llmProvider.chat(any(LlmPrompt.class))).thenReturn("AI가 생성한 요약입니다.");

        String result = service.summarize(notification);

        assertThat(result).isEqualTo("AI가 생성한 요약입니다.");
        verify(notificationRepository).updateAiSummary(eq(1L), eq("AI가 생성한 요약입니다."));
    }

    @Test
    void githubSourceNotificationSkipsSummarization() {
        Notification notification = Notification.builder()
            .id(2L).userId(10L)
            .source(NotificationSource.GITHUB)
            .title("PR #456 opened")
            .body("A new pull request has been opened for review.")
            .priority(NotificationPriority.HIGH)
            .build();

        String result = service.summarize(notification);

        assertThat(result).isNull();
        verify(ragRetriever, never()).retrieve(anyLong(), anyString(), any());
        verify(llmProvider, never()).chat(any());
        verify(notificationRepository, never()).updateAiSummary(anyLong(), anyString());
    }

    @Test
    void slackSourceNotificationSkipsSummarization() {
        Notification notification = Notification.builder()
            .id(3L).userId(10L)
            .source(NotificationSource.SLACK)
            .title("채널 메시지")
            .body("팀 채널에서 새 메시지가 도착했습니다.")
            .priority(NotificationPriority.LOW)
            .build();

        String result = service.summarize(notification);

        assertThat(result).isNull();
        verify(notificationRepository, never()).updateAiSummary(anyLong(), anyString());
    }

    @Test
    void codexSourceNotificationSummarySavedToDatabase() {
        Notification notification = Notification.builder()
            .id(4L).userId(10L)
            .source(NotificationSource.CODEX)
            .title("Codex 분석 완료")
            .body("코드 분석 결과가 준비되었습니다.")
            .priority(NotificationPriority.MEDIUM)
            .build();

        when(ragRetriever.retrieve(anyLong(), anyString(), any())).thenReturn(List.of());
        when(llmProvider.chat(any(LlmPrompt.class))).thenReturn("Codex 분석 요약");

        String result = service.summarize(notification);

        assertThat(result).isEqualTo("Codex 분석 요약");
        verify(notificationRepository).updateAiSummary(eq(4L), eq("Codex 분석 요약"));
    }
}
