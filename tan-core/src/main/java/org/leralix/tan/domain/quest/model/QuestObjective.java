package org.leralix.tan.domain.quest.model;

/**
 * Represents a single objective within a quest.
 * <p>
 * Quest objectives track progress towards specific goals
 * (e.g., "Harvest 50 Wheat", "Kill 20 Zombies").
 * </p>
 */
public interface QuestObjective {

    /**
     * Unique identifier for this objective within the quest.
     */
    String getId();

    /**
     * Human-readable description of what needs to be done.
     */
    String getDescription();

    /**
     * The target type (MATERIAL, ENTITY, BLOCK, ACTIVITY).
     */
    TargetType getTargetType();

    /**
     * The specific target identifier (e.g., "WHEAT", "ZOMBIE", "ANY").
     */
    String getTargetId();

    /**
     * The amount required to complete this objective.
     */
    int getRequiredAmount();

    /**
     * Checks if a specific target matches this objective's target.
     *
     * @param targetId The target to check (e.g., material name, entity type)
     * @return true if this objective cares about this target
     */
    default boolean matchesTarget(String targetId) {
        return "ANY".equals(getTargetId()) || getTargetId().equalsIgnoreCase(targetId);
    }

    /**
     * Types of quest objectives.
     */
    enum TargetType {
        /**
         * Breaking specific blocks (crops for farming).
         */
        MATERIAL,

        /**
         * Killing specific entities (mobs/players for combat).
         */
        ENTITY,

        /**
         * Placing specific blocks (building).
         */
        BLOCK,

        /**
         * Passive activity tracking (online time, etc.).
         */
        ACTIVITY
    }
}
