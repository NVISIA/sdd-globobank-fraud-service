package com.globobank.fraud.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Simple risk assessment response model for GloboBank Fraud Detection Service.
 * 
 * Contains only the essential fields required by the OpenAPI specification: - riskScore: Integer
 * from 0-1000 indicating fraud risk level - fraudulent: Boolean flag indicating if transaction is
 * fraudulent - timestamp: ISO8601 timestamp when assessment was performed
 * 
 * This simplified model matches the OpenAPI contract and supports the basic fraud detection logic:
 * 1000 for fraudulent cards, 0 for clean cards.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
public class RiskAssessment {

    /**
     * Risk score from 0 to 1000. 0 = no fraud risk (card not in fraudulent_cards table) 1000 =
     * maximum fraud risk (card found in fraudulent_cards table)
     */
    @NotNull(message = "Risk score is required")
    @Min(value = 0, message = "Risk score must be between 0 and 1000")
    @Max(value = 1000, message = "Risk score must be between 0 and 1000")
    @JsonProperty("riskScore")
    private Integer riskScore;

    /**
     * Boolean flag indicating if the transaction is fraudulent. true = card number found in
     * fraudulent_cards table false = card number not found in fraudulent_cards table
     */
    @NotNull(message = "Fraudulent flag is required")
    @JsonProperty("fraudulent")
    private Boolean fraudulent;

    /**
     * ISO8601 timestamp when the risk assessment was performed.
     */
    @NotNull(message = "Timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Default constructor for JSON deserialization.
     */
    public RiskAssessment() {}

    /**
     * Constructor with all required fields.
     * 
     * @param riskScore Risk score from 0-1000
     * @param fraudulent Fraudulent flag
     * @param timestamp Assessment timestamp
     */
    public RiskAssessment(Integer riskScore, Boolean fraudulent, LocalDateTime timestamp) {
        this.riskScore = riskScore;
        this.fraudulent = fraudulent;
        this.timestamp = timestamp;
    }

    // Getters and setters

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public Boolean getFraudulent() {
        return fraudulent;
    }

    public void setFraudulent(Boolean fraudulent) {
        this.fraudulent = fraudulent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "RiskAssessment{" + "riskScore=" + riskScore + ", fraudulent=" + fraudulent
                + ", timestamp=" + timestamp + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RiskAssessment that = (RiskAssessment) o;

        if (riskScore != null ? !riskScore.equals(that.riskScore) : that.riskScore != null)
            return false;
        if (fraudulent != null ? !fraudulent.equals(that.fraudulent) : that.fraudulent != null)
            return false;
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
    }

    @Override
    public int hashCode() {
        int result = riskScore != null ? riskScore.hashCode() : 0;
        result = 31 * result + (fraudulent != null ? fraudulent.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}
