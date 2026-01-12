package org.leralix.tan.gui.user.territory.prestige;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.progression.TownProgressionComponent;
import org.leralix.tan.domain.prestige.model.PrestigePoints;
import org.leralix.tan.gui.user.territory.TownMenu;
import org.leralix.tan.service.prestige.PrestigeService;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.text.TanChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI showing prestige balance and information.
 * <p>
 * This menu displays:
 * <ul>
 *   <li>Current prestige balance</li>
 *   <li>Total prestige earned</li>
 *   <li>Total prestige spent</li>
 *   <li>Transaction history</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
public class PrestigeShopMenu {

    private final Player player;
    private final ITanPlayer tanPlayer;
    private final TownData townData;
    private final Gui gui;

    private PrestigeShopMenu(Player player, ITanPlayer tanPlayer, TownData townData) {
        this.player = player;
        this.tanPlayer = tanPlayer;
        this.townData = townData;

        this.gui = Gui.gui()
                .title(Component.text("Prestige Points", NamedTextColor.GOLD))
                .rows(4)
                .create();
    }

    /**
     * Opens the prestige menu asynchronously.
     *
     * @param player the player to open for
     * @param townData the town to show prestige for
     */
    public static void open(Player player, TownData townData) {
        PlayerDataStorage.getInstance()
                .get(player)
                .thenAccept(tanPlayer -> {
                    org.leralix.tan.utils.FoliaScheduler.runTask(
                            org.leralix.tan.TownsAndNations.getPlugin(),
                            () -> new PrestigeShopMenu(player, tanPlayer, townData).open()
                    );
                });
    }

    /**
     * Opens and displays the GUI.
     */
    public void open() {
        PrestigePoints prestige = townData.getPrestigePoints();

        // Balance display
        gui.setItem(13, getBalanceDisplay(prestige));

        // Stats
        gui.setItem(20, getStatsDisplay(prestige));

        // Info
        gui.setItem(31, getInfoButton());

        // Back button
        gui.setItem(36, getBackButton());

        // Fill empty slots
        fillBackground();

        gui.open(player);
    }

    /**
     * Creates the balance display item.
     */
    private GuiItem getBalanceDisplay(PrestigePoints prestige) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§6§lPrestige Balance"));
            meta.setLore(List.of(
                    "§f",
                    "§e" + prestige.currentBalance() + " §7points",
                    "§f",
                    "§7Earn prestige by completing",
                    "§7quests and achievements!"
            ));
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item).asGuiItem(event -> event.setCancelled(true));
    }

    /**
     * Creates the stats display item.
     */
    private GuiItem getStatsDisplay(PrestigePoints prestige) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§b§lStatistics"));
            List<String> lore = new ArrayList<>();
            lore.add("§f");
            lore.add("§7Total Earned: §a" + prestige.totalEarned());
            lore.add("§7Total Spent: §c" + prestige.totalSpent());
            lore.add("§7Transactions: §e" + prestige.transactionHistory().size());
            lore.add("§f");
            lore.add("§7Conversion Rate:");
            lore.add("§7100 XP = §e1 Prestige");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item).asGuiItem(event -> event.setCancelled(true));
    }

    /**
     * Creates the info button.
     */
    private GuiItem getInfoButton() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§eHow to earn Prestige"));
            meta.setLore(List.of(
                    "§f",
                    "§7• Complete town quests",
                    "§7• Level up your town",
                    "§7• Ascend to higher tiers",
                    "§7• Achieve milestones",
                    "§f",
                    "§aSpend prestige on upgrades"
            ));
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item).asGuiItem(event -> event.setCancelled(true));
    }

    /**
     * Creates the back button.
     */
    private GuiItem getBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§c← Back"));
            meta.setLore(List.of("§7Return to town menu"));
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item)
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    TownMenu.open(player, townData);
                });
    }

    /**
     * Fills empty slots with background glass.
     */
    private void fillBackground() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            filler.setItemMeta(meta);
        }

        gui.getFiller().fill(ItemBuilder.from(filler).asGuiItem(event -> event.setCancelled(true)));
    }
}
