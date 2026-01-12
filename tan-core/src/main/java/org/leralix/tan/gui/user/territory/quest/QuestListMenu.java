package org.leralix.tan.gui.user.territory.quest;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.domain.quest.model.Quest;
import org.leralix.tan.domain.quest.model.QuestType;
import org.leralix.tan.gui.user.territory.TownMenu;
import org.leralix.tan.service.quest.QuestService;
import org.leralix.tan.storage.stored.PlayerDataStorage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * GUI displaying available quests for a player's town.
 */
public class QuestListMenu {

    private static final Logger LOGGER = TownsAndNations.getPlugin().getLogger();

    public static void open(Player player, String townId) {
        // Load data async, then open GUI on main thread
        PlayerDataStorage.getInstance().get(player)
                .thenCompose(tanPlayer -> {
                    return QuestService.getInstance().getAvailableQuests(townId)
                            .thenAccept(quests -> {
                                // Open GUI on main thread
                                org.leralix.tan.utils.FoliaScheduler.runTask(
                                        org.leralix.tan.TownsAndNations.getPlugin(),
                                        () -> {
                                            new QuestListMenu(player, tanPlayer, townId, quests).openGui();
                                        }
                                );
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.severe("Failed to open quest menu: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    public static void open(Player player, org.leralix.tan.dataclass.territory.TownData townData) {
        open(player, townData.getID());
    }

    private final Player player;
    private final ITanPlayer tanPlayer;
    private final String townId;
    private final List<Quest> quests;
    private final Gui gui;

    private QuestListMenu(Player player, ITanPlayer tanPlayer, String townId, List<Quest> quests) {
        this.player = player;
        this.tanPlayer = tanPlayer;
        this.townId = townId;
        this.quests = quests;

        this.gui = Gui.gui()
                .title(Component.text("Town Quests", NamedTextColor.GOLD))
                .rows(6)
                .create();
    }

    public void openGui() {
        // Clear existing items
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        gui.getFiller().fill(dev.triumphteam.gui.builder.item.ItemBuilder.from(filler).asGuiItem(event -> event.setCancelled(true)));

        // Add quest items
        int slot = 10;
        for (Quest quest : quests) {
            if (slot >= 45) break; // Don't overflow the GUI

            gui.setItem(slot, createQuestItem(quest));
            slot++;
        }

        // Add back button
        gui.setItem(45, createBackButton());

        gui.open(player);
    }

    private GuiItem createQuestItem(Quest quest) {
        Material icon = getIconForQuestType(quest.getType());
        ItemStack item = new ItemStack(icon);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Use legacy color codes (Folia doesn't support Adventure Components in global tasks)
            meta.setDisplayName("§e" + quest.getName());

            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + quest.getDescription());
            lore.add("");
            lore.add("§fType: " + quest.getType().getDisplayName());
            lore.add("§fRequired Tier: " + quest.getRequiredTier().getName());
            lore.add("");
            lore.add("§aRewards:");

            quest.getRewards().forEach(reward -> {
                lore.add("§7• " + reward.getDescription());
            });

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        // Create GuiItem with click handler
        return dev.triumphteam.gui.builder.item.ItemBuilder.from(item)
                .asGuiItem(event -> {
                    event.setCancelled(true);

                    // Accept the quest
                    QuestService.getInstance().assignQuest(player, quest.getId());
                });
    }

    private GuiItem createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§c← Back");
            item.setItemMeta(meta);
        }

        return dev.triumphteam.gui.builder.item.ItemBuilder.from(item)
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                });
    }

    private Material getIconForQuestType(QuestType type) {
        return switch (type) {
            case FARM -> Material.WHEAT;
            case MINING -> Material.IRON_PICKAXE;
            case COMBAT -> Material.DIAMOND_SWORD;
            case BUILD -> Material.OAK_PLANKS;
            case ACTIVITY -> Material.CLOCK;
        };
    }
}
