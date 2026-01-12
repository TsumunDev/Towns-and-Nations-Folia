package org.leralix.tan.domain.prestige.model;

/**
 * Sources of prestige points for towns.
 * <p>
 * Prestige points are the currency used to purchase town upgrades.
 * They can be earned through various activities in the game.
 * </p>
 */
public enum PrestigeSource {
    /**
     * Earned by completing quests.
     */
    QUEST_COMPLETION,

    /**
     * Earned by reaching level milestones (every 5, 10, 25 levels).
     */
    LEVEL_MILESTONE,

    /**
     * Earned by ascending to a new civilization tier.
     */
    TIER_ASCENSION,

    /**
     * Earned by daily activity (being online, participating).
     */
    DAILY_ACTIVITY,

    /**
     * Spent in the prestige shop (used for negative transactions).
     */
    SHOP_PURCHASE,

    /**
     * Granted/removed via admin commands.
     */
    ADMIN_COMMAND,

    /**
     * Earned from war victories (Post-MVP).
     */
    WAR_VICTORY,

    /**
     * Earned from capturing landmarks (Post-MVP).
     */
    LANDMARK_CAPTURE
}
