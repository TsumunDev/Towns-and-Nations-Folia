-- COMPLETE MIGRATION SCRIPT FOR TAN → CCN
-- This script handles both table renaming AND data migration
-- Execute this entire script in phpMyAdmin

-- =====================================================
-- PART 1: RENAME TABLES (if not already done)
-- =====================================================

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

-- =====================================================
-- PART 2: ADD MISSING COLUMNS TO ccn_towns
-- =====================================================

-- Add creator_uuid column if not exists
SET @dbname = DATABASE();
SET @tablename = 'ccn_towns';
SET @columnname = 'creator_uuid';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `creator_uuid` VARCHAR(255) NULL AFTER `town_name`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add creator_name column if not exists
SET @columnname = 'creator_name';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `creator_name` VARCHAR(255) NULL AFTER `creator_uuid`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add leader_uuid column if not exists
SET @columnname = 'leader_uuid';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `leader_uuid` VARCHAR(255) NULL AFTER `creator_name`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add leader_name column if not exists
SET @columnname = 'leader_name';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `leader_name` VARCHAR(255) NULL AFTER `leader_uuid`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- =====================================================
-- PART 3: ADD MISSING COLUMNS TO ccn_players
-- =====================================================

SET @tablename = 'ccn_players';

-- Add town_name column if not exists
SET @columnname = 'town_name';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `town_name` VARCHAR(255) NULL AFTER `player_name`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add nation_name column if not exists
SET @columnname = 'nation_name';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN `nation_name` VARCHAR(255) NULL AFTER `town_name`')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- =====================================================
-- PART 4: POPULATE DATA FOR ccn_towns
-- =====================================================

-- Update creator_uuid from JSON data
UPDATE `ccn_towns`
SET `creator_uuid` = JSON_UNQUOTE(JSON_EXTRACT(`data`, '$.uuidLeader'))
WHERE `creator_uuid` IS NULL OR `creator_uuid` = '';

-- Update creator_name from players table
UPDATE `ccn_towns` t
SET `creator_name` = (
    SELECT `player_name`
    FROM `ccn_players`
    WHERE `id` = t.`creator_uuid`
    LIMIT 1
)
WHERE `creator_name` IS NULL OR `creator_name` = '';

-- Update leader_uuid (same as creator for now)
UPDATE `ccn_towns`
SET `leader_uuid` = JSON_UNQUOTE(JSON_EXTRACT(`data`, '$.uuidLeader'))
WHERE `leader_uuid` IS NULL OR `leader_uuid` = '';

-- Update leader_name from players table
UPDATE `ccn_towns` t
SET `leader_name` = (
    SELECT `player_name`
    FROM `ccn_players`
    WHERE `id` = t.`leader_uuid`
    LIMIT 1
)
WHERE `leader_name` IS NULL OR `leader_name` = '';

-- =====================================================
-- PART 5: POPULATE DATA FOR ccn_players
-- =====================================================

-- Update town_name from towns table
UPDATE `ccn_players` p
SET `town_name` = (
    SELECT `town_name`
    FROM `ccn_towns`
    WHERE `id` = p.`town_id`
    LIMIT 1
)
WHERE p.`town_id` IS NOT NULL
  AND (p.`town_name` IS NULL OR p.`town_name` = '');

-- Update nation_name from regions table
UPDATE `ccn_players` p
SET `nation_name` = (
    SELECT `name`
    FROM `ccn_regions`
    WHERE `id` = p.`nation_id`
    LIMIT 1
)
WHERE p.`nation_id` IS NOT NULL
  AND (p.`nation_name` IS NULL OR p.`nation_name` = '');

-- =====================================================
-- PART 6: VERIFICATION
-- =====================================================

-- Check towns data
SELECT
    'CCN_TOWNS SAMPLE DATA:' as info;
SELECT
    id,
    town_name,
    creator_uuid,
    creator_name,
    leader_uuid,
    leader_name
FROM `ccn_towns`
LIMIT 5;

-- Check players data
SELECT
    'CCN_PLAYERS SAMPLE DATA:' as info;
SELECT
    id,
    player_name,
    town_name,
    nation_name
FROM `ccn_players`
LIMIT 5;

-- =====================================================
-- END OF MIGRATION SCRIPT
-- =====================================================
