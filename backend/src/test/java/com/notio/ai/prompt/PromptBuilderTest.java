package com.notio.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.notio.ai.rag.RagDocument;
import com.notio.chat.domain.ChatMessage;
import com.notio.chat.domain.ChatMessageRole;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void buildChatPromptIncludesSystemInstructionsRagContextAndHistory() {
        final RagDocument document = new RagDocument(
                10L,
                "GITHUB",
                "PR 리뷰 요청",
                "결제 모듈 변경에 대한 리뷰가 요청되었습니다.",
                "HIGH",
                Instant.parse("2026-04-22T01:00:00Z"),
                0.91321
        );
        final ChatMessage userMessage = chatMessage(
                1L,
                ChatMessageRole.USER,
                "오늘 중요한 알림 알려줘",
                OffsetDateTime.parse("2026-04-22T01:01:00Z")
        );
        final ChatMessage assistantMessage = chatMessage(
                2L,
                ChatMessageRole.ASSISTANT,
                "GitHub 리뷰 요청이 중요합니다.",
                OffsetDateTime.parse("2026-04-22T01:02:00Z")
        );

        final LlmPrompt prompt = promptBuilder.buildChatPrompt(
                "내가 먼저 처리할 일은?",
                List.of(document),
                List.of(assistantMessage, userMessage)
        );

        assertThat(prompt.system())
                .contains("개발자를 위한 알림 관리 AI 어시스턴트")
                .contains("기본 응답 언어는 한국어")
                .contains("컨텍스트에 없는 사실은 추측하거나 단정하지 마라");
        assertThat(prompt.user())
                .contains("source: GITHUB")
                .contains("title: PR 리뷰 요청")
                .contains("body_summary: 결제 모듈 변경에 대한 리뷰가 요청되었습니다.")
                .contains("priority: HIGH")
                .contains("created_at: 2026-04-22T01:00:00Z")
                .contains("similarity_score: 0.9132")
                .contains("- user: 오늘 중요한 알림 알려줘")
                .contains("- assistant: GitHub 리뷰 요청이 중요합니다.")
                .contains("답변은 한국어로 작성하고 4000자 이하로 제한하라")
                .contains("User:\n내가 먼저 처리할 일은?");
    }

    @Test
    void buildChatPromptIncludesFallbackInstructionWhenRagContextIsEmpty() {
        final LlmPrompt prompt = promptBuilder.buildChatPrompt(
                "관련 알림이 있어?",
                List.of(),
                List.of()
        );

        assertThat(prompt.user())
                .contains("관련 알림 컨텍스트 없음")
                .contains("현재 검색 가능한 관련 알림이 없다고 명확히 말하라")
                .contains("최근 대화 없음");
    }

    @Test
    void buildDailySummaryPromptIncludesNotificationsAndSummaryConstraints() {
        final Notification notification = Notification.builder()
                .id(20L)
                .userId(1L)
                .source(NotificationSource.SLACK)
                .title("장애 대응 채널 호출")
                .body("API 오류율이 상승해 온콜 확인이 필요합니다.")
                .priority(NotificationPriority.HIGH)
                .read(false)
                .build();
        ReflectionTestUtils.setField(notification, "createdAt", Instant.parse("2026-04-22T02:00:00Z"));

        final LlmPrompt prompt = promptBuilder.buildDailySummaryPrompt(
                LocalDate.of(2026, 4, 22),
                List.of(notification)
        );

        assertThat(prompt.user())
                .contains("2026-04-22에 수집된 알림 목록")
                .contains("전체 요약")
                .contains("중요한 알림")
                .contains("사용자가 바로 처리하면 좋은 항목")
                .contains("주요 topic")
                .contains("source: SLACK")
                .contains("title: 장애 대응 채널 호출")
                .contains("body_summary: API 오류율이 상승해 온콜 확인이 필요합니다.")
                .contains("priority: HIGH")
                .contains("created_at: 2026-04-22T02:00:00Z")
                .contains("summary 본문은 2000자 이하로 제한하라")
                .contains("제공된 알림 목록에 없는 사실은 추측하거나 단정하지 마라");
    }

    private ChatMessage chatMessage(
            final Long id,
            final ChatMessageRole role,
            final String content,
            final OffsetDateTime createdAt
    ) {
        final ChatMessage message = new ChatMessage(1L, role, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt.withOffsetSameInstant(ZoneOffset.UTC));
        ReflectionTestUtils.setField(message, "updatedAt", createdAt.withOffsetSameInstant(ZoneOffset.UTC));
        return message;
    }
}
