package org.leralix.tan.storage.invitation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
public class TownInviteDataStorage {
  private static final Map<String, List<String>> townInviteList = new ConcurrentHashMap<>();
  public static void addInvitation(String playerUUID, String townId) {
    townInviteList.computeIfAbsent(playerUUID, k -> new CopyOnWriteArrayList<>()).add(townId);
  }
  public static void removeInvitation(String playerUUID) {
    townInviteList.remove(playerUUID);
  }
  public static boolean isInvited(String playerUUID, String townID) {
    List<String> invitations = townInviteList.get(playerUUID);
    return invitations != null && invitations.contains(townID);
  }
}