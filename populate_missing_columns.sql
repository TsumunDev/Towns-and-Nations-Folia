-- Migration script to populate NULL columns with data from JSON
-- This script fills creator_uuid, creator_name, town_name, and nation_name columns
-- Run this AFTER migrate_tables.sql to populate existing data

-- STEP 1: Rename tables first (if not done already)
-- Run migrate_tables.sql first, then continue with this script

-- STEP 2: Populate ccn_towns creator_uuid and creator_name from JSON data
-- This extracts the leader information from the JSON data column
UPDATE `ccn_towns`
SET
    `creator_uuid` = JSON_UNQUOTE(JSON_EXTRACT(`data`, '$.uuidLeader')),
    `creator_name` = (SELECT `player_name` FROM `ccn_players` WHERE `id` = JSON_UNQUOTE(JSON_EXTRACT(`data`, '$.uuidLeader')) LIMIT 1)
WHERE `creator_uuid` IS NULL OR `creator_uuid` = '';

-- STEP 3: Populate ccn_players town_name from the towns table
UPDATE `ccn_players` p
SET `town_name` = (
    SELECT `town_name`
    FROM `ccn_towns`
    WHERE `id` = p.`town_id`
    LIMIT 1
)
WHERE p.`town_id` IS NOT NULL
  AND (p.`town_name` IS NULL OR p.`town_name` = '');

-- STEP 4: Populate ccn_players nation_name from the regions table
UPDATE `ccn_players` p
SET `nation_name` = (
    SELECT `name`
    FROM `ccn_regions`
    WHERE `id` = p.`nation_id`
    LIMIT 1
)
WHERE p.`nation_id` IS NOT NULL
  AND (p.`nation_name` IS NULL OR p.`nation_name` = '');

-- STEP 5: Update leader_uuid and leader_name in ccn_towns (should match creator for existing towns)
UPDATE `ccn_towns`
SET
    `leader_uuid` = JSON_UNQUOTE(JSON_EXTRACT(`data`, '$.uuidLeader')),
    `leader_name` = (SELECT `player_name` FROM `ccn_players` WHERE `id` = JSON_UNQUOTE(JSON_EXTRACT(`data`, '$.uuidLeader')) LIMIT 1)
WHERE `leader_uuid` IS NULL OR `leader_uuid` = '';

-- Verification queries - Run these to check the results
-- SELECT id, town_name, creator_uuid, creator_name, leader_uuid, leader_name FROM `ccn_towns` LIMIT 10;
-- SELECT id, player_name, town_name, nation_name FROM `ccn_players` LIMIT 10;
