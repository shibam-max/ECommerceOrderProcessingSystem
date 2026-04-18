package com.ecommerce.order.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Enables ETag-based conditional GET requests via Spring's ShallowEtagHeaderFilter.
 *
 * How it works:
 *   1. Server computes an MD5 hash of the response body → sets ETag header
 *   2. Client sends If-None-Match: "etag-value" on next request
 *   3. If response body hasn't changed → server returns 304 Not Modified (no body)
 *
 * This saves bandwidth on repeated list/detail calls where data hasn't changed.
 * The filter only applies to /api/** paths to avoid interfering with Swagger/Actuator.
 */
@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("etagFilter");
        registration.setOrder(10);
        return registration;
    }
}
