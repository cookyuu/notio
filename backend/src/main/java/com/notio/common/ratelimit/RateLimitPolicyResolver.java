package com.notio.common.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.auth.util.JwtTokenProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RateLimitPolicyResolver {

    private static final Pattern WEBHOOK_KEY_PATTERN = Pattern.compile("^ntio_wh_([^_]+)_.+$");

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public Optional<ResolvedRateLimitRequest> resolve(final CachedBodyHttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        final String method = request.getMethod();
        final String clientIp = resolveClientIp(request);

        if ("/api/v1/auth/login".equals(requestUri) && "POST".equals(method)) {
            return Optional.of(new ResolvedRateLimitRequest(
                "login",
                List.of(
                    new RateLimitRule(bucket("login", "ip", clientIp, "1m"), 5, Duration.ofMinutes(1)),
                    new RateLimitRule(bucket("login", "ip", clientIp, "1h"), 30, Duration.ofHours(1))
                )
            ));
        }

        if ("/api/v1/auth/refresh".equals(requestUri) && "POST".equals(method)) {
            final List<RateLimitRule> rules = new ArrayList<>();
            rules.add(new RateLimitRule(bucket("refresh", "ip", clientIp, "1m"), 30, Duration.ofMinutes(1)));
            extractRefreshUserId(request).ifPresent(userId ->
                rules.add(new RateLimitRule(bucket("refresh", "user", userId, "1h"), 60, Duration.ofHours(1)))
            );
            return Optional.of(new ResolvedRateLimitRequest("refresh", rules));
        }

        if (requestUri.startsWith("/api/v1/webhook/")) {
            final List<RateLimitRule> rules = new ArrayList<>();
            rules.add(new RateLimitRule(bucket("webhook", "ip", clientIp, "1m"), 60, Duration.ofMinutes(1)));
            extractWebhookKeyPrefix(request).ifPresent(prefix -> {
                rules.add(new RateLimitRule(bucket("webhook", "prefix", prefix, "1m"), 30, Duration.ofMinutes(1)));
                rules.add(new RateLimitRule(bucket("webhook", "prefix", prefix, "1d"), 5_000, Duration.ofDays(1)));
            });
            return Optional.of(new ResolvedRateLimitRequest("webhook", rules));
        }

        final Long userId = currentUserId();
        if (userId == null) {
            return Optional.empty();
        }

        if (requestUri.startsWith("/api/v1/notifications")) {
            if ("GET".equals(method)) {
                return Optional.of(singleUserPolicy("notifications-read", userId, 120, Duration.ofMinutes(1)));
            }

            if ("PATCH".equals(method) || "DELETE".equals(method)) {
                return Optional.of(singleUserPolicy("notifications-write", userId, 60, Duration.ofMinutes(1)));
            }
        }

        if (requestUri.startsWith("/api/v1/chat")) {
            return Optional.of(new ResolvedRateLimitRequest(
                "chat-ai",
                List.of(
                    new RateLimitRule(bucket("chat-ai", "user", userId, "1m"), 20, Duration.ofMinutes(1)),
                    new RateLimitRule(bucket("chat-ai", "user", userId, "1d"), 200, Duration.ofDays(1))
                )
            ));
        }

        if ("/api/v1/devices/register".equals(requestUri) && "POST".equals(method)) {
            return Optional.of(new ResolvedRateLimitRequest(
                "device-register",
                List.of(
                    new RateLimitRule(bucket("device-register", "user", userId, "1m"), 10, Duration.ofMinutes(1)),
                    new RateLimitRule(bucket("device-register", "ip", clientIp, "1m"), 30, Duration.ofMinutes(1))
                )
            ));
        }

        return Optional.empty();
    }

    private ResolvedRateLimitRequest singleUserPolicy(
        final String policyName,
        final Long userId,
        final long limit,
        final Duration window
    ) {
        return new ResolvedRateLimitRequest(
            policyName,
            List.of(new RateLimitRule(bucket(policyName, "user", userId, suffix(window)), limit, window))
        );
    }

    private Optional<Long> extractRefreshUserId(final CachedBodyHttpServletRequest request) {
        try {
            final JsonNode root = objectMapper.readTree(request.cachedBodyAsString());
            final JsonNode refreshToken = root.get("refreshToken");
            if (refreshToken == null || refreshToken.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(jwtTokenProvider.getUserId(refreshToken.asText())));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<String> extractWebhookKeyPrefix(final CachedBodyHttpServletRequest request) {
        final String apiKey = extractApiKey(request);
        if (!StringUtils.hasText(apiKey)) {
            return Optional.empty();
        }

        final Matcher matcher = WEBHOOK_KEY_PATTERN.matcher(apiKey);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }

    private String extractApiKey(final CachedBodyHttpServletRequest request) {
        final String explicit = request.getHeader("X-Notio-Webhook-Key");
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }

        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ntio_wh_")) {
            return authorization.substring("Bearer ".length());
        }

        return null;
    }

    private Long currentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String resolveClientIp(final CachedBodyHttpServletRequest request) {
        final String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String bucket(final String policy, final String dimension, final Object subject, final String window) {
        return "rateLimit:" + policy + ":" + dimension + ":" + subject + ":" + window;
    }

    private String suffix(final Duration window) {
        if (Duration.ofDays(1).equals(window)) {
            return "1d";
        }
        if (Duration.ofHours(1).equals(window)) {
            return "1h";
        }
        return "1m";
    }
}
