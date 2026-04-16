package com.notio.connection.dto;

import com.notio.connection.domain.Connection;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.connection.domain.ConnectionStatus;
import java.time.Instant;

public record ConnectionResponse(
    Long id,
    ConnectionProvider provider,
    ConnectionAuthType authType,
    String displayName,
    String accountLabel,
    String externalAccountId,
    String externalWorkspaceId,
    String subscriptionId,
    ConnectionStatus status,
    String capabilities,
    String metadata,
    Instant lastUsedAt,
    Instant createdAt,
    Instant updatedAt
) {

    public static ConnectionResponse from(final Connection connection) {
        return new ConnectionResponse(
            connection.getId(),
            connection.getProvider(),
            connection.getAuthType(),
            connection.getDisplayName(),
            connection.getAccountLabel(),
            connection.getExternalAccountId(),
            connection.getExternalWorkspaceId(),
            connection.getSubscriptionId(),
            connection.getStatus(),
            connection.getCapabilities(),
            connection.getMetadata(),
            connection.getLastUsedAt(),
            connection.getCreatedAt(),
            connection.getUpdatedAt()
        );
    }
}
