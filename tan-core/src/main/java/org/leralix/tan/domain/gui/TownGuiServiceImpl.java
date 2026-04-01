package org.leralix.tan.domain.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.enums.RolePermission;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.item.HeadUtils;
import org.leralix.tan.utils.text.ComponentUtil;
import org.leralix.tan.utils.text.StringUtil;
import org.leralix.tan.utils.text.TanChatUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Implementation of TownGuiService.
 * Extracts GUI-related logic from TownData following the Strangler Fig Pattern.
 *
 * <p>This service is enabled via the feature flag {@code development.use-new-gui-service}.</p>
 *
 * @see TownGuiService
 * @since 2.0.0
 */
public class TownGuiServiceImpl implements TownGuiService {

    private static final Logger LOGGER = Logger.getLogger(TownGuiServiceImpl.class.getName());

    private final TownDataStorage townStorage;
    private final PlayerDataStorage playerStorage;

    /**
     * Creates a new TownGuiServiceImpl.
     *
     * @param townStorage The town data storage
     * @param playerStorage The player data storage
     */
    public TownGuiServiceImpl(TownDataStorage townStorage, PlayerDataStorage playerStorage) {
        this.townStorage = townStorage;
        this.playerStorage = playerStorage;
    }

    @Override
    public CompletableFuture<ItemStack> getIconWithName(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("TownGuiService: Town not found: " + townId);
                    return new ItemStack(Material.BARRIER);
                }
                return createIconWithName(town);
            });
    }

    @Override
    public CompletableFuture<ItemStack> getIconWithInformations(String townId, LangType langType) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("TownGuiService: Town not found: " + townId);
                    ItemStack errorIcon = new ItemStack(Material.BARRIER);
                    ItemMeta meta = errorIcon.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§cUnknown Town");
                        errorIcon.setItemMeta(meta);
                    }
                    return errorIcon;
                }
                return createIconWithInformations(town, langType);
            });
    }

    @Override
    public CompletableFuture<List<GuiItem>> getOrderedMemberList(String townId, ITanPlayer viewer) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warning("TownGuiService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }

                List<GuiItem> memberItems = new ArrayList<>();
                List<CompletableFuture<GuiItem>> futures = new ArrayList<>();

                for (String playerUuid : town.getOrderedPlayerIDListSync()) {
                    CompletableFuture<GuiItem> memberFuture = playerStorage.get(playerUuid)
                        .thenApply(playerData -> {
                            if (playerData != null) {
                                return createMemberGuiItem(playerData, viewer, town);
                            }
                            return null;
                        });
                    futures.add(memberFuture);
                }

                // Wait for all members and filter out nulls
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<GuiItem> result = new ArrayList<>();
                        for (CompletableFuture<GuiItem> future : futures) {
                            try {
                                GuiItem item = future.join();
                                if (item != null) {
                                    result.add(item);
                                }
                            } catch (Exception e) {
                                LOGGER.warning("TownGuiService: Error loading member: " + e.getMessage());
                            }
                        }
                        return result;
                    });
            });
    }

    @Override
    public boolean isEnabled() {
        return TownsAndNations.getPlugin()
            .getConfig()
            .getBoolean("development.use-new-gui-service", false);
    }

    /**
     * Creates an ItemStack icon with the town's name.
     *
     * @param town The town data
     * @return ItemStack with display name
     */
    private ItemStack createIconWithName(TownData town) {
        ItemStack itemStack = town.getIcon();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            ComponentUtil.setDisplayName(meta, "§a" + town.getName());
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * Creates an ItemStack icon with detailed town information.
     *
     * @param town The town data
     * @param langType The language type
     * @return ItemStack with display name and lore
     */
    private ItemStack createIconWithInformations(TownData town, LangType langType) {
        ItemStack icon = town.getIcon();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            ComponentUtil.setDisplayName(meta, "§a" + town.getName());

            List<String> lore = new ArrayList<>();
            lore.add(Lang.GUI_TOWN_INFO_DESC0.get(langType, town.getDescription()));
            lore.add(Lang.GUI_TOWN_INFO_DESC1.get(langType, town.getLeaderNameSync()));
            lore.add(Lang.GUI_TOWN_INFO_DESC2.get(langType, Integer.toString(town.getPlayerIDList().size())));
            lore.add(Lang.GUI_TOWN_INFO_DESC3.get(langType, Integer.toString(town.getNumberOfClaimedChunk())));

            town.getOverlord().ifPresentOrElse(
                overlord -> lore.add(Lang.GUI_TOWN_INFO_DESC5_REGION.get(langType, overlord.getName())),
                () -> lore.add(Lang.GUI_TOWN_INFO_DESC5_NO_REGION.get(langType))
            );

            ComponentUtil.setLore(meta, lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    /**
     * Creates a GUI item for a town member.
     *
     * @param playerData The player's data
     * @param viewer The player viewing the GUI
     * @param town The town data
     * @return GuiItem for the member
     */
    private GuiItem createMemberGuiItem(ITanPlayer playerData, ITanPlayer viewer, TownData town) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerData.getUUID());
        LangType langType = viewer.getLang();

        ItemStack playerHead = HeadUtils.getPlayerHead(
            offlinePlayer,
            Lang.GUI_TOWN_MEMBER_DESC1.get(langType, playerData.getTownRank().getColoredName()),
            Lang.GUI_TOWN_MEMBER_DESC2.get(langType, StringUtil.formatMoney(EconomyUtil.getBalance(offlinePlayer))),
            town.doesPlayerHavePermission(viewer, RolePermission.KICK_PLAYER)
                ? Lang.GUI_TOWN_MEMBER_DESC3.get(langType)
                : ""
        );

        return ItemBuilder.from(playerHead)
            .asGuiItem(event -> {
                event.setCancelled(true);
                handleMemberClick(event, playerData, viewer, town);
            });
    }

    /**
     * Handles click events on member items.
     *
     * @param event The click event
     * @param clickedPlayer The player whose item was clicked
     * @param viewer The player who clicked
     * @param town The town data
     */
    private void handleMemberClick(
        org.bukkit.event.inventory.InventoryClickEvent event,
        ITanPlayer clickedPlayer,
        ITanPlayer viewer,
        TownData town
    ) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) {
            return;
        }

        LangType langType = viewer.getLang();

        if (event.getClick().isRightClick()) {
            if (!town.doesPlayerHavePermission(viewer, RolePermission.KICK_PLAYER)) {
                TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(langType));
                return;
            }

            if (town.getRank(clickedPlayer).isSuperiorTo(town.getRank(viewer))) {
                TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION_RANK_DIFFERENCE.get(langType));
                return;
            }

            // Kick action - open confirmation or execute kick
            // For now, just log it
            LOGGER.info("TownGuiService: Player " + viewer.getID() + " wants to kick " + clickedPlayer.getID());
            // In a full implementation, this would open a confirmation GUI or execute the kick
        }
    }
}
