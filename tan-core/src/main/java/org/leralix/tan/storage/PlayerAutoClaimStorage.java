package org.leralix.tan.storage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.leralix.tan.enums.ChunkType;
public class PlayerAutoClaimStorage {
  private static final Map<Player, ChunkType> playerList = new ConcurrentHashMap<>();
  public static void addPlayer(Player player, ChunkType chunkType) {
    playerList.put(player, chunkType);
  }
  public static void removePlayer(Player player) {
    playerList.remove(player);
  }
  public static boolean containsPlayer(Player player) {
    return playerList.containsKey(player);
  }

  /**
   * Call from PlayerQuitListener to prevent memory leaks.
   * Removes the player from the auto-claim map so the Player object can be garbage-collected.
   */
  public static void cleanupOnQuit(Player player) {
    playerList.remove(player);
  }
  public static ChunkType getChunkType(Player player) {
    return playerList.get(player);
  }
}