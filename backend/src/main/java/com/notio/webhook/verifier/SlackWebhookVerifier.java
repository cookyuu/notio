package com.notio.webhook.verifier;

import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.config.WebhookProperties;
import com.notio.webhook.dto.WebhookRequestContext;
import com.notio.webhook.verifier.WebhookVerifier;
import com.notio.common.util.HmacUtils;
import org.springframework.stereotype.Component;

@Component
public class SlackWebhookVerifier implements WebhookVerifier {

    private final WebhookProperties webhookProperties;

    public SlackWebhookVerifier(final WebhookProperties webhookProperties) {
        this.webhookProperties = webhookProperties;
    }

    @Override
    public NotificationSource supports() {
        return NotificationSource.SLACK;
    }

    @Override
    public boolean verify(final WebhookRequestContext context) {
        final String configuredSecret = webhookProperties.slack().signingSecret();
        final String timestamp = context.headers().getFirst("X-Slack-Request-Timestamp");
        final String signature = context.headers().getFirst("X-Slack-Signature");
        if (configuredSecret == null || configuredSecret.isBlank() || timestamp == null || signature == null) {
            return false;
        }

        final String baseString = "v0:" + timestamp + ":" + context.rawBody();
        final String expectedSignature = "v0=" + HmacUtils.hmacSha256Hex(configuredSecret, baseString);
        return expectedSignature.equals(signature);
    }
}
