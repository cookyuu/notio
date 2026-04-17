package com.notio.notification.repository;

import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.Instant;

public interface NotificationSummaryProjection {

    Long getId();

    NotificationSource getSource();

    String getTitle();

    NotificationPriority getPriority();

    boolean isRead();

    Instant getCreatedAt();

    String getBody();
}
