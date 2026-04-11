package com.notio.notification.api;

import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;

public record NotificationFilterRequest(
        NotificationSource source,
        NotificationPriority priority,
        Boolean isRead,
        String keyword
) {
}

