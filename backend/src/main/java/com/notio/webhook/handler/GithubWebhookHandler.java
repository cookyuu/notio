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
public class GithubWebhookHandler implements WebhookHandler {

    @Override
    public NotificationSource supports() {
        return NotificationSource.GITHUB;
    }

    @Override
    public NotificationEvent handle(final WebhookRequestContext context) {
        final Map<String, Object> payload = context.payload();
        final String repository = WebhookPayloadExtractor.stringValue(payload, "repository.full_name");
        final String action = WebhookPayloadExtractor.stringValue(payload, "action");
        final String title = firstNonBlank(action, "GitHub event")
                + (repository == null ? "" : " - " + repository);

        return new NotificationEvent(
                NotificationSource.GITHUB,
                title,
                firstNonBlank(
                        WebhookPayloadExtractor.stringValue(
                                payload,
                                "pull_request.title",
                                "issue.title",
                                "head_commit.message"
                        ),
                        context.rawBody()
                ),
                NotificationPriority.HIGH,
                WebhookPayloadExtractor.stringValue(payload, "delivery", "after", "pull_request.id", "issue.id"),
                WebhookPayloadExtractor.stringValue(
                        payload,
                        "pull_request.html_url",
                        "issue.html_url",
                        "repository.html_url"
                ),
                payload
        );
    }

    private String firstNonBlank(final String primary, final String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
