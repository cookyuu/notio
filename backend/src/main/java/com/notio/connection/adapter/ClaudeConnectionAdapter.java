package com.notio.connection.adapter;

import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.dto.NotificationEvent;
import com.notio.webhook.dto.WebhookPrincipal;
import com.notio.webhook.dto.WebhookRequestContext;
import com.notio.webhook.service.WebhookCredentialAuthenticationService;
import com.notio.webhook.util.WebhookPayloadExtractor;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ClaudeConnectionAdapter implements ConnectionProviderAdapter {

    private final WebhookCredentialAuthenticationService authenticationService;

    public ClaudeConnectionAdapter(final WebhookCredentialAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public ConnectionProvider supports() {
        return ConnectionProvider.CLAUDE;
    }

    @Override
    public boolean supportsAuthType(final ConnectionAuthType authType) {
        return authType == ConnectionAuthType.API_KEY;
    }

    @Override
    public WebhookPrincipal authenticateWebhook(final WebhookRequestContext context) {
        return authenticationService.authenticateApiKey(context.headers(), ConnectionProvider.CLAUDE);
    }

    @Override
    public NotificationEvent toNotificationEvent(final WebhookRequestContext context) {
        final Map<String, Object> payload = context.payload();
        return new NotificationEvent(
            NotificationSource.CLAUDE,
            firstNonBlank(WebhookPayloadExtractor.stringValue(payload, "title", "notification.title"), "Claude notification"),
            firstNonBlank(WebhookPayloadExtractor.stringValue(payload, "body", "content", "message", "notification.message"), context.rawBody()),
            safePriority(WebhookPayloadExtractor.stringValue(payload, "priority")),
            WebhookPayloadExtractor.stringValue(payload, "external_id", "id", "notification.id"),
            WebhookPayloadExtractor.stringValue(payload, "external_url", "url", "notification.url"),
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
