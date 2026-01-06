package org.leralix.tan.gui.user;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.leralix.lib.data.SoundEnum;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.gui.BasicGui;
import org.leralix.tan.gui.cosmetic.IconKey;
import org.leralix.tan.gui.cosmetic.LayoutManager;
import org.leralix.tan.gui.user.player.PlayerMenu;
import org.leralix.tan.gui.user.territory.NoRegionMenu;
import org.leralix.tan.gui.user.territory.NoTownMenu;
import org.leralix.tan.lang.FilledLang;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.timezone.TimeZoneManager;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.utils.deprecated.GuiUtil;
import org.leralix.tan.utils.text.TanChatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class MainMenu extends BasicGui {
  private static final Logger logger = LoggerFactory.getLogger(MainMenu.class);
  @Nullable private final TownData townData;
  @Nullable private final RegionData regionData;
  private MainMenu(
      Player player,
      ITanPlayer tanPlayer,
      @Nullable TownData townData,
      @Nullable RegionData regionData) {
    super(player, tanPlayer, Lang.HEADER_MAIN_MENU.get(tanPlayer.getLang()), "main_menu", 3);
    this.townData = townData;
    this.regionData = regionData;
  }
  public static void open(Player player) {
    logger.info("[MainMenu] Starting async load for player: {}", player.getName());
    PlayerDataStorage.getInstance()
        .get(player)
        .thenCompose(
            tanPlayer -> {
              logger.info("[MainMenu] Player data loaded for: {}", player.getName());
              CompletableFuture<TownData> townFuture =
                  tanPlayer.hasTown()
                      ? tanPlayer.getTown()
                      : CompletableFuture.completedFuture(null);
              CompletableFuture<RegionData> regionFuture =
                  tanPlayer.hasRegion()
                      ? tanPlayer.getRegion()
                      : CompletableFuture.completedFuture(null);
              return CompletableFuture.allOf(townFuture, regionFuture)
                  .thenApply(v -> {
                    logger.info("[MainMenu] Town/Region data loaded for: {}", player.getName());
                    return new Object[] {tanPlayer, townFuture.join(), regionFuture.join()};
                  });
            })
        .thenAccept(
            data -> {
              ITanPlayer tanPlayer = (ITanPlayer) ((Object[]) data)[0];
              TownData townData = (TownData) ((Object[]) data)[1];
              RegionData regionData = (RegionData) ((Object[]) data)[2];
              logger.info("[MainMenu] Scheduling GUI open on player's thread for: {}", player.getName());
              FoliaScheduler.runEntityTask(
                  TownsAndNations.getPlugin(),
                  player,
                  () -> {
                    try {
                      logger.info("[MainMenu] Opening GUI NOW for: {}", player.getName());
                      new MainMenu(player, tanPlayer, townData, regionData).open();
                      logger.info("[MainMenu] GUI opened successfully for: {}", player.getName());
                    } catch (Exception e) {
                      logger.error("[MainMenu] FAILED to open GUI for: {}", player.getName(), e);
                      TanChatUtils.message(player, "§cFailed to open menu. Check console for errors.");
                    }
                  });
            })
        .exceptionally(e -> {
          logger.error("[MainMenu] Exception during async load for: {}", player.getName(), e);
          TanChatUtils.message(player, "§cFailed to load menu data. Check console for errors.");
          return null;
        });
  }
  @Override
  public void open() {
    logger.info("[MainMenu] Building GUI for player: {}", player.getName());
    LayoutManager layout = LayoutManager.getInstance();
    logger.info("[MainMenu] LayoutManager loaded");
    try {
      gui.setItem(layout.getSlotOrDefault("main_menu", "time_icon", 4), getTimeIcon());
      logger.info("[MainMenu] Time icon set");
    } catch (Exception e) {
      logger.error("[MainMenu] Failed to set time icon", e);
    }
    int nationPosition = layout.getSlotOrDefault("main_menu", "nation", 10);
    int regionPosition = layout.getSlotOrDefault("main_menu", "region", 12);
    int townPosition = layout.getSlotOrDefault("main_menu", "town", 14);
    int playerPosition = layout.getSlotOrDefault("main_menu", "player", 16);
    if (Constants.enableRegion()) {
      if (Constants.enableNation()) {
        gui.setItem(nationPosition, getNationButton(tanPlayer));
        logger.info("[MainMenu] Nation button set at slot {}", nationPosition);
      }
      gui.setItem(regionPosition, getRegionButton(tanPlayer));
      logger.info("[MainMenu] Region button set at slot {}", regionPosition);
    }
    gui.setItem(townPosition, getTownButton(tanPlayer));
    logger.info("[MainMenu] Town button set at slot {}", townPosition);
    gui.setItem(playerPosition, getPlayerButton(tanPlayer));
    logger.info("[MainMenu] Player button set at slot {}", playerPosition);
    gui.setItem(layout.getSlotOrDefault("main_menu", "back", 18),
        GuiUtil.createBackArrow(player, HumanEntity::closeInventory));
    logger.info("[MainMenu] Back button set");
    logger.info("[MainMenu] About to call gui.open() for player: {}", player.getName());
    gui.open(player);
    logger.info("[MainMenu] gui.open() completed for player: {}", player.getName());
  }
  private @NotNull GuiItem getTimeIcon() {
    TimeZoneManager timeManager = TimeZoneManager.getInstance();
    IconKey icon =
        timeManager.isDayForServer() ? IconKey.TIMEZONE_ICON_DAY : IconKey.TIMEZONE_ICON_NIGHT;
    return iconManager
        .get(icon)
        .setName(Lang.GUI_SERVER_TIME.get(tanPlayer))
        .setDescription(
            Lang.CURRENT_SERVER_TIME.get(timeManager.formatDateNowForServer().get(langType)),
            Lang.CURRENT_PLAYER_TIME.get(
                timeManager.formatDateNowForPlayer(tanPlayer).get(langType)))
        .asGuiItem(player, langType);
  }
  private GuiItem getNationButton(ITanPlayer tanPlayer) {
    return iconManager
        .get(IconKey.NATION_BASE_ICON)
        .setName(Lang.GUI_KINGDOM_ICON.get(tanPlayer))
        .setDescription(Lang.GUI_WARNING_STILL_IN_DEV.get())
        .setAction(
            action ->
                TanChatUtils.message(
                    player, Lang.GUI_WARNING_STILL_IN_DEV.get(tanPlayer), SoundEnum.NOT_ALLOWED))
        .asGuiItem(player, langType);
  }
  private GuiItem getRegionButton(ITanPlayer tanPlayer) {
    List<FilledLang> description = new ArrayList<>();
    if (regionData != null) {
      description.add(Lang.GUI_REGION_ICON_DESC1_REGION.get(regionData.getColoredName()));
      description.add(
          Lang.GUI_REGION_ICON_DESC2_REGION.get(regionData.getRank(tanPlayer).getColoredName()));
    } else {
      description.add(Lang.GUI_REGION_ICON_DESC1_NO_REGION.get());
    }
    return iconManager
        .get(IconKey.REGION_BASE_ICON)
        .setName(Lang.GUI_REGION_ICON.get(tanPlayer))
        .setDescription(description)
        .setAction(
            action -> {
              if (regionData != null) {
                regionData.openMainMenu(player);
              } else {
                NoRegionMenu.open(player);
              }
            })
        .asGuiItem(player, langType);
  }
  private GuiItem getTownButton(ITanPlayer tanPlayer) {
    List<FilledLang> description = new ArrayList<>();
    if (townData != null) {
      description.add(Lang.GUI_TOWN_ICON_DESC1_HAVE_TOWN.get(townData.getColoredName()));
      description.add(
          Lang.GUI_TOWN_ICON_DESC2_HAVE_TOWN.get(townData.getRank(tanPlayer).getColoredName()));
    } else {
      description.add(Lang.GUI_TOWN_ICON_DESC1_NO_TOWN.get());
    }
    return iconManager
        .get(IconKey.TOWN_BASE_ICON)
        .setName(Lang.GUI_TOWN_ICON.get(tanPlayer))
        .setDescription(description)
        .setAction(
            action -> {
              if (townData != null) {
                townData.openMainMenu(player);
              } else {
                NoTownMenu.open(player);
              }
            })
        .asGuiItem(player, langType);
  }
  private GuiItem getPlayerButton(ITanPlayer tanPlayer) {
    return iconManager
        .get(IconKey.PLAYER_BASE_ICON)
        .setName(Lang.GUI_PLAYER_MENU_ICON.get(tanPlayer, player.getName()))
        .setAction(action -> PlayerMenu.open(player))
        .asGuiItem(player, langType);
  }
}