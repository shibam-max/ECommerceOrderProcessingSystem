package com.ecommerce.order.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that logs execution time for all public methods in the
 * service and controller layers. Helps dev/QA identify slow operations
 * without adding timing boilerplate to every method.
 *
 * WARN threshold: 500ms — anything above gets logged at WARN level
 * so it stands out in log aggregators and alerts.
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("PERF");
    private static final long SLOW_THRESHOLD_MS = 500;

    @Around("execution(public * com.ecommerce.order.service..*(..)) || " +
            "execution(public * com.ecommerce.order.controller..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            logTiming(methodName, elapsed, true);
            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            logTiming(methodName, elapsed, false);
            throw ex;
        }
    }

    private void logTiming(String method, long elapsedMs, boolean success) {
        String status = success ? "OK" : "FAIL";
        if (elapsedMs >= SLOW_THRESHOLD_MS) {
            log.warn("SLOW {} {} [{}ms]", status, method, elapsedMs);
        } else {
            log.debug("PERF {} {} [{}ms]", status, method, elapsedMs);
        }
    }
}
