package org.leralix.tan.domain.quest.model;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a reward granted upon quest completion.
 * <p>
 * Quest rewards can include:
 * <ul>
 *   <li>XP for the town (progression system)</li>
 *   <li>Prestige points (currency for upgrades)</li>
 *   <li>Money (via Vault integration)</li>
 *   <li>Items (physical ItemStacks)</li>
 * </ul>
 * </p>
 */
public interface QuestReward {

    /**
     * Gets the type of reward.
     */
    RewardType getType();

    /**
     * Gets a human-readable description of this reward.
     */
    String getDescription();

    /**
     * Applies this reward to the appropriate recipient.
     *
     * @param player The player who completed the quest
     */
    void grant(org.bukkit.entity.Player player);

    /**
     * Types of quest rewards.
     */
    enum RewardType {
        /**
         * Experience points for the town.
         */
        XP,

        /**
         * Prestige points for purchasing upgrades.
         */
        PRESTIGE,

        /**
         * Money via Vault economy.
         */
        MONEY,

        /**
         * Physical item rewards.
         */
        ITEM
    }
}
