package com.globobank.fraud.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Transaction request model for GloboBank Fraud Detection Service.
 * 
 * Represents an incoming transaction that requires fraud risk assessment. Contains all necessary
 * transaction details and metadata for comprehensive fraud analysis including amount, location,
 * merchant information, and customer context.
 * 
 * This model serves as the primary input for the fraud detection algorithm and must contain
 * sufficient information to enable accurate risk scoring while maintaining data privacy and
 * security requirements.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {

    /**
     * Unique transaction identifier from the source system. Used for correlation, debugging, and
     * audit trails.
     */
    @NotBlank(message = "Transaction ID is required")
    @Size(max = 50, message = "Transaction ID must not exceed 50 characters")
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Account number associated with the transaction. Used for account-level fraud pattern
     * detection.
     */
    @NotBlank(message = "Account number is required")
    @Size(max = 20, message = "Account number must not exceed 20 characters")
    @Pattern(regexp = "^[0-9]{10,20}$", message = "Account number must be 10-20 digits")
    @JsonProperty("accountNumber")
    private String accountNumber;

    /**
     * Transaction amount in the transaction currency. Must be positive and within reasonable limits
     * for fraud detection.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @DecimalMax(value = "999999.99", message = "Amount exceeds maximum limit")
    @Digits(integer = 6, fraction = 2,
            message = "Amount must have maximum 6 integer and 2 decimal places")
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * ISO 4217 currency code for the transaction. Used for currency-specific fraud patterns and
     * conversion analysis.
     */
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    @JsonProperty("currency")
    private String currency;

    /**
     * Transaction timestamp when the transaction was initiated. Critical for temporal fraud pattern
     * analysis.
     */
    @NotNull(message = "Transaction timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Merchant identifier where the transaction occurred. Used for merchant-based fraud pattern
     * detection.
     */
    @Size(max = 50, message = "Merchant ID must not exceed 50 characters")
    @JsonProperty("merchantId")
    private String merchantId;

    /**
     * Merchant Category Code (MCC) for transaction categorization. Enables category-specific fraud
     * detection rules.
     */
    @Pattern(regexp = "^[0-9]{4}$", message = "MCC must be a 4-digit code")
    @JsonProperty("merchantCategoryCode")
    private String merchantCategoryCode;

    /**
     * Geographic location where the transaction occurred. Used for location-based fraud detection
     * and geofencing.
     */
    @JsonProperty("location")
    private TransactionLocation location;

    /**
     * Channel through which the transaction was processed. Enables channel-specific fraud detection
     * patterns.
     */
    @NotBlank(message = "Transaction channel is required")
    @Pattern(regexp = "^(ATM|POS|ONLINE|MOBILE|PHONE)$",
            message = "Channel must be one of: ATM, POS, ONLINE, MOBILE, PHONE")
    @JsonProperty("channel")
    private String channel;

    /**
     * Type of transaction being processed. Different transaction types have different fraud risk
     * profiles.
     */
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(PURCHASE|WITHDRAWAL|TRANSFER|PAYMENT|REFUND)$",
            message = "Type must be one of: PURCHASE, WITHDRAWAL, TRANSFER, PAYMENT, REFUND")
    @JsonProperty("type")
    private String type;

    /**
     * Additional metadata and context for enhanced fraud detection. May include device
     * fingerprints, IP addresses, user behavior patterns.
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Default constructor for JSON deserialization
    public TransactionRequest() {}

    // Constructor with required fields
    public TransactionRequest(String transactionId, String accountNumber, BigDecimal amount,
            String currency, LocalDateTime timestamp, String channel, String type) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.channel = channel;
        this.type = type;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantCategoryCode() {
        return merchantCategoryCode;
    }

    public void setMerchantCategoryCode(String merchantCategoryCode) {
        this.merchantCategoryCode = merchantCategoryCode;
    }

    public TransactionLocation getLocation() {
        return location;
    }

    public void setLocation(TransactionLocation location) {
        this.location = location;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "TransactionRequest{" + "transactionId='" + transactionId + '\''
                + ", accountNumber='" + accountNumber + '\'' + ", amount=" + amount + ", currency='"
                + currency + '\'' + ", timestamp=" + timestamp + ", channel='" + channel + '\''
                + ", type='" + type + '\'' + '}';
    }

    /**
     * Nested class for transaction location information. Supports both coordinate-based and
     * address-based location data.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionLocation {

        @JsonProperty("latitude")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        private Double latitude;

        @JsonProperty("longitude")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        private Double longitude;

        @JsonProperty("country")
        @Size(max = 3, message = "Country code must not exceed 3 characters")
        @Pattern(regexp = "^[A-Z]{2,3}$", message = "Country must be a valid ISO country code")
        private String country;

        @JsonProperty("city")
        @Size(max = 100, message = "City name must not exceed 100 characters")
        private String city;

        @JsonProperty("region")
        @Size(max = 100, message = "Region name must not exceed 100 characters")
        private String region;

        @JsonProperty("postalCode")
        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        private String postalCode;

        // Default constructor
        public TransactionLocation() {}

        // Constructor with coordinates
        public TransactionLocation(Double latitude, Double longitude, String country) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.country = country;
        }

        // Getters and setters
        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }
    }
}
