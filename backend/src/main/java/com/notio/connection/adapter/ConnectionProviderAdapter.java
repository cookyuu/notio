package com.notio.connection.adapter;

import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.webhook.dto.NotificationEvent;
import com.notio.webhook.dto.WebhookPrincipal;
import com.notio.webhook.dto.WebhookRequestContext;

public interface ConnectionProviderAdapter {

    ConnectionProvider supports();

    boolean supportsAuthType(ConnectionAuthType authType);

    WebhookPrincipal authenticateWebhook(WebhookRequestContext context);

    NotificationEvent toNotificationEvent(WebhookRequestContext context);
}
