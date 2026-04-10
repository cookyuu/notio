package com.notio.notification.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarkAllReadResponse(
    @JsonProperty("updated_count")
    int updatedCount
) {
}
