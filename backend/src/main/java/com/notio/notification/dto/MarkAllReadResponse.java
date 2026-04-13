package com.notio.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarkAllReadResponse(
    @JsonProperty("updated_count")
    int updatedCount
) {
}
