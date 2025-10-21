package com.globobank.fraud.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

/**
 * Simple transaction request model for GloboBank Fraud Detection Service.
 * 
 * Contains only the essential fields required for the simple fraud detection logic: -
 * transactionId: Unique identifier for the transaction - creditCardNumber: Card number to check
 * against fraudulent_cards table
 * 
 * This simplified model matches the OpenAPI specification and implements the basic fraud detection
 * rule: check if card exists in database, return risk score.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
public class TransactionRequest {

    /**
     * Unique transaction identifier. Must be a valid UUID format for correlation and audit trails.
     */
    @NotBlank(message = "Transaction ID is required")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Transaction ID must be a valid UUID format")
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Credit card number to check for fraud. Must be 13-19 digits as per credit card industry
     * standards.
     */
    @NotBlank(message = "Credit card number is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Credit card number must be 13-19 digits")
    @JsonProperty("creditCardNumber")
    private String creditCardNumber;

    /**
     * Default constructor for JSON deserialization.
     */
    public TransactionRequest() {}

    /**
     * Constructor with all required fields.
     * 
     * @param transactionId Unique transaction identifier (UUID format)
     * @param creditCardNumber Credit card number (13-19 digits)
     */
    public TransactionRequest(String transactionId, String creditCardNumber) {
        this.transactionId = transactionId;
        this.creditCardNumber = creditCardNumber;
    }

    // Getters and setters

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    @Override
    public String toString() {
        return "TransactionRequest{" + "transactionId='" + transactionId + '\''
                + ", creditCardNumber='****"
                + (creditCardNumber != null && creditCardNumber.length() >= 4
                        ? creditCardNumber.substring(creditCardNumber.length() - 4)
                        : "****")
                + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TransactionRequest that = (TransactionRequest) o;

        if (transactionId != null ? !transactionId.equals(that.transactionId)
                : that.transactionId != null)
            return false;
        return creditCardNumber != null ? creditCardNumber.equals(that.creditCardNumber)
                : that.creditCardNumber == null;
    }

    @Override
    public int hashCode() {
        int result = transactionId != null ? transactionId.hashCode() : 0;
        result = 31 * result + (creditCardNumber != null ? creditCardNumber.hashCode() : 0);
        return result;
    }
}
