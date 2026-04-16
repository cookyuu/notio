package com.notio.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.exception.ErrorCode;
import com.notio.common.response.ApiError;
import com.notio.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            final CachedBodyHttpServletRequest wrappedRequest = wrapRequest(request);
            validateChatInputLength(wrappedRequest);

            final RateLimitEvaluation evaluation = rateLimitService.evaluate(wrappedRequest);
            applyRateLimitHeaders(response, evaluation.decision());
            if (!evaluation.allowed()) {
                writeErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS.value(), ErrorCode.RATE_LIMIT_EXCEEDED);
                return;
            }

            filterChain.doFilter(wrappedRequest, response);
        } catch (PayloadTooLargeException exception) {
            writeErrorResponse(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, ErrorCode.PAYLOAD_TOO_LARGE);
        } catch (RateLimitStoreException exception) {
            log.error("Rate limit store unavailable for fail-closed endpoint path={}", request.getRequestURI(), exception);
            writeErrorResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v1/")) {
            return true;
        }

        return uri.startsWith("/swagger-ui/")
            || "/swagger-ui.html".equals(uri)
            || uri.startsWith("/api-docs/")
            || "/api-docs".equals(uri)
            || "/actuator/health".equals(uri)
            || "/health".equals(uri);
    }

    private CachedBodyHttpServletRequest wrapRequest(final HttpServletRequest request) throws IOException {
        if (!shouldCacheBody(request)) {
            return new CachedBodyHttpServletRequest(request, 0);
        }

        return new CachedBodyHttpServletRequest(request, resolveMaxBodyBytes(request));
    }

    private boolean shouldCacheBody(final HttpServletRequest request) {
        if ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()) || "OPTIONS".equals(request.getMethod())) {
            return false;
        }

        final String contentType = request.getContentType();
        final long contentLength = request.getContentLengthLong();
        return contentLength != 0
            || request.getHeader("Transfer-Encoding") != null
            || (StringUtils.hasText(contentType) && contentType.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    private int resolveMaxBodyBytes(final HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/v1/webhook/")) {
            return properties.getWebhookBodyMaxBytes();
        }
        return properties.getJsonBodyMaxBytes();
    }

    private void validateChatInputLength(final CachedBodyHttpServletRequest request) throws IOException {
        if (!request.getRequestURI().startsWith("/api/v1/chat")
            || !"POST".equals(request.getMethod())
            || request.getCachedBody().length == 0) {
            return;
        }

        final var root = objectMapper.readTree(request.cachedBodyAsString());
        final var contentNode = root.get("content");
        if (contentNode != null && contentNode.asText("").length() > properties.getChatMaxChars()) {
            throw new PayloadTooLargeException(properties.getChatMaxChars());
        }
    }

    private void applyRateLimitHeaders(final HttpServletResponse response, final RateLimitDecision decision) {
        if (decision == null) {
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetAt().getEpochSecond()));
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds(Instant.now())));
    }

    private void writeErrorResponse(
        final HttpServletResponse response,
        final int status,
        final ErrorCode errorCode
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        final ApiResponse<Void> body = ApiResponse.error(new ApiError(errorCode.getCode(), errorCode.getMessage()));
        objectMapper.writeValue(response.getWriter(), body);
    }
}
