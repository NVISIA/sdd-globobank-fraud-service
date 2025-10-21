package com.globobank.fraud.service;

import com.globobank.fraud.model.FraudulentCard;
import com.globobank.fraud.repository.FraudulentCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing fraudulent card entries with active flag support. Provides CRUD operations
 * and administrative functions for the fraudulent cards database.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Service
public class FraudulentCardManagementService {

    private static final Logger logger =
            LoggerFactory.getLogger(FraudulentCardManagementService.class);

    private final FraudulentCardRepository repository;
    private final TransactionAuditService auditService;

    @Autowired
    public FraudulentCardManagementService(FraudulentCardRepository repository,
            TransactionAuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /**
     * Add a new card to the fraudulent cards list.
     * 
     * @param cardNumber The credit card number to add
     * @param reason Reason for marking the card as fraudulent
     * @param reportedBy Who reported this card as fraudulent
     * @return The created FraudulentCard entity
     */
    @Transactional
    public FraudulentCard addFraudulentCard(String cardNumber, String reason, String reportedBy) {
        logger.info("Adding fraudulent card: ****{} reported by: {}",
                cardNumber.substring(Math.max(0, cardNumber.length() - 4)), reportedBy);

        // Check if card already exists
        Optional<FraudulentCard> existing = repository.findByCardNumber(cardNumber);
        if (existing.isPresent()) {
            if (existing.get().getActive()) {
                logger.warn("Card ****{} is already in fraudulent list and active",
                        cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
                return existing.get();
            } else {
                // Reactivate existing inactive card
                return reactivateCard(cardNumber, reportedBy);
            }
        }

        // Create new fraudulent card entry
        FraudulentCard fraudulentCard = new FraudulentCard();
        fraudulentCard.setCardNumber(cardNumber);
        fraudulentCard.setReason(reason);
        fraudulentCard.setActive(true);
        fraudulentCard.setCreatedAt(LocalDateTime.now());

        FraudulentCard saved = repository.save(fraudulentCard);

        // Log security event for audit
        auditService.logSecurityEvent("FRAUDULENT_CARD_ADDED", reportedBy,
                "Card: ****" + cardNumber.substring(Math.max(0, cardNumber.length() - 4)), true);

        logger.info("Successfully added fraudulent card with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Deactivate (soft delete) a fraudulent card.
     * 
     * @param cardNumber The card number to deactivate
     * @param deactivatedBy Who deactivated the card
     * @return true if card was deactivated, false if not found or already inactive
     */
    @Transactional
    public boolean deactivateCard(String cardNumber, String deactivatedBy) {
        logger.info("Deactivating fraudulent card: ****{} by: {}",
                cardNumber.substring(Math.max(0, cardNumber.length() - 4)), deactivatedBy);

        int updatedRows = repository.deactivateByCardNumber(cardNumber);

        if (updatedRows > 0) {
            // Log security event for audit
            auditService.logSecurityEvent("FRAUDULENT_CARD_DEACTIVATED", deactivatedBy,
                    "Card: ****" + cardNumber.substring(Math.max(0, cardNumber.length() - 4)),
                    true);

            logger.info("Successfully deactivated fraudulent card: ****{}",
                    cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
            return true;
        } else {
            logger.warn("No active fraudulent card found to deactivate: ****{}",
                    cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
            return false;
        }
    }

    /**
     * Reactivate a previously deactivated fraudulent card.
     * 
     * @param cardNumber The card number to reactivate
     * @param reactivatedBy Who reactivated the card
     * @return Updated FraudulentCard entity or throws exception if not found
     */
    @Transactional
    public FraudulentCard reactivateCard(String cardNumber, String reactivatedBy) {
        logger.info("Reactivating fraudulent card: ****{} by: {}",
                cardNumber.substring(Math.max(0, cardNumber.length() - 4)), reactivatedBy);

        int updatedRows = repository.reactivateByCardNumber(cardNumber);

        if (updatedRows > 0) {
            // Log security event for audit
            auditService.logSecurityEvent("FRAUDULENT_CARD_REACTIVATED", reactivatedBy,
                    "Card: ****" + cardNumber.substring(Math.max(0, cardNumber.length() - 4)),
                    true);

            // Retrieve and return the updated card
            Optional<FraudulentCard> updated = repository.findByCardNumber(cardNumber);
            if (updated.isPresent()) {
                logger.info("Successfully reactivated fraudulent card: ****{}",
                        cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
                return updated.get();
            }
        }

        throw new IllegalArgumentException("Card not found or could not be reactivated: " + "****"
                + cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
    }

    /**
     * Get fraudulent card information by card number.
     * 
     * @param cardNumber The card number to look up
     * @return Optional containing FraudulentCard if found
     */
    @Transactional(readOnly = true)
    public Optional<FraudulentCard> getCardInfo(String cardNumber) {
        return repository.findByCardNumber(cardNumber);
    }

    /**
     * Check if a card is actively flagged as fraudulent.
     * 
     * @param cardNumber The card number to check
     * @return true if card is fraudulent and active, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isCardActiveFraudulent(String cardNumber) {
        return repository.existsByCardNumberAndActiveTrue(cardNumber);
    }

    /**
     * Batch check multiple cards for fraud status.
     * 
     * @param cardNumbers List of card numbers to check
     * @return List of card numbers that are fraudulent and active
     */
    @Transactional(readOnly = true)
    public List<String> findActiveFraudulentCards(List<String> cardNumbers) {
        if (cardNumbers == null || cardNumbers.isEmpty()) {
            return List.of();
        }

        return repository.findFraudulentCardNumbers(cardNumbers);
    }

    /**
     * Get recently added fraudulent cards for monitoring.
     * 
     * @param since DateTime threshold for "recent" entries
     * @return List of recently added fraudulent cards
     */
    @Transactional(readOnly = true)
    public List<FraudulentCard> getRecentlyAddedCards(LocalDateTime since) {
        return repository.findRecentlyAdded(since);
    }

    /**
     * Get statistics about fraudulent card entries.
     * 
     * @return FraudulentCardStats with counts and metrics
     */
    @Transactional(readOnly = true)
    public FraudulentCardStats getStatistics() {
        List<Object[]> statsData = repository.getStatsByActiveStatus();

        FraudulentCardStats stats = new FraudulentCardStats();
        stats.setTimestamp(LocalDateTime.now());

        for (Object[] row : statsData) {
            Boolean active = (Boolean) row[0];
            Long count = (Long) row[1];

            if (active != null && active) {
                stats.setActiveCount(count);
            } else {
                stats.setInactiveCount(count);
            }
        }

        stats.setTotalCount(stats.getActiveCount() + stats.getInactiveCount());

        return stats;
    }

    /**
     * Bulk import fraudulent cards from a list.
     * 
     * @param cardNumbers List of card numbers to import
     * @param reason Common reason for all cards
     * @param importedBy Who imported the cards
     * @return Number of cards successfully imported
     */
    @Transactional
    public int bulkImportCards(List<String> cardNumbers, String reason, String importedBy) {
        logger.info("Bulk importing {} fraudulent cards by: {}", cardNumbers.size(), importedBy);

        int importedCount = 0;
        for (String cardNumber : cardNumbers) {
            try {
                addFraudulentCard(cardNumber, reason, importedBy);
                importedCount++;
            } catch (Exception e) {
                logger.error("Failed to import card ****{}: {}",
                        cardNumber.substring(Math.max(0, cardNumber.length() - 4)), e.getMessage());
            }
        }

        // Log the bulk import event
        auditService.logSecurityEvent("BULK_IMPORT", importedBy,
                String.format("Imported %d out of %d cards", importedCount, cardNumbers.size()),
                true);

        logger.info("Bulk import completed: {} out of {} cards imported", importedCount,
                cardNumbers.size());

        return importedCount;
    }

    /**
     * Data class for fraudulent card statistics.
     */
    public static class FraudulentCardStats {
        private LocalDateTime timestamp;
        private Long activeCount = 0L;
        private Long inactiveCount = 0L;
        private Long totalCount = 0L;

        // Getters and setters
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public Long getActiveCount() {
            return activeCount;
        }

        public void setActiveCount(Long activeCount) {
            this.activeCount = activeCount;
        }

        public Long getInactiveCount() {
            return inactiveCount;
        }

        public void setInactiveCount(Long inactiveCount) {
            this.inactiveCount = inactiveCount;
        }

        public Long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Long totalCount) {
            this.totalCount = totalCount;
        }

        @Override
        public String toString() {
            return "FraudulentCardStats{" + "timestamp=" + timestamp + ", activeCount="
                    + activeCount + ", inactiveCount=" + inactiveCount + ", totalCount="
                    + totalCount + '}';
        }
    }
}
