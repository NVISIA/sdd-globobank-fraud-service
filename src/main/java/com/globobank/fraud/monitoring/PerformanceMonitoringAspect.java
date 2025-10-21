package com.globobank.fraud.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Performance monitoring aspect for GloboBank Fraud Detection Service.
 * 
 * Provides automatic method execution time tracking and SLA monitoring for critical fraud detection
 * operations. Tracks metrics using Micrometer and logs performance violations for sub-200ms SLA
 * requirements.
 * 
 * Features: - Automatic method timing via @Timed annotation - SLA violation detection and alerting
 * - Correlation ID tracking for request tracing - Business metric collection for fraud detection
 * analytics
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
    private static final long SLA_THRESHOLD_MS = 200; // 200ms SLA threshold

    private final MeterRegistry meterRegistry;

    @Autowired
    public PerformanceMonitoringAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Annotation for marking methods that should be automatically timed. Provides flexible metric
     * naming and SLA tracking configuration.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Timed {
        /**
         * Name of the timer metric. If empty, uses class.method format.
         */
        String value() default "";

        /**
         * Description of the metric for monitoring dashboards.
         */
        String description() default "";

        /**
         * Whether to check SLA compliance for this method.
         */
        boolean slaCheck() default true;

        /**
         * Custom SLA threshold in milliseconds. If 0, uses default 200ms.
         */
        long slaThresholdMs() default 0;
    }

    /**
     * Around advice for methods annotated with @Timed. Measures execution time and tracks SLA
     * compliance.
     *
     * @param joinPoint The method execution join point
     * @param timed The timing annotation with configuration
     * @return The method result
     * @throws Throwable If the method execution fails
     */
    @Around("@annotation(timed)")
    public Object timeMethod(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        String correlationId = MDC.get("correlationId");
        String methodName = getMethodName(joinPoint, timed);
        long slaThreshold = timed.slaThresholdMs() > 0 ? timed.slaThresholdMs() : SLA_THRESHOLD_MS;

        Timer timer = Timer.builder(methodName).description(timed.description())
                .tag("class", joinPoint.getTarget().getClass().getSimpleName())
                .tag("method", joinPoint.getSignature().getName()).register(meterRegistry);

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // Record successful execution
            sample.stop(timer);

            // Check SLA compliance
            if (timed.slaCheck() && executionTime > slaThreshold) {
                handleSlaViolation(methodName, executionTime, slaThreshold, correlationId);
            }

            // Log performance for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Method {} executed in {}ms [correlationId={}]", methodName,
                        executionTime, correlationId);
            }

            return result;

        } catch (Exception e) {
            // Record failed execution
            sample.stop(Timer.builder(methodName + ".error")
                    .description("Failed execution of " + methodName)
                    .tag("class", joinPoint.getTarget().getClass().getSimpleName())
                    .tag("method", joinPoint.getSignature().getName())
                    .tag("exception", e.getClass().getSimpleName()).register(meterRegistry));

            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Method {} failed after {}ms [correlationId={}]: {}", methodName,
                    executionTime, correlationId, e.getMessage());

            throw e;
        }
    }

    /**
     * Around advice for all fraud service controller methods. Provides comprehensive API timing
     * without requiring annotations.
     */
    @Around("execution(* com.globobank.fraud.controller.*.*(..))")
    public Object timeControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = MDC.get("correlationId");
        String methodName = "fraud.api." + joinPoint.getSignature().getName();

        Timer timer = Timer.builder(methodName).description("API endpoint execution time")
                .tag("controller", joinPoint.getTarget().getClass().getSimpleName())
                .tag("endpoint", joinPoint.getSignature().getName()).register(meterRegistry);

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            sample.stop(timer);

            // Always check SLA for API endpoints
            if (executionTime > SLA_THRESHOLD_MS) {
                handleSlaViolation(methodName, executionTime, SLA_THRESHOLD_MS, correlationId);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Record API error
            meterRegistry.counter("fraud.api.errors.total", "controller",
                    joinPoint.getTarget().getClass().getSimpleName(), "endpoint",
                    joinPoint.getSignature().getName(), "exception", e.getClass().getSimpleName())
                    .increment();

            logger.error("API endpoint {} failed after {}ms [correlationId={}]: {}", methodName,
                    executionTime, correlationId, e.getMessage());

            throw e;
        }
    }

    /**
     * Generate metric name for the method being timed.
     */
    private String getMethodName(ProceedingJoinPoint joinPoint, Timed timed) {
        if (!timed.value().isEmpty()) {
            return timed.value();
        }

        return "fraud.method." + joinPoint.getTarget().getClass().getSimpleName().toLowerCase()
                + "." + joinPoint.getSignature().getName();
    }

    /**
     * Handle SLA violation detection and alerting.
     */
    private void handleSlaViolation(String methodName, long executionTime, long slaThreshold,
            String correlationId) {
        // Record SLA violation metric
        meterRegistry.counter("fraud.sla.violations.total", "method", methodName, "threshold",
                String.valueOf(slaThreshold)).increment();

        // Log SLA violation for alerting
        logger.warn("SLA VIOLATION: {} executed in {}ms (threshold: {}ms) [correlationId={}]",
                methodName, executionTime, slaThreshold, correlationId);
    }
}
