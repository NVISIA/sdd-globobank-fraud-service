package com.globobank.fraud.service;

import com.globobank.fraud.model.TransactionRequest;
import com.globobank.fraud.model.RiskAssessment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Service for audit logging of transaction processing activities. Provides comprehensive logging
 * for compliance and monitoring purposes.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Service
public class TransactionAuditService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionAuditService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    private static final DateTimeFormatter AUDIT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    public TransactionAuditService() {}

    /**
     * Log transaction processing start event.
     * 
     * @param transactionRequest The transaction being processed
     */
    public void logTransactionStart(TransactionRequest transactionRequest) {
        String timestamp = LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT);
        String maskedCardNumber = maskCardNumber(transactionRequest.getCreditCardNumber());

        String auditMessage = String.format("TRANSACTION_START|%s|%s|%s", timestamp,
                transactionRequest.getTransactionId(), maskedCardNumber);

        auditLogger.info(auditMessage);
        logger.debug("Logged transaction start for: {}", transactionRequest.getTransactionId());
    }

    /**
     * Log transaction processing completion event.
     * 
     * @param transactionRequest The original transaction request
     * @param riskAssessment The fraud risk assessment result
     * @param processingTimeMs Time taken to process the transaction
     */
    public void logTransactionComplete(TransactionRequest transactionRequest,
            RiskAssessment riskAssessment, long processingTimeMs) {
        String timestamp = LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT);
        String maskedCardNumber = maskCardNumber(transactionRequest.getCreditCardNumber());

        String auditMessage = String.format("TRANSACTION_COMPLETE|%s|%s|%s|%.3f|%s|%dms", timestamp,
                transactionRequest.getTransactionId(), maskedCardNumber,
                riskAssessment.getRiskScore(), riskAssessment.getFraudulent() ? "FRAUD" : "CLEAN",
                processingTimeMs);

        auditLogger.info(auditMessage);
        logger.debug("Logged transaction completion for: {}",
                transactionRequest.getTransactionId());
    }

    /**
     * Log transaction processing error event.
     * 
     * @param transactionRequest The transaction that failed
     * @param error The error that occurred
     * @param processingTimeMs Time taken before error occurred
     */
    public void logTransactionError(TransactionRequest transactionRequest, Throwable error,
            long processingTimeMs) {
        String timestamp = LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT);
        String maskedCardNumber = transactionRequest != null
                ? maskCardNumber(transactionRequest.getCreditCardNumber())
                : "UNKNOWN";
        String transactionId =
                transactionRequest != null ? transactionRequest.getTransactionId() : "UNKNOWN";

        String auditMessage = String.format("TRANSACTION_ERROR|%s|%s|%s|%s|%dms", timestamp,
                transactionId, maskedCardNumber,
                error.getClass().getSimpleName() + ": " + error.getMessage(), processingTimeMs);

        auditLogger.error(auditMessage);
        logger.debug("Logged transaction error for: {}", transactionId);
    }

    /**
     * Log database health check events asynchronously.
     * 
     * @param isHealthy Whether the database check passed
     * @param responseTimeMs Database response time
     */
    public void logDatabaseHealthCheck(boolean isHealthy, long responseTimeMs) {
        // Use async logging to avoid impacting performance
        CompletableFuture.runAsync(() -> {
            String timestamp = LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT);

            String auditMessage = String.format("DATABASE_HEALTH|%s|%s|%dms", timestamp,
                    isHealthy ? "HEALTHY" : "UNHEALTHY", responseTimeMs);

            auditLogger.info(auditMessage);
        });
    }

    /**
     * Log system performance metrics.
     * 
     * @param metricName Name of the performance metric
     * @param value Metric value
     * @param unit Unit of measurement
     */
    public void logPerformanceMetric(String metricName, double value, String unit) {
        CompletableFuture.runAsync(() -> {
            String timestamp = LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT);

            String auditMessage = String.format("PERFORMANCE_METRIC|%s|%s|%.3f|%s", timestamp,
                    metricName, value, unit);

            auditLogger.info(auditMessage);
        });
    }

    /**
     * Log configuration changes for compliance.
     * 
     * @param configKey Configuration key that changed
     * @param oldValue Previous value
     * @param newValue New value
     * @param changedBy User or system that made the change
     */
    public void logConfigurationChange(String configKey, String oldValue, String newValue,
            String changedBy) {
        String timestamp = LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT);

        String auditMessage = String.format("CONFIG_CHANGE|%s|%s|%s|%s|%s", timestamp, configKey,
                oldValue != null ? oldValue : "NULL", newValue != null ? newValue : "NULL",
                changedBy);

        auditLogger.warn(auditMessage); // Use WARN level for configuration changes
        logger.info("Configuration change logged: {} changed by {}", configKey, changedBy);
    }

    /**
     * Log security events (authentication, authorization, etc.).
     * 
     * @param eventType Type of security event
     * @param userId User involved in the event
     * @param details Additional event details
     * @param successful Whether the security operation was successful
     */
    public void logSecurityEvent(String eventType, String userId, String details,
            boolean successful) {
        String timestamp = LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT);

        String auditMessage = String.format("SECURITY_EVENT|%s|%s|%s|%s|%s", timestamp, eventType,
                userId != null ? userId : "UNKNOWN", successful ? "SUCCESS" : "FAILURE", details);

        if (successful) {
            auditLogger.info(auditMessage);
        } else {
            auditLogger.warn(auditMessage);
        }

        logger.debug("Security event logged: {} for user: {}", eventType, userId);
    }

    /**
     * Create a comprehensive audit summary for a given time period.
     * 
     * @param fromTime Start time for the audit period
     * @param toTime End time for the audit period
     * @return AuditSummary containing aggregated audit information
     */
    public AuditSummary createAuditSummary(LocalDateTime fromTime, LocalDateTime toTime) {
        // This is a placeholder implementation
        // In a real system, this would query audit logs from a database or log aggregation system

        AuditSummary summary = new AuditSummary();
        summary.setPeriodStart(fromTime);
        summary.setPeriodEnd(toTime);
        summary.setGeneratedAt(LocalDateTime.now());

        // Placeholder metrics - would be calculated from actual audit logs
        summary.setTotalTransactions(0L);
        summary.setFraudulentTransactions(0L);
        summary.setErrorCount(0L);
        summary.setAverageProcessingTime(0.0);

        logger.info("Generated audit summary for period: {} to {}", fromTime, toTime);
        return summary;
    }

    /**
     * Mask credit card number for audit logging (PCI compliance). Shows only last 4 digits.
     * 
     * @param cardNumber Full credit card number
     * @return Masked card number
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }

        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "****-****-****-" + lastFour;
    }

    /**
     * Data class for audit summary information.
     */
    public static class AuditSummary {
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private LocalDateTime generatedAt;
        private Long totalTransactions;
        private Long fraudulentTransactions;
        private Long errorCount;
        private Double averageProcessingTime;

        // Getters and setters
        public LocalDateTime getPeriodStart() {
            return periodStart;
        }

        public void setPeriodStart(LocalDateTime periodStart) {
            this.periodStart = periodStart;
        }

        public LocalDateTime getPeriodEnd() {
            return periodEnd;
        }

        public void setPeriodEnd(LocalDateTime periodEnd) {
            this.periodEnd = periodEnd;
        }

        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
        }

        public Long getTotalTransactions() {
            return totalTransactions;
        }

        public void setTotalTransactions(Long totalTransactions) {
            this.totalTransactions = totalTransactions;
        }

        public Long getFraudulentTransactions() {
            return fraudulentTransactions;
        }

        public void setFraudulentTransactions(Long fraudulentTransactions) {
            this.fraudulentTransactions = fraudulentTransactions;
        }

        public Long getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(Long errorCount) {
            this.errorCount = errorCount;
        }

        public Double getAverageProcessingTime() {
            return averageProcessingTime;
        }

        public void setAverageProcessingTime(Double averageProcessingTime) {
            this.averageProcessingTime = averageProcessingTime;
        }

        @Override
        public String toString() {
            return "AuditSummary{" + "periodStart=" + periodStart + ", periodEnd=" + periodEnd
                    + ", generatedAt=" + generatedAt + ", totalTransactions=" + totalTransactions
                    + ", fraudulentTransactions=" + fraudulentTransactions + ", errorCount="
                    + errorCount + ", averageProcessingTime=" + averageProcessingTime + '}';
        }
    }
}
