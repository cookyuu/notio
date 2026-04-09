package com.notio.webhook.application;

import com.notio.notification.domain.NotificationSource;

public interface WebhookVerifier {

    NotificationSource supports();

    boolean verify(WebhookRequestContext context);
}
