package com.notio.webhook.api;

public record WebhookReceiptResponse(
        long notificationId,
        boolean received
) {
}
