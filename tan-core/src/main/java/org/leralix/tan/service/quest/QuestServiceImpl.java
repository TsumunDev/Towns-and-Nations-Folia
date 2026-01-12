package org.leralix.tan.service.quest;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.domain.quest.model.Quest;
import org.leralix.tan.domain.quest.model.QuestObjective;
import org.leralix.tan.domain.quest.model.QuestReward;
import org.leralix.tan.domain.quest.model.QuestStatus;
import org.leralix.tan.domain.quest.model.QuestType;
import org.leralix.tan.domain.quest.rewards.PrestigeQuestReward;
import org.leralix.tan.domain.quest.queststatic.StaticQuest;
import org.leralix.tan.service.prestige.PrestigeService;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Implementation of the quest service.
 * <p>
 * This service manages the complete quest lifecycle with async operations
 * and proper Folia thread safety.
 * </p>
 */
public class QuestServiceImpl implements QuestService {

    private static QuestServiceImpl instance;

    private final Logger logger;
    private final Map<String, Quest> questsById;
    private final Map<QuestType, List<Quest>> questsByType;
    private final Map<String, QuestProgress> activeQuests; // playerUuid_questId -> progress
    private final Map<String, Long> questCooldowns; // playerUuid_questId -> completionTime

    private boolean initialized = false;

    private QuestServiceImpl() {
        this.logger = TownsAndNations.getPlugin().getLogger();
        this.questsById = new ConcurrentHashMap<>();
        this.questsByType = new ConcurrentHashMap<>();
        this.activeQuests = new ConcurrentHashMap<>();
        this.questCooldowns = new ConcurrentHashMap<>();

        // Initialize quest type lists
        for (QuestType type : QuestType.values()) {
            questsByType.put(type, new ArrayList<>());
        }
    }

    /**
     * Gets the singleton instance.
     */
    public static QuestServiceImpl getInstance() {
        if (instance == null) {
            instance = new QuestServiceImpl();
        }
        return instance;
    }

    @Override
    public void initialize() {
        if (initialized) {
            logger.warning("QuestService already initialized!");
            return;
        }

        logger.info("Initializing QuestService...");

        // TODO: Load quests from configuration file (quests.yml)
        // For now, we'll create a simple test quest manually
        loadDefaultQuests();

        initialized = true;
        logger.info("QuestService initialized with " + questsById.size() + " quests.");
    }

    /**
     * Loads default quests for testing.
     * TODO: Replace with YAML config loading
     */
    private void loadDefaultQuests() {
        // Create a simple farming quest
        StaticQuest wheatQuest = new StaticQuest(
                "wheat_harvest_i",
                "Wheat Harvest I",
                "Harvest 50 wheat for your town.",
                QuestType.FARM,
                List.of(new org.leralix.tan.domain.quest.queststatic.StaticQuestObjective(
                        "wheat",
                        "Harvest Wheat",
                        org.leralix.tan.domain.quest.model.QuestObjective.TargetType.MATERIAL,
                        "WHEAT",
                        50
                )),
                List.of(
                        new org.leralix.tan.domain.quest.rewards.XpQuestReward(100),
                        new PrestigeQuestReward(5),
                        new org.leralix.tan.domain.quest.rewards.MoneyQuestReward(50.0)
                ),
                org.leralix.tan.dataclass.territory.progression.TownTier.CAMPING,
                true,
                24 * 60 * 60 * 1000L // 24 hours cooldown
        );

        registerQuest(wheatQuest);

        logger.info("Loaded default quest: " + wheatQuest.getName());
    }

    /**
     * Registers a quest in the service.
     */
    private void registerQuest(Quest quest) {
        questsById.put(quest.getId(), quest);
        questsByType.get(quest.getType()).add(quest);
    }

    @Override
    public CompletableFuture<List<Quest>> getAvailableQuests(String townId) {
        return TownDataStorage.getInstance().get(townId)
                .thenApply(town -> {
                    if (town == null) {
                        return List.of();
                    }

                    return questsById.values().stream()
                            .filter(quest -> quest.isAvailableForTier(town.getTownTier()))
                            .toList();
                });
    }

    @Override
    public CompletableFuture<Optional<Quest>> getQuest(String questId) {
        return CompletableFuture.completedFuture(Optional.ofNullable(questsById.get(questId)));
    }

    @Override
    public CompletableFuture<Void> assignQuest(Player player, String questId) {
        return getQuest(questId).thenAccept(questOpt -> {
            if (questOpt.isEmpty()) {
                logger.warning("Quest not found: " + questId);
                return;
            }

            Quest quest = questOpt.get();

            // Check cooldown
            if (isOnCooldown(player.getUniqueId(), questId)) {
                player.sendMessage("§cThis quest is on cooldown!");
                return;
            }

            // Create quest progress
            String progressKey = getProgressKey(player.getUniqueId(), questId);

            // Initialize progress with zero for all objectives
            Map<String, Integer> initialProgress = new HashMap<>();
            for (QuestObjective objective : quest.getObjectives()) {
                initialProgress.put(objective.getId(), 0);
            }

            QuestProgress progress = QuestProgress.createWithObjectives(
                    questId,
                    player.getUniqueId(),
                    initialProgress
            );

            activeQuests.put(progressKey, progress);

            player.sendMessage("§aQuest accepted: §e" + quest.getName());
            player.sendMessage("§7" + quest.getDescription());
        });
    }

    @Override
    public CompletableFuture<List<String>> updateProgress(
            Player player,
            QuestType questType,
            String targetId,
            int amount
    ) {
        return PlayerDataStorage.getInstance().get(player)
                .thenCompose(tanPlayer -> {
                    String townId = tanPlayer.getTownId();
                    if (townId == null) {
                        return CompletableFuture.completedFuture(List.of());
                    }

                    // Get all quests of this type for the player's town
                    return getAvailableQuests(townId).thenApply(quests -> {
                        List<String> completedQuests = new ArrayList<>();

                        for (Quest quest : quests) {
                            if (quest.getType() != questType) {
                                continue;
                            }

                            String progressKey = getProgressKey(player.getUniqueId(), quest.getId());
                            QuestProgress currentProgress = activeQuests.get(progressKey);

                            if (currentProgress == null || currentProgress.status() != QuestStatus.ACTIVE) {
                                continue;
                            }

                            // Check if any objective matches this target
                            boolean anyMatch = false;
                            for (QuestObjective objective : quest.getObjectives()) {
                                if (objective.matchesTarget(targetId)) {
                                    anyMatch = true;
                                    break;
                                }
                            }

                            if (!anyMatch) {
                                continue;
                            }

                            // Update progress for matching objectives
                            QuestProgress newProgress = currentProgress;
                            for (QuestObjective objective : quest.getObjectives()) {
                                if (objective.matchesTarget(targetId)) {
                                    newProgress = newProgress.updateObjective(objective.getId(), amount);
                                }
                            }

                            activeQuests.put(progressKey, newProgress);

                            // Check if all objectives are completed
                            Map<String, Integer> requiredAmounts = new HashMap<>();
                            for (QuestObjective objective : quest.getObjectives()) {
                                requiredAmounts.put(objective.getId(), objective.getRequiredAmount());
                            }

                            if (newProgress.areAllObjectivesCompleted(requiredAmounts)) {
                                QuestProgress completedProgress = newProgress.markCompleted();
                                activeQuests.put(progressKey, completedProgress);
                                completedQuests.add(quest.getId());

                                // Notify player
                                player.sendMessage("§aQuest completed: §e" + quest.getName());
                                player.sendMessage("§7Click to claim rewards!");
                            }
                        }

                        return completedQuests;
                    });
                });
    }

    @Override
    public CompletableFuture<Void> completeQuest(Player player, String questId) {
        String progressKey = getProgressKey(player.getUniqueId(), questId);
        QuestProgress progress = activeQuests.get(progressKey);

        if (progress == null || progress.status() != QuestStatus.COMPLETED) {
            player.sendMessage("§cQuest not completed or not found!");
            return CompletableFuture.completedFuture(null);
        }

        return getQuest(questId).thenAccept(questOpt -> {
            if (questOpt.isEmpty()) {
                return;
            }

            Quest quest = questOpt.get();

            // Grant rewards
            for (QuestReward reward : quest.getRewards()) {
                try {
                    reward.grant(player);
                } catch (Exception e) {
                    logger.severe("Failed to grant reward: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Mark as claimed
            QuestProgress claimedProgress = progress.markClaimed();
            activeQuests.put(progressKey, claimedProgress);

            // Set cooldown if repeatable
            if (quest.isRepeatable()) {
                questCooldowns.put(progressKey, System.currentTimeMillis());
            }

            player.sendMessage("§a§lQuest Claimed!");
            player.sendMessage("§7Rewards granted successfully.");
        });
    }

    @Override
    public CompletableFuture<Void> abandonQuest(Player player, String questId) {
        String progressKey = getProgressKey(player.getUniqueId(), questId);
        QuestProgress progress = activeQuests.get(progressKey);

        if (progress == null) {
            player.sendMessage("§cQuest not found!");
            return CompletableFuture.completedFuture(null);
        }

        QuestProgress abandonedProgress = progress.markAbandoned();
        activeQuests.put(progressKey, abandonedProgress);

        player.sendMessage("§cQuest abandoned: " + questId);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<QuestProgress>> getActiveQuests(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            List<QuestProgress> result = new ArrayList<>();

            for (Map.Entry<String, QuestProgress> entry : activeQuests.entrySet()) {
                if (entry.getKey().startsWith(player.getUniqueId().toString()) &&
                    entry.getValue().status() == QuestStatus.ACTIVE) {
                    result.add(entry.getValue());
                }
            }

            return result;
        });
    }

    @Override
    public CompletableFuture<Optional<QuestProgress>> getQuestProgress(Player player, String questId) {
        return CompletableFuture.supplyAsync(() -> {
            String progressKey = getProgressKey(player.getUniqueId(), questId);
            return Optional.ofNullable(activeQuests.get(progressKey));
        });
    }

    @Override
    public boolean isOnCooldown(UUID playerUuid, String questId) {
        String progressKey = getProgressKey(playerUuid, questId);
        Long completionTime = questCooldowns.get(progressKey);

        if (completionTime == null) {
            return false;
        }

        // Get the quest to check cooldown duration
        Quest quest = questsById.get(questId);
        if (quest == null || !quest.isRepeatable()) {
            return false;
        }

        long elapsedTime = System.currentTimeMillis() - completionTime;
        return elapsedTime < quest.getCooldownMs();
    }

    /**
     * Creates a unique key for storing quest progress.
     */
    private String getProgressKey(UUID playerUuid, String questId) {
        return playerUuid + "_" + questId;
    }
}
