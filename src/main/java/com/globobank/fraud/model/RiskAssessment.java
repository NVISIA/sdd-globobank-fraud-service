package com.globobank.fraud.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Risk assessment response model for GloboBank Fraud Detection Service.
 * 
 * Represents the fraud risk analysis result for a transaction request, including risk score,
 * recommendation, detailed risk factors, and processing metadata for audit and compliance purposes.
 * 
 * This model provides comprehensive fraud analysis results that enable downstream systems to make
 * informed decisions about transaction approval, additional verification requirements, or
 * transaction blocking.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskAssessment {

    /**
     * Original transaction ID for correlation and audit purposes.
     */
    @NotBlank(message = "Transaction ID is required")
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Account number associated with the risk assessment.
     */
    @NotBlank(message = "Account number is required")
    @JsonProperty("accountNumber")
    private String accountNumber;

    /**
     * Numerical risk score between 0 (no risk) and 100 (highest risk). Used for automated decision
     * making and risk thresholds.
     */
    @NotNull(message = "Risk score is required")
    @DecimalMin(value = "0.0", message = "Risk score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Risk score must be between 0 and 100")
    @JsonProperty("riskScore")
    private Double riskScore;

    /**
     * Risk level classification based on score thresholds. Provides human-readable risk
     * categorization.
     */
    @NotNull(message = "Risk level is required")
    @JsonProperty("riskLevel")
    private RiskLevel riskLevel;

    /**
     * Processing recommendation for the transaction. Guides downstream systems on appropriate
     * action.
     */
    @NotNull(message = "Recommendation is required")
    @JsonProperty("recommendation")
    private Recommendation recommendation;

    /**
     * Timestamp when the risk assessment was completed.
     */
    @NotNull(message = "Assessment timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("assessmentTimestamp")
    private LocalDateTime assessmentTimestamp;

    /**
     * Processing time in milliseconds for performance monitoring.
     */
    @Min(value = 0, message = "Processing time must be non-negative")
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;

    /**
     * Detailed risk factors that contributed to the overall score. Provides transparency and
     * enables investigation of fraud patterns.
     */
    @JsonProperty("riskFactors")
    private List<RiskFactor> riskFactors;

    /**
     * Additional context and metadata for the assessment. May include model versions, configuration
     * parameters, etc.
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Correlation ID for request tracing and debugging.
     */
    @JsonProperty("correlationId")
    private String correlationId;

    // Default constructor for JSON deserialization
    public RiskAssessment() {}

    // Constructor with required fields
    public RiskAssessment(String transactionId, String accountNumber, Double riskScore,
            RiskLevel riskLevel, Recommendation recommendation) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.recommendation = recommendation;
        this.assessmentTimestamp = LocalDateTime.now();
    }

    // Getters and setters

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(Recommendation recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getAssessmentTimestamp() {
        return assessmentTimestamp;
    }

    public void setAssessmentTimestamp(LocalDateTime assessmentTimestamp) {
        this.assessmentTimestamp = assessmentTimestamp;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public List<RiskFactor> getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(List<RiskFactor> riskFactors) {
        this.riskFactors = riskFactors;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        return "RiskAssessment{" + "transactionId='" + transactionId + '\'' + ", accountNumber='"
                + accountNumber + '\'' + ", riskScore=" + riskScore + ", riskLevel=" + riskLevel
                + ", recommendation=" + recommendation + ", assessmentTimestamp="
                + assessmentTimestamp + ", processingTimeMs=" + processingTimeMs + '}';
    }

    /**
     * Risk level enumeration for standardized risk categorization.
     */
    public enum RiskLevel {
        @JsonProperty("LOW")
        LOW("Low risk - transaction appears normal"),

        @JsonProperty("MEDIUM")
        MEDIUM("Medium risk - requires additional monitoring"),

        @JsonProperty("HIGH")
        HIGH("High risk - requires immediate attention"),

        @JsonProperty("CRITICAL")
        CRITICAL("Critical risk - potential fraud detected");

        private final String description;

        RiskLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Processing recommendation enumeration for transaction handling guidance.
     */
    public enum Recommendation {
        @JsonProperty("APPROVE")
        APPROVE("Approve transaction - low fraud risk"),

        @JsonProperty("REVIEW")
        REVIEW("Review transaction - moderate risk detected"),

        @JsonProperty("CHALLENGE")
        CHALLENGE("Challenge customer - additional verification required"),

        @JsonProperty("BLOCK")
        BLOCK("Block transaction - high fraud risk detected");

        private final String description;

        Recommendation(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Risk factor model representing individual fraud indicators.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RiskFactor {

        @NotBlank(message = "Risk factor type is required")
        @JsonProperty("type")
        private String type;

        @NotNull(message = "Risk factor score is required")
        @DecimalMin(value = "0.0", message = "Risk factor score must be non-negative")
        @JsonProperty("score")
        private Double score;

        @JsonProperty("description")
        private String description;

        @JsonProperty("details")
        private Map<String, Object> details;

        // Default constructor
        public RiskFactor() {}

        // Constructor with required fields
        public RiskFactor(String type, Double score, String description) {
            this.type = type;
            this.score = score;
            this.description = description;
        }

        // Getters and setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }

        @Override
        public String toString() {
            return "RiskFactor{" + "type='" + type + '\'' + ", score=" + score + ", description='"
                    + description + '\'' + '}';
        }
    }
}
