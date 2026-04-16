package com.notio.connection.dto;

public record ConnectionSecretResponse(
    ConnectionResponse connection,
    String apiKey
) {
}
