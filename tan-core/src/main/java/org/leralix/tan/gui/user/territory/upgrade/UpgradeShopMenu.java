package org.leralix.tan.gui.user.territory.upgrade;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.domain.prestige.model.PrestigePoints;
import org.leralix.tan.domain.upgrade.model.TownUpgrade;
import org.leralix.tan.gui.user.territory.TownMenu;
import org.leralix.tan.service.upgrade.UpgradeService;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.text.TanChatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for purchasing town upgrades with prestige points.
 * <p>
 * This menu shows:
 * <ul>
 *   <li>Available upgrades</li>
 *   <li>Purchased upgrades</li>
 *   <li>Upgrade costs and requirements</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
public class UpgradeShopMenu {

    private final Player player;
    private final ITanPlayer tanPlayer;
    private final TownData townData;
    private final Gui gui;
    private final UpgradeService upgradeService;
    private final org.leralix.tan.lang.LangType langType;

    private UpgradeShopMenu(Player player, ITanPlayer tanPlayer, TownData townData) {
        this.player = player;
        this.tanPlayer = tanPlayer;
        this.townData = townData;
        this.upgradeService = UpgradeService.getInstance();
        this.langType = tanPlayer.getLang();

        this.gui = Gui.gui()
                .title(Component.text("Town Upgrades", NamedTextColor.DARK_PURPLE))
                .rows(6)
                .create();
    }

    /**
     * Opens the upgrade shop asynchronously.
     *
     * @param player the player to open for
     * @param townData the town to show upgrades for
     */
    public static void open(Player player, TownData townData) {
        PlayerDataStorage.getInstance()
                .get(player)
                .thenAccept(tanPlayer -> {
                    FoliaScheduler.runTask(
                            TownsAndNations.getPlugin(),
                            () -> new UpgradeShopMenu(player, tanPlayer, townData).open()
                    );
                });
    }

    /**
     * Opens and displays the GUI.
     */
    public void open() {
        PrestigePoints prestige = townData.getPrestigePoints();

        // Load upgrades async, then display
        upgradeService.getAvailableUpgrades(townData.getID())
                .thenAccept(availableUpgrades -> {
                    org.leralix.tan.utils.FoliaScheduler.runTask(
                            org.leralix.tan.TownsAndNations.getPlugin(),
                            () -> {
                                // Balance display
                                gui.setItem(4, getBalanceDisplay(prestige));

                                // Upgrade items
                                int slot = 10;
                                for (TownUpgrade upgrade : availableUpgrades) {
                                    if (slot >= 44) break;

                                    gui.setItem(slot, createUpgradeItem(upgrade, prestige));
                                    slot++;
                                }

                                // Info button
                                gui.setItem(49, getInfoButton());

                                // Back button
                                gui.setItem(53, getBackButton());

                                // Fill empty slots
                                fillBackground();

                                gui.open(player);
                            }
                    );
                });
    }

    /**
     * Creates the balance display item.
     */
    private GuiItem getBalanceDisplay(PrestigePoints prestige) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§6§lYour Prestige: §e" + prestige.currentBalance()));
            meta.setLore(List.of(
                    "§f",
                    "§7Spend prestige on powerful",
                    "§7town upgrades!"
            ));
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item).asGuiItem(event -> event.setCancelled(true));
    }

    /**
     * Creates an upgrade item.
     */
    private GuiItem createUpgradeItem(TownUpgrade upgrade, PrestigePoints prestige) {
        boolean canAfford = prestige.canAfford(upgrade.cost());
        boolean canPurchase = upgrade.canPurchase(townData);
        boolean purchased = townData.hasPurchasedUpgrade(upgrade.id());

        ItemStack item = new ItemStack(upgrade.icon());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Name
            if (purchased) {
                meta.displayName(Component.text("§a✓ " + upgrade.name()));
            } else if (canPurchase && canAfford) {
                meta.displayName(Component.text("§e" + upgrade.name()));
            } else {
                meta.displayName(Component.text("§c" + upgrade.name()));
            }

            // Lore
            List<String> lore = new ArrayList<>();
            lore.add("§f");
            lore.add("§7" + upgrade.description());
            lore.add("§f");

            // Cost
            if (purchased) {
                lore.add("§a§lPURCHASED");
            } else {
                lore.add("§7Cost: §e" + upgrade.cost() + " Prestige");
                lore.add("§f");

                // Requirements
                if (!upgrade.requirements().isEmpty()) {
                    lore.add("§7Requirements:");
                    for (var req : upgrade.requirements()) {
                        boolean satisfied = req.isSatisfied(townData);
                        lore.add((satisfied ? "§a✓ " : "§c✗ ") + req.getDescription(langType));
                    }
                    lore.add("§f");
                }

                // Status
                if (!canAfford) {
                    lore.add("§cNot enough prestige!");
                } else if (!canPurchase) {
                    lore.add("§cRequirements not met");
                } else {
                    lore.add("§aClick to purchase!");
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        // Glow effect if can purchase
        if (canPurchase && canAfford && !purchased) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
        }

        return ItemBuilder.from(item)
                .asGuiItem(event -> {
                    event.setCancelled(true);

                    if (purchased) {
                        TanChatUtils.message(player, "§cYou already own this upgrade!");
                        return;
                    }

                    if (!canAfford) {
                        TanChatUtils.message(player, "§cNot enough prestige points!");
                        return;
                    }

                    if (!canPurchase) {
                        TanChatUtils.message(player, "§cRequirements not met!");
                        return;
                    }

                    // Purchase the upgrade
                    upgradeService.purchaseUpgrade(player, townData.getID(), upgrade.id())
                            .thenAccept(result -> {
                                org.leralix.tan.utils.FoliaScheduler.runTask(
                                        org.leralix.tan.TownsAndNations.getPlugin(),
                                        () -> {
                                            if (result == org.leralix.tan.service.upgrade.UpgradeService.PurchaseResult.SUCCESS) {
                                                TanChatUtils.message(player, "§aSuccessfully purchased " + upgrade.name() + "!");
                                                open(player, townData); // Refresh the menu
                                            } else {
                                                TanChatUtils.message(player, "§cFailed to purchase upgrade: " + result);
                                            }
                                        }
                                );
                            });
                });
    }

    /**
     * Creates the info button.
     */
    private GuiItem getInfoButton() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§eAbout Upgrades"));
            meta.setLore(List.of(
                    "§f",
                    "§7Upgrades provide permanent",
                    "§7bonuses to your town.",
                    "§f",
                    "§7Earn prestige from quests",
                    "§7to purchase upgrades!"
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
