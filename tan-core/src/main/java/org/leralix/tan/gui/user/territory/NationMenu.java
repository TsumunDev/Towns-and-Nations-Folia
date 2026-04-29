package org.leralix.tan.gui.user.territory;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.NationData;
import org.leralix.tan.gui.cosmetic.IconKey;
import org.leralix.tan.gui.cosmetic.IconManager;
import org.leralix.tan.gui.user.MainMenu;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.gui.GuiUtil;
public class NationMenu extends TerritoryMenu {
  private final NationData nationData;
  private NationMenu(Player player, ITanPlayer tanPlayer, NationData nationData) {
    super(player, tanPlayer, Lang.HEADER_NATION_MENU.get(player, nationData.getName()), "nation_menu", nationData);
    this.nationData = nationData;
  }
  public static void open(Player player, NationData nationData) {
    PlayerDataStorage.getInstance()
        .get(player)
        .thenCompose(
            tanPlayer -> {
              return nationData.getLeaderName()
                  .thenApply(leaderName -> tanPlayer);
            })
        .thenAccept(
            tanPlayer -> {
              org.leralix.tan.utils.FoliaScheduler.runTask(
                  org.leralix.tan.TownsAndNations.getPlugin(),
                  () -> new NationMenu(player, tanPlayer, nationData).open()
              );
            });
  }
  @Override
  public void open() {
    gui.setItem(1, 5, getTerritoryInfo());
    gui.getFiller().fillTop(GuiUtil.getUnnamedItem(Material.YELLOW_STAINED_GLASS_PANE));
    gui.setItem(2, 2, getTownTreasuryButton());
    gui.setItem(2, 3, getMemberButton());
    gui.setItem(2, 4, getLandButton());
    gui.setItem(2, 5, getBrowseButton());
    gui.setItem(2, 6, getDiplomacyButton());
    gui.setItem(2, 7, getLevelButton());
    gui.setItem(2, 8, getSettingsButton());
    gui.setItem(3, 2, getBuildingButton());
    gui.setItem(3, 3, getAttackButton());
    gui.setItem(3, 4, getHierarchyButton());
    gui.setItem(4, 1, GuiUtil.createBackArrow(player, p -> MainMenu.open(player)));
    gui.open(player);
  }
  private GuiItem getSettingsButton() {
    return IconManager.getInstance()
        .get(IconKey.TERRITORY_SETTINGS_ICON)
        .setName(Lang.GUI_TOWN_SETTINGS_ICON.get(tanPlayer.getLang()))
        .setDescription(Lang.GUI_TOWN_SETTINGS_ICON_DESC1.get())
        .setAction(event -> NationSettingsMenu.open(player, nationData))
        .asGuiItem(player, langType);
  }
}
