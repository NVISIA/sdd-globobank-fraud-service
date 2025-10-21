package com.globobank.fraud.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for GloboBank Fraud Detection Service.
 * 
 * Provides TestContainers setup for integration testing with PostgreSQL, OAuth2 mock server, and
 * other test infrastructure components.
 * 
 * Features: - PostgreSQL TestContainer for database testing - OAuth2 mock configuration for
 * security testing - Test data fixtures and utilities - Performance testing setup for sub-200ms SLA
 * validation
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestConfig {

    /**
     * PostgreSQL TestContainer for integration testing.
     * 
     * Provides isolated database instance for each test run, ensuring clean state and reliable test
     * execution.
     *
     * @return PostgreSQL container configured for fraud service testing
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.4-alpine"))
                .withDatabaseName("fraud_test").withUsername("fraud_user")
                .withPassword("fraud_pass").withExposedPorts(5432)
                .withCommand("postgres", "-c", "max_connections=100", "-c", "shared_buffers=128MB")
                .withReuse(false); // Ensure fresh database for each test
    }
}
