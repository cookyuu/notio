package com.notio.connection.dto;

import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateConnectionRequest(
    @NotNull ConnectionProvider provider,
    ConnectionAuthType authType,
    @NotBlank @Size(max = 100) String displayName,
    @Size(max = 255) String accountLabel,
    @Size(max = 255) String externalAccountId,
    @Size(max = 255) String externalWorkspaceId,
    @Size(max = 255) String subscriptionId
) {
}
