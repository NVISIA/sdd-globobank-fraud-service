package com.globobank.fraud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring database health and connection status. Implements circuit breaker pattern
 * for database operations.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Service
public class DatabaseHealthService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthService.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    // Circuit breaker state
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);

    // Configuration
    private static final long CIRCUIT_BREAKER_TIMEOUT = 30000; // 30 seconds
    private static final long MAX_CONSECUTIVE_FAILURES = 3;
    private static final int CONNECTION_TIMEOUT_MS = 5000; // 5 seconds

    @Autowired
    public DatabaseHealthService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Check if the database is available and responsive.
     * 
     * @return true if database is healthy, false otherwise
     */
    public boolean isDatabaseHealthy() {
        if (isCircuitOpen()) {
            logger.warn("Circuit breaker is open - skipping database health check");
            return false;
        }

        try {
            // Simple connectivity test
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            onSuccess();
            return true;
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            onFailure();
            return false;
        }
    }

    /**
     * Get detailed database connection information.
     * 
     * @return DatabaseConnectionInfo object with connection details
     */
    public DatabaseConnectionInfo getConnectionInfo() {
        DatabaseConnectionInfo info = new DatabaseConnectionInfo();
        info.setTimestamp(LocalDateTime.now());
        info.setHealthy(isDatabaseHealthy());
        info.setCircuitOpen(circuitOpen.get());
        info.setConsecutiveFailures(consecutiveFailures.get());

        try (Connection connection = dataSource.getConnection()) {
            info.setDatabaseProductName(connection.getMetaData().getDatabaseProductName());
            info.setDatabaseProductVersion(connection.getMetaData().getDatabaseProductVersion());
            info.setConnectionValid(connection.isValid(CONNECTION_TIMEOUT_MS / 1000));

            // Set default connection pool values (actual implementation would depend on connection
            // pool)
            info.setActiveConnections(-1); // Unknown
            info.setIdleConnections(-1); // Unknown
            info.setMaxConnections(-1); // Unknown
        } catch (SQLException e) {
            logger.error("Error getting database connection info", e);
            info.setError(e.getMessage());
        }

        return info;
    }

    /**
     * Test database connectivity with a simple query.
     * 
     * @return true if test query succeeds, false otherwise
     */
    public boolean testConnectivity() {
        if (isCircuitOpen()) {
            return false;
        }

        try {
            Long result = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM fraudulent_cards WHERE active = true", Long.class);
            onSuccess();
            logger.debug("Database connectivity test passed. Active fraudulent cards: {}", result);
            return true;
        } catch (Exception e) {
            logger.error("Database connectivity test failed", e);
            onFailure();
            return false;
        }
    }

    /**
     * Reset the circuit breaker to closed state. Use with caution - typically for administrative
     * purposes.
     */
    public void resetCircuitBreaker() {
        circuitOpen.set(false);
        consecutiveFailures.set(0);
        lastFailureTime.set(0);
        logger.info("Circuit breaker manually reset");
    }

    /**
     * Check if the circuit breaker is open.
     * 
     * @return true if circuit is open (blocking requests), false if closed
     */
    private boolean isCircuitOpen() {
        if (!circuitOpen.get()) {
            return false;
        }

        // Check if timeout has passed and we should try again
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        if (timeSinceLastFailure > CIRCUIT_BREAKER_TIMEOUT) {
            logger.info("Circuit breaker timeout reached, attempting to close circuit");
            circuitOpen.set(false);
            return false;
        }

        return true;
    }

    /**
     * Handle successful database operation.
     */
    private void onSuccess() {
        consecutiveFailures.set(0);
        if (circuitOpen.get()) {
            circuitOpen.set(false);
            logger.info("Circuit breaker closed after successful operation");
        }
    }

    /**
     * Handle failed database operation.
     */
    private void onFailure() {
        long failures = consecutiveFailures.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        if (failures >= MAX_CONSECUTIVE_FAILURES && !circuitOpen.get()) {
            circuitOpen.set(true);
            logger.error("Circuit breaker opened after {} consecutive failures", failures);
        }
    }

    /**
     * Data class for database connection information.
     */
    public static class DatabaseConnectionInfo {
        private LocalDateTime timestamp;
        private boolean healthy;
        private boolean circuitOpen;
        private long consecutiveFailures;
        private String databaseProductName;
        private String databaseProductVersion;
        private boolean connectionValid;
        private int activeConnections;
        private int idleConnections;
        private int maxConnections;
        private String error;

        // Getters and setters
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public boolean isCircuitOpen() {
            return circuitOpen;
        }

        public void setCircuitOpen(boolean circuitOpen) {
            this.circuitOpen = circuitOpen;
        }

        public long getConsecutiveFailures() {
            return consecutiveFailures;
        }

        public void setConsecutiveFailures(long consecutiveFailures) {
            this.consecutiveFailures = consecutiveFailures;
        }

        public String getDatabaseProductName() {
            return databaseProductName;
        }

        public void setDatabaseProductName(String databaseProductName) {
            this.databaseProductName = databaseProductName;
        }

        public String getDatabaseProductVersion() {
            return databaseProductVersion;
        }

        public void setDatabaseProductVersion(String databaseProductVersion) {
            this.databaseProductVersion = databaseProductVersion;
        }

        public boolean isConnectionValid() {
            return connectionValid;
        }

        public void setConnectionValid(boolean connectionValid) {
            this.connectionValid = connectionValid;
        }

        public int getActiveConnections() {
            return activeConnections;
        }

        public void setActiveConnections(int activeConnections) {
            this.activeConnections = activeConnections;
        }

        public int getIdleConnections() {
            return idleConnections;
        }

        public void setIdleConnections(int idleConnections) {
            this.idleConnections = idleConnections;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return "DatabaseConnectionInfo{" + "timestamp=" + timestamp + ", healthy=" + healthy
                    + ", circuitOpen=" + circuitOpen + ", consecutiveFailures="
                    + consecutiveFailures + ", databaseProductName='" + databaseProductName + '\''
                    + ", databaseProductVersion='" + databaseProductVersion + '\''
                    + ", connectionValid=" + connectionValid + ", activeConnections="
                    + activeConnections + ", idleConnections=" + idleConnections
                    + ", maxConnections=" + maxConnections + ", error='" + error + '\'' + '}';
        }
    }
}
