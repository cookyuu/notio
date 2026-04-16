package com.notio.common.ratelimit;

import java.time.Instant;

public record RateLimitDecision(
    long limit,
    long remaining,
    Instant resetAt
) {

    public long retryAfterSeconds(final Instant now) {
        return Math.max(0L, resetAt.getEpochSecond() - now.getEpochSecond());
    }
}
