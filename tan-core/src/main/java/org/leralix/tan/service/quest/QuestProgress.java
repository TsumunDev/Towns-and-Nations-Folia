package org.leralix.tan.service.quest;

import org.leralix.tan.domain.quest.model.QuestStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks a player's progress on a specific quest.
 * <p>
 * This record is immutable and creates new instances when progress is updated.
 * </p>
 *
 * @param questId The ID of the quest
 * @param playerUuid The UUID of the player undertaking the quest
 * @param objectiveProgress Map of objective ID to current progress amount
 * @param status Current status of the quest
 * @param startTime When the quest was started (unix timestamp)
 * @param completionTime When the quest was completed (unix timestamp, 0 if not completed)
 */
public record QuestProgress(
        String questId,
        UUID playerUuid,
        Map<String, Integer> objectiveProgress,
        QuestStatus status,
        long startTime,
        long completionTime
) {

    public QuestProgress {
        // Make the map unmodifiable for immutability
        objectiveProgress = Map.copyOf(objectiveProgress);
    }

    /**
     * Creates a new quest progress for the given player.
     */
    public static QuestProgress create(String questId, UUID playerUuid) {
        return new QuestProgress(
                questId,
                playerUuid,
                new HashMap<>(),
                QuestStatus.ACTIVE,
                System.currentTimeMillis(),
                0
        );
    }

    /**
     * Creates a quest progress with initial objective values.
     */
    public static QuestProgress createWithObjectives(String questId, UUID playerUuid, Map<String, Integer> initialProgress) {
        return new QuestProgress(
                questId,
                playerUuid,
                new HashMap<>(initialProgress),
                QuestStatus.ACTIVE,
                System.currentTimeMillis(),
                0
        );
    }

    /**
     * Updates progress for a specific objective.
     *
     * @param objectiveId The objective to update
     * @param additionalProgress Amount to add
     * @return New instance with updated progress
     */
    public QuestProgress updateObjective(String objectiveId, int additionalProgress) {
        Map<String, Integer> newProgress = new HashMap<>(objectiveProgress);
        int current = newProgress.getOrDefault(objectiveId, 0);
        newProgress.put(objectiveId, current + additionalProgress);

        return new QuestProgress(
                questId,
                playerUuid,
                newProgress,
                status,
                startTime,
                completionTime
        );
    }

    /**
     * Gets the current progress for a specific objective.
     */
    public int getObjectiveProgress(String objectiveId) {
        return objectiveProgress.getOrDefault(objectiveId, 0);
    }

    /**
     * Marks the quest as completed.
     */
    public QuestProgress markCompleted() {
        return new QuestProgress(
                questId,
                playerUuid,
                objectiveProgress,
                QuestStatus.COMPLETED,
                startTime,
                System.currentTimeMillis()
        );
    }

    /**
     * Marks the quest as claimed (rewards received).
     */
    public QuestProgress markClaimed() {
        return new QuestProgress(
                questId,
                playerUuid,
                objectiveProgress,
                QuestStatus.CLAIMED,
                startTime,
                completionTime
        );
    }

    /**
     * Marks the quest as abandoned.
     */
    public QuestProgress markAbandoned() {
        return new QuestProgress(
                questId,
                playerUuid,
                objectiveProgress,
                QuestStatus.ABANDONED,
                startTime,
                completionTime
        );
    }

    /**
     * Checks if all objectives are completed.
     */
    public boolean areAllObjectivesCompleted(Map<String, Integer> requiredAmounts) {
        return requiredAmounts.entrySet().stream()
                .allMatch(entry -> {
                    int current = getObjectiveProgress(entry.getKey());
                    return current >= entry.getValue();
                });
    }

    /**
     * Calculates overall progress percentage.
     */
    public double getOverallProgressPercentage(Map<String, Integer> requiredAmounts) {
        if (requiredAmounts.isEmpty()) {
            return 1.0;
        }

        double totalPercentage = requiredAmounts.entrySet().stream()
                .mapToDouble(entry -> {
                    int current = getObjectiveProgress(entry.getKey());
                    int required = entry.getValue();
                    return Math.min(1.0, (double) current / required);
                })
                .sum();

        return totalPercentage / requiredAmounts.size();
    }
}
