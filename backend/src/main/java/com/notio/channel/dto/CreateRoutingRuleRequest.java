package com.notio.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notio.channel.domain.DeliveryMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateRoutingRuleRequest(
    @NotBlank @JsonProperty("rule_name") String ruleName,
    @JsonProperty("priority_order") int priorityOrder,
    RoutingConditionDto conditions,
    @NotEmpty @JsonProperty("channel_ids") List<Long> channelIds,
    @JsonProperty("stop_on_match") boolean stopOnMatch,
    @NotNull @JsonProperty("delivery_mode") DeliveryMode deliveryMode,
    @JsonProperty("digest_interval_min") Integer digestIntervalMin
) {}
