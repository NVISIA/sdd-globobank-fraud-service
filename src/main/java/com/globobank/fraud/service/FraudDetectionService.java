package com.globobank.fraud.service;

import com.globobank.fraud.model.RiskAssessment;
import com.globobank.fraud.model.TransactionRequest;
import com.globobank.fraud.repository.FraudulentCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Simple fraud detection service implementation.
 * 
 * Provides the core fraud detection logic by checking if a credit card number exists in the
 * fraudulent_cards database table. Returns a high risk score (1000) if the card is found, low risk
 * score (0) if not found.
 * 
 * This implements the simple fraud detection rule specified in the requirements: - Check if credit
 * card number exists in fraudulent_cards table - Return risk score 1000 if found (fraudulent) -
 * Return risk score 0 if not found (clean)
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Service
public class FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);

    /**
     * Risk score for fraudulent cards. Maximum value in the 0-1000 range.
     */
    private static final Integer FRAUDULENT_RISK_SCORE = 1000;

    /**
     * Risk score for clean (non-fraudulent) cards. Minimum value in the 0-1000 range.
     */
    private static final Integer CLEAN_RISK_SCORE = 0;

    private final FraudulentCardRepository fraudulentCardRepository;

    /**
     * Constructor with dependency injection.
     * 
     * @param fraudulentCardRepository Repository for fraudulent card data access
     */
    public FraudDetectionService(FraudulentCardRepository fraudulentCardRepository) {
        this.fraudulentCardRepository = fraudulentCardRepository;
    }

    /**
     * Assess the fraud risk for a transaction.
     * 
     * Implements the simple fraud detection logic: 1. Extract credit card number from transaction
     * request 2. Check if it exists in the fraudulent_cards database table 3. Return high risk
     * score (1000) if found, low risk score (0) if not
     * 
     * @param request The transaction request containing transaction ID and credit card number
     * @return RiskAssessment with risk score, fraudulent flag, and timestamp
     * @throws IllegalArgumentException if request or card number is null/empty
     */
    public RiskAssessment assessRisk(TransactionRequest request) {
        if (request == null) {
            logger.error("Transaction request is null");
            throw new IllegalArgumentException("Transaction request cannot be null");
        }

        if (request.getCreditCardNumber() == null
                || request.getCreditCardNumber().trim().isEmpty()) {
            logger.error("Credit card number is null or empty for transaction: {}",
                    request.getTransactionId());
            throw new IllegalArgumentException("Credit card number cannot be null or empty");
        }

        String cardNumber = request.getCreditCardNumber().trim();
        logger.debug("Assessing fraud risk for transaction: {} with card: ****{}",
                request.getTransactionId(),
                cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4) : "****");

        try {
            // Check if card number exists in fraudulent cards table
            boolean isFraudulent =
                    fraudulentCardRepository.existsByCardNumberAndActiveTrue(cardNumber);

            // Determine risk score based on fraud status
            Integer riskScore = isFraudulent ? FRAUDULENT_RISK_SCORE : CLEAN_RISK_SCORE;

            logger.info(
                    "Risk assessment completed for transaction: {} - Fraudulent: {}, Risk Score: {}",
                    request.getTransactionId(), isFraudulent, riskScore);

            // Create and return risk assessment
            RiskAssessment assessment = new RiskAssessment();
            assessment.setRiskScore(riskScore);
            assessment.setFraudulent(isFraudulent);
            assessment.setTimestamp(LocalDateTime.now());

            return assessment;

        } catch (Exception e) {
            logger.error("Error during fraud risk assessment for transaction: {} - {}",
                    request.getTransactionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to assess fraud risk", e);
        }
    }

    /**
     * Get statistics about the fraud detection service.
     * 
     * @return String with basic service statistics
     */
    public String getServiceStats() {
        try {
            long fraudulentCardsCount = fraudulentCardRepository.countByActiveTrue();
            return String.format("FraudDetectionService - Active fraudulent cards: %d",
                    fraudulentCardsCount);
        } catch (Exception e) {
            logger.warn("Failed to retrieve service statistics: {}", e.getMessage());
            return "FraudDetectionService - Statistics unavailable";
        }
    }
}
