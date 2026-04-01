package org.leralix.tan.gui.user.territory.progression;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.progression.TownProgressionComponent;
import org.leralix.tan.dataclass.territory.progression.TownTier;
import org.leralix.tan.dataclass.territory.progression.TownTierConfig;
import org.leralix.tan.dataclass.territory.progression.TownProgressionService;
import org.leralix.tan.gui.user.territory.TownMenu;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.text.TanChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI displaying town progression through civilization tiers.
 * <p>
 * This menu shows:
 * <ul>
 *   <li>Current tier and level</li>
 *   <li>XP progress bar</li>
 *   <li>Tier progression path</li>
 *   <li>Ascension requirements</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
public class ProgressionMenu {

    private final Player player;
    private final ITanPlayer tanPlayer;
    private final TownData townData;
    private final LangType langType;
    private final Gui gui;
    private final TownProgressionService progressionService;

    private ProgressionMenu(Player player, ITanPlayer tanPlayer, TownData townData) {
        this.player = player;
        this.tanPlayer = tanPlayer;
        this.townData = townData;
        this.langType = tanPlayer.getLang();
        this.progressionService = TownProgressionService.getInstance();

        this.gui = Gui.gui()
                .title(Component.text("Town Progression"))
                .rows(5)
                .create();
    }

    /**
     * Opens the progression menu asynchronously.
     *
     * @param player the player to open for
     * @param townData the town to show progression for
     */
    public static void open(Player player, TownData townData) {
        PlayerDataStorage.getInstance()
                .get(player)
                .thenAccept(tanPlayer -> {
                    new ProgressionMenu(player, tanPlayer, townData).open();
                });
    }

    /**
     * Opens and displays the GUI.
     */
    public void open() {
        TownProgressionComponent progression = townData.getProgression();
        TownTier currentTier = progression.getCurrentTier();
        int currentLevel = progression.getCurrentLevel();
        long currentXp = progression.getCurrentXp();

        TownTierConfig config = progressionService.getConfig(currentTier);

        // Title with tier info
        gui.setItem(4, getTierDisplay(currentTier));

        // XP Progress Bar
        gui.setItem(13, getXpProgressBar(progression, config));

        // Level Info
        gui.setItem(22, getLevelInfo(progression, config));

        // Tier Path (show all tiers)
        fillTierPath(currentTier);

        // Ascension Button (if available)
        if (progressionService.canAscend(townData)) {
            gui.setItem(40, getAscensionButton());
        } else {
            gui.setItem(40, getAscensionLockedButton(progression, config));
        }

        // Back button
        gui.setItem(36, getBackButton());

        // Fill empty slots
        fillBackground();

        gui.open(player);
    }

    /**
     * Creates the main tier display item.
     */
    private GuiItem getTierDisplay(TownTier tier) {
        ItemStack item = tier.getIcon();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(tier.getColoredName());

            List<String> lore = new ArrayList<>();
            lore.add("§7Current civilization tier");
            lore.add("");
            lore.add("§aNext Tier: §7" + (tier.next() != null ? tier.next().getName() : "Maximum"));
            lore.add("§cPrevious Tier: §7" + (tier.previous() != null ? tier.previous().getName() : "None"));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item).asGuiItem(event -> event.setCancelled(true));
    }

    /**
     * Creates the XP progress bar.
     */
    private GuiItem getXpProgressBar(TownProgressionComponent progression, TownTierConfig config) {
        int currentLevel = progression.getCurrentLevel();
        long currentXp = progression.getCurrentXp();
        long xpToNext = progression.getXpToNextLevel(config);

        Material barMaterial = switch ((int) (progression.getLevelProgress(config) * 5)) {
            case 0 -> Material.RED_STAINED_GLASS_PANE;
            case 1 -> Material.ORANGE_STAINED_GLASS_PANE;
            case 2 -> Material.YELLOW_STAINED_GLASS_PANE;
            case 3 -> Material.LIME_STAINED_GLASS_PANE;
            case 4 -> Material.GREEN_STAINED_GLASS_PANE;
            default -> Material.GREEN_STAINED_GLASS_PANE;
        };

        ItemStack item = new ItemStack(barMaterial);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§eXP Progress"));

            List<String> lore = new ArrayList<>();
            lore.add("§7Level: §a" + currentLevel);
            lore.add("§7Current XP: §b" + currentXp);

            if (xpToNext > 0) {
                double progress = progression.getLevelProgress(config) * 100;
                lore.add("§7XP to next level: §e" + xpToNext);
                lore.add("");
                lore.add("§6Progress: §a" + String.format("%.1f%%", progress));
                lore.add(createProgressBar(progression.getLevelProgress(config)));
            } else {
                lore.add("§6Maximum level reached!");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item).asGuiItem(event -> event.setCancelled(true));
    }

    /**
     * Creates a visual progress bar.
     */
    private String createProgressBar(double progress) {
        int totalBars = 20;
        int filledBars = (int) (progress * totalBars);

        StringBuilder bar = new StringBuilder("§a[");
        for (int i = 0; i < filledBars; i++) {
            bar.append("█");
        }
        bar.append("§7");
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("░");
        }
        bar.append("§a]");

        return bar.toString();
    }

    /**
     * Creates the level info item.
     */
    private GuiItem getLevelInfo(TownProgressionComponent progression, TownTierConfig config) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§bLevel Information"));

            List<String> lore = new ArrayList<>();
            lore.add("§7Current Level: §a" + progression.getCurrentLevel());
            lore.add("§7Max Level (this tier): §e" + config.getMaxLevel());
            lore.add("");
            lore.add("§7Required Level to Ascend: §6" + config.getRequiredLevel());

            if (progression.getCurrentLevel() >= config.getRequiredLevel()) {
                lore.add("");
                lore.add("§a✓ You can ascend to the next tier!");
            } else {
                int levelsNeeded = config.getRequiredLevel() - progression.getCurrentLevel();
                lore.add("");
                lore.add("§c✗ Need " + levelsNeeded + " more level(s) to ascend");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item).asGuiItem(event -> event.setCancelled(true));
    }

    /**
     * Fills the tier path showing all tiers.
     */
    private void fillTierPath(TownTier currentTier) {
        TownTier[] tiers = TownTier.values();
        int startSlot = 10;

        for (int i = 0; i < tiers.length; i++) {
            TownTier tier = tiers[i];
            int slot = startSlot + (i * 2);

            if (slot < 18) {
                gui.setItem(slot, getTierPathItem(tier, tier == currentTier, currentTier));
            }
        }
    }

    /**
     * Creates a tier path item.
     */
    private GuiItem getTierPathItem(TownTier tier, boolean isCurrent, TownTier currentTier) {
        ItemStack item = tier.getIcon();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (isCurrent) {
                meta.displayName(Component.text("§a✓ ") .append(tier.getColoredName()));
                // Add glow effect by making it enchanted
                // PAPER 1.21+ UPGRADE: DURABILITY removed, use UNBREAKING instead
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            } else {
                meta.displayName(Component.text("§7○ ") .append(Component.text(tier.getName(), tier.getColor())));
            }

            List<String> lore = new ArrayList<>();
            lore.add("§7Tier Level: §e" + tier.getLevel());

            if (isCurrent) {
                lore.add("§aCurrent Tier");
            } else if (tier.getLevel() < currentTier.getLevel()) {
                lore.add("§7Completed");
            } else {
                lore.add("§cLocked");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item).asGuiItem(event -> event.setCancelled(true));
    }

    /**
     * Creates the ascension button.
     */
    private GuiItem getAscensionButton() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§6§lASCEND TO NEXT TIER"));

            List<String> lore = new ArrayList<>();
            lore.add("§eClick to ascend your town!");
            lore.add("");
            lore.add("§7This will:");
            lore.add("§a• Unlock new features");
            lore.add("§a• Increase player and claim caps");
            lore.add("§a• Move to the next civilization tier");
            lore.add("");
            lore.add("§c§lWarning: Your level will reset to 1");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return ItemBuilder.from(item)
                .asGuiItem(event -> {
                    event.setCancelled(true);

                    // Attempt ascension
                    boolean success = progressionService.ascend(townData);

                    if (success) {
                        TanChatUtils.message(player, "§aYour town has ascended to " +
                                townData.getTownTier().getName() + "!");
                        TownMenu.open(player, townData);
                    } else {
                        TanChatUtils.message(player, "§cFailed to ascend. Requirements not met.");
                    }
                });
    }

    /**
     * Creates the locked ascension button.
     */
    private GuiItem getAscensionLockedButton(TownProgressionComponent progression, TownTierConfig config) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§cAscension Locked"));

            List<String> lore = new ArrayList<>();
            lore.add("§7Requirements:");
            lore.add("§c✗ Reach level " + config.getRequiredLevel());
            lore.add("");
            lore.add("§7Current Level: §e" + progression.getCurrentLevel());

            int levelsNeeded = config.getRequiredLevel() - progression.getCurrentLevel();
            lore.add("§7Levels Needed: §c" + levelsNeeded);

            meta.setLore(lore);
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
