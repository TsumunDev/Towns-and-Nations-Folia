package org.leralix.tan.domain.quest.queststatic;

import org.leralix.tan.domain.quest.model.QuestObjective;

/**
 * Immutable implementation of a quest objective.
 * <p>
 * Used for static quests defined in configuration files.
 * </p>
 *
 * @param id Unique identifier for this objective
 * @param description Human-readable description
 * @param targetType The type of target (MATERIAL, ENTITY, BLOCK, ACTIVITY)
 * @param targetId The specific target identifier
 * @param requiredAmount Amount required to complete
 */
public record StaticQuestObjective(
        String id,
        String description,
        TargetType targetType,
        String targetId,
        int requiredAmount
) implements QuestObjective {

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public TargetType getTargetType() {
        return targetType;
    }

    @Override
    public String getTargetId() {
        return targetId;
    }

    @Override
    public int getRequiredAmount() {
        return requiredAmount;
    }

    /**
     * Creates a new objective with updated progress.
     * This is used for tracking progress without modifying the original (immutable).
     */
    public StaticQuestObjective withProgress(int currentProgress) {
        return this; // Progress is tracked separately in QuestProgress
    }
}
