package com.notio.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderRequest(
    @NotEmpty @JsonProperty("ordered_ids") List<Long> orderedIds
) {}
