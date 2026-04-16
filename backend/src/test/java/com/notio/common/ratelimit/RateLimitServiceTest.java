package com.notio.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitServiceTest {

    @Test
    void evaluateReturnsDeniedWhenLimitIsExceeded() throws Exception {
        final RateLimitStore store = mock(RateLimitStore.class);
        final RateLimitPolicyResolver resolver = mock(RateLimitPolicyResolver.class);
        final Environment environment = mock(Environment.class);
        final RateLimitProperties properties = new RateLimitProperties();
        final RateLimitService service = new RateLimitService(store, resolver, properties, environment);
        final CachedBodyHttpServletRequest request = cachedRequest("GET", "/api/v1/notifications");

        when(resolver.resolve(request)).thenReturn(Optional.of(new ResolvedRateLimitRequest(
            "notifications-read",
            List.of(new RateLimitRule("rateLimit:test", 1, Duration.ofMinutes(1)))
        )));
        when(store.incrementAndGet("rateLimit:test", 1, Duration.ofMinutes(1)))
            .thenReturn(new RateLimitCounter(1, 2, Instant.now().plusSeconds(60)));

        final RateLimitEvaluation evaluation = service.evaluate(request);

        assertThat(evaluation.allowed()).isFalse();
        assertThat(evaluation.decision()).isNotNull();
        assertThat(evaluation.decision().remaining()).isZero();
    }

    @Test
    void evaluateFailsOpenOnLocalProfileWhenRedisIsUnavailable() throws Exception {
        final RateLimitStore store = mock(RateLimitStore.class);
        final RateLimitPolicyResolver resolver = mock(RateLimitPolicyResolver.class);
        final Environment environment = mock(Environment.class);
        final RateLimitProperties properties = new RateLimitProperties();
        final RateLimitService service = new RateLimitService(store, resolver, properties, environment);
        final CachedBodyHttpServletRequest request = cachedRequest("POST", "/api/v1/auth/login");

        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(resolver.resolve(request)).thenReturn(Optional.of(new ResolvedRateLimitRequest(
            "login",
            List.of(new RateLimitRule("rateLimit:test", 5, Duration.ofMinutes(1)))
        )));
        when(store.incrementAndGet("rateLimit:test", 5, Duration.ofMinutes(1)))
            .thenThrow(new RateLimitStoreException("down", new RuntimeException("down")));

        final RateLimitEvaluation evaluation = service.evaluate(request);

        assertThat(evaluation.allowed()).isTrue();
        assertThat(evaluation.degraded()).isTrue();
    }

    @Test
    void evaluateFailsClosedOnSensitivePathOutsideFailOpenProfiles() throws Exception {
        final RateLimitStore store = mock(RateLimitStore.class);
        final RateLimitPolicyResolver resolver = mock(RateLimitPolicyResolver.class);
        final Environment environment = mock(Environment.class);
        final RateLimitProperties properties = new RateLimitProperties();
        properties.setFailOpenProfiles(List.of("local", "dev"));
        properties.setFailClosedPaths(List.of("/api/v1/webhook/**"));
        final RateLimitService service = new RateLimitService(store, resolver, properties, environment);
        final CachedBodyHttpServletRequest request = cachedRequest("POST", "/api/v1/webhook/claude");

        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(resolver.resolve(request)).thenReturn(Optional.of(new ResolvedRateLimitRequest(
            "webhook",
            List.of(new RateLimitRule("rateLimit:test", 30, Duration.ofMinutes(1)))
        )));
        when(store.incrementAndGet("rateLimit:test", 30, Duration.ofMinutes(1)))
            .thenThrow(new RateLimitStoreException("down", new RuntimeException("down")));

        assertThatThrownBy(() -> service.evaluate(request))
            .isInstanceOf(RateLimitStoreException.class);
    }

    private CachedBodyHttpServletRequest cachedRequest(final String method, final String path) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        return new CachedBodyHttpServletRequest(request, 0);
    }
}
