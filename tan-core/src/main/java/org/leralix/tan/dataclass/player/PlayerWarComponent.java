package org.leralix.tan.dataclass.player;

import java.util.Iterator;
import java.util.List;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.storage.CurrentAttacksStorage;
import org.leralix.tan.wars.legacy.CurrentAttack;

/**
 * Manages war involvement for a player — logic for tracking attacks the player participates in.
 * Data (attackInvolvedIn list) remains in PlayerData for Gson serialization compatibility.
 */
public class PlayerWarComponent {
  private final ITanPlayer player;
  private final List<String> attackList;

  public PlayerWarComponent(ITanPlayer player, List<String> attackList) {
    this.player = player;
    this.attackList = attackList;
  }

  public void addWar(CurrentAttack currentAttacks) {
    String id = currentAttacks.getAttackData().getID();
    if (!attackList.contains(id)) {
      attackList.add(id);
    }
  }

  public void updateCurrentAttack() {
    Iterator<String> iterator = attackList.iterator();
    while (iterator.hasNext()) {
      String attackID = iterator.next();
      CurrentAttack currentAttack = CurrentAttacksStorage.get(attackID);
      if (currentAttack == null || !currentAttack.containsPlayer(player)) {
        iterator.remove();
      } else {
        currentAttack.addPlayer(player);
      }
    }
  }

  public boolean isAtWarWith(TerritoryData territoryData) {
    if (territoryData == null) {
      return false;
    }
    Iterator<String> iterator = attackList.iterator();
    while (iterator.hasNext()) {
      String attackID = iterator.next();
      CurrentAttack currentAttack = CurrentAttacksStorage.get(attackID);
      if (currentAttack == null) {
        iterator.remove();
        continue;
      }
      if (currentAttack.getAttackData().getDefendingTerritories().contains(territoryData)) {
        return true;
      }
    }
    return false;
  }

  public void removeWar(CurrentAttack currentAttacks) {
    attackList.remove(currentAttacks.getAttackData().getID());
  }
}
