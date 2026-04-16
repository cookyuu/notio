package com.notio.webhook.dto;

import com.notio.connection.domain.ConnectionProvider;

public record WebhookPrincipal(
    Long connectionId,
    Long userId,
    ConnectionProvider provider
) {
}
