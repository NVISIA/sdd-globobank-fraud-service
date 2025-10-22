package com.globobank.fraud.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

/**
 * Metrics configuration for GloboBank Fraud Detection Service.
 * 
 * Configures performance monitoring, SLA tracking, and business metrics collection to ensure
 * sub-200ms response times and comprehensive fraud detection analytics.
 * 
 * Key Metrics: - Request processing time (SLA: < 200ms) - Risk assessment accuracy rates - Database
 * connection pool performance - Circuit breaker status and failures - Rate limiting violations -
 * Fraud detection outcomes
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Configuration
public class MetricsConfig {

    /**
     * Common tags for all metrics to enable proper filtering and aggregation.
     *
     * @return MeterRegistryCustomizer with standard tags
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "fraud-detection-service",
                "environment", "default", // Will be overridden by environment-specific profiles
                "version", "1.0.0");
    }

    /**
     * Timer for measuring risk assessment processing time. Critical for SLA monitoring (target: <
     * 200ms).
     *
     * @param meterRegistry Micrometer meter registry
     * @return Timer for risk assessment operations
     */
    @Bean
    public Timer riskAssessmentTimer(MeterRegistry meterRegistry) {
        return Timer.builder("fraud.risk.assessment.duration")
                .description("Time taken to complete risk assessment")
                .tag("operation", "risk-assessment").register(meterRegistry);
    }

    /**
     * Counter for tracking risk assessment outcomes. Used for accuracy analysis and fraud detection
     * effectiveness.
     *
     * @param meterRegistry Micrometer meter registry
     * @return Counter for risk assessment results
     */
    @Bean
    public Counter riskAssessmentCounter(MeterRegistry meterRegistry) {
        return Counter.builder("fraud.risk.assessment.total")
                .description("Total number of risk assessments performed")
                .tag("operation", "risk-assessment").register(meterRegistry);
    }

    /**
     * Counter for tracking database operations performance. Monitors database connectivity and
     * query performance.
     *
     * @param meterRegistry Micrometer meter registry
     * @return Counter for database operations
     */
    @Bean
    public Counter databaseOperationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("fraud.database.operations.total")
                .description("Total number of database operations").tag("operation", "database")
                .register(meterRegistry);
    }

    /**
     * Timer for measuring database query execution time. Critical for identifying database
     * performance bottlenecks.
     *
     * @param meterRegistry Micrometer meter registry
     * @return Timer for database operations
     */
    @Bean
    public Timer databaseOperationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("fraud.database.query.duration")
                .description("Time taken to execute database queries")
                .tag("operation", "database-query").register(meterRegistry);
    }

    /**
     * Counter for tracking rate limiting violations. Monitors API abuse and helps tune rate
     * limiting policies.
     *
     * @param meterRegistry Micrometer meter registry
     * @return Counter for rate limit violations
     */
    @Bean
    public Counter rateLimitViolationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("fraud.security.rate.limit.violations.total")
                .description("Total number of rate limit violations")
                .tag("operation", "rate-limiting").register(meterRegistry);
    }

    /**
     * Counter for tracking circuit breaker state changes. Monitors system resilience and failure
     * patterns.
     *
     * @param meterRegistry Micrometer meter registry
     * @return Counter for circuit breaker events
     */
    @Bean
    public Counter circuitBreakerCounter(MeterRegistry meterRegistry) {
        return Counter.builder("fraud.circuit.breaker.events.total")
                .description("Total number of circuit breaker state changes")
                .tag("operation", "circuit-breaker").register(meterRegistry);
    }

    /**
     * Timer for measuring overall API response time. Primary SLA metric for fraud detection
     * service.
     *
     * @param meterRegistry Micrometer meter registry
     * @return Timer for API response times
     */
    @Bean
    public Timer apiResponseTimer(MeterRegistry meterRegistry) {
        return Timer.builder("fraud.api.response.duration")
                .description("Time taken for complete API request processing")
                .tag("operation", "api-request").register(meterRegistry);
    }
}
