package org.leralix.tan.dataclass.territory.components;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntSupplier;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.events.EventManager;
import org.leralix.tan.events.events.PlayerJoinTownRequestInternalEvent;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.upgrade.rewards.numeric.TownPlayerCap;
public class TownRecruitmentComponent {
  private final TownData town;
  private boolean isRecruiting;
  private HashSet<String> playerJoinRequestSet;
  public TownRecruitmentComponent(TownData town) {
    this.town = town;
    this.isRecruiting = false;
    this.playerJoinRequestSet = new HashSet<>();
  }
  public boolean isRecruiting() {
    return isRecruiting;
  }
  public void swapRecruiting() {
    this.isRecruiting = !this.isRecruiting;
  }
  public void setRecruiting(boolean recruiting) {
    this.isRecruiting = recruiting;
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
    this.playerJoinRequestSet = set != null ? set : new HashSet<>();
  }
}