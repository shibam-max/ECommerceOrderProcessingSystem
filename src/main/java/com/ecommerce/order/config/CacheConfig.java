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
 *     TTL 10 minutes, max 500 entries. Evicted on write operations.
 *
 * Trade-off: We intentionally do NOT cache list/paginated queries because
 * the combinatorial explosion of page/size/sort/filter params makes cache
 * keys impractical and stale-prone. Single-entity caching gives 80% of the
 * benefit with none of the invalidation complexity.
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
