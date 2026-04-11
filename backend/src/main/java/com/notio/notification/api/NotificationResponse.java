package com.notio.notification.api;

import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.OffsetDateTime;
import java.util.Map;

public record NotificationResponse(
        long id,
        NotificationSource source,
        String title,
        String body,
        NotificationPriority priority,
        boolean isRead,
        OffsetDateTime createdAt,
        String externalId,
        String externalUrl,
        Map<String, Object> metadata
) {

    public static NotificationResponse from(final Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getSource(),
                notification.getTitle(),
                notification.getBody(),
                notification.getPriority(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getExternalId(),
                notification.getExternalUrl(),
                notification.getMetadata()
        );
    }
}

