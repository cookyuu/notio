package com.notio.webhook.verifier;

import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.dto.WebhookRequestContext;
import org.springframework.stereotype.Component;

@Deprecated
@Component
public class ClaudeWebhookVerifier implements WebhookVerifier {

    @Override
    public NotificationSource supports() {
        return NotificationSource.CLAUDE;
    }

    @Override
    public boolean verify(final WebhookRequestContext context) {
        return false;
    }
}
