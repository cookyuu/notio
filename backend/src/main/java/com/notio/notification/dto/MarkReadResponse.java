package com.notio.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarkReadResponse(
    Long id,

    @JsonProperty("is_read")
    boolean isRead
) {
}
