package com.notio.notification.service;

import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.ai.rag.RagDocument;
import com.notio.ai.rag.RagRetriever;
import com.notio.common.config.properties.NotioAiProperties;
import com.notio.notification.domain.Notification;
import com.notio.notification.metrics.NotificationFlowMetrics;
import com.notio.notification.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSummaryService {

    private final RagRetriever ragRetriever;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final NotificationRepository notificationRepository;
    private final NotioAiProperties aiProperties;
    private final NotificationFlowMetrics metrics;

    @Nullable
    @Transactional
    public String summarize(Notification notification) {
        if (!shouldSummarize(notification)) {
            log.debug("event=ai_summarization_skipped notification_id={} source={}",
                notification.getId(), notification.getSource());
            return null;
        }

        Instant start = Instant.now();
        try {
            List<RagDocument> context = ragRetriever.retrieve(
                notification.getUserId(),
                notification.getTitle() + " " + notification.getBody(),
                Optional.empty()
            );

            LlmPrompt prompt = promptBuilder.buildNotificationSummaryPrompt(notification, context);
            String summary = llmProvider.chat(prompt);

            notificationRepository.updateAiSummary(notification.getId(), summary);

            metrics.recordAiSummarization("success", Duration.between(start, Instant.now()));
            log.info("event=ai_summarization_success notification_id={} source={} summary_len={}",
                notification.getId(), notification.getSource(), summary.length());
            return summary;

        } catch (Exception e) {
            metrics.recordAiSummarization("failure", Duration.between(start, Instant.now()));
            log.warn("event=ai_summarization_failed notification_id={} source={} error={}",
                notification.getId(), notification.getSource(), e.getMessage(), e);
            return null;
        }
    }

    private boolean shouldSummarize(Notification notification) {
        List<String> configured = aiProperties.summarizeSources();
        if (configured == null || configured.isEmpty()) return true;
        return configured.contains(notification.getSource().name());
    }
}
