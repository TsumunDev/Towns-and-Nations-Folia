package org.leralix.tan.service.quest;

import org.bukkit.entity.Player;
import org.leralix.tan.domain.quest.model.Quest;
import org.leralix.tan.domain.quest.model.QuestType;
import org.leralix.tan.domain.quest.queststatic.StaticQuest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for quest management.
 * <p>
 * This service orchestrates the complete quest lifecycle:
 * <ul>
 *   <li>Loading quests from configuration</li>
 *   <li>Assigning quests to players</li>
 *   <li>Tracking progress via event listeners</li>
 *   <li>Completing quests and granting rewards</li>
 * </ul>
 * </p>
 * <p>
 * All operations are async and Folia-compatible.
 * </p>
 */
public interface QuestService {

    /**
     * Gets the singleton instance of the quest service.
     */
    static QuestService getInstance() {
        return QuestServiceImpl.getInstance();
    }

    /**
     * Initializes the quest service and loads all quests from config.
     */
    void initialize();

    /**
     * Gets all available quests for a town (filtered by tier).
     *
     * @param townId The town ID
     * @return Future completing with list of available quests
     */
    CompletableFuture<List<Quest>> getAvailableQuests(String townId);

    /**
     * Gets a specific quest by ID.
     *
     * @param questId The quest ID
     * @return Future completing with optional quest
     */
    CompletableFuture<Optional<Quest>> getQuest(String questId);

    /**
     * Assigns a quest to a player.
     *
     * @param player The player to assign the quest to
     * @param questId The quest to assign
     * @return Future completing when assignment is complete
     */
    CompletableFuture<Void> assignQuest(Player player, String questId);

    /**
     * Updates quest progress for a player.
     * <p>
     * This method is called by event listeners when players perform
     * quest-related actions (breaking blocks, killing mobs, etc.).
     * </p>
     *
     * @param player The player
     * @param questType The type of quest
     * @param targetId The target (e.g., "WHEAT", "ZOMBIE")
     * @param amount The amount to add
     * @return Future completing with list of completed quest IDs
     */
    CompletableFuture<List<String>> updateProgress(
            Player player,
            QuestType questType,
            String targetId,
            int amount
    );

    /**
     * Completes a quest and grants rewards.
     *
     * @param player The player
     * @param questId The quest to complete
     * @return Future completing when rewards are granted
     */
    CompletableFuture<Void> completeQuest(Player player, String questId);

    /**
     * Abandons an active quest.
     *
     * @param player The player
     * @param questId The quest to abandon
     * @return Future completing when abandoned
     */
    CompletableFuture<Void> abandonQuest(Player player, String questId);

    /**
     * Gets all active quests for a player.
     *
     * @param player The player
     * @return Future completing with list of active quest progress
     */
    CompletableFuture<List<QuestProgress>> getActiveQuests(Player player);

    /**
     * Gets progress for a specific quest.
     *
     * @param player The player
     * @param questId The quest ID
     * @return Future completing with optional progress
     */
    CompletableFuture<Optional<QuestProgress>> getQuestProgress(Player player, String questId);

    /**
     * Checks if a quest is on cooldown for a player.
     *
     * @param playerUuid The player's UUID
     * @param questId The quest ID
     * @return true if on cooldown
     */
    boolean isOnCooldown(java.util.UUID playerUuid, String questId);
}
