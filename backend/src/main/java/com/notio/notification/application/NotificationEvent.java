package com.notio.notification.application;

import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record NotificationEvent(
        @NotNull NotificationSource source,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 2000) String body,
        @NotNull NotificationPriority priority,
        @Size(max = 255) String externalId,
        @Size(max = 500) String externalUrl,
        Map<String, Object> metadata
) {
}
