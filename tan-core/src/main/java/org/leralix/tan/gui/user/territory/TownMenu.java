package org.leralix.tan.gui.user.territory;

import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.gui.cosmetic.IconKey;
import org.leralix.tan.gui.cosmetic.IconManager;
import org.leralix.tan.gui.cosmetic.LayoutManager;
import org.leralix.tan.gui.user.MainMenu;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.deprecated.GuiUtil;
import org.leralix.tan.utils.text.TanChatUtils;

public class TownMenu extends TerritoryMenu {

  private final TownData townData;

  private TownMenu(Player player, ITanPlayer tanPlayer, TownData townData) {
    super(
        player,
        tanPlayer,
        Lang.HEADER_TOWN_MENU.get(tanPlayer.getLang(), townData.getName()),
        "town_menu",
        townData);
    this.townData = townData;
  }

  public static void open(Player player, TownData townData) {
    PlayerDataStorage.getInstance()
        .get(player)
        .thenCompose(
            tanPlayer -> {
              // Preload leader name asynchronously to avoid blocking in getTerritoryInfo()
              return townData.getLeaderName()
                  .thenApply(leaderName -> tanPlayer); // Ignore result, just trigger cache
            })
        .thenAccept(
            tanPlayer -> {
              new TownMenu(player, tanPlayer, townData).open();
            });
  }

  @Override
  public void open() {
    LayoutManager layout = LayoutManager.getInstance();
    
    gui.setItem(layout.getSlotOrDefault("town_menu", "territory_info", 4), getTerritoryInfo());
    gui.getFiller().fillTop(GuiUtil.getUnnamedItem(
        layout.getFillerOrDefault("town_menu", Material.BLUE_STAINED_GLASS_PANE)));

    gui.setItem(layout.getSlotOrDefault("town_menu", "treasury", 10), getTownTreasuryButton());
    gui.setItem(layout.getSlotOrDefault("town_menu", "members", 11), getMemberButton());
    gui.setItem(layout.getSlotOrDefault("town_menu", "land", 12), getLandButton());
    gui.setItem(layout.getSlotOrDefault("town_menu", "browse", 13), getBrowseButton());
    gui.setItem(layout.getSlotOrDefault("town_menu", "diplomacy", 14), getDiplomacyButton());
    gui.setItem(layout.getSlotOrDefault("town_menu", "level", 15), getLevelButton());
    gui.setItem(layout.getSlotOrDefault("town_menu", "settings", 16), getSettingsButton());

    gui.setItem(layout.getSlotOrDefault("town_menu", "building", 19), getBuildingButton());
    gui.setItem(layout.getSlotOrDefault("town_menu", "attack", 20), getAttackButton());
    gui.setItem(layout.getSlotOrDefault("town_menu", "hierarchy", 21), getHierarchyButton());

    gui.setItem(layout.getSlotOrDefault("town_menu", "landmarks", 25), getLandmarksButton());

    gui.setItem(layout.getSlotOrDefault("town_menu", "back", 27), 
        GuiUtil.createBackArrow(player, MainMenu::open));

    gui.open(player);
  }

  private GuiItem getSettingsButton() {
    return IconManager.getInstance()
        .get(IconKey.TERRITORY_SETTINGS_ICON)
        .setName(Lang.GUI_TOWN_SETTINGS_ICON.get(tanPlayer.getLang()))
        .setDescription(Lang.GUI_TOWN_SETTINGS_ICON_DESC1.get())
        .setAction(event -> TownSettingsMenu.open(player, townData))
        .asGuiItem(player, langType);
  }

  private GuiItem getLandmarksButton() {
    return IconManager.getInstance()
        .get(IconKey.TOWN_LANDMARKS_ICON)
        .setName(Lang.ADMIN_GUI_LANDMARK_ICON.get(tanPlayer.getLang()))
        .setDescription(Lang.ADMIN_GUI_LANDMARK_DESC1.get())
        .setAction(
            event -> {
              // TODO: Implement owned landmark GUI after PlayerGUI migration
              // Original: PlayerGUI.openOwnedLandmark(player, townData, 0)
              TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(tanPlayer.getLang()));
            })
        .asGuiItem(player, langType);
  }
}
