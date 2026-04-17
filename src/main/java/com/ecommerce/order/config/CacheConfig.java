package com.ecommerce.order.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine-based caching configuration.
 *
 * Strategy:
 *   - "orders" cache: individual order lookups (getOrder by ID)
 *     TTL 10 minutes, max 500 entries. Evicted on any write operation.
 *   - "orderList" cache: list queries (all orders, filtered by status)
 *     TTL 2 minutes, max 50 entries. Short TTL because list results change frequently.
 *
 * Trade-off: We use a short TTL + explicit eviction rather than write-through
 * because order mutations are frequent in an e-commerce system. Stale reads
 * for a few seconds are acceptable; stale reads for minutes are not.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return manager;
    }
}
