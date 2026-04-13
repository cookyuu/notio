package com.notio.webhook.verifier;

import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.dto.WebhookRequestContext;

public interface WebhookVerifier {

    NotificationSource supports();

    boolean verify(WebhookRequestContext context);
}
