package org.leralix.tan.domain.upgrade.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.lang.LangType;

/**
 * Rewards granted when an upgrade is purchased.
 * <p>
 * Upgrade rewards can increase stats, unlock features, or provide other benefits.
 * </p>
 */
public interface UpgradeReward {

    /**
     * Applies this reward to the given town.
     *
     * @param town The town to apply the reward to
     */
    void apply(TownData town);

    /**
     * Gets a human-readable description of this reward.
     *
     * @param lang The language type
     * @return Description component
     */
    Component getDescription(LangType lang);

    /**
     * Increases a numeric stat (player cap, chunk cap, etc.).
     */
    record StatIncrease(StatType statType, int value) implements UpgradeReward {
        @Override
        public void apply(TownData town) {
            // Apply stat increase based on type
            switch (statType) {
                case PLAYER_CAP -> {
                    // Increase player cap using existing upgrade system
                    town.getUpgradesStatus().addStat(
                            org.leralix.tan.upgrade.rewards.numeric.TownPlayerCap.class,
                            value
                    );
                }
                case CHUNK_CAP -> {
                    town.getUpgradesStatus().addStat(
                            org.leralix.tan.upgrade.rewards.numeric.ChunkCap.class,
                            value
                    );
                }
                case PROPERTY_CAP -> {
                    town.getUpgradesStatus().addStat(
                            org.leralix.tan.upgrade.rewards.numeric.PropertyCap.class,
                            value
                    );
                }
                case LANDMARK_CAP -> {
                    town.getUpgradesStatus().addStat(
                            org.leralix.tan.upgrade.rewards.numeric.LandmarkCap.class,
                            value
                    );
                }
            }
        }

        @Override
        public Component getDescription(LangType lang) {
            return Component.text("+" + value + " " + statType.displayName, NamedTextColor.GREEN);
        }
    }

    /**
     * Unlocks a boolean feature (mob ban, spawn, etc.).
     */
    record FeatureUnlock(FeatureType feature) implements UpgradeReward {
        @Override
        public void apply(TownData town) {
            // Unlock the feature
            switch (feature) {
                case MOB_BAN -> {
                    town.getUpgradesStatus().setStat(
                            org.leralix.tan.upgrade.rewards.bool.EnableMobBan.class,
                            true
                    );
                }
                case TOWN_SPAWN -> {
                    town.getUpgradesStatus().setStat(
                            org.leralix.tan.upgrade.rewards.bool.EnableTownSpawn.class,
                            true
                    );
                }
                // Post-MVP features will be added here
            }
        }

        @Override
        public Component getDescription(LangType lang) {
            return Component.text("Unlocks: " + feature.displayName, NamedTextColor.AQUA);
        }
    }

    /**
     * Types of numeric stats that can be increased.
     */
    enum StatType {
        PLAYER_CAP("Player Slots"),
        CHUNK_CAP("Chunk Claims"),
        PROPERTY_CAP("Properties"),
        LANDMARK_CAP("Landmarks");

        private final String displayName;

        StatType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Types of features that can be unlocked.
     */
    enum FeatureType {
        MOB_BAN("Mob Protection"),
        TOWN_SPAWN("Town Spawn"),
        WAR_DECLARATION("War Declaration"),      // Post-MVP
        NATION_CREATION("Nation Creation");       // Post-MVP

        private final String displayName;

        FeatureType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
