package com.notio.webhook.handler;

import com.notio.webhook.dto.NotificationEvent;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.handler.WebhookHandler;
import com.notio.webhook.dto.WebhookRequestContext;
import com.notio.webhook.util.WebhookPayloadExtractor;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SlackWebhookHandler implements WebhookHandler {

    @Override
    public NotificationSource supports() {
        return NotificationSource.SLACK;
    }

    @Override
    public NotificationEvent handle(final WebhookRequestContext context) {
        final Map<String, Object> payload = context.payload();
        final String text = firstNonBlank(
                WebhookPayloadExtractor.stringValue(payload, "text", "event.text", "message.text"),
                "Slack notification"
        );

        return new NotificationEvent(
                NotificationSource.SLACK,
                truncate(text, 120),
                text,
                NotificationPriority.HIGH,
                WebhookPayloadExtractor.stringValue(payload, "event_id", "event.ts", "ts"),
                WebhookPayloadExtractor.stringValue(payload, "external_url", "permalink"),
                payload
        );
    }

    private String firstNonBlank(final String primary, final String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private String truncate(final String value, final int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
