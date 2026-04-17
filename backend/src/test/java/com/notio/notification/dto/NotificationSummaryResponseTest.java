package com.notio.notification.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationSummaryResponseTest {

    @Test
    void createBodyPreviewNormalizesWhitespaceWithoutTruncation() {
        assertThat(NotificationSummaryResponse.createBodyPreview(" line1\n\n line2\tline3 "))
                .isEqualTo("line1 line2 line3");
    }

    @Test
    void createBodyPreviewTruncatesLongBody() {
        final String body = "a".repeat(130);

        assertThat(NotificationSummaryResponse.createBodyPreview(body))
                .hasSize(120)
                .endsWith("...");
    }
}
