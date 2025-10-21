package com.globobank.fraud.controller;

import com.globobank.fraud.model.RiskAssessment;
import com.globobank.fraud.model.TransactionRequest;
import com.globobank.fraud.monitoring.PerformanceMonitoringAspect;
import com.globobank.fraud.logging.FraudAuditLogger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Fraud Detection Controller for GloboBank Fraud Detection Service.
 * 
 * Provides REST API endpoints for real-time transaction fraud risk assessment. Implements the fraud
 * detection API contract with comprehensive input validation, security controls, performance
 * monitoring, and structured error handling.
 * 
 * Key Features: - Real-time fraud risk assessment (< 200ms SLA) - OAuth2 + PKCE authentication and
 * authorization - Comprehensive input validation and sanitization - Performance monitoring and
 * metrics collection - Correlation ID tracking for audit and debugging - Circuit breaker pattern
 * for database resilience
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@RestController
@RequestMapping("/risk-assessments")
@Validated
@CrossOrigin(origins = "${fraud.security.cors.allowed-origins:*}")
public class FraudController {

    private static final Logger logger = LoggerFactory.getLogger(FraudController.class);

    private final Timer riskAssessmentTimer;
    private final Counter riskAssessmentCounter;
    private final FraudAuditLogger auditLogger;

    @Autowired
    public FraudController(Timer riskAssessmentTimer, Counter riskAssessmentCounter,
            FraudAuditLogger auditLogger) {
        this.riskAssessmentTimer = riskAssessmentTimer;
        this.riskAssessmentCounter = riskAssessmentCounter;
        this.auditLogger = auditLogger;
    }

    /**
     * Assess fraud risk for a transaction.
     * 
     * Primary endpoint for real-time fraud detection. Analyzes transaction characteristics against
     * fraud patterns and returns comprehensive risk assessment with processing recommendations.
     * 
     * @param transactionRequest Transaction details for risk assessment
     * @return Risk assessment with score, level, and recommendation
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_fraud:assess')")
    @PerformanceMonitoringAspect.Timed(value = "fraud.api.risk.assessment.duration",
            description = "Time taken to complete fraud risk assessment", slaCheck = true,
            slaThresholdMs = 200)
    public ResponseEntity<RiskAssessment> assessRisk(
            @Valid @RequestBody TransactionRequest transactionRequest,
            HttpServletRequest httpRequest) {
        String correlationId = MDC.get("correlationId");
        String clientIp = getClientIpAddress(httpRequest);

        logger.info("Starting fraud risk assessment for transaction {} [correlationId={}]",
                transactionRequest.getTransactionId(), correlationId);

        // Log API request for audit
        auditLogger.logApiRequest(transactionRequest.getTransactionId(),
                transactionRequest.getAccountNumber(), transactionRequest.getAmount().toString(),
                transactionRequest.getCurrency(), transactionRequest.getChannel(), clientIp);

        Timer.Sample sample = Timer.start();
        long startTime = System.currentTimeMillis();

        try {
            // Increment request counter
            riskAssessmentCounter.increment();

            // Perform risk assessment (mock implementation for now)
            RiskAssessment assessment = performRiskAssessment(transactionRequest, correlationId);

            // Record processing time
            long processingTime = System.currentTimeMillis() - startTime;
            assessment.setProcessingTimeMs(processingTime);

            // Stop timer
            sample.stop(riskAssessmentTimer);

            logger.info(
                    "Completed fraud risk assessment for transaction {} with score {} in {}ms [correlationId={}]",
                    transactionRequest.getTransactionId(), assessment.getRiskScore(),
                    processingTime, correlationId);

            // Log audit event for completed assessment
            auditLogger.logRiskAssessment(transactionRequest.getTransactionId(),
                    transactionRequest.getAccountNumber(), assessment.getRiskScore(),
                    assessment.getRiskLevel().toString(), assessment.getRecommendation().toString(),
                    processingTime);

            return ResponseEntity.ok(assessment);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            // Log system error
            auditLogger.logSystemError("risk_assessment", e.getMessage(),
                    transactionRequest.getTransactionId());

            logger.error(
                    "Failed to complete fraud risk assessment for transaction {} after {}ms [correlationId={}]: {}",
                    transactionRequest.getTransactionId(), processingTime, correlationId,
                    e.getMessage());
            throw e;
        }
    }

    /**
     * Get risk assessment by transaction ID.
     * 
     * Retrieves previously calculated risk assessment for audit purposes and historical analysis.
     * 
     * @param transactionId Transaction identifier
     * @return Risk assessment if found
     */
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAuthority('SCOPE_fraud:read')")
    @PerformanceMonitoringAspect.Timed(value = "fraud.api.risk.retrieval.duration",
            description = "Time taken to retrieve risk assessment")
    public ResponseEntity<RiskAssessment> getRiskAssessment(@PathVariable String transactionId) {
        String correlationId = MDC.get("correlationId");

        logger.info("Retrieving risk assessment for transaction {} [correlationId={}]",
                transactionId, correlationId);

        // Mock implementation - would typically query database
        // For now, return a sample assessment indicating not found
        logger.warn(
                "Risk assessment retrieval not yet implemented for transaction {} [correlationId={}]",
                transactionId, correlationId);

        return ResponseEntity.notFound().build();
    }

    /**
     * Health check endpoint specific to fraud detection functionality.
     * 
     * @return Health status of fraud detection service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "fraud-detection");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");

        return ResponseEntity.ok(health);
    }

    /**
     * Perform the actual fraud risk assessment logic.
     * 
     * This is a mock implementation that demonstrates the expected behavior. In a real
     * implementation, this would involve: - Machine learning model inference - Historical pattern
     * analysis - Geographic and behavioral analysis - Real-time rule engine evaluation
     * 
     * @param request Transaction request
     * @param correlationId Correlation ID for tracking
     * @return Risk assessment result
     */
    private RiskAssessment performRiskAssessment(TransactionRequest request, String correlationId) {
        // Mock risk calculation based on simple rules
        double riskScore = calculateMockRiskScore(request);

        RiskAssessment.RiskLevel riskLevel = determineRiskLevel(riskScore);
        RiskAssessment.Recommendation recommendation = determineRecommendation(riskLevel);

        RiskAssessment assessment = new RiskAssessment(request.getTransactionId(),
                request.getAccountNumber(), riskScore, riskLevel, recommendation);

        assessment.setAssessmentTimestamp(LocalDateTime.now());
        assessment.setCorrelationId(correlationId);

        // Add mock risk factors
        assessment.setRiskFactors(Arrays.asList(
                new RiskAssessment.RiskFactor("AMOUNT_ANALYSIS", riskScore * 0.3,
                        "Transaction amount compared to account history"),
                new RiskAssessment.RiskFactor("LOCATION_ANALYSIS", riskScore * 0.2,
                        "Geographic location compared to normal patterns"),
                new RiskAssessment.RiskFactor("TEMPORAL_ANALYSIS", riskScore * 0.25,
                        "Transaction time compared to typical behavior"),
                new RiskAssessment.RiskFactor("MERCHANT_ANALYSIS", riskScore * 0.25,
                        "Merchant category and history analysis")));

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modelVersion", "1.0.0");
        metadata.put("assessmentEngine", "fraud-detection-v1");
        metadata.put("processingNode", "node-1");
        assessment.setMetadata(metadata);

        return assessment;
    }

    /**
     * Calculate mock risk score based on transaction characteristics.
     */
    private double calculateMockRiskScore(TransactionRequest request) {
        double score = 0.0;

        // Amount-based risk (higher amounts = higher risk)
        if (request.getAmount().doubleValue() > 1000) {
            score += 20.0;
        } else if (request.getAmount().doubleValue() > 500) {
            score += 10.0;
        }

        // Channel-based risk
        switch (request.getChannel()) {
            case "ONLINE":
                score += 15.0;
                break;
            case "ATM":
                score += 5.0;
                break;
            case "POS":
                score += 3.0;
                break;
        }

        // Time-based risk (night transactions = higher risk)
        int hour = request.getTimestamp().getHour();
        if (hour < 6 || hour > 22) {
            score += 10.0;
        }

        // Add some randomness for demonstration
        score += Math.random() * 20;

        return Math.min(score, 100.0); // Cap at 100
    }

    /**
     * Determine risk level based on numerical score.
     */
    private RiskAssessment.RiskLevel determineRiskLevel(double score) {
        if (score >= 75) {
            return RiskAssessment.RiskLevel.CRITICAL;
        } else if (score >= 50) {
            return RiskAssessment.RiskLevel.HIGH;
        } else if (score >= 25) {
            return RiskAssessment.RiskLevel.MEDIUM;
        } else {
            return RiskAssessment.RiskLevel.LOW;
        }
    }

    /**
     * Determine processing recommendation based on risk level.
     */
    private RiskAssessment.Recommendation determineRecommendation(RiskAssessment.RiskLevel level) {
        switch (level) {
            case CRITICAL:
                return RiskAssessment.Recommendation.BLOCK;
            case HIGH:
                return RiskAssessment.Recommendation.CHALLENGE;
            case MEDIUM:
                return RiskAssessment.Recommendation.REVIEW;
            case LOW:
            default:
                return RiskAssessment.Recommendation.APPROVE;
        }
    }

    /**
     * Extract client IP address from HTTP request. Handles X-Forwarded-For and X-Real-IP headers
     * for load balancer scenarios.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
