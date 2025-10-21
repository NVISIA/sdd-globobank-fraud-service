package com.globobank.fraud.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * Entity representing fraudulent credit card numbers in the database.
 * 
 * This table contains credit card numbers that are known to be associated with fraudulent activity.
 * Used for lookup during fraud detection to determine if a card should receive a high risk score.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Entity
@Table(name = "fraudulent_cards",
        indexes = {@Index(name = "idx_fraudulent_cards_card_number", columnList = "card_number")})
public class FraudulentCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Credit card number that is known to be fraudulent. Indexed for fast lookup during fraud
     * detection.
     */
    @Column(name = "card_number", nullable = false, unique = true, length = 19)
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13-19 digits")
    private String cardNumber;

    /**
     * Optional reason or source for why this card is marked as fraudulent.
     */
    @Column(name = "reason", length = 255)
    private String reason;

    /**
     * Timestamp when this card was added to the fraudulent list.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Flag to enable/disable this fraudulent card entry without deletion.
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Default constructor.
     */
    public FraudulentCard() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor with card number.
     * 
     * @param cardNumber The fraudulent card number
     */
    public FraudulentCard(String cardNumber) {
        this();
        this.cardNumber = cardNumber;
    }

    /**
     * Constructor with card number and reason.
     * 
     * @param cardNumber The fraudulent card number
     * @param reason Reason for marking as fraudulent
     */
    public FraudulentCard(String cardNumber, String reason) {
        this(cardNumber);
        this.reason = reason;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "FraudulentCard{" + "id=" + id + ", cardNumber='****"
                + (cardNumber != null && cardNumber.length() >= 4
                        ? cardNumber.substring(cardNumber.length() - 4)
                        : "****")
                + '\'' + ", reason='" + reason + '\'' + ", createdAt=" + createdAt + ", active="
                + active + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        FraudulentCard that = (FraudulentCard) o;

        return cardNumber != null ? cardNumber.equals(that.cardNumber) : that.cardNumber == null;
    }

    @Override
    public int hashCode() {
        return cardNumber != null ? cardNumber.hashCode() : 0;
    }
}
