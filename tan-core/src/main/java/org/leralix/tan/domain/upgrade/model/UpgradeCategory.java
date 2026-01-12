package org.leralix.tan.domain.upgrade.model;

/**
 * Categories of town upgrades.
 * Used for organizing upgrades in the prestige shop GUI.
 */
public enum UpgradeCategory {
    /**
     * General upgrades that don't fit other categories.
     */
    GENERAL("General"),

    /**
     * Economy-related upgrades (taxes, costs, etc.).
     */
    ECONOMY("Economy"),

    /**
     * Military upgrades (war, defense - Post-MVP).
     */
    MILITARY("Military"),

    /**
     * Infrastructure upgrades (claims, buildings).
     */
    INFRASTRUCTURE("Infrastructure"),

    /**
     * Diplomacy upgrades (relations, alliances - Post-MVP).
     */
    DIPLOMACY("Diplomacy");

    private final String displayName;

    UpgradeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
