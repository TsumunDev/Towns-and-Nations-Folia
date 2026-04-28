package org.leralix.tan.utils.graphic;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.constants.Constants;
public class PrefixUtil {
  private PrefixUtil() {
    throw new AssertionError("Utility class");
  }
  public static void updatePrefix(Player player) {
    if (!Constants.enableTownTag()) {
      return;
    }
    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              tanPlayer
                  .getTown()
                  .thenAccept(
                      town -> {
                        FoliaScheduler.runTaskAtLocation(
                            TownsAndNations.getPlugin(),
                            player.getLocation(),
                            () -> {
                              if (town != null) {
                                String prefix = town.getColoredTag() + " ";
                                player.setPlayerListName(prefix + player.getName());
                                player.setDisplayName(prefix + player.getName());
                              } else {
                                player.setPlayerListName(player.getName());
                                player.setDisplayName(player.getName());
                              }
                            });
                      });
            });
  }
}