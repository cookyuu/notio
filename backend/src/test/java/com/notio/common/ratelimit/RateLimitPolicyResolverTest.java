package com.notio.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.auth.config.JwtProperties;
import com.notio.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitPolicyResolverTest {

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties());
    private final RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(jwtTokenProvider, new ObjectMapper());

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveWebhookUsesKeyPrefixAndIpPolicies() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/webhook/claude");
        request.addHeader("X-Notio-Webhook-Key", "ntio_wh_prefix123_secret1234567890");
        request.setRemoteAddr("127.0.0.1");

        final var resolved = resolver.resolve(new CachedBodyHttpServletRequest(request, 65_536));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().policyName()).isEqualTo("webhook");
        assertThat(resolved.get().rules())
            .extracting(RateLimitRule::key)
            .contains(
                "rateLimit:webhook:ip:127.0.0.1:1m",
                "rateLimit:webhook:prefix:prefix123:1m",
                "rateLimit:webhook:prefix:prefix123:1d"
            );
    }

    @Test
    void resolveRefreshUsesIpAndUserPoliciesWhenRefreshTokenIsValid() throws Exception {
        final String refreshToken = jwtTokenProvider.generateRefreshToken("42");
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.setContentType("application/json");
        request.setContent(("{\"refreshToken\":\"" + refreshToken + "\"}").getBytes());
        request.setRemoteAddr("10.0.0.2");

        final var resolved = resolver.resolve(new CachedBodyHttpServletRequest(request, 1_048_576));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().rules())
            .extracting(RateLimitRule::key)
            .contains(
                "rateLimit:refresh:ip:10.0.0.2:1m",
                "rateLimit:refresh:user:42:1h"
            );
    }

    @Test
    void resolveNotificationReadUsesAuthenticatedUserPolicy() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("9", null));

        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");

        final var resolved = resolver.resolve(new CachedBodyHttpServletRequest(request, 0));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().policyName()).isEqualTo("notifications-read");
        assertThat(resolved.get().rules())
            .extracting(RateLimitRule::key)
            .containsExactly("rateLimit:notifications-read:user:9:1m");
    }

    @Test
    void resolveNotificationWriteUsesSeparateQuota() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("9", null));

        final MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/notifications/1/read");

        final var resolved = resolver.resolve(new CachedBodyHttpServletRequest(request, 0));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().policyName()).isEqualTo("notifications-write");
        assertThat(resolved.get().rules())
            .extracting(RateLimitRule::key)
            .containsExactly("rateLimit:notifications-write:user:9:1m");
    }

    @Test
    void resolveWebhookWithMalformedKeyFallsBackToIpPolicyOnly() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/webhook/claude");
        request.addHeader("X-Notio-Webhook-Key", "malformed");
        request.setRemoteAddr("127.0.0.1");

        final var resolved = resolver.resolve(new CachedBodyHttpServletRequest(request, 65_536));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().rules())
            .extracting(RateLimitRule::key)
            .containsExactly("rateLimit:webhook:ip:127.0.0.1:1m");
    }

    @Test
    void resolveChatUsesLowerMinuteAndDailyQuota() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("9", null));
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat");
        request.setContentType("application/json");
        request.setContent("{\"content\":\"hello\"}".getBytes());

        final var resolved = resolver.resolve(new CachedBodyHttpServletRequest(request, 1_048_576));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().policyName()).isEqualTo("chat-ai");
        assertThat(resolved.get().rules())
            .extracting(RateLimitRule::key)
            .containsExactly(
                "rateLimit:chat-ai:user:9:1m",
                "rateLimit:chat-ai:user:9:1d"
            );
    }

    @Test
    void resolveReturnsSignupPolicyForPublicSignupEndpoint() throws Exception {
        final MockHttpServletRequest request = jsonRequest(
                "POST",
                "/api/v1/auth/signup",
                "{\"email\":\"user@example.com\",\"password\":\"password123\"}"
        );

        final var resolved = resolver.resolve(new CachedBodyHttpServletRequest(request, 4_096));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().policyName()).isEqualTo("signup");
        assertThat(resolved.get().rules()).hasSize(2);
    }

    @Test
    void resolveReturnsEmailScopedPolicyForPasswordResetRequest() throws Exception {
        final MockHttpServletRequest request = jsonRequest(
                "POST",
                "/api/v1/auth/password-reset/request",
                "{\"email\":\"user@example.com\"}"
        );

        final var resolved = resolver.resolve(new CachedBodyHttpServletRequest(request, 4_096));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().policyName()).isEqualTo("password-reset-request");
        assertThat(resolved.get().rules())
                .extracting(RateLimitRule::key)
                .anyMatch(bucket -> bucket.contains("email:user@example.com"));
    }

    @Test
    void resolveReturnsCallbackPolicyForOauthCallbackEndpoint() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/oauth/callback/google");
        request.setRemoteAddr("127.0.0.1");

        final var resolved = resolver.resolve(new CachedBodyHttpServletRequest(request, 0));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().policyName()).isEqualTo("oauth-callback");
        assertThat(resolved.get().rules()).hasSize(2);
    }

    private MockHttpServletRequest jsonRequest(
            final String method,
            final String requestUri,
            final String body
    ) {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent(body.getBytes());
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private JwtProperties jwtProperties() {
        final JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setExpiration(86_400_000L);
        return properties;
    }
}
