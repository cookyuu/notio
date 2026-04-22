package com.notio.notification.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import org.junit.jupiter.api.Test;

class NotificationEmbeddingInputBuilderTest {

    private final NotificationEmbeddingInputBuilder inputBuilder = new NotificationEmbeddingInputBuilder();

    @Test
    void buildUsesTitleBodyAndSelectedMetadata() {
        final Notification notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .source(NotificationSource.GITHUB)
                .title("PR review requested")
                .body("Review backend changes")
                .priority(NotificationPriority.HIGH)
                .metadata("{\"repository\":\"notio\",\"sender\":\"github\"}")
                .build();

        final NotificationEmbeddingInput input = inputBuilder.build(notification);

        assertThat(input.content()).contains(
                "source: GITHUB",
                "priority: HIGH",
                "title: PR review requested",
                "body: Review backend changes",
                "metadata: {\"repository\":\"notio\",\"sender\":\"github\"}"
        );
        assertThat(input.contentHash()).hasSize(64);
    }

    @Test
    void buildCreatesStableHashForSameContent() {
        final Notification notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .source(NotificationSource.SLACK)
                .title("Deploy")
                .body("Deploy finished")
                .priority(NotificationPriority.MEDIUM)
                .metadata("{\"channel\":\"dev\"}")
                .build();

        final NotificationEmbeddingInput first = inputBuilder.build(notification);
        final NotificationEmbeddingInput second = inputBuilder.build(notification);

        assertThat(first.contentHash()).isEqualTo(second.contentHash());
    }
}
