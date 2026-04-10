package com.notio.notification.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarkReadResponse(
    Long id,

    @JsonProperty("is_read")
    boolean isRead
) {
}
