package com.notio.channel.provider;

import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;

import java.time.Instant;

public record ChannelMessage(
    Long notificationId,
    String title,
    String body,
    NotificationSource source,
    NotificationPriority priority,
    String externalUrl,
    Instant notifiedAt
) {}
