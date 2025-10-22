-- Migration: V1__create_fraudulent_cards_table.sql
-- Description: Create fraudulent_cards table for storing known fraudulent credit card numbers
-- Author: GloboBank Fraud Detection Team
-- Date: 2025-10-21

-- Create fraudulent_cards table with proper constraints and indexing
CREATE TABLE fraudulent_cards (
    -- Primary key with auto-increment
    id BIGSERIAL PRIMARY KEY,
    
    -- Credit card number (13-19 digits, stored as VARCHAR for flexibility)
    card_number VARCHAR(19) NOT NULL,
    
    -- Audit timestamps
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP,
    
    -- Active flag for operational control (default: true)
    active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Ensure card_number uniqueness
    CONSTRAINT uk_fraudulent_cards_card_number UNIQUE (card_number),
    
    -- Validate card_number format (13-19 digits only)
    CONSTRAINT chk_fraudulent_cards_card_number_format 
        CHECK (card_number ~ '^[0-9]{13,19}$'),
    
    -- Ensure created_date is not in the future
    CONSTRAINT chk_fraudulent_cards_created_date 
        CHECK (created_date <= CURRENT_TIMESTAMP),
    
    -- Ensure updated_date is after created_date when present
    CONSTRAINT chk_fraudulent_cards_updated_date 
        CHECK (updated_date IS NULL OR updated_date >= created_date)
);

-- Create index for fast lookup during fraud detection
-- This is critical for sub-200ms performance requirement
CREATE INDEX idx_fraudulent_cards_card_number 
    ON fraudulent_cards (card_number) 
    WHERE active = true;

-- Create index for active flag filtering
CREATE INDEX idx_fraudulent_cards_active 
    ON fraudulent_cards (active);

-- Create index for audit queries by creation date
CREATE INDEX idx_fraudulent_cards_created_date 
    ON fraudulent_cards (created_date);

-- Add comments for documentation
COMMENT ON TABLE fraudulent_cards 
    IS 'Storage for credit card numbers flagged as fraudulent for real-time fraud detection';

COMMENT ON COLUMN fraudulent_cards.id 
    IS 'Primary key, auto-generated unique identifier';

COMMENT ON COLUMN fraudulent_cards.card_number 
    IS 'Credit card number (13-19 digits) flagged as fraudulent';

COMMENT ON COLUMN fraudulent_cards.created_date 
    IS 'Timestamp when the card was added to the fraudulent list';

COMMENT ON COLUMN fraudulent_cards.updated_date 
    IS 'Timestamp when the record was last modified (NULL if never updated)';

COMMENT ON COLUMN fraudulent_cards.active 
    IS 'Flag indicating if the fraud rule is currently active (true=active, false=inactive)';

-- Insert initial test data for development and testing
-- These are test card numbers from payment processors for testing purposes
INSERT INTO fraudulent_cards (card_number, created_date, active) VALUES
    ('4000000000000002', CURRENT_TIMESTAMP, true),  -- Visa test card for fraud simulation
    ('5555555555554444', CURRENT_TIMESTAMP, true),  -- Mastercard test card for fraud simulation
    ('378282246310005', CURRENT_TIMESTAMP, true),   -- American Express test card for fraud simulation
    ('4111111111111111', CURRENT_TIMESTAMP, false), -- Inactive test card
    ('4012888888881881', CURRENT_TIMESTAMP, true);  -- Additional Visa test card

-- Create function to automatically update updated_date on row modifications
CREATE OR REPLACE FUNCTION update_fraudulent_cards_updated_date()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_date = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically set updated_date on UPDATE operations
CREATE TRIGGER trg_fraudulent_cards_update_timestamp
    BEFORE UPDATE ON fraudulent_cards
    FOR EACH ROW
    EXECUTE FUNCTION update_fraudulent_cards_updated_date();

-- Grant appropriate permissions for application user
-- Note: In production, these would be managed through proper role-based access
GRANT SELECT, INSERT, UPDATE ON fraudulent_cards TO frauduser;
GRANT USAGE ON SEQUENCE fraudulent_cards_id_seq TO frauduser;