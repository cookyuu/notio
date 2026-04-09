package com.notio.webhook.application;

import com.notio.notification.application.NotificationEvent;
import com.notio.notification.domain.NotificationSource;

public interface WebhookHandler {

    NotificationSource supports();

    NotificationEvent handle(WebhookRequestContext context);
}
