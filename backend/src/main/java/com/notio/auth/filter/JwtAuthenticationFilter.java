package com.notio.auth.filter;

import com.notio.auth.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 인증 필터
 * - Authorization 헤더에서 JWT 토큰을 추출
 * - 토큰 유효성 검증
 * - SecurityContext에 인증 정보 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String WEBHOOK_PATH_PREFIX = "/api/v1/webhook/";
    private static final String EVENT_KEY = "event";
    private static final String OUTCOME_KEY = "outcome";
    private static final String CORRELATION_ID_KEY = "correlation_id";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        try {
            final String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                final JwtTokenProvider.JwtValidationResult validationResult = jwtTokenProvider.validateTokenWithReason(jwt);
                if (!validationResult.isValid()) {
                    logAuthenticationFailure(request, validationResult.reason());
                    filterChain.doFilter(request, response);
                    return;
                }

                final String userId = jwtTokenProvider.getUserId(jwt);
                final String email = jwtTokenProvider.getEmail(jwt);

                final UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, null);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logAuthenticationSuccess(request, userId, email);
            }
        } catch (final Exception ex) {
            logAuthenticationFailure(request, "authentication_processing_error");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Request의 Authorization 헤더에서 JWT 토큰 추출
     */
    private String extractJwtFromRequest(final HttpServletRequest request) {
        final String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return request.getRequestURI().startsWith(WEBHOOK_PATH_PREFIX);
    }

    private void logAuthenticationSuccess(
            final HttpServletRequest request,
            final String userId,
            final String email
    ) {
        MDC.put(EVENT_KEY, "jwt_authentication_succeeded");
        MDC.put(OUTCOME_KEY, "success");
        try {
            log.debug(
                    "event=jwt_authentication_succeeded user_id={} email={} route={}",
                    userId,
                    email,
                    request.getRequestURI()
            );
        } finally {
            MDC.remove(OUTCOME_KEY);
            MDC.remove(EVENT_KEY);
        }
    }

    private void logAuthenticationFailure(final HttpServletRequest request, final String reason) {
        MDC.put(EVENT_KEY, "jwt_authentication_failed");
        MDC.put(OUTCOME_KEY, "failure");
        try {
            log.warn(
                    "event=jwt_authentication_failed reason={} route={} correlation_id={}",
                    reason,
                    request.getRequestURI(),
                    MDC.get(CORRELATION_ID_KEY)
            );
        } finally {
            MDC.remove(OUTCOME_KEY);
            MDC.remove(EVENT_KEY);
        }
    }
}
