package com.globobank.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the GloboBank Fraud Detection Service.
 * 
 * This microservice provides real-time transaction risk assessment through
 * fraud detection algorithms and known fraudulent card pattern matching.
 * 
 * Features:
 * - REST API for transaction fraud scoring
 * - OAuth 2.0 + PKCE authentication
 * - PostgreSQL database integration
 * - Sub-200ms response time performance
 * - Circuit breaker patterns for resilience
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@SpringBootApplication
public class FraudServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudServiceApplication.class, args);
    }
}