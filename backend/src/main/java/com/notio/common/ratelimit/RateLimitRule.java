package com.notio.common.ratelimit;

import java.time.Duration;

public record RateLimitRule(
    String key,
    long limit,
    Duration window
) {
}
