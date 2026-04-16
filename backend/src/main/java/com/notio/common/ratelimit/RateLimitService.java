package com.notio.common.ratelimit;

import java.time.Instant;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitStore rateLimitStore;
    private final RateLimitPolicyResolver policyResolver;
    private final RateLimitProperties properties;
    private final Environment environment;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitEvaluation evaluate(final CachedBodyHttpServletRequest request) {
        final var resolved = policyResolver.resolve(request);
        if (resolved.isEmpty() || resolved.get().rules().isEmpty()) {
            return RateLimitEvaluation.allow();
        }

        try {
            RateLimitDecision primaryDecision = null;
            for (RateLimitRule rule : resolved.get().rules()) {
                final RateLimitCounter counter = rateLimitStore.incrementAndGet(rule.key(), rule.limit(), rule.window());
                final RateLimitDecision decision =
                    new RateLimitDecision(counter.limit(), counter.remaining(), counter.resetAt());
                if (counter.exceeded()) {
                    return RateLimitEvaluation.deny(decision);
                }
                primaryDecision = selectPrimaryDecision(primaryDecision, decision);
            }
            return RateLimitEvaluation.allow(false, primaryDecision);
        } catch (RateLimitStoreException exception) {
            if (shouldFailOpen()) {
                log.warn("Rate limit store unavailable, failing open for path={}", request.getRequestURI(), exception);
                return RateLimitEvaluation.allow(true, null);
            }

            if (matchesFailClosedPath(request.getRequestURI())) {
                throw exception;
            }

            log.warn("Rate limit store unavailable for non-sensitive path={}, failing open", request.getRequestURI(), exception);
            return RateLimitEvaluation.allow(true, null);
        }
    }

    private boolean shouldFailOpen() {
        for (String profile : properties.getFailOpenProfiles()) {
            for (String activeProfile : environment.getActiveProfiles()) {
                if (profile.equals(activeProfile)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesFailClosedPath(final String requestUri) {
        return properties.getFailClosedPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }

    private RateLimitDecision selectPrimaryDecision(
        final RateLimitDecision current,
        final RateLimitDecision candidate
    ) {
        if (current == null) {
            return candidate;
        }

        final Comparator<RateLimitDecision> comparator = Comparator
            .comparingLong(RateLimitDecision::remaining)
            .thenComparing(RateLimitDecision::resetAt, Comparator.naturalOrder())
            .thenComparingLong(decision -> decision.retryAfterSeconds(Instant.now()));
        return comparator.compare(candidate, current) < 0 ? candidate : current;
    }
}
