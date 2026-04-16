package com.notio.common.ratelimit;

import java.time.Instant;

public record RateLimitCounter(
    long limit,
    long current,
    Instant resetAt
) {

    public boolean exceeded() {
        return current > limit;
    }

    public long remaining() {
        return Math.max(limit - current, 0L);
    }
}
