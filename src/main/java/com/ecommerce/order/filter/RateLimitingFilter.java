package com.ecommerce.order.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token-bucket rate limiter implemented per client IP.
 *
 * Protects the API from:
 *   - Accidental retry storms (client bug sending 1000 requests/sec)
 *   - Basic DDoS mitigation
 *   - Noisy neighbors in a shared environment
 *
 * Returns HTTP 429 (Too Many Requests) with Retry-After header.
 *
 * In production, this would use Redis + sliding window for distributed
 * rate limiting across multiple service instances. Here we use Caffeine
 * for single-instance demo with a fixed-window counter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RateLimitingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int WINDOW_SECONDS = 60;

    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterWrite(WINDOW_SECONDS, TimeUnit.SECONDS)
            .build();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        String clientIp = getClientIp(httpReq);
        AtomicInteger counter = requestCounts.get(clientIp, k -> new AtomicInteger(0));
        int currentCount = counter.incrementAndGet();

        httpRes.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        httpRes.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount)));

        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP={} (count={})", clientIp, currentCount);
            httpRes.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpRes.setContentType("application/json");
            httpRes.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
            httpRes.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\"," +
                    "\"message\":\"Rate limit exceeded. Max " + MAX_REQUESTS_PER_MINUTE +
                    " requests per minute.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
