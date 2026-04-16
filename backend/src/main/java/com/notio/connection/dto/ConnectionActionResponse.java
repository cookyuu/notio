package com.notio.connection.dto;

public record ConnectionActionResponse(
    Long id,
    String status,
    String message
) {
}
