package com.notio.common.ratelimit;

public record RateLimitEvaluation(
    boolean allowed,
    boolean degraded,
    RateLimitDecision decision
) {

    public static RateLimitEvaluation allow() {
        return new RateLimitEvaluation(true, false, null);
    }

    public static RateLimitEvaluation allow(final boolean degraded, final RateLimitDecision decision) {
        return new RateLimitEvaluation(true, degraded, decision);
    }

    public static RateLimitEvaluation deny(final RateLimitDecision decision) {
        return new RateLimitEvaluation(false, false, decision);
    }
}
