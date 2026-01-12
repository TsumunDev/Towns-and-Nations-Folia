package org.leralix.tan.domain.quest.model;

import org.leralix.tan.dataclass.territory.progression.TownTier;

import java.util.List;

/**
 * Represents a quest in the game.
 * <p>
 * All quest implementations must be immutable for thread safety.
 * Quests can be static (configured in YAML) or dynamic (generated procedurally).
 * </p>
 *
 * @since 2.0
 */
public interface Quest {

    /**
     * Unique identifier for this quest (e.g., "wheat_harvest_i").
     */
    String getId();

    /**
     * Human-readable name (e.g., "Wheat Harvest I").
     */
    String getName();

    /**
     * Detailed description of what the quest requires.
     */
    String getDescription();

    /**
     * Type of quest (FARM, MINING, COMBAT, BUILD, ACTIVITY).
     */
    QuestType getType();

    /**
     * Objectives that must be completed to finish this quest.
     */
    List<QuestObjective> getObjectives();

    /**
     * Rewards granted upon completion.
     */
    List<QuestReward> getRewards();

    /**
     * Minimum civilization tier required to accept this quest.
     */
    TownTier getRequiredTier();

    /**
     * Whether this quest can be repeated after completion.
     */
    boolean isRepeatable();

    /**
     * Cooldown in milliseconds before this quest can be accepted again.
     * Only applies if repeatable is true.
     */
    long getCooldownMs();

    /**
     * Checks if this quest is available for the given town tier.
     */
    default boolean isAvailableForTier(TownTier tier) {
        return tier.getLevel() >= getRequiredTier().getLevel();
    }
}
