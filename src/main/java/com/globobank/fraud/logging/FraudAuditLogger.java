package com.globobank.fraud.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Fraud detection audit logger for GloboBank Fraud Detection Service.
 * 
 * Provides comprehensive audit logging for fraud detection operations including transaction
 * analysis, risk assessments, and system events. Supports regulatory compliance and forensic
 * analysis requirements.
 * 
 * Features: - Structured JSON logging for audit compliance - Correlation ID tracking for request
 * tracing - Performance metrics logging for SLA monitoring - Security event logging for fraud
 * investigation - Anonymized data logging for privacy protection
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Component
public class FraudAuditLogger {

    private static final Logger auditLogger = LoggerFactory.getLogger("FRAUD_AUDIT");
    private static final Logger performanceLogger = LoggerFactory.getLogger("FRAUD_PERFORMANCE");
    private static final Logger securityLogger = LoggerFactory.getLogger("FRAUD_SECURITY");

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ObjectMapper objectMapper;

    @Autowired
    public FraudAuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Log fraud risk assessment completion for audit purposes.
     *
     * @param transactionId Transaction identifier
     * @param accountNumber Account number (will be masked)
     * @param riskScore Calculated risk score
     * @param riskLevel Risk level assessment
     * @param recommendation Processing recommendation
     * @param processingTimeMs Processing time in milliseconds
     */
    public void logRiskAssessment(String transactionId, String accountNumber, double riskScore,
            String riskLevel, String recommendation, long processingTimeMs) {
        try {
            Map<String, Object> auditEvent = createBaseAuditEvent("RISK_ASSESSMENT_COMPLETED");
            auditEvent.put("transactionId", transactionId);
            auditEvent.put("accountNumber", maskAccountNumber(accountNumber));
            auditEvent.put("riskScore", riskScore);
            auditEvent.put("riskLevel", riskLevel);
            auditEvent.put("recommendation", recommendation);
            auditEvent.put("processingTimeMs", processingTimeMs);
            auditEvent.put("slaCompliant", processingTimeMs <= 200);

            auditLogger.info(objectMapper.writeValueAsString(auditEvent));

            // Also log performance metrics
            logPerformanceMetric("risk_assessment", processingTimeMs, transactionId);

        } catch (Exception e) {
            auditLogger.error("Failed to log risk assessment audit event for transaction {}: {}",
                    transactionId, e.getMessage());
        }
    }

    /**
     * Log fraud detection API request initiation.
     *
     * @param transactionId Transaction identifier
     * @param accountNumber Account number (will be masked)
     * @param amount Transaction amount
     * @param currency Transaction currency
     * @param channel Transaction channel
     * @param clientIp Client IP address
     */
    public void logApiRequest(String transactionId, String accountNumber, String amount,
            String currency, String channel, String clientIp) {
        try {
            Map<String, Object> auditEvent = createBaseAuditEvent("API_REQUEST_RECEIVED");
            auditEvent.put("transactionId", transactionId);
            auditEvent.put("accountNumber", maskAccountNumber(accountNumber));
            auditEvent.put("amount", amount);
            auditEvent.put("currency", currency);
            auditEvent.put("channel", channel);
            auditEvent.put("clientIp", maskIpAddress(clientIp));

            auditLogger.info(objectMapper.writeValueAsString(auditEvent));

        } catch (Exception e) {
            auditLogger.error("Failed to log API request audit event for transaction {}: {}",
                    transactionId, e.getMessage());
        }
    }

    /**
     * Log security events such as authentication failures, rate limiting, etc.
     *
     * @param eventType Type of security event
     * @param details Event details
     * @param clientIp Client IP address
     * @param userId User identifier (if available)
     */
    public void logSecurityEvent(String eventType, Map<String, Object> details, String clientIp,
            String userId) {
        try {
            Map<String, Object> securityEvent = createBaseAuditEvent(eventType);
            securityEvent.put("clientIp", maskIpAddress(clientIp));
            if (userId != null) {
                securityEvent.put("userId", userId);
            }
            if (details != null) {
                securityEvent.putAll(details);
            }

            securityLogger.warn(objectMapper.writeValueAsString(securityEvent));

        } catch (Exception e) {
            securityLogger.error("Failed to log security event {}: {}", eventType, e.getMessage());
        }
    }

    /**
     * Log performance metrics for SLA monitoring.
     *
     * @param operation Operation name
     * @param durationMs Duration in milliseconds
     * @param transactionId Transaction identifier (optional)
     */
    public void logPerformanceMetric(String operation, long durationMs, String transactionId) {
        try {
            Map<String, Object> performanceEvent = createBaseAuditEvent("PERFORMANCE_METRIC");
            performanceEvent.put("operation", operation);
            performanceEvent.put("durationMs", durationMs);
            performanceEvent.put("slaViolation", durationMs > 200);
            if (transactionId != null) {
                performanceEvent.put("transactionId", transactionId);
            }

            if (durationMs > 200) {
                performanceLogger.warn(objectMapper.writeValueAsString(performanceEvent));
            } else {
                performanceLogger.info(objectMapper.writeValueAsString(performanceEvent));
            }

        } catch (Exception e) {
            performanceLogger.error("Failed to log performance metric for operation {}: {}",
                    operation, e.getMessage());
        }
    }

    /**
     * Log fraud detection model execution details.
     *
     * @param transactionId Transaction identifier
     * @param modelVersion Model version used
     * @param features Features used in analysis
     * @param score Model output score
     * @param executionTimeMs Model execution time
     */
    public void logModelExecution(String transactionId, String modelVersion,
            Map<String, Object> features, double score, long executionTimeMs) {
        try {
            Map<String, Object> modelEvent = createBaseAuditEvent("MODEL_EXECUTION");
            modelEvent.put("transactionId", transactionId);
            modelEvent.put("modelVersion", modelVersion);
            modelEvent.put("featureCount", features != null ? features.size() : 0);
            modelEvent.put("score", score);
            modelEvent.put("executionTimeMs", executionTimeMs);

            auditLogger.info(objectMapper.writeValueAsString(modelEvent));

        } catch (Exception e) {
            auditLogger.error("Failed to log model execution event for transaction {}: {}",
                    transactionId, e.getMessage());
        }
    }

    /**
     * Log system errors and exceptions for monitoring and alerting.
     *
     * @param operation Operation that failed
     * @param error Error details
     * @param transactionId Transaction identifier (if available)
     */
    public void logSystemError(String operation, String error, String transactionId) {
        try {
            Map<String, Object> errorEvent = createBaseAuditEvent("SYSTEM_ERROR");
            errorEvent.put("operation", operation);
            errorEvent.put("error", error);
            if (transactionId != null) {
                errorEvent.put("transactionId", transactionId);
            }

            auditLogger.error(objectMapper.writeValueAsString(errorEvent));

        } catch (Exception e) {
            auditLogger.error("Failed to log system error event for operation {}: {}", operation,
                    e.getMessage());
        }
    }

    /**
     * Create base audit event structure with common fields.
     */
    private Map<String, Object> createBaseAuditEvent(String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().format(ISO_FORMATTER));
        event.put("eventType", eventType);
        event.put("service", "fraud-detection-service");
        event.put("version", "1.0.0");
        event.put("correlationId", MDC.get("correlationId"));
        event.put("host", System.getenv("HOSTNAME"));
        return event;
    }

    /**
     * Mask account number for privacy protection. Shows only first 4 and last 4 digits.
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return "****";
        }

        String first4 = accountNumber.substring(0, 4);
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return first4 + "****" + last4;
    }

    /**
     * Mask IP address for privacy protection. Shows only first 3 octets for IPv4.
     */
    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return "unknown";
        }

        if (ipAddress.contains(".")) {
            // IPv4
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
            }
        }

        return "masked";
    }
}
