package org.leralix.tan.gui.user.territory;
import static org.leralix.lib.data.SoundEnum.GOOD;
import static org.leralix.lib.data.SoundEnum.NOT_ALLOWED;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.leralix.lib.utils.SoundUtil;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.NationData;
import org.leralix.tan.gui.cosmetic.IconKey;
import org.leralix.tan.gui.user.MainMenu;
import org.leralix.tan.gui.utils.ConfirmMenu;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.file.FileUtil;
import org.leralix.tan.utils.gui.GuiUtil;
import org.leralix.tan.utils.text.TanChatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class NationSettingsMenu extends SettingsMenus {
  private static final Logger LOGGER = LoggerFactory.getLogger(NationSettingsMenu.class);
  private final NationData nationData;
  public NationSettingsMenu(Player player, ITanPlayer tanPlayer, NationData nationData) {
    super(player, tanPlayer, Lang.HEADER_SETTINGS.get(player), nationData, 3);
    this.nationData = nationData;
  }
  public static void open(Player player, NationData nationData) {
    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              FoliaScheduler.runTask(
                  TownsAndNations.getPlugin(),
                  () -> new NationSettingsMenu(player, tanPlayer, nationData).open()
              );
            });
  }
  @Override
  public void open() {
    gui.setItem(1, 5, getTerritoryInfo());
    gui.getFiller().fillTop(GuiUtil.getUnnamedItem(Material.YELLOW_STAINED_GLASS_PANE));
    gui.setItem(2, 2, getRenameButton());
    gui.setItem(2, 3, getChangeDescriptionButton());
    gui.setItem(2, 4, getChangeColorButton());
    gui.setItem(2, 6, getChangeCapitalButton());
    gui.setItem(2, 7, getDeleteButton());
    gui.setItem(3, 1, GuiUtil.createBackArrow(player, p -> NationMenu.open(player, nationData)));
    gui.open(player);
  }
  private @NotNull GuiItem getChangeCapitalButton() {
    return iconManager
        .get(IconKey.REGION_CHANGE_OWNERSHIP_ICON)
        .setName(Lang.GUI_NATION_CHANGE_CAPITAL.get(tanPlayer))
        .setDescription(Lang.GUI_NATION_CHANGE_CAPITAL_DESC.get())
        .setAction(
            event -> {
              event.setCancelled(true);
              if (!nationData.isLeader(tanPlayer)) {
                TanChatUtils.message(player, Lang.GUI_NEED_TO_BE_LEADER_OF_REGION.get(tanPlayer));
                return;
              }
              TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(tanPlayer), NOT_ALLOWED);
            })
        .asGuiItem(player, langType);
  }
  private GuiItem getDeleteButton() {
    return iconManager
        .get(IconKey.REGION_DELETE_REGION_ICON)
        .setName(Lang.GUI_NATION_DELETE.get(tanPlayer))
        .setDescription(Lang.GUI_NATION_DELETE_DESC.get())
        .setAction(
            event -> {
              event.setCancelled(true);
              if (!nationData.isLeader(tanPlayer)) {
                TanChatUtils.message(player, Lang.GUI_NEED_TO_BE_LEADER_OF_REGION.get(tanPlayer));
                return;
              }
              if (!player.hasPermission("tan.base.nation.disband")) {
                TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(tanPlayer), NOT_ALLOWED);
                return;
              }
              ConfirmMenu.open(
                  player,
                  Lang.GUI_CONFIRM_DELETE_REGION.get(),
                  p -> {
                    FileUtil.addLineToHistory(
                        Lang.NATION_DELETED_NEWSLETTER.get(player.getName(), nationData.getName()));
                    nationData.delete().thenRun(() -> {
                      FoliaScheduler.runTaskAtLocation(TownsAndNations.getPlugin(), player.getLocation(), () -> {
                        SoundUtil.playSound(player, GOOD);
                        MainMenu.open(player);
                      });
                    }).exceptionally(ex -> {
                      LOGGER.error("Failed to delete nation {}", nationData.getID(), ex);
                      FoliaScheduler.runTaskAtLocation(TownsAndNations.getPlugin(), player.getLocation(), () -> {
                        player.sendMessage("§cAn error occurred while deleting the nation.");
                      });
                      return null;
                    });
                  },
                  p -> open());
            })
        .asGuiItem(player, langType);
  }
}
