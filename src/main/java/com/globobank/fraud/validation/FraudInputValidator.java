package com.globobank.fraud.validation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Input validation and sanitization utility for GloboBank Fraud Detection Service.
 * 
 * Provides comprehensive validation and sanitization for fraud detection inputs to ensure data
 * integrity, security, and compliance with banking regulations. Implements defense-in-depth
 * security controls for financial transaction data.
 * 
 * Features: - Financial data validation (amounts, currencies, account numbers) - Geographic data
 * validation (coordinates, country codes) - Temporal data validation (transaction timestamps) -
 * Input sanitization to prevent injection attacks - Business rule validation for fraud detection
 * context
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Component
public class FraudInputValidator {

    // Regex patterns for validation
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{10,20}$");
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Z]{2,3}$");
    private static final Pattern MCC_PATTERN = Pattern.compile("^[0-9]{4}$");
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");
    private static final Pattern MERCHANT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");

    // Business rule constants
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");
    private static final int MAX_TRANSACTION_AGE_HOURS = 24;

    /**
     * Validate transaction ID format and security.
     *
     * @param transactionId Transaction identifier to validate
     * @return ValidationResult with outcome and error details
     */
    public ValidationResult validateTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return ValidationResult.invalid("Transaction ID is required");
        }

        String sanitized = sanitizeAlphanumeric(transactionId);
        if (!TRANSACTION_ID_PATTERN.matcher(sanitized).matches()) {
            return ValidationResult.invalid(
                    "Transaction ID must be 1-50 alphanumeric characters, hyphens, or underscores");
        }

        return ValidationResult.valid(sanitized);
    }

    /**
     * Validate account number format and structure.
     *
     * @param accountNumber Account number to validate
     * @return ValidationResult with outcome and error details
     */
    public ValidationResult validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return ValidationResult.invalid("Account number is required");
        }

        String sanitized = sanitizeNumeric(accountNumber);
        if (!ACCOUNT_NUMBER_PATTERN.matcher(sanitized).matches()) {
            return ValidationResult.invalid("Account number must be 10-20 digits");
        }

        // Additional Luhn algorithm validation could be added here
        return ValidationResult.valid(sanitized);
    }

    /**
     * Validate transaction amount for financial accuracy and business rules.
     *
     * @param amount Transaction amount to validate
     * @param currency Currency code for context
     * @return ValidationResult with outcome and error details
     */
    public ValidationResult validateAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            return ValidationResult.invalid("Amount is required");
        }

        if (amount.compareTo(MIN_AMOUNT) < 0) {
            return ValidationResult.invalid("Amount must be at least " + MIN_AMOUNT);
        }

        if (amount.compareTo(MAX_AMOUNT) > 0) {
            return ValidationResult.invalid("Amount exceeds maximum limit of " + MAX_AMOUNT);
        }

        // Validate decimal places for currency
        if (amount.scale() > 2) {
            return ValidationResult.invalid("Amount cannot have more than 2 decimal places");
        }

        // Currency-specific validations
        if ("JPY".equals(currency) || "KRW".equals(currency)) {
            if (amount.scale() > 0) {
                return ValidationResult
                        .invalid("Amount for " + currency + " cannot have decimal places");
            }
        }

        return ValidationResult.valid(amount);
    }

    /**
     * Validate currency code format and supported currencies.
     *
     * @param currency Currency code to validate
     * @return ValidationResult with outcome and error details
     */
    public ValidationResult validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            return ValidationResult.invalid("Currency is required");
        }

        String sanitized = sanitizeAlphabetic(currency).toUpperCase();
        if (!CURRENCY_CODE_PATTERN.matcher(sanitized).matches()) {
            return ValidationResult.invalid("Currency must be a valid 3-letter ISO code");
        }

        // Validate against supported currencies (simplified list)
        String[] supportedCurrencies =
                {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "SEK", "NZD"};
        boolean isSupported = false;
        for (String supported : supportedCurrencies) {
            if (supported.equals(sanitized)) {
                isSupported = true;
                break;
            }
        }

        if (!isSupported) {
            return ValidationResult.invalid("Currency " + sanitized + " is not supported");
        }

        return ValidationResult.valid(sanitized);
    }

    /**
     * Validate transaction timestamp for temporal fraud detection.
     *
     * @param timestamp Transaction timestamp to validate
     * @return ValidationResult with outcome and error details
     */
    public ValidationResult validateTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return ValidationResult.invalid("Transaction timestamp is required");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minTime = now.minusHours(MAX_TRANSACTION_AGE_HOURS);
        LocalDateTime maxTime = now.plusMinutes(5); // Allow 5 minutes clock skew

        if (timestamp.isBefore(minTime)) {
            return ValidationResult.invalid("Transaction timestamp is too old (max "
                    + MAX_TRANSACTION_AGE_HOURS + " hours)");
        }

        if (timestamp.isAfter(maxTime)) {
            return ValidationResult.invalid("Transaction timestamp is in the future");
        }

        return ValidationResult.valid(timestamp);
    }

    /**
     * Validate geographic coordinates for location-based fraud detection.
     *
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return ValidationResult with outcome and error details
     */
    public ValidationResult validateCoordinates(Double latitude, Double longitude) {
        if (latitude != null) {
            if (latitude < -90.0 || latitude > 90.0) {
                return ValidationResult.invalid("Latitude must be between -90 and 90 degrees");
            }
        }

        if (longitude != null) {
            if (longitude < -180.0 || longitude > 180.0) {
                return ValidationResult.invalid("Longitude must be between -180 and 180 degrees");
            }
        }

        return ValidationResult.valid(new Double[] {latitude, longitude});
    }

    /**
     * Validate country code format.
     *
     * @param countryCode Country code to validate
     * @return ValidationResult with outcome and error details
     */
    public ValidationResult validateCountryCode(String countryCode) {
        if (countryCode == null || countryCode.trim().isEmpty()) {
            return ValidationResult.valid(null); // Country code is optional
        }

        String sanitized = sanitizeAlphabetic(countryCode).toUpperCase();
        if (!COUNTRY_CODE_PATTERN.matcher(sanitized).matches()) {
            return ValidationResult.invalid("Country code must be 2-3 letter ISO code");
        }

        return ValidationResult.valid(sanitized);
    }

    /**
     * Validate merchant category code format.
     *
     * @param mcc Merchant Category Code to validate
     * @return ValidationResult with outcome and error details
     */
    public ValidationResult validateMcc(String mcc) {
        if (mcc == null || mcc.trim().isEmpty()) {
            return ValidationResult.valid(null); // MCC is optional
        }

        String sanitized = sanitizeNumeric(mcc);
        if (!MCC_PATTERN.matcher(sanitized).matches()) {
            return ValidationResult.invalid("Merchant Category Code must be 4 digits");
        }

        return ValidationResult.valid(sanitized);
    }

    /**
     * Sanitize input to allow only alphanumeric characters, hyphens, and underscores.
     */
    private String sanitizeAlphanumeric(String input) {
        if (input == null)
            return null;
        return input.replaceAll("[^A-Za-z0-9_-]", "").trim();
    }

    /**
     * Sanitize input to allow only numeric characters.
     */
    private String sanitizeNumeric(String input) {
        if (input == null)
            return null;
        return input.replaceAll("[^0-9]", "").trim();
    }

    /**
     * Sanitize input to allow only alphabetic characters.
     */
    private String sanitizeAlphabetic(String input) {
        if (input == null)
            return null;
        return input.replaceAll("[^A-Za-z]", "").trim();
    }

    /**
     * Validation result container class.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Object sanitizedValue;

        private ValidationResult(boolean valid, String errorMessage, Object sanitizedValue) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.sanitizedValue = sanitizedValue;
        }

        public static ValidationResult valid(Object sanitizedValue) {
            return new ValidationResult(true, null, sanitizedValue);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @SuppressWarnings("unchecked")
        public <T> T getSanitizedValue() {
            return (T) sanitizedValue;
        }
    }
}
