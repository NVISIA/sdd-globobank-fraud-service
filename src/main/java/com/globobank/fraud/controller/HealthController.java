package com.globobank.fraud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for GloboBank Fraud Detection Service.
 * 
 * Provides comprehensive health status information including database connectivity, fraud detection
 * capabilities, and system readiness for load balancer integration.
 * 
 * Health Check Features: - Database connectivity verification - Fraud detection system readiness -
 * Response time monitoring - Service availability status - Component-level health details
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@RestController
@RequestMapping("/fraud/v1")
public class HealthController {

    private final DataSource dataSource;

    @Autowired
    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Basic health check endpoint for load balancer integration.
     * 
     * Returns simple UP/DOWN status for quick health verification. This endpoint requires no
     * authentication for infrastructure monitoring.
     * 
     * @return ResponseEntity with basic health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Check database connectivity
            boolean databaseHealthy = checkDatabaseHealth();

            // Determine overall health status
            String status = databaseHealthy ? "UP" : "DOWN";

            health.put("status", status);
            health.put("timestamp", Instant.now().toString());

            // Add basic component status
            Map<String, String> components = new HashMap<>();
            components.put("database", databaseHealthy ? "UP" : "DOWN");
            components.put("fraud-rules", "UP"); // Simple check for now
            health.put("components", components);

            // Return appropriate HTTP status
            if ("UP".equals(status)) {
                return ResponseEntity.ok(health);
            } else {
                return ResponseEntity.status(503).body(health);
            }

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("timestamp", Instant.now().toString());
            health.put("error", "Health check failed: " + e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Detailed health check endpoint with comprehensive system information.
     * 
     * Provides detailed health status for operational monitoring and debugging. Includes
     * component-level details and performance metrics.
     * 
     * @return ResponseEntity with detailed health information
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();

            // Check all components
            boolean databaseHealthy = checkDatabaseHealth();
            boolean fraudRulesHealthy = checkFraudRulesHealth();

            long responseTime = System.currentTimeMillis() - startTime;

            // Determine overall health
            boolean overallHealthy = databaseHealthy && fraudRulesHealthy;
            String status = overallHealthy ? "UP" : "DOWN";

            health.put("status", status);
            health.put("timestamp", Instant.now().toString());
            health.put("responseTime", responseTime + "ms");

            // Detailed component status
            Map<String, Object> components = new HashMap<>();

            // Database component
            Map<String, Object> database = new HashMap<>();
            database.put("status", databaseHealthy ? "UP" : "DOWN");
            database.put("description", "PostgreSQL database connectivity");
            components.put("database", database);

            // Fraud rules component
            Map<String, Object> fraudRules = new HashMap<>();
            fraudRules.put("status", fraudRulesHealthy ? "UP" : "DOWN");
            fraudRules.put("description", "Fraud detection rules engine");
            components.put("fraud-rules", fraudRules);

            health.put("components", components);

            // System information
            Map<String, Object> system = new HashMap<>();
            system.put("javaVersion", System.getProperty("java.version"));
            system.put("maxMemory", Runtime.getRuntime().maxMemory());
            system.put("totalMemory", Runtime.getRuntime().totalMemory());
            system.put("freeMemory", Runtime.getRuntime().freeMemory());
            health.put("system", system);

            // Return appropriate HTTP status
            if (overallHealthy) {
                return ResponseEntity.ok(health);
            } else {
                return ResponseEntity.status(503).body(health);
            }

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("timestamp", Instant.now().toString());
            health.put("error", "Detailed health check failed: " + e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Readiness probe endpoint for Kubernetes deployment.
     * 
     * Indicates whether the service is ready to receive traffic. Used by Kubernetes for readiness
     * probes and load balancer configuration.
     * 
     * @return ResponseEntity with readiness status
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> readiness = new HashMap<>();

        try {
            // Check if service is ready to handle requests
            boolean databaseReady = checkDatabaseHealth();
            boolean serviceReady = databaseReady; // Add more readiness checks as needed

            readiness.put("ready", serviceReady);
            readiness.put("timestamp", Instant.now().toString());

            if (serviceReady) {
                return ResponseEntity.ok(readiness);
            } else {
                return ResponseEntity.status(503).body(readiness);
            }

        } catch (Exception e) {
            readiness.put("ready", false);
            readiness.put("timestamp", Instant.now().toString());
            readiness.put("error", e.getMessage());
            return ResponseEntity.status(503).body(readiness);
        }
    }

    /**
     * Liveness probe endpoint for Kubernetes deployment.
     * 
     * Indicates whether the service is alive and should be restarted if failing. Used by Kubernetes
     * for liveness probes.
     * 
     * @return ResponseEntity with liveness status
     */
    @GetMapping("/health/live")
    public ResponseEntity<Map<String, Object>> live() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("alive", true);
        liveness.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(liveness);
    }

    /**
     * Checks database connectivity health.
     * 
     * @return true if database is healthy, false otherwise
     */
    private boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Checks fraud detection rules health.
     * 
     * @return true if fraud rules are healthy, false otherwise
     */
    private boolean checkFraudRulesHealth() {
        try {
            // Simple check for now - can be enhanced with actual rule validation
            // In future iterations, this could check:
            // - Rule engine connectivity
            // - Model availability
            // - Configuration validity
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
