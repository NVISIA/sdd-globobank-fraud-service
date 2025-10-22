package com.globobank.fraud.service;

import com.globobank.fraud.model.RiskAssessment;
import com.globobank.fraud.model.TransactionRequest;
import com.globobank.fraud.repository.FraudulentCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudulentCardRepository fraudulentCardRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @Test
    void shouldDetectFraudulentCard() {
        TransactionRequest request =
                new TransactionRequest("550e8400-e29b-41d4-a716-446655440000", "4532123456789012");

        when(fraudulentCardRepository.existsByCardNumberAndActiveTrue("4532123456789012"))
                .thenReturn(true);

        RiskAssessment result = fraudDetectionService.assessRisk(request);

        assertThat(result.getFraudulent()).isTrue();
        assertThat(result.getRiskScore()).isEqualTo(1000);
    }
}
