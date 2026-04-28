package org.leralix.tan.gui.admin;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.gui.IteratorGUI;
import org.leralix.tan.gui.user.player.PlayerMenu;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.gui.GuiUtil;
public class AdminPlayerMenu extends IteratorGUI {
  private AdminPlayerMenu(Player player, ITanPlayer tanPlayer) {
    super(player, tanPlayer, "Admin - Players List", 6);
  }
  public static void open(Player player) {
    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              org.leralix.tan.utils.FoliaScheduler.runTask(
                  org.leralix.tan.TownsAndNations.getPlugin(),
                  () -> {
                    // Entity validity guard — admin player may have disconnected during async load
                    if (!player.isOnline()) return;
                    new AdminPlayerMenu(player, tanPlayer).open();
                  }
              );
            });
  }
  @Override
  public void open() {
    org.leralix.tan.utils.gui.AsyncGuiHelper.loadAsync(
        player,
        this::loadAllPlayersGuiItems,
        guiItems -> {
          GuiUtil.createIterator(
              gui,
              guiItems,
              page,
              player,
              p -> AdminMainMenu.open(player),
              p -> nextPage(),
              p -> previousPage());
          gui.open(player);
        });
  }
  private List<GuiItem> loadAllPlayersGuiItems() {
    List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
    List<GuiItem> guiItems = new ArrayList<>();
    MiniMessage mm = MiniMessage.miniMessage();
    for (Player targetPlayer : onlinePlayers) {
      try {
        ITanPlayer tanPlayerData = PlayerDataStorage.getInstance().get(targetPlayer).join();
        String townInfo = "No Town";
        String regionInfo = "No Region";
        try {
          if (tanPlayerData.hasTown()) {
            townInfo = tanPlayerData.getTown().get().getColoredName();
          }
        } catch (Exception e) {
        }
        try {
          if (tanPlayerData.hasRegion()) {
            regionInfo = tanPlayerData.getRegion().get().getColoredName();
          }
        } catch (Exception e) {
        }
        guiItems.add(
            ItemBuilder.from(Material.PLAYER_HEAD)
                .name(mm.deserialize(targetPlayer.getName()))
                .lore(mm.deserialize("Town: " + townInfo), mm.deserialize("Region: " + regionInfo))
                .asGuiItem(
                    event -> {
                      event.setCancelled(true);
                      PlayerMenu.open(targetPlayer);
                    }));
      } catch (Exception e) {
      }
    }
    return guiItems;
  }
}