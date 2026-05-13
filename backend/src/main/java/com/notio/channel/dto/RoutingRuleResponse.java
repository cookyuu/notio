package com.notio.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notio.channel.domain.DeliveryMode;
import com.notio.channel.domain.RoutingRule;

import java.time.Instant;
import java.util.List;

public record RoutingRuleResponse(
    Long id,
    @JsonProperty("rule_name") String ruleName,
    @JsonProperty("priority_order") int priorityOrder,
    RoutingConditionDto conditions,
    @JsonProperty("channel_ids") List<Long> channelIds,
    @JsonProperty("stop_on_match") boolean stopOnMatch,
    @JsonProperty("is_enabled") boolean isEnabled,
    @JsonProperty("delivery_mode") DeliveryMode deliveryMode,
    @JsonProperty("digest_interval_min") Integer digestIntervalMin,
    @JsonProperty("created_at") Instant createdAt
) {
    public static RoutingRuleResponse from(RoutingRule rule) {
        return new RoutingRuleResponse(
            rule.getId(),
            rule.getRuleName(),
            rule.getPriorityOrder(),
            RoutingConditionDto.from(rule.getConditions()),
            rule.getChannelIds(),
            rule.isStopOnMatch(),
            rule.isEnabled(),
            rule.getDeliveryMode(),
            rule.getDigestIntervalMin(),
            rule.getCreatedAt()
        );
    }
}
