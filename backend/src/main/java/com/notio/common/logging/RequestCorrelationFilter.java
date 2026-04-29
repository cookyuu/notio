package com.notio.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RequestCorrelationFilter extends OncePerRequestFilter {

    static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    static final String CORRELATION_ID_KEY = "correlation_id";
    static final String ROUTE_KEY = "route";
    static final String HTTP_METHOD_KEY = "http_method";

    private final CorrelationIdGenerator correlationIdGenerator;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        final long startedAt = System.currentTimeMillis();
        final String correlationId = resolveCorrelationId(request);
        final String route = request.getRequestURI();
        final String httpMethod = request.getMethod();

        MDC.put(CORRELATION_ID_KEY, correlationId);
        MDC.put(ROUTE_KEY, route);
        MDC.put(HTTP_METHOD_KEY, httpMethod);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        log.info(
                "event=request_started client_ip={} user_agent={}",
                resolveClientIp(request),
                request.getHeader("User-Agent")
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info(
                    "event=request_completed status={} elapsed_ms={} authenticated={}",
                    response.getStatus(),
                    System.currentTimeMillis() - startedAt,
                    isAuthenticated()
            );
            MDC.remove(HTTP_METHOD_KEY);
            MDC.remove(ROUTE_KEY);
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    private String resolveCorrelationId(final HttpServletRequest request) {
        final String headerValue = request.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.hasText(headerValue)) {
            return headerValue;
        }
        return correlationIdGenerator.generate();
    }

    private String resolveClientIp(final HttpServletRequest request) {
        final String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isAuthenticated() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
