package com.ecommerce.order.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Logs every HTTP request with method, URI, query string, response status,
 * and execution time in milliseconds. Runs after CorrelationIdFilter so the
 * correlation ID is already in MDC and appears in the log line automatically.
 *
 * Sample output:
 *   HTTP 200 POST /api/orders [23ms]
 *   HTTP 404 GET /api/orders/999 [5ms]
 *   HTTP 401 GET /api/orders [1ms] (no auth token)
 *
 * For QA: when you report a bug, include the correlation ID from the
 * X-Correlation-ID response header. Dev can search logs for that ID
 * and see the full request lifecycle.
 */
@Component
@Order(2)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String fullPath = query != null ? uri + "?" + query : uri;

            if (status >= 500) {
                log.error("HTTP {} {} {} [{}ms]", status, method, fullPath, duration);
            } else if (status >= 400) {
                log.warn("HTTP {} {} {} [{}ms]", status, method, fullPath, duration);
            } else {
                log.info("HTTP {} {} {} [{}ms]", status, method, fullPath, duration);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console");
    }
}
