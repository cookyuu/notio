package com.notio.webhook.application;

import com.notio.notification.domain.NotificationSource;
import java.util.Map;
import org.springframework.http.HttpHeaders;

public record WebhookRequestContext(
        NotificationSource source,
        HttpHeaders headers,
        String rawBody,
        Map<String, Object> payload
) {
}
