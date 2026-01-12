package org.leralix.tan.listeners.quest;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.domain.quest.model.QuestType;
import org.leralix.tan.service.quest.QuestService;
import org.leralix.tan.storage.stored.PlayerDataStorage;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Listens for entity kill events and updates combat quest progress.
 * <p>
 * This listener handles COMBAT quests where players must kill mobs.
 * </p>
 */
public class QuestEntityKillListener implements Listener {

    private final Logger logger;
    private final QuestService questService;

    // Entity types that count for combat quests
    private static final Set<EntityType> COMBAT_ENTITIES = Set.of(
            // Hostile mobs
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.ENDERMAN,
            EntityType.WITCH,
            EntityType.SLIME,
            EntityType.PHANTOM,
            EntityType.DROWNED,
            EntityType.HUSK,
            EntityType.STRAY,
            EntityType.CAVE_SPIDER,
            EntityType.SILVERFISH,
            EntityType.BLAZE,
            EntityType.GHAST,
            EntityType.MAGMA_CUBE,
            EntityType.WITHER_SKELETON,
            EntityType.PIGLIN,
            EntityType.HOGLIN,
            EntityType.ZOGLIN,
            // Neutral mobs
            EntityType.WOLF,
            EntityType.POLAR_BEAR,
            EntityType.LLAMA,
            EntityType.TRADER_LLAMA,
            EntityType.PANDA,
            EntityType.IRON_GOLEM,
            // Bosses
            EntityType.ENDER_DRAGON,
            EntityType.WITHER
    );

    public QuestEntityKillListener(TownsAndNations plugin) {
        this.logger = plugin.getLogger();
        this.questService = QuestService.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if killer is a player
        if (!(event.getEntity().getKiller() instanceof Player player)) {
            return;
        }

        // Check if entity type is relevant for combat quests
        EntityType entityType = event.getEntityType();
        if (!COMBAT_ENTITIES.contains(entityType)) {
            return;
        }

        // Only process if player is in a town
        PlayerDataStorage.getInstance().get(player)
                .thenAccept(tanPlayer -> {
                    String townId = tanPlayer.getTownId();
                    if (townId == null) {
                        return;
                    }

                    String entityName = entityType.name();

                    // Update combat quest progress
                    questService.updateProgress(
                            player,
                            QuestType.COMBAT,
                            entityName,
                            1
                    ).thenAccept(completedQuests -> {
                        if (!completedQuests.isEmpty()) {
                            logger.fine("Player " + player.getName() +
                                    " completed combat quests: " + completedQuests);
                        }
                    }).exceptionally(ex -> {
                        logger.warning("Failed to update quest progress: " + ex.getMessage());
                        return null;
                    });
                });
    }
}
