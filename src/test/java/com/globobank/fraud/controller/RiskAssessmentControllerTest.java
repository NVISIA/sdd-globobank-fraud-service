package com.globobank.fraud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globobank.fraud.BaseIntegrationTest;
import com.globobank.fraud.model.TransactionRequest;
import com.globobank.fraud.model.RiskAssessment;
import com.globobank.fraud.service.FraudDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class RiskAssessmentControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudDetectionService fraudDetectionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "testuser", authorities = {"SCOPE_fraud:read"})
    void shouldAssessTransactionRisk_WhenValidRequest() throws Exception {
        // Given
        String transactionId = UUID.randomUUID().toString();
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId(transactionId);
        request.setCreditCardNumber("1234567890123456");

        RiskAssessment expectedResponse = new RiskAssessment();
        expectedResponse.setRiskScore(0);
        expectedResponse.setFraudulent(false);
        expectedResponse.setTimestamp(LocalDateTime.now());

        when(fraudDetectionService.assessRisk(any(TransactionRequest.class)))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/risk-assessments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.riskScore").value(0))
                .andExpect(jsonPath("$.fraudulent").value(false));
    }
}
