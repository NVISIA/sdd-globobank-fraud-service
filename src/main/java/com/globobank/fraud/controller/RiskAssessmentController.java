package com.globobank.fraud.controller;

import com.globobank.fraud.model.RiskAssessment;
import com.globobank.fraud.model.TransactionRequest;
import com.globobank.fraud.service.FraudDetectionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Risk Assessment Controller for GloboBank Fraud Detection Service.
 * 
 * Provides a simple REST API endpoint for real-time transaction fraud risk assessment. Delegates
 * all business logic to the FraudDetectionService, keeping the controller focused only on HTTP
 * request/response handling.
 * 
 * This controller implements the simple fraud detection logic specified in the requirements: -
 * Accepts TransactionRequest with transactionId and creditCardNumber - Returns RiskAssessment with
 * riskScore (0-1000), fraudulent flag, and timestamp - Uses database lookup to determine if card is
 * fraudulent
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@RestController
@RequestMapping("/risk-assessments")
public class RiskAssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(RiskAssessmentController.class);

    private final FraudDetectionService fraudDetectionService;

    /**
     * Constructor with dependency injection.
     * 
     * @param fraudDetectionService Service for fraud detection logic
     */
    public RiskAssessmentController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    /**
     * Assess fraud risk for a transaction.
     * 
     * Primary endpoint for real-time fraud detection. Accepts a transaction request and returns a
     * risk assessment based on simple database lookup logic.
     * 
     * Simple fraud detection rule: - Check if creditCardNumber exists in fraudulent_cards table -
     * Return riskScore 1000 and fraudulent=true if found - Return riskScore 0 and fraudulent=false
     * if not found
     * 
     * @param transactionRequest Transaction details for risk assessment
     * @return Risk assessment with score, fraudulent flag, and timestamp
     */
    @PostMapping
    public ResponseEntity<RiskAssessment> assessRisk(
            @Valid @RequestBody TransactionRequest transactionRequest) {

        logger.info("Assessing fraud risk for transaction: {}",
                transactionRequest.getTransactionId());

        try {
            // Delegate to service for fraud detection logic
            RiskAssessment assessment = fraudDetectionService.assessRisk(transactionRequest);

            logger.info(
                    "Risk assessment completed for transaction: {} - Risk Score: {}, Fraudulent: {}",
                    transactionRequest.getTransactionId(), assessment.getRiskScore(),
                    assessment.getFraudulent());

            return ResponseEntity.ok(assessment);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for transaction: {} - {}",
                    transactionRequest.getTransactionId(), e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            logger.error("Failed to assess risk for transaction: {} - {}",
                    transactionRequest.getTransactionId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
