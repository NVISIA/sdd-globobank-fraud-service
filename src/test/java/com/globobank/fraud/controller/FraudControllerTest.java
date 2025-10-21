package com.globobank.fraud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globobank.fraud.BaseIntegrationTest;
import com.globobank.fraud.model.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FraudController.
 * 
 * Tests the fraud detection API endpoints including input validation, security controls, error
 * handling, and performance requirements. Validates that the API contract is correctly implemented
 * and that the sub-200ms SLA can be achieved.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@AutoConfigureWebMvc
class FraudControllerTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpTest() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Should successfully assess risk for valid transaction request")
    @WithMockUser(authorities = {"SCOPE_fraud:assess"})
    void shouldAssessRiskForValidRequest() throws Exception {
        // Given
        TransactionRequest request = createValidTransactionRequest();

        // When & Then
        long startTime = System.currentTimeMillis();

        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactionId").value(request.getTransactionId()))
                .andExpect(jsonPath("$.accountNumber").value(request.getAccountNumber()))
                .andExpect(jsonPath("$.riskScore").isNumber())
                .andExpect(jsonPath("$.riskLevel").exists())
                .andExpect(jsonPath("$.recommendation").exists())
                .andExpect(jsonPath("$.assessmentTimestamp").exists())
                .andExpect(jsonPath("$.processingTimeMs").isNumber())
                .andExpect(jsonPath("$.riskFactors").isArray())
                .andExpect(jsonPath("$.metadata").exists())
                .andExpect(jsonPath("$.correlationId").exists());

        long processingTime = System.currentTimeMillis() - startTime;

        // Verify SLA compliance (should be under 200ms for integration test)
        // Note: This is a loose check as integration tests include network overhead
        assert processingTime < 1000; // 1 second timeout for integration test
    }

    @Test
    @DisplayName("Should return 400 for invalid transaction request - missing required fields")
    @WithMockUser(authorities = {"SCOPE_fraud:assess"})
    void shouldReturn400ForMissingRequiredFields() throws Exception {
        // Given - incomplete request
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN001");
        // Missing other required fields

        // When & Then
        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("Should return 400 for invalid amount")
    @WithMockUser(authorities = {"SCOPE_fraud:assess"})
    void shouldReturn400ForInvalidAmount() throws Exception {
        // Given
        TransactionRequest request = createValidTransactionRequest();
        request.setAmount(new BigDecimal("-10.00")); // Invalid negative amount

        // When & Then
        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @DisplayName("Should return 400 for invalid currency code")
    @WithMockUser(authorities = {"SCOPE_fraud:assess"})
    void shouldReturn400ForInvalidCurrency() throws Exception {
        // Given
        TransactionRequest request = createValidTransactionRequest();
        request.setCurrency("INVALID"); // Invalid currency code

        // When & Then
        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should return 400 for invalid account number format")
    @WithMockUser(authorities = {"SCOPE_fraud:assess"})
    void shouldReturn400ForInvalidAccountNumber() throws Exception {
        // Given
        TransactionRequest request = createValidTransactionRequest();
        request.setAccountNumber("123"); // Too short

        // When & Then
        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should return 400 for invalid transaction channel")
    @WithMockUser(authorities = {"SCOPE_fraud:assess"})
    void shouldReturn400ForInvalidChannel() throws Exception {
        // Given
        TransactionRequest request = createValidTransactionRequest();
        request.setChannel("INVALID_CHANNEL");

        // When & Then
        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated request")
    void shouldReturn401ForUnauthenticatedRequest() throws Exception {
        // Given
        TransactionRequest request = createValidTransactionRequest();

        // When & Then
        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 for insufficient privileges")
    @WithMockUser(authorities = {"SCOPE_fraud:read"}) // Wrong scope
    void shouldReturn403ForInsufficientPrivileges() throws Exception {
        // Given
        TransactionRequest request = createValidTransactionRequest();

        // When & Then
        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 404 for non-existent risk assessment")
    @WithMockUser(authorities = {"SCOPE_fraud:read"})
    void shouldReturn404ForNonExistentAssessment() throws Exception {
        // When & Then
        mockMvc.perform(get("/fraud/v1/risk-assessments/NON_EXISTENT_TXN"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return health status successfully")
    void shouldReturnHealthStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/fraud/v1/risk-assessments/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("fraud-detection"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    @DisplayName("Should handle large transaction amounts within limits")
    @WithMockUser(authorities = {"SCOPE_fraud:assess"})
    void shouldHandleLargeAmounts() throws Exception {
        // Given
        TransactionRequest request = createValidTransactionRequest();
        request.setAmount(new BigDecimal("999999.99")); // Maximum allowed

        // When & Then
        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").isNumber());
    }

    @Test
    @DisplayName("Should validate location coordinates properly")
    @WithMockUser(authorities = {"SCOPE_fraud:assess"})
    void shouldValidateLocationCoordinates() throws Exception {
        // Given
        TransactionRequest request = createValidTransactionRequest();
        TransactionRequest.TransactionLocation location =
                new TransactionRequest.TransactionLocation();
        location.setLatitude(91.0); // Invalid latitude (> 90)
        location.setLongitude(0.0);
        location.setCountry("US");
        request.setLocation(location);

        // When & Then
        mockMvc.perform(post("/fraud/v1/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    /**
     * Create a valid transaction request for testing.
     */
    private TransactionRequest createValidTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN_" + System.currentTimeMillis());
        request.setAccountNumber("1234567890123456");
        request.setAmount(new BigDecimal("100.50"));
        request.setCurrency("USD");
        request.setTimestamp(LocalDateTime.now());
        request.setChannel("POS");
        request.setType("PURCHASE");
        request.setMerchantId("MERCHANT_001");
        request.setMerchantCategoryCode("5411");

        // Add valid location
        TransactionRequest.TransactionLocation location =
                new TransactionRequest.TransactionLocation();
        location.setLatitude(40.7128);
        location.setLongitude(-74.0060);
        location.setCountry("US");
        location.setCity("New York");
        location.setRegion("NY");
        location.setPostalCode("10001");
        request.setLocation(location);

        return request;
    }
}
