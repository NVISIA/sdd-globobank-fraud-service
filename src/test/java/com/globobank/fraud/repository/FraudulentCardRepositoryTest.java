package com.globobank.fraud.repository;

import com.globobank.fraud.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class FraudulentCardRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private FraudulentCardRepository fraudulentCardRepository;

    @Test
    void shouldNotFindNonExistentCard() {
        boolean exists =
                fraudulentCardRepository.existsByCardNumberAndActiveTrue("9999999999999999");
        assertThat(exists).isFalse();
    }
}
