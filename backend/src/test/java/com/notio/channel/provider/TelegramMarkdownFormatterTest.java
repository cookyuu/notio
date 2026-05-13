package com.notio.channel.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramMarkdownFormatterTest {

    private final TelegramMarkdownFormatter formatter = new TelegramMarkdownFormatter();

    @Test
    void escapeLeavesPlainTextUnchanged() {
        assertThat(formatter.escape("Hello World 123")).isEqualTo("Hello World 123");
    }

    @Test
    void escapeHandlesNullAsEmptyString() {
        assertThat(formatter.escape(null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(chars = {'_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'})
    void escapeAddsBackslashBeforeSpecialChar(char special) {
        String result = formatter.escape(String.valueOf(special));
        assertThat(result).isEqualTo("\\" + special);
    }

    @Test
    void escapeHandlesBackslashCharacter() {
        String result = formatter.escape("\\");
        assertThat(result).isEqualTo("\\\\");
    }

    @Test
    void escapeHandlesMixedText() {
        String result = formatter.escape("PR #42 [urgent]");
        assertThat(result).isEqualTo("PR \\#42 \\[urgent\\]");
    }

    @Test
    void formatIncludesTitleAsBold() {
        ChannelMessage message = new ChannelMessage(
            1L, "PR Review", "Check this PR", NotificationSource.GITHUB,
            NotificationPriority.HIGH, null, Instant.now()
        );

        String result = formatter.format(message);

        assertThat(result).startsWith("*PR Review*");
    }

    @Test
    void formatIncludesBodyAfterTitle() {
        ChannelMessage message = new ChannelMessage(
            1L, "title", "Please review changes", NotificationSource.GITHUB,
            NotificationPriority.MEDIUM, null, Instant.now()
        );

        String result = formatter.format(message);

        assertThat(result).contains("Please review changes");
    }

    @Test
    void formatIncludesSourceAndPriority() {
        ChannelMessage message = new ChannelMessage(
            1L, "title", "body", NotificationSource.SLACK,
            NotificationPriority.URGENT, null, Instant.now()
        );

        String result = formatter.format(message);

        assertThat(result).contains("SLACK").contains("URGENT");
    }

    @Test
    void formatIncludesLinkWhenExternalUrlIsPresent() {
        ChannelMessage message = new ChannelMessage(
            1L, "title", "body", NotificationSource.GITHUB,
            NotificationPriority.HIGH, "https://github.com/pr/1", Instant.now()
        );

        String result = formatter.format(message);

        assertThat(result).contains("[자세히 보기](https://github.com/pr/1)");
    }

    @Test
    void formatExcludesLinkWhenExternalUrlIsNull() {
        ChannelMessage message = new ChannelMessage(
            1L, "title", "body", NotificationSource.GITHUB,
            NotificationPriority.HIGH, null, Instant.now()
        );

        String result = formatter.format(message);

        assertThat(result).doesNotContain("자세히 보기");
    }

    @Test
    void formatExcludesLinkWhenExternalUrlIsBlank() {
        ChannelMessage message = new ChannelMessage(
            1L, "title", "body", NotificationSource.GITHUB,
            NotificationPriority.HIGH, "  ", Instant.now()
        );

        String result = formatter.format(message);

        assertThat(result).doesNotContain("자세히 보기");
    }

    @Test
    void formatEscapesSpecialCharactersInTitle() {
        ChannelMessage message = new ChannelMessage(
            1L, "PR [urgent] #42", "body", NotificationSource.GITHUB,
            NotificationPriority.HIGH, null, Instant.now()
        );

        String result = formatter.format(message);

        assertThat(result).contains("PR \\[urgent\\] \\#42");
    }
}
