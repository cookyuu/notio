package com.notio.channel.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SlackBlockKitFormatterTest {

    private final SlackBlockKitFormatter formatter = new SlackBlockKitFormatter();

    @Test
    void formatSetsUrgentColor() {
        ChannelMessage message = message(NotificationPriority.URGENT);

        Map<String, Object> payload = formatter.format("C123", message);

        assertThat(attachmentColor(payload)).isEqualTo("#FF0000");
    }

    @Test
    void formatSetsHighColor() {
        ChannelMessage message = message(NotificationPriority.HIGH);

        Map<String, Object> payload = formatter.format("C123", message);

        assertThat(attachmentColor(payload)).isEqualTo("#FF8C00");
    }

    @Test
    void formatSetsMediumColor() {
        ChannelMessage message = message(NotificationPriority.MEDIUM);

        Map<String, Object> payload = formatter.format("C123", message);

        assertThat(attachmentColor(payload)).isEqualTo("#4A90E2");
    }

    @Test
    void formatSetsLowColor() {
        ChannelMessage message = message(NotificationPriority.LOW);

        Map<String, Object> payload = formatter.format("C123", message);

        assertThat(attachmentColor(payload)).isEqualTo("#9B9B9B");
    }

    @Test
    void formatIncludesChannelInPayload() {
        ChannelMessage message = message(NotificationPriority.MEDIUM);

        Map<String, Object> payload = formatter.format("C-GENERAL", message);

        assertThat(payload.get("channel")).isEqualTo("C-GENERAL");
    }

    @Test
    void formatEscapesHtmlSpecialCharactersInTitle() {
        ChannelMessage message = new ChannelMessage(
            1L, "<Alert> & 'special'", "body", NotificationSource.GITHUB,
            NotificationPriority.HIGH, null, Instant.now()
        );

        Map<String, Object> payload = formatter.format("C123", message);

        String titleText = titleText(payload);
        assertThat(titleText).contains("&lt;Alert&gt; &amp; 'special'");
    }

    @Test
    void formatIncludesSourceAndPriorityInContextBlock() {
        ChannelMessage message = new ChannelMessage(
            1L, "title", "body", NotificationSource.SLACK,
            NotificationPriority.HIGH, null, Instant.now()
        );

        Map<String, Object> payload = formatter.format("C123", message);

        String contextText = contextText(payload);
        assertThat(contextText).contains("SLACK").contains("HIGH");
    }

    @Test
    void formatIncludesExternalUrlInContextWhenPresent() {
        ChannelMessage message = new ChannelMessage(
            1L, "title", "body", NotificationSource.GITHUB,
            NotificationPriority.HIGH, "https://github.com/pr/1", Instant.now()
        );

        Map<String, Object> payload = formatter.format("C123", message);

        String contextText = contextText(payload);
        assertThat(contextText).contains("https://github.com/pr/1");
    }

    @Test
    void formatExcludesLinkWhenExternalUrlIsNull() {
        ChannelMessage message = new ChannelMessage(
            1L, "title", "body", NotificationSource.GITHUB,
            NotificationPriority.MEDIUM, null, Instant.now()
        );

        Map<String, Object> payload = formatter.format("C123", message);

        String contextText = contextText(payload);
        assertThat(contextText).doesNotContain("자세히 보기");
    }

    @Test
    void formatTruncatesBodyAt3000Characters() {
        String longBody = "a".repeat(4000);
        ChannelMessage message = new ChannelMessage(
            1L, "title", longBody, NotificationSource.GITHUB,
            NotificationPriority.MEDIUM, null, Instant.now()
        );

        Map<String, Object> payload = formatter.format("C123", message);

        String bodyText = bodyText(payload);
        assertThat(bodyText).hasSize(3000);
        assertThat(bodyText).endsWith("...");
    }

    @SuppressWarnings("unchecked")
    private String attachmentColor(Map<String, Object> payload) {
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) payload.get("attachments");
        return (String) attachments.get(0).get("color");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> blocks(Map<String, Object> payload) {
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) payload.get("attachments");
        return (List<Map<String, Object>>) attachments.get(0).get("blocks");
    }

    @SuppressWarnings("unchecked")
    private String titleText(Map<String, Object> payload) {
        Map<String, Object> titleBlock = blocks(payload).get(0);
        Map<String, Object> text = (Map<String, Object>) titleBlock.get("text");
        return (String) text.get("text");
    }

    @SuppressWarnings("unchecked")
    private String bodyText(Map<String, Object> payload) {
        Map<String, Object> bodyBlock = blocks(payload).get(1);
        Map<String, Object> text = (Map<String, Object>) bodyBlock.get("text");
        return (String) text.get("text");
    }

    @SuppressWarnings("unchecked")
    private String contextText(Map<String, Object> payload) {
        Map<String, Object> contextBlock = blocks(payload).get(2);
        List<Map<String, Object>> elements = (List<Map<String, Object>>) contextBlock.get("elements");
        return (String) elements.get(0).get("text");
    }

    private ChannelMessage message(NotificationPriority priority) {
        return new ChannelMessage(
            1L, "Test title", "Test body", NotificationSource.GITHUB,
            priority, null, Instant.now()
        );
    }
}
