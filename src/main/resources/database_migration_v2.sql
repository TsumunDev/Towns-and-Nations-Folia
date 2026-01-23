-- ====================================================================
-- Towns and Nations - Database Schema Migration v2.0
-- ====================================================================
-- This script adds SQL-exposed columns to existing tables for better
-- queryability and admin readability.
--
-- Features:
-- - Non-breaking (adds columns, preserves existing data)
-- - Backwards compatible (JSON data column still used)
-- - Adds indexes for performance
-- - Normalizes foreign key references (town_id, nation_id)
-- ====================================================================

-- ====================================================================
-- TABLE: tan_players
-- ====================================================================

-- Add IP address column for security/admin tracking
ALTER TABLE tan_players ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);

-- Add first_seen timestamp for analytics
ALTER TABLE tan_players ADD COLUMN IF NOT EXISTS first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add balance column for SQL queries (economy)
ALTER TABLE tan_players ADD COLUMN IF NOT EXISTS balance DOUBLE DEFAULT 0.0;

-- Add online status for fast queries
ALTER TABLE tan_players ADD COLUMN IF NOT EXISTS is_online BOOLEAN DEFAULT FALSE;

-- Add normalized foreign keys (redundant with JSON, but queryable)
ALTER TABLE tan_players ADD COLUMN IF NOT EXISTS town_id VARCHAR(255);
ALTER TABLE tan_players ADD COLUMN IF NOT EXISTS nation_id VARCHAR(255);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_players_town_id ON tan_players(town_id);
CREATE INDEX IF NOT EXISTS idx_players_nation_id ON tan_players(nation_id);
CREATE INDEX IF NOT EXISTS idx_players_balance ON tan_players(balance);
CREATE INDEX IF NOT EXISTS idx_players_online ON tan_players(is_online);
CREATE INDEX IF NOT EXISTS idx_players_ip ON tan_players(ip_address);
CREATE INDEX IF NOT EXISTS idx_players_first_seen ON tan_players(first_seen);

-- ====================================================================
-- TABLE: tan_towns
-- ====================================================================

-- Add leader columns (current mayor)
ALTER TABLE tan_towns ADD COLUMN IF NOT EXISTS leader_uuid VARCHAR(255);
ALTER TABLE tan_towns ADD COLUMN IF NOT EXISTS leader_name VARCHAR(255);

-- Add nation foreign key (normalized)
ALTER TABLE tan_towns ADD COLUMN IF NOT EXISTS nation_id VARCHAR(255);

-- Add economy column for SQL queries
ALTER TABLE tan_towns ADD COLUMN IF NOT EXISTS bank_balance DOUBLE DEFAULT 0.0;

-- Add stats columns for fast queries
ALTER TABLE tan_towns ADD COLUMN IF NOT EXISTS claims_count INT DEFAULT 0;
ALTER TABLE tan_towns ADD COLUMN IF NOT EXISTS members_count INT DEFAULT 0;

-- Add settings column
ALTER TABLE tan_towns ADD COLUMN IF NOT EXISTS is_open BOOLEAN DEFAULT FALSE;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_towns_leader_uuid ON tan_towns(leader_uuid);
CREATE INDEX IF NOT EXISTS idx_towns_nation_id ON tan_towns(nation_id);
CREATE INDEX IF NOT EXISTS idx_towns_balance ON tan_towns(bank_balance);
CREATE INDEX IF NOT EXISTS idx_towns_members ON tan_towns(members_count);
CREATE INDEX IF NOT EXISTS idx_towns_claims ON tan_towns(claims_count);
CREATE INDEX IF NOT EXISTS idx_towns_is_open ON tan_towns(is_open);

-- ====================================================================
-- TABLE: tan_regions
-- ====================================================================

-- Add leader columns
ALTER TABLE tan_regions ADD COLUMN IF NOT EXISTS leader_uuid VARCHAR(255);
ALTER TABLE tan_regions ADD COLUMN IF NOT EXISTS leader_name VARCHAR(255);

-- Add capital foreign key (normalized)
ALTER TABLE tan_regions ADD COLUMN IF NOT EXISTS capital_id VARCHAR(255);

-- Add stats column
ALTER TABLE tan_regions ADD COLUMN IF NOT EXISTS members_count INT DEFAULT 0;

-- Add creation_date if not exists
ALTER TABLE tan_regions ADD COLUMN IF NOT EXISTS creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_regions_leader_uuid ON tan_regions(leader_uuid);
CREATE INDEX IF NOT EXISTS idx_regions_capital_id ON tan_regions(capital_id);
CREATE INDEX IF NOT EXISTS idx_regions_members ON tan_regions(members_count);
CREATE INDEX IF NOT EXISTS idx_regions_creation_date ON tan_regions(creation_date);

-- ====================================================================
-- MIGRATION COMPLETE
-- ====================================================================
-- Next steps:
-- 1. Restart the plugin
-- 2. Run the in-game migration command: /tan admin migrate-database
-- 3. Verify data with: SELECT player_name, balance, town_name FROM tan_players;
-- ====================================================================
