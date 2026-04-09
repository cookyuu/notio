package com.notio.webhook.infrastructure;

import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.application.WebhookRequestContext;
import com.notio.webhook.application.WebhookVerifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class ClaudeWebhookVerifier implements WebhookVerifier {

    private final WebhookProperties webhookProperties;

    public ClaudeWebhookVerifier(final WebhookProperties webhookProperties) {
        this.webhookProperties = webhookProperties;
    }

    @Override
    public NotificationSource supports() {
        return NotificationSource.CLAUDE;
    }

    @Override
    public boolean verify(final WebhookRequestContext context) {
        final String configuredToken = webhookProperties.claude().bearerToken();
        final String authorization = context.headers().getFirst(HttpHeaders.AUTHORIZATION);
        return configuredToken != null
                && !configuredToken.isBlank()
                && authorization != null
                && authorization.equals("Bearer " + configuredToken);
    }
}
