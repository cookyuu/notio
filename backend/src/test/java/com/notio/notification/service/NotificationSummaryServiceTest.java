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
import com.notio.ai.metrics.LlmMetrics;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.ai.rag.RagDocument;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSummaryServiceTest {

    @Mock private RagRetriever ragRetriever;
    @Mock private PromptBuilder promptBuilder;
    @Mock private LlmProvider llmProvider;
    @Mock private NotificationRepository notificationRepository;

    private NotificationFlowMetrics metrics;
    private NotificationSummaryService service;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        metrics = new NotificationFlowMetrics(new NotioMetrics(meterRegistry, new NotioMetricsTagPolicy()));

        NotioAiProperties aiProperties = new NotioAiProperties(
            "ollama", Duration.ofSeconds(30), Duration.ofSeconds(10),
            Duration.ofSeconds(30), List.of("CLAUDE", "CODEX")
        );

        service = new NotificationSummaryService(
            ragRetriever, promptBuilder, llmProvider, notificationRepository, aiProperties, metrics
        );
    }

    @Test
    void shouldSummarizeReturnsTrueWhenSourceIsInConfiguredList() {
        Notification notification = notification(NotificationSource.CLAUDE);

        when(ragRetriever.retrieve(anyLong(), anyString(), any())).thenReturn(List.of());
        when(promptBuilder.buildNotificationSummaryPrompt(any(), any()))
            .thenReturn(new LlmPrompt("system", "user"));
        when(llmProvider.chat(any())).thenReturn("AI summary");

        String result = service.summarize(notification);

        assertThat(result).isEqualTo("AI summary");
        verify(ragRetriever).retrieve(anyLong(), anyString(), eq(Optional.empty()));
    }

    @Test
    void shouldSummarizeReturnsTrueWhenSourceIsCodex() {
        Notification notification = notification(NotificationSource.CODEX);

        when(ragRetriever.retrieve(anyLong(), anyString(), any())).thenReturn(List.of());
        when(promptBuilder.buildNotificationSummaryPrompt(any(), any()))
            .thenReturn(new LlmPrompt("system", "user"));
        when(llmProvider.chat(any())).thenReturn("Codex summary");

        String result = service.summarize(notification);

        assertThat(result).isEqualTo("Codex summary");
    }

    @Test
    void shouldSummarizeReturnsFalseWhenSourceIsGithub() {
        Notification notification = notification(NotificationSource.GITHUB);

        String result = service.summarize(notification);

        assertThat(result).isNull();
        verify(ragRetriever, never()).retrieve(anyLong(), anyString(), any());
        verify(llmProvider, never()).chat(any());
    }

    @Test
    void shouldSummarizeReturnsFalseWhenSourceIsSlack() {
        Notification notification = notification(NotificationSource.SLACK);

        String result = service.summarize(notification);

        assertThat(result).isNull();
        verify(ragRetriever, never()).retrieve(anyLong(), anyString(), any());
    }

    @Test
    void summarizeReturnsNullAndDoesNotPropagateWhenLlmFails() {
        Notification notification = notification(NotificationSource.CLAUDE);

        when(ragRetriever.retrieve(anyLong(), anyString(), any())).thenReturn(List.of());
        when(promptBuilder.buildNotificationSummaryPrompt(any(), any()))
            .thenReturn(new LlmPrompt("system", "user"));
        when(llmProvider.chat(any())).thenThrow(new RuntimeException("LLM timeout"));

        String result = service.summarize(notification);

        assertThat(result).isNull();
    }

    @Test
    void summarizeReturnsNullAndDoesNotPropagateWhenRagFails() {
        Notification notification = notification(NotificationSource.CLAUDE);

        when(ragRetriever.retrieve(anyLong(), anyString(), any()))
            .thenThrow(new RuntimeException("RAG failure"));

        String result = service.summarize(notification);

        assertThat(result).isNull();
        verify(llmProvider, never()).chat(any());
    }

    @Test
    void summarizeCallsUpdateAiSummaryOnSuccess() {
        Notification notification = notification(NotificationSource.CLAUDE);

        when(ragRetriever.retrieve(anyLong(), anyString(), any())).thenReturn(List.of());
        when(promptBuilder.buildNotificationSummaryPrompt(any(), any()))
            .thenReturn(new LlmPrompt("system", "user"));
        when(llmProvider.chat(any())).thenReturn("Generated summary");

        service.summarize(notification);

        verify(notificationRepository).updateAiSummary(eq(1L), eq("Generated summary"));
    }

    @Test
    void summarizePassesRagContextToPromptBuilder() {
        Notification notification = notification(NotificationSource.CLAUDE);
        List<RagDocument> ragDocs = List.of(
            new RagDocument(2L, "GITHUB", "Similar PR", "body", "HIGH",
                java.time.Instant.now(), 0.95)
        );

        when(ragRetriever.retrieve(anyLong(), anyString(), any())).thenReturn(ragDocs);
        when(promptBuilder.buildNotificationSummaryPrompt(any(), any()))
            .thenReturn(new LlmPrompt("system", "user"));
        when(llmProvider.chat(any())).thenReturn("Summary");

        service.summarize(notification);

        verify(promptBuilder).buildNotificationSummaryPrompt(eq(notification), eq(ragDocs));
    }

    private Notification notification(NotificationSource source) {
        return Notification.builder()
            .id(1L).userId(10L).source(source)
            .title("Test notification").body("Test body")
            .priority(NotificationPriority.HIGH).build();
    }
}
