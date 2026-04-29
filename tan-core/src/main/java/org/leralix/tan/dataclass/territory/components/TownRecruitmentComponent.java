package org.leralix.tan.dataclass.territory.components;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntSupplier;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.events.EventManager;
import org.leralix.tan.events.events.PlayerJoinTownRequestInternalEvent;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.upgrade.rewards.numeric.TownPlayerCap;

/**
 * Manages recruitment for a town. Operates on data fields owned by TownData
 * (Gson serialization safety — data stays on the data class).
 */
public class TownRecruitmentComponent {
  private final TownData town;
  private final boolean[] isRecruitingHolder;
  private final HashSet<String> playerJoinRequestSet;

  public TownRecruitmentComponent(TownData town, boolean isRecruiting, HashSet<String> playerJoinRequestSet) {
    this.town = town;
    this.isRecruitingHolder = new boolean[]{isRecruiting};
    this.playerJoinRequestSet = playerJoinRequestSet;
  }

  public boolean isRecruiting() {
    return isRecruitingHolder[0];
  }

  public void swapRecruiting() {
    this.isRecruitingHolder[0] = !this.isRecruitingHolder[0];
  }

  public void setRecruiting(boolean recruiting) {
    this.isRecruitingHolder[0] = recruiting;
  }

  public boolean isFull(IntSupplier playerCountSupplier) {
    return !town.getNewLevel()
        .getStat(TownPlayerCap.class)
        .canDoAction(playerCountSupplier.getAsInt());
  }

  public void addPlayerJoinRequest(Player player) {
    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              if (tanPlayer != null) {
                EventManager.getInstance()
                    .callEvent(new PlayerJoinTownRequestInternalEvent(tanPlayer, town));
                addPlayerJoinRequest(tanPlayer.getID());
              }
            });
  }

  public void addPlayerJoinRequest(String playerUUID) {
    this.playerJoinRequestSet.add(playerUUID);
  }

  public void removePlayerJoinRequest(String playerUUID) {
    playerJoinRequestSet.remove(playerUUID);
  }

  public void removePlayerJoinRequest(Player player) {
    removePlayerJoinRequest(player.getUniqueId().toString());
  }

  public boolean isPlayerAlreadyRequested(String playerUUID) {
    return playerJoinRequestSet.contains(playerUUID);
  }

  public boolean isPlayerAlreadyRequested(Player player) {
    return isPlayerAlreadyRequested(player.getUniqueId().toString());
  }

  public Set<String> getPlayerJoinRequestSet() {
    return this.playerJoinRequestSet;
  }

  public void setPlayerJoinRequestSet(HashSet<String> set) {
    this.playerJoinRequestSet.clear();
    if (set != null) {
      this.playerJoinRequestSet.addAll(set);
    }
  }
}
