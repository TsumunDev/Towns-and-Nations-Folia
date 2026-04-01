package org.leralix.tan.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.listeners.chat.PlayerChatListenerStorage;
import org.leralix.tan.listeners.interact.RightClickListener;
import org.leralix.tan.storage.PlayerAutoClaimStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;

/**
 * Listener for player quit events.
 *
 * <p>This listener handles updating player tracking data when a player disconnects,
 * specifically setting the online status to false.</p>
 */
public class PlayerQuitListener implements Listener {

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (player == null) {
      return;
    }

    // Clean up Player-keyed static maps to prevent memory leaks
    RightClickListener.cleanupOnQuit(player);
    PlayerChatListenerStorage.cleanupOnQuit(player);
    PlayerAutoClaimStorage.cleanupOnQuit(player);

    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              if (tanPlayer == null) {
                TownsAndNations.getPlugin()
                    .getLogger()
                    .warning("TanPlayer is null for " + player.getName());
                return;
              }

              // Update online status
              tanPlayer.setOnline(false);
              PlayerDataStorage.getInstance().update(tanPlayer);
            })
        .exceptionally(
            ex -> {
              if (ex != null && TownsAndNations.getPlugin() != null) {
                TownsAndNations.getPlugin()
                    .getLogger()
                    .warning(
                        "Error updating player data on quit for "
                            + player.getName()
                            + ": "
                            + ex.getMessage());
              }
              return null;
            });
  }
}
