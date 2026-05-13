package com.notio.channel.domain;

import java.util.List;

public record RoutingCondition(
    List<String> sources,
    List<String> priorities
) {
    public static RoutingCondition empty() {
        return new RoutingCondition(null, null);
    }
}
