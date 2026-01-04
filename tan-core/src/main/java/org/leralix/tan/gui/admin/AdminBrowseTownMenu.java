package org.leralix.tan.gui.admin;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.gui.IteratorGUI;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.deprecated.GuiUtil;
import org.leralix.tan.utils.gui.AsyncGuiHelper;

public class AdminBrowseTownMenu extends IteratorGUI {

  private List<GuiItem> cachedTowns = new ArrayList<>();
  private boolean isLoaded = false;

  private AdminBrowseTownMenu(Player player, ITanPlayer tanPlayer) {
    super(player, tanPlayer, "Admin - Towns List", "admin_browse_towns_menu", 6);
  }

  public static void open(Player player) {
    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              new AdminBrowseTownMenu(player, tanPlayer).open();
            });
  }

  @Override
  public void open() {
    GuiUtil.createIterator(
        gui,
        cachedTowns,
        page,
        player,
        p -> AdminMainMenu.open(player),
        p -> nextPage(),
        p -> previousPage());

    gui.open(player);

    if (!isLoaded) {
      AsyncGuiHelper.loadAsync(
          player,
          this::getTowns,
          items -> {
            cachedTowns = items;
            isLoaded = true;
            GuiUtil.createIterator(
                gui,
                items,
                page,
                player,
                p -> AdminMainMenu.open(player),
                p -> nextPage(),
                p -> previousPage());
            gui.update();
          });
    }
  }

  private List<GuiItem> getTowns() {
    // ✅ FIX: Use getAllAsync().join() instead of getAllSync()
    // Safe because we're in AsyncGuiHelper.loadAsync() context
    List<TownData> townList = new ArrayList<>(TownDataStorage.getInstance().getAllAsync().join().values());

    ArrayList<GuiItem> townGuiItems = new ArrayList<>();

    for (TownData townData : townList) {
      ItemStack townIcon = townData.getIconWithInformationAndRelation(null, tanPlayer.getLang());
      GuiItem townGUI =
          ItemBuilder.from(townIcon)
              .asGuiItem(
                  event -> {
                    townData.openMainMenu(player);
                  });

      townGuiItems.add(townGUI);
    }
    return townGuiItems;
  }
}
