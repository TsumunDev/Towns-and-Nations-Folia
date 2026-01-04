package org.leralix.tan.gui.admin;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.gui.IteratorGUI;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.utils.deprecated.GuiUtil;
import org.leralix.tan.utils.gui.AsyncGuiHelper;

public class AdminBrowseRegionMenu extends IteratorGUI {

  private List<GuiItem> cachedRegions = new ArrayList<>();
  private boolean isLoaded = false;

  private AdminBrowseRegionMenu(Player player, ITanPlayer tanPlayer) {
    super(player, tanPlayer, "Admin - Regions List", "admin_browse_regions_menu", 6);
  }

  public static void open(Player player) {
    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              new AdminBrowseRegionMenu(player, tanPlayer).open();
            });
  }

  @Override
  public void open() {
    GuiUtil.createIterator(
        gui,
        cachedRegions,
        page,
        player,
        p -> AdminMainMenu.open(player),
        p -> nextPage(),
        p -> previousPage());

    gui.open(player);

    if (!isLoaded) {
      AsyncGuiHelper.loadAsync(
          player,
          this::getRegions,
          items -> {
            cachedRegions = items;
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

  private List<GuiItem> getRegions() {
    // ✅ FIX: Use getAllAsync().join() instead of getAllSync()
    // Safe because we're in AsyncGuiHelper.loadAsync() context
    List<RegionData> regionList =
        new ArrayList<>(RegionDataStorage.getInstance().getAllAsync().join().values());

    ArrayList<GuiItem> regionGuiItems = new ArrayList<>();

    for (RegionData regionData : regionList) {
      ItemStack regionIcon =
          regionData.getIconWithInformationAndRelation(null, tanPlayer.getLang());
      GuiItem regionGUI =
          ItemBuilder.from(regionIcon)
              .asGuiItem(
                  event -> {
                    regionData.openMainMenu(player);
                  });

      regionGuiItems.add(regionGUI);
    }
    return regionGuiItems;
  }
}
