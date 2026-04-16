package com.notio.webhook.dto;

public record WebhookDispatchResult(
    NotificationEvent event,
    WebhookPrincipal principal
) {
}
