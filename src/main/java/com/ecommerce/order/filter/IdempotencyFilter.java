package com.ecommerce.order.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Idempotency guarantee for mutating API calls.
 *
 * Clients send X-Idempotency-Key with POST requests. If we've already
 * processed that key, we return the cached response (status + body)
 * without executing the operation again.
 *
 * This is CRITICAL in distributed systems where network retries,
 * load balancer timeouts, or client-side retries can cause duplicate
 * order creation. Without idempotency, a retry after a timeout could
 * create two orders for the same purchase.
 *
 * In production, the key store would be Redis (shared across instances).
 * Here we use Caffeine with a 24-hour TTL for single-instance demo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class IdempotencyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final Cache<String, CachedResponse> idempotencyStore = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        if (!"POST".equalsIgnoreCase(httpReq.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpReq.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = httpReq.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        CachedResponse cached = idempotencyStore.getIfPresent(idempotencyKey);
        if (cached != null) {
            log.info("Idempotency HIT for key={}, returning cached response (status={})",
                    idempotencyKey, cached.status);
            httpRes.setStatus(cached.status);
            httpRes.setContentType("application/json");
            httpRes.setHeader("X-Idempotent-Replayed", "true");
            httpRes.getOutputStream().write(cached.body);
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpRes);
        chain.doFilter(request, wrappedResponse);

        byte[] body = wrappedResponse.getContentAsByteArray();
        int status = wrappedResponse.getStatus();

        if (status >= 200 && status < 300) {
            idempotencyStore.put(idempotencyKey, new CachedResponse(status, body));
            log.debug("Idempotency STORED key={} status={}", idempotencyKey, status);
        }

        wrappedResponse.copyBodyToResponse();
    }

    private static final class CachedResponse {
        final int status;
        final byte[] body;

        CachedResponse(int status, byte[] body) {
            this.status = status;
            this.body = body;
        }
    }
}
