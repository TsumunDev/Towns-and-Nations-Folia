package org.leralix.tan.listeners.quest;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.domain.quest.model.QuestType;
import org.leralix.tan.service.quest.QuestService;
import org.leralix.tan.storage.stored.PlayerDataStorage;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Listens for block break events and updates quest progress.
 * <p>
 * This listener handles:
 * <ul>
 *   <li>FARM quests (breaking crops)</li>
 *   <li>MINING quests (breaking ores)</li>
 * </ul>
 * </p>
 */
public class QuestBlockBreakListener implements Listener {

    private final Logger logger;
    private final QuestService questService;

    // Materials that count as farming
    private static final Set<Material> FARM_MATERIALS = Set.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.SUGAR_CANE,
            Material.PUMPKIN,
            Material.MELON,
            Material.COCOA_BEANS,
            Material.SWEET_BERRIES,
            Material.BAMBOO
    );

    // Materials that count as mining
    private static final Set<Material> MINING_MATERIALS = Set.of(
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS
    );

    public QuestBlockBreakListener(TownsAndNations plugin) {
        this.logger = plugin.getLogger();
        this.questService = QuestService.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Only process if player is in a town
        PlayerDataStorage.getInstance().get(event.getPlayer())
                .thenAccept(tanPlayer -> {
                    String townId = tanPlayer.getTownId();
                    if (townId == null) {
                        return;
                    }

                    Material blockType = event.getBlock().getType();
                    String materialName = blockType.name();

                    // Determine quest type and process
                    QuestType questType = null;

                    if (FARM_MATERIALS.contains(blockType)) {
                        questType = QuestType.FARM;
                    } else if (MINING_MATERIALS.contains(blockType)) {
                        questType = QuestType.MINING;
                    }

                    if (questType != null) {
                        // Update quest progress asynchronously
                        questService.updateProgress(
                                event.getPlayer(),
                                questType,
                                materialName,
                                1
                        ).thenAccept(completedQuests -> {
                            if (!completedQuests.isEmpty()) {
                                logger.fine("Player " + event.getPlayer().getName() +
                                        " completed quests: " + completedQuests);
                            }
                        }).exceptionally(ex -> {
                            logger.warning("Failed to update quest progress: " + ex.getMessage());
                            return null;
                        });
                    }
                });
    }
}
