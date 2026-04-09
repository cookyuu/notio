package com.notio.webhook.infrastructure;

import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.application.WebhookRequestContext;
import com.notio.webhook.application.WebhookVerifier;
import com.notio.webhook.infrastructure.support.HmacUtils;
import org.springframework.stereotype.Component;

@Component
public class GithubWebhookVerifier implements WebhookVerifier {

    private final WebhookProperties webhookProperties;

    public GithubWebhookVerifier(final WebhookProperties webhookProperties) {
        this.webhookProperties = webhookProperties;
    }

    @Override
    public NotificationSource supports() {
        return NotificationSource.GITHUB;
    }

    @Override
    public boolean verify(final WebhookRequestContext context) {
        final String configuredSecret = webhookProperties.github().webhookSecret();
        final String signature = context.headers().getFirst("X-Hub-Signature-256");
        if (configuredSecret == null || configuredSecret.isBlank() || signature == null) {
            return false;
        }

        final String expectedSignature = "sha256=" + HmacUtils.hmacSha256Hex(configuredSecret, context.rawBody());
        return expectedSignature.equals(signature);
    }
}
