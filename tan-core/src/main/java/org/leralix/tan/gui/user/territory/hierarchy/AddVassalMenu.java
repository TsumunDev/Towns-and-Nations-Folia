package org.leralix.tan.gui.user.territory.hierarchy;

import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.gui.IteratorGUI;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.text.TanChatUtils;
import org.leralix.tan.utils.gui.AsyncGuiHelper;

public class AddVassalMenu extends IteratorGUI {

  private final TerritoryData overlordTerritory;
  private List<GuiItem> cachedTowns = new ArrayList<>();
  private boolean isLoaded = false;

  private AddVassalMenu(Player player, ITanPlayer tanPlayer, TerritoryData overlordTerritory) {
    super(player, tanPlayer, Lang.GUI_INVITE_TOWN_TO_REGION.get(tanPlayer), "add_vassal_menu", 6);
    this.overlordTerritory = overlordTerritory;
  }

  public static void open(Player player, TerritoryData overlordTerritory) {
    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              new AddVassalMenu(player, tanPlayer, overlordTerritory).open();
            });
  }

  @Override
  public void open() {
    iterator(cachedTowns, p -> VassalsMenu.open(player, overlordTerritory));
    gui.open(player);

    if (!isLoaded) {
      AsyncGuiHelper.loadAsync(
          player,
          this::getAvailableTowns,
          items -> {
            cachedTowns = items;
            isLoaded = true;
            iterator(items, p -> VassalsMenu.open(player, overlordTerritory));
            gui.update();
          });
    }
  }

  private List<GuiItem> getAvailableTowns() {
    List<GuiItem> items = new ArrayList<>();
    // ✅ FIX: Use getAllAsync().join() instead of getAllSync()
    // Safe because we're in AsyncGuiHelper.loadAsync() context
    List<TownData> allTowns = new ArrayList<>(TownDataStorage.getInstance().getAllAsync().join().values());

    for (TownData town : allTowns) {
      // Skip if already a vassal or is the overlord itself
      if (overlordTerritory.getID().equals(town.getID())
          || overlordTerritory.getVassals().contains(town)) {
        continue;
      }

      // Skip if town already has an overlord
      if (town.haveOverlord()) {
        continue;
      }

      // Skip if proposal already sent
      if (town.containsVassalisationProposal(overlordTerritory)) {
        continue;
      }

      ItemStack townIcon =
          town.getIconWithInformationAndRelation(overlordTerritory, tanPlayer.getLang());

      GuiItem townButton =
          iconManager
              .get(town.getIcon())
              .setName(town.getColoredName())
              .setDescription(
                  Lang.GUI_TOWN_INFO_DESC0.get(town.getDescription()),
                  Lang.GUI_TOWN_INFO_DESC1.get(town.getLeaderNameSync()),
                  Lang.GUI_TOWN_INFO_DESC2.get(Integer.toString(town.getPlayerIDList().size())),
                  Lang.GUI_TOWN_INFO_DESC3.get(Integer.toString(town.getNumberOfClaimedChunk())))
              .setClickToAcceptMessage(Lang.GUI_GENERIC_CLICK_TO_PROCEED)
              .setAction(
                  action -> {
                    town.addVassalisationProposal(overlordTerritory);
                    TanChatUtils.message(
                        player, Lang.VASSALISATION_PROPOSAL_SENT_SUCCESS.get(tanPlayer));
                    open();
                  })
              .asGuiItem(player, langType);

      items.add(townButton);
    }

    return items;
  }
}
