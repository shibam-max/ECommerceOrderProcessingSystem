package com.ecommerce.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures a bounded thread pool for @Async methods (e.g., non-critical
 * notifications, Kafka publishing). Keeps the main request thread fast
 * by offloading slow side-effects.
 *
 * Pool sizing rationale:
 *   core=4:  handles normal load without creating threads
 *   max=8:   burst capacity for spikes
 *   queue=50: back-pressure — rejects if both pool and queue full
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler((r, e) ->
                log.warn("Async task rejected — pool and queue full. Consider scaling."));
        executor.initialize();
        return executor;
    }
}
