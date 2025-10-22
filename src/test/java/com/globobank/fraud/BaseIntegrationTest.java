package com.globobank.fraud;

import com.globobank.fraud.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for integration tests in GloboBank Fraud Detection Service.
 * 
 * Provides common test infrastructure including: - PostgreSQL TestContainer setup - OAuth2 mock
 * configuration - Test data management utilities - Performance testing utilities for sub-200ms SLA
 * validation
 * 
 * All integration tests should extend this class to ensure consistent test environment and proper
 * cleanup between tests.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@SpringBootTest(classes = FraudServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(TestConfig.class)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected DataSource dataSource;

    @Container
    static PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15.4-alpine").withDatabaseName("fraud_test")
                    .withUsername("fraud_user").withPassword("fraud_pass").withReuse(false); // TestContainers
                                                                                             // handles
                                                                                             // resource
                                                                                             // cleanup
                                                                                             // automatically

    /**
     * Configure test properties dynamically based on TestContainer setup.
     *
     * @param registry Dynamic property registry for test configuration
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA configuration for testing
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");

        // Security configuration for testing
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:8080/auth/realms/test");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:8080/auth/realms/test/protocol/openid_connect/certs");

        // Fraud service test configuration
        registry.add("fraud.security.rate-limit.requests-per-minute", () -> "1000"); // Higher limit
                                                                                     // for testing
        registry.add("fraud.detection.default-timeout", () -> "2000"); // Shorter timeout for
                                                                       // testing
    }

    /**
     * Setup method run before each test. Override this method in subclasses for test-specific
     * setup.
     */
    @BeforeEach
    void setUp() {
        // Common setup for all integration tests
        // Subclasses can override for specific setup
    }

    /**
     * Get the base URL for the test server.
     *
     * @return Base URL for making HTTP requests during tests
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port + "/fraud/v1";
    }

    /**
     * Utility method to measure execution time for performance testing.
     *
     * @param operation The operation to time
     * @return Execution time in milliseconds
     */
    protected long measureExecutionTime(Runnable operation) {
        long startTime = System.currentTimeMillis();
        operation.run();
        return System.currentTimeMillis() - startTime;
    }
}
