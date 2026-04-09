package com.notio.webhook.infrastructure;

import com.notio.notification.application.NotificationEvent;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.application.WebhookHandler;
import com.notio.webhook.application.WebhookRequestContext;
import com.notio.webhook.infrastructure.support.WebhookPayloadExtractor;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ClaudeWebhookHandler implements WebhookHandler {

    @Override
    public NotificationSource supports() {
        return NotificationSource.CLAUDE;
    }

    @Override
    public NotificationEvent handle(final WebhookRequestContext context) {
        final Map<String, Object> payload = context.payload();
        return new NotificationEvent(
                NotificationSource.CLAUDE,
                firstNonBlank(
                        WebhookPayloadExtractor.stringValue(payload, "title", "event.title"),
                        "Claude notification"
                ),
                firstNonBlank(
                        WebhookPayloadExtractor.stringValue(payload, "body", "content", "message", "event.body"),
                        context.rawBody()
                ),
                safePriority(WebhookPayloadExtractor.stringValue(payload, "priority")),
                WebhookPayloadExtractor.stringValue(payload, "external_id", "id", "event.id"),
                WebhookPayloadExtractor.stringValue(payload, "external_url", "url", "event.url"),
                payload
        );
    }

    private NotificationPriority safePriority(final String value) {
        try {
            return value == null ? NotificationPriority.MEDIUM : NotificationPriority.from(value);
        } catch (IllegalArgumentException exception) {
            return NotificationPriority.MEDIUM;
        }
    }

    private String firstNonBlank(final String primary, final String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
