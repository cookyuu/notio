package com.notio.common.ratelimit;

import java.util.List;

public record ResolvedRateLimitRequest(
    String policyName,
    List<RateLimitRule> rules
) {
}
