package com.notio.notification.embedding;

import com.notio.ai.embedding.EmbeddingProvider;
import com.notio.common.config.properties.NotioRagProperties;
import com.notio.notification.domain.Notification;
import com.notio.notification.repository.NotificationEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEmbeddingService {

    private final EmbeddingProvider embeddingProvider;
    private final NotificationEmbeddingInputBuilder inputBuilder;
    private final NotificationEmbeddingRepository embeddingRepository;
    private final NotioRagProperties ragProperties;

    public void embedNotification(final Notification notification) {
        final NotificationEmbeddingInput input = inputBuilder.build(notification);
        if (embeddingRepository.existsByNotificationIdAndContentHash(notification.getId(), input.contentHash())) {
            return;
        }

        final float[] embedding = embeddingProvider.embed(input.content());
        validateDimension(notification.getId(), embedding);
        embeddingRepository.save(notification, input.contentHash(), embedding);
    }

    private void validateDimension(final Long notificationId, final float[] embedding) {
        if (embedding.length != ragProperties.embeddingDimension()) {
            throw new IllegalStateException(
                    "Unexpected embedding dimension for notificationId=%d".formatted(notificationId)
            );
        }
    }
}
