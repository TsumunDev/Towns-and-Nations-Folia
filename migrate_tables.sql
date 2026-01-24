-- Migration script to rename TAN tables to CCN tables
-- This script renames all existing tables from tan_* to ccn_*
-- Execute this script in your MySQL/MariaDB database before running the updated plugin

-- Rename players table
RENAME TABLE `tan_players` TO `ccn_players`;

-- Rename towns table
RENAME TABLE `tan_towns` TO `ccn_towns`;

-- Rename regions table
RENAME TABLE `tan_regions` TO `ccn_regions`;

-- Rename claimed_chunks table
RENAME TABLE `tan_claimed_chunks` TO `ccn_claimed_chunks`;

-- Rename wars table
RENAME TABLE `tan_wars` TO `ccn_wars`;

-- Rename truces table
RENAME TABLE `tan_truces` TO `ccn_truces`;

-- Rename landmarks table
RENAME TABLE `tan_landmarks` TO `ccn_landmarks`;

-- Rename planned_attacks table
RENAME TABLE `tan_planned_attacks` TO `ccn_planned_attacks`;

-- Rename premium_accounts table
RENAME TABLE `tan_premium_accounts` TO `ccn_premium_accounts`;

-- Verify the migration
-- Run this query to check that all tables have been renamed:
-- SHOW TABLES LIKE 'ccn_%';
