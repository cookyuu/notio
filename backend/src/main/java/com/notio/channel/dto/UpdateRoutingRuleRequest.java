package com.notio.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notio.channel.domain.DeliveryMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateRoutingRuleRequest(
    @NotBlank @JsonProperty("rule_name") String ruleName,
    @JsonProperty("priority_order") Integer priorityOrder,
    RoutingConditionDto conditions,
    @JsonProperty("channel_ids") List<Long> channelIds,
    @JsonProperty("stop_on_match") boolean stopOnMatch,
    @JsonProperty("is_enabled") boolean isEnabled,
    @NotNull @JsonProperty("delivery_mode") DeliveryMode deliveryMode,
    @JsonProperty("digest_interval_min") Integer digestIntervalMin
) {}
