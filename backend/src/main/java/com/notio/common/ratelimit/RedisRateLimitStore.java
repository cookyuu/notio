package com.notio.common.ratelimit;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRateLimitStore implements RateLimitStore {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public RateLimitCounter incrementAndGet(final String key, final long limit, final Duration window) {
        try {
            final Long currentValue = stringRedisTemplate.opsForValue().increment(key);
            if (currentValue == null) {
                throw new IllegalStateException("Redis increment returned null");
            }

            if (currentValue == 1L) {
                stringRedisTemplate.expire(key, window);
            }

            final Long ttlSeconds = stringRedisTemplate.getExpire(key);
            final long safeTtlSeconds = ttlSeconds == null || ttlSeconds < 0 ? window.toSeconds() : ttlSeconds;
            return new RateLimitCounter(
                limit,
                currentValue,
                Instant.now().plusSeconds(Math.max(safeTtlSeconds, 0L))
            );
        } catch (Exception exception) {
            throw new RateLimitStoreException("Rate limit store is unavailable", exception);
        }
    }
}
