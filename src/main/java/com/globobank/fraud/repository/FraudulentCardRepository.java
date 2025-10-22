package com.globobank.fraud.repository;

import com.globobank.fraud.model.FraudulentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FraudulentCard entity operations.
 * 
 * Provides data access methods for querying the fraudulent_cards table to determine if a credit
 * card number is known to be fraudulent.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Repository
public interface FraudulentCardRepository extends JpaRepository<FraudulentCard, Long> {

    /**
     * Check if a credit card number exists in the fraudulent cards table. Only considers active
     * (non-disabled) fraudulent card entries.
     * 
     * This is the primary method used for fraud detection - if this returns true, the card should
     * receive a high risk score.
     * 
     * @param cardNumber The credit card number to check
     * @return true if the card number is in the fraudulent list and active, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FraudulentCard f "
            + "WHERE f.cardNumber = :cardNumber AND f.active = true")
    boolean existsByCardNumberAndActiveTrue(@Param("cardNumber") String cardNumber);

    /**
     * Find a fraudulent card by card number (including inactive ones).
     * 
     * @param cardNumber The credit card number to find
     * @return Optional containing the FraudulentCard if found, empty otherwise
     */
    Optional<FraudulentCard> findByCardNumber(String cardNumber);

    /**
     * Find an active fraudulent card by card number.
     * 
     * @param cardNumber The credit card number to find
     * @return Optional containing the active FraudulentCard if found, empty otherwise
     */
    Optional<FraudulentCard> findByCardNumberAndActiveTrue(String cardNumber);

    /**
     * Count total number of active fraudulent cards.
     * 
     * @return Number of active fraudulent card entries
     */
    long countByActiveTrue();

    /**
     * Batch check multiple card numbers for fraud status. Uses optimized query to check multiple
     * cards in a single database call.
     * 
     * @param cardNumbers List of card numbers to check
     * @return List of card numbers that are fraudulent and active
     */
    @Query("SELECT f.cardNumber FROM FraudulentCard f "
            + "WHERE f.cardNumber IN :cardNumbers AND f.active = true")
    List<String> findFraudulentCardNumbers(@Param("cardNumbers") List<String> cardNumbers);

    /**
     * Soft delete (deactivate) a fraudulent card entry.
     * 
     * @param cardNumber The card number to deactivate
     * @return Number of rows affected
     */
    @Modifying
    @Transactional
    @Query("UPDATE FraudulentCard f SET f.active = false "
            + "WHERE f.cardNumber = :cardNumber AND f.active = true")
    int deactivateByCardNumber(@Param("cardNumber") String cardNumber);

    /**
     * Reactivate a fraudulent card entry.
     * 
     * @param cardNumber The card number to reactivate
     * @return Number of rows affected
     */
    @Modifying
    @Transactional
    @Query("UPDATE FraudulentCard f SET f.active = true "
            + "WHERE f.cardNumber = :cardNumber AND f.active = false")
    int reactivateByCardNumber(@Param("cardNumber") String cardNumber);

    /**
     * Find recently added fraudulent cards. Useful for monitoring and audit purposes.
     * 
     * @param since DateTime threshold for "recent" entries
     * @return List of recently added fraudulent cards
     */
    @Query("SELECT f FROM FraudulentCard f "
            + "WHERE f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<FraudulentCard> findRecentlyAdded(@Param("since") LocalDateTime since);

    /**
     * Get statistics about fraudulent card entries. Returns counts grouped by active status.
     * 
     * @return Array containing [active_count, inactive_count]
     */
    @Query("SELECT f.active, COUNT(f) FROM FraudulentCard f GROUP BY f.active")
    List<Object[]> getStatsByActiveStatus();
}
