package com.notio.ai.prompt;

import com.notio.ai.rag.RagDocument;
import com.notio.chat.domain.ChatMessage;
import com.notio.notification.domain.Notification;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    private static final int CHAT_RESPONSE_MAX_CHARS = 4_000;
    private static final int DAILY_SUMMARY_MAX_CHARS = 2_000;
    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int MAX_CONTEXT_TEXT_LENGTH = 500;
    private static final int MAX_DAILY_NOTIFICATIONS = 50;

    private static final String SYSTEM_PROMPT = """
            너는 개발자를 위한 알림 관리 AI 어시스턴트다.
            사용자의 알림 데이터를 근거로 요약, 우선순위 판단, 할일 후보 제안을 도와라.
            제공된 컨텍스트에 없는 사실은 추측하거나 단정하지 마라.
            기본 응답 언어는 한국어다.
            중요한 알림을 언급할 때는 source, title, priority 근거를 함께 제시하라.
            """;

    public LlmPrompt buildChatPrompt(
            final String userMessage,
            final List<RagDocument> documents,
            final List<ChatMessage> recentMessages
    ) {
        final String userPrompt = """
                다음 RAG context와 최근 대화 히스토리를 바탕으로 사용자 질문에 답하라.
                RAG context가 비어 있으면 현재 검색 가능한 관련 알림이 없다고 명확히 말하라.
                답변은 한국어로 작성하고 %d자 이하로 제한하라.

                RAG context:
                %s

                Recent conversation:
                %s

                User:
                %s
                """.formatted(
                CHAT_RESPONSE_MAX_CHARS,
                formatRagContext(documents),
                formatRecentMessages(recentMessages),
                normalize(userMessage)
        );

        return new LlmPrompt(SYSTEM_PROMPT, userPrompt);
    }

    public LlmPrompt buildDailySummaryPrompt(
            final LocalDate date,
            final List<Notification> notifications
    ) {
        final String userPrompt = """
                %s에 수집된 알림 목록을 바탕으로 다음을 요약하라.
                제공된 알림 목록에 없는 사실은 추측하거나 단정하지 마라.
                응답은 간결한 한국어로 작성하고 summary 본문은 %d자 이하로 제한하라.

                포함할 내용:
                1. 전체 요약
                2. 중요한 알림
                3. 사용자가 바로 처리하면 좋은 항목
                4. 주요 topic

                Today's notifications:
                %s
                """.formatted(
                date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                DAILY_SUMMARY_MAX_CHARS,
                formatDailyNotifications(notifications)
        );

        return new LlmPrompt(SYSTEM_PROMPT, userPrompt);
    }

    private String formatRagContext(final List<RagDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "- 관련 알림 컨텍스트 없음";
        }

        final StringBuilder builder = new StringBuilder();
        for (final RagDocument document : documents) {
            builder.append("- source: ").append(normalize(document.source())).append('\n')
                    .append("  title: ").append(normalize(document.title())).append('\n')
                    .append("  body_summary: ").append(truncate(document.bodySummary())).append('\n')
                    .append("  priority: ").append(normalize(document.priority())).append('\n')
                    .append("  created_at: ").append(document.createdAt()).append('\n')
                    .append("  similarity_score: ")
                    .append(String.format(Locale.ROOT, "%.4f", document.similarityScore()))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String formatRecentMessages(final List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "- 최근 대화 없음";
        }

        final List<ChatMessage> orderedMessages = messages.stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt)
                        .thenComparing(ChatMessage::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .skip(Math.max(0, messages.size() - MAX_HISTORY_MESSAGES))
                .toList();

        final StringBuilder builder = new StringBuilder();
        for (final ChatMessage message : orderedMessages) {
            builder.append("- ")
                    .append(message.getRole().name().toLowerCase(Locale.ROOT))
                    .append(": ")
                    .append(truncate(message.getContent()))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String formatDailyNotifications(final List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return "- 오늘 수집된 알림 없음";
        }

        final StringBuilder builder = new StringBuilder();
        notifications.stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_DAILY_NOTIFICATIONS)
                .forEach(notification -> builder
                        .append("- source: ").append(notification.getSource().name()).append('\n')
                        .append("  title: ").append(normalize(notification.getTitle())).append('\n')
                        .append("  body_summary: ").append(truncate(notification.getBody())).append('\n')
                        .append("  priority: ").append(notification.getPriority().name()).append('\n')
                        .append("  created_at: ").append(notification.getCreatedAt()).append('\n')
                        .append("  is_read: ").append(notification.isRead()).append('\n'));
        return builder.toString().trim();
    }

    private String truncate(final String value) {
        final String normalized = normalize(value);
        if (normalized.length() <= MAX_CONTEXT_TEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_CONTEXT_TEXT_LENGTH - 3) + "...";
    }

    private String normalize(final String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
