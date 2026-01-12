package org.leralix.tan.listeners;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.leralix.lib.utils.config.ConfigTag;
import org.leralix.lib.utils.config.ConfigUtil;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.TeleportationRegister;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.text.TanChatUtils;
public class SpawnListener implements Listener {
  @EventHandler
  public void onPlayerHit(EntityDamageByEntityEvent event) {
    if (event.getEntity() instanceof Player player
        && TeleportationRegister.isPlayerRegistered(player.getUniqueId().toString())
        && !TeleportationRegister.getTeleportationData(player).isCancelled()
        && ConfigUtil.getCustomConfig(ConfigTag.MAIN).getBoolean("cancelTeleportOnDamage", true)) {
      // Load player data asynchronously to avoid blocking I/O during combat
      PlayerDataStorage.getInstance()
          .get(player.getUniqueId())
          .thenAccept(tanPlayer -> {
            TeleportationRegister.getTeleportationData(tanPlayer).setCancelled(true);
            TanChatUtils.message(player, Lang.TELEPORTATION_CANCELLED.get(player));
          })
          .exceptionally(throwable -> {
            org.leralix.tan.TownsAndNations.getPlugin()
                .getLogger()
                .warning("Failed to cancel teleportation on hit: " + throwable.getMessage());
            return null;
          });
    }
  }
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (TeleportationRegister.isPlayerRegistered(player.getUniqueId().toString())
        && !TeleportationRegister.getTeleportationData(player).isCancelled()) {
      Location locationFrom = event.getFrom();
      Location locationTo = event.getTo();
      if (locationFrom.getBlockX() == locationTo.getBlockX()
          && locationFrom.getBlockZ() == locationTo.getBlockZ()) {
        if (ConfigUtil.getCustomConfig(ConfigTag.MAIN)
            .getBoolean("cancelTeleportOnMoveHead", false)) {
          cancelTeleportation(player);
        }
      } else {
        if (ConfigUtil.getCustomConfig(ConfigTag.MAIN)
            .getBoolean("cancelTeleportOnMovePosition", true)) {
          cancelTeleportation(player);
        }
      }
    }
  }
  public void cancelTeleportation(Player player) {
    // Load player data asynchronously to avoid blocking I/O
    PlayerDataStorage.getInstance()
        .get(player.getUniqueId())
        .thenAccept(tanPlayer -> {
          TeleportationRegister.getTeleportationData(tanPlayer).setCancelled(true);
          TanChatUtils.message(player, Lang.TELEPORTATION_CANCELLED.get(player));
        })
        .exceptionally(throwable -> {
          org.leralix.tan.TownsAndNations.getPlugin()
              .getLogger()
              .warning("Failed to cancel teleportation: " + throwable.getMessage());
          return null;
        });
  }
}