-- Migration: V2__optimize_fraudulent_cards_queries.sql
-- Description: Add database query optimizations for sub-200ms performance requirement
-- Author: GloboBank Fraud Detection Team
-- Date: 2025-10-21

-- Add composite index for the most common query pattern: card_number lookup with active filter
-- This optimizes the primary fraud detection query: existsByCardNumberAndActiveTrue
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_fraudulent_cards_card_number_active 
    ON fraudulent_cards (card_number, active);

-- Add statistics collection for query optimizer
-- This helps PostgreSQL choose the most efficient execution plan
ANALYZE fraudulent_cards;

-- Add comment for documentation
COMMENT ON INDEX idx_fraudulent_cards_card_number_active IS 
    'Composite index optimized for fraud detection queries filtering by card_number and active status';

-- Create partial index specifically for active fraud lookups (most common case)
-- This is smaller and faster than the full composite index
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_fraudulent_cards_active_lookup 
    ON fraudulent_cards (card_number) 
    WHERE active = true;

-- Add comment for the partial index
COMMENT ON INDEX idx_fraudulent_cards_active_lookup IS 
    'Partial index for active fraud card lookups - optimized for sub-200ms performance requirement';

-- Update table statistics to ensure query planner has current information
ANALYZE fraudulent_cards;