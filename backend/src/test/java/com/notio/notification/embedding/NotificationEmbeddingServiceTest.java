package com.notio.notification.embedding;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.ai.embedding.EmbeddingProvider;
import com.notio.common.config.properties.NotioRagProperties;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.repository.NotificationEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEmbeddingServiceTest {

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private NotificationEmbeddingRepository embeddingRepository;

    private NotificationEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new NotificationEmbeddingService(
                embeddingProvider,
                new NotificationEmbeddingInputBuilder(),
                embeddingRepository,
                new NotioRagProperties(5, 3)
        );
    }

    @Test
    void embedNotificationSkipsWhenSameContentHashExists() {
        final Notification notification = notification();
        final String contentHash = new NotificationEmbeddingInputBuilder().build(notification).contentHash();
        when(embeddingRepository.existsByNotificationIdAndContentHash(notification.getId(), contentHash)).thenReturn(true);

        embeddingService.embedNotification(notification);

        verify(embeddingProvider, never()).embed(org.mockito.ArgumentMatchers.anyString());
        verify(embeddingRepository, never()).save(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void embedNotificationSavesGeneratedEmbedding() {
        final Notification notification = notification();
        final NotificationEmbeddingInput input = new NotificationEmbeddingInputBuilder().build(notification);
        final float[] embedding = new float[] {0.1f, 0.2f, 0.3f};
        when(embeddingRepository.existsByNotificationIdAndContentHash(notification.getId(), input.contentHash())).thenReturn(false);
        when(embeddingProvider.embed(input.content())).thenReturn(embedding);

        embeddingService.embedNotification(notification);

        verify(embeddingProvider).embed(input.content());
        verify(embeddingRepository).save(notification, input.contentHash(), embedding);
    }

    private Notification notification() {
        return Notification.builder()
                .id(1L)
                .userId(10L)
                .source(NotificationSource.GITHUB)
                .title("PR review")
                .body("Review requested")
                .priority(NotificationPriority.HIGH)
                .metadata("{\"repository\":\"notio\"}")
                .build();
    }
}
