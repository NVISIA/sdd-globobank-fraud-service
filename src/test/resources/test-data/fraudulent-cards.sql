-- Test data for fraudulent cards
INSERT INTO fraudulent_cards (credit_card_number, reason_code, created_at, updated_at) 
VALUES ('4532123456789012', 'STOLEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO fraudulent_cards (credit_card_number, reason_code, created_at, updated_at) 
VALUES ('4111111111111111', 'COMPROMISED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
