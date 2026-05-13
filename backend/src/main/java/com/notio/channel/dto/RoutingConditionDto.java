package com.notio.channel.dto;

import com.notio.channel.domain.RoutingCondition;

import java.util.List;

public record RoutingConditionDto(
    List<String> sources,
    List<String> priorities
) {
    public RoutingCondition toDomain() {
        return new RoutingCondition(sources, priorities);
    }

    public static RoutingConditionDto from(RoutingCondition condition) {
        if (condition == null) {
            return new RoutingConditionDto(null, null);
        }
        return new RoutingConditionDto(condition.sources(), condition.priorities());
    }
}
