package com.notio.webhook.handler;

import com.notio.webhook.dto.NotificationEvent;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.dto.WebhookRequestContext;

public interface WebhookHandler {

    NotificationSource supports();

    NotificationEvent handle(WebhookRequestContext context);
}
