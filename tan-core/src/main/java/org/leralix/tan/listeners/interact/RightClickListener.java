package org.leralix.tan.listeners.interact;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.leralix.lib.data.SoundEnum;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.text.TanChatUtils;
public class RightClickListener implements Listener {
  private static final ConcurrentHashMap<Player, RightClickListenerEvent> events = new ConcurrentHashMap<>();
  @EventHandler
  public void OnPlayerInteractEvent(PlayerInteractEvent event) {
    if (event.getHand() == EquipmentSlot.OFF_HAND) return;
    if (event.getItem() != null) {
      return;
    }
    Player player = event.getPlayer();
    if (event.getAction().isRightClick()) {
      RightClickListenerEvent listenerEvent = events.get(player);
      if (listenerEvent == null) return;
      event.setCancelled(true);
      ListenerState state = listenerEvent.execute(event);
      if (state == ListenerState.SUCCESS) {
        events.remove(player);
      }
      if (state == ListenerState.FAILURE) {
        LangType langType = PlayerDataStorage.getInstance().getSync(player).getLang();
        TanChatUtils.message(
            player, Lang.WRITE_CANCEL_TO_CANCEL.get(langType, Lang.CANCEL_WORD.get(langType)));
      }
    }
  }
  public static void removePlayer(Player player) {
    events.remove(player);
  }
  /**
   * Call from existing PlayerQuitListener to prevent memory leaks.
   */
  public static void cleanupOnQuit(Player player) {
    events.remove(player);
  }
  public static void register(Player player, RightClickListenerEvent rightClickListenerEvent) {
    player.closeInventory();
    LangType langType = PlayerDataStorage.getInstance().getSync(player).getLang();
    TanChatUtils.message(
        player,
        Lang.WRITE_CANCEL_TO_CANCEL.get(langType, Lang.CANCEL_WORD.get(langType)),
        SoundEnum.MINOR_GOOD);
    events.put(player, rightClickListenerEvent);
  }
}