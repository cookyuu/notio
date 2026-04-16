package com.notio.common.ratelimit;

import java.time.Duration;

public interface RateLimitStore {

    RateLimitCounter incrementAndGet(String key, long limit, Duration window);
}
