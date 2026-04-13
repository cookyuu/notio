package com.notio.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record WebhookReceiptResponse(
    @JsonProperty("notification_id")
    long notificationId,

    @JsonProperty("processed_at")
    Instant processedAt
) {
    public static WebhookReceiptResponse of(long notificationId, Instant processedAt) {
        return new WebhookReceiptResponse(notificationId, processedAt);
    }
}
