package com.notio.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.notio.ai.rag.RagDocument;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void buildNotificationSummaryPromptIncludesSystemRules() {
        Notification notification = notification();
        LlmPrompt prompt = promptBuilder.buildNotificationSummaryPrompt(notification, List.of());

        assertThat(prompt.system())
            .contains("2~4문장")
            .contains("500자")
            .contains("마크다운");
    }

    @Test
    void buildNotificationSummaryPromptIncludesNotificationFields() {
        Notification notification = Notification.builder()
            .id(1L).userId(1L)
            .source(NotificationSource.GITHUB)
            .title("PR #123 merged")
            .body("The feature branch has been merged into main.")
            .priority(NotificationPriority.HIGH)
            .build();

        LlmPrompt prompt = promptBuilder.buildNotificationSummaryPrompt(notification, List.of());

        assertThat(prompt.user())
            .contains("GITHUB")
            .contains("PR #123 merged")
            .contains("HIGH")
            .contains("The feature branch has been merged into main.");
    }

    @Test
    void buildNotificationSummaryPromptIncludesExternalUrlWhenPresent() {
        Notification notification = Notification.builder()
            .id(1L).userId(1L)
            .source(NotificationSource.GITHUB)
            .title("PR opened")
            .body("body")
            .priority(NotificationPriority.MEDIUM)
            .externalUrl("https://github.com/pr/123")
            .build();

        LlmPrompt prompt = promptBuilder.buildNotificationSummaryPrompt(notification, List.of());

        assertThat(prompt.user()).contains("https://github.com/pr/123");
    }

    @Test
    void buildNotificationSummaryPromptIncludesRagContextWhenPresent() {
        Notification notification = notification();
        RagDocument doc1 = new RagDocument(2L, "GITHUB", "Similar PR", "summary1", "HIGH",
            Instant.parse("2026-05-01T10:00:00Z"), 0.93);
        RagDocument doc2 = new RagDocument(3L, "SLACK", "Alert", "summary2", "URGENT",
            Instant.parse("2026-05-02T10:00:00Z"), 0.87);

        LlmPrompt prompt = promptBuilder.buildNotificationSummaryPrompt(notification, List.of(doc1, doc2));

        assertThat(prompt.user())
            .contains("유사 과거 알림")
            .contains("Similar PR")
            .contains("0.93")
            .contains("Alert");
    }

    @Test
    void buildNotificationSummaryPromptLimitsRagContextToThreeDocuments() {
        Notification notification = notification();
        List<RagDocument> docs = List.of(
            ragDocument(10L, "Doc1", 0.99),
            ragDocument(11L, "Doc2", 0.95),
            ragDocument(12L, "Doc3", 0.90),
            ragDocument(13L, "Doc4", 0.85)
        );

        LlmPrompt prompt = promptBuilder.buildNotificationSummaryPrompt(notification, docs);

        assertThat(prompt.user())
            .contains("Doc1").contains("Doc2").contains("Doc3")
            .doesNotContain("Doc4");
    }

    @Test
    void buildNotificationSummaryPromptExcludesRagSectionWhenEmpty() {
        Notification notification = notification();

        LlmPrompt prompt = promptBuilder.buildNotificationSummaryPrompt(notification, List.of());

        assertThat(prompt.user()).doesNotContain("유사 과거 알림");
    }

    @Test
    void buildDigestSummaryPromptIncludesSystemRules() {
        List<Notification> notifications = List.of(notification());

        LlmPrompt prompt = promptBuilder.buildDigestSummaryPrompt(notifications);

        assertThat(prompt.system())
            .contains("첫 줄")
            .contains("1000자")
            .contains("마크다운 목록");
    }

    @Test
    void buildDigestSummaryPromptIncludesAllNotifications() {
        Notification n1 = Notification.builder()
            .id(1L).userId(1L).source(NotificationSource.GITHUB)
            .title("PR merged").body("Feature branch merged").priority(NotificationPriority.HIGH).build();
        Notification n2 = Notification.builder()
            .id(2L).userId(1L).source(NotificationSource.SLACK)
            .title("Alert").body("Server is down").priority(NotificationPriority.URGENT).build();

        LlmPrompt prompt = promptBuilder.buildDigestSummaryPrompt(List.of(n1, n2));

        assertThat(prompt.user())
            .contains("2개")
            .contains("PR merged")
            .contains("GITHUB")
            .contains("Alert")
            .contains("SLACK");
    }

    @Test
    void buildDigestSummaryPromptUsesAiSummaryWhenAvailable() {
        Notification notification = Notification.builder()
            .id(1L).userId(1L).source(NotificationSource.CLAUDE)
            .title("title").body("original body").priority(NotificationPriority.MEDIUM)
            .aiSummary("AI generated summary").build();

        LlmPrompt prompt = promptBuilder.buildDigestSummaryPrompt(List.of(notification));

        assertThat(prompt.user()).contains("AI generated summary");
        assertThat(prompt.user()).doesNotContain("original body");
    }

    @Test
    void buildDigestSummaryPromptTruncatesBodyAt300Characters() {
        String longBody = "x".repeat(400);
        Notification notification = Notification.builder()
            .id(1L).userId(1L).source(NotificationSource.GITHUB)
            .title("title").body(longBody).priority(NotificationPriority.LOW).build();

        LlmPrompt prompt = promptBuilder.buildDigestSummaryPrompt(List.of(notification));

        assertThat(prompt.user()).contains("x".repeat(300) + "...");
    }

    private Notification notification() {
        Notification n = Notification.builder()
            .id(1L).userId(1L)
            .source(NotificationSource.GITHUB)
            .title("Test notification")
            .body("Test body content")
            .priority(NotificationPriority.HIGH)
            .build();
        ReflectionTestUtils.setField(n, "createdAt", Instant.parse("2026-05-13T10:00:00Z"));
        return n;
    }

    private RagDocument ragDocument(Long id, String title, double score) {
        return new RagDocument(id, "GITHUB", title, "summary", "HIGH", Instant.now(), score);
    }
}
