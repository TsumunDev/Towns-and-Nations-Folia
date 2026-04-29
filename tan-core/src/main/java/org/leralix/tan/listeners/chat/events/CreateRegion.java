package org.leralix.tan.listeners.chat.events;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.leralix.lib.utils.config.ConfigTag;
import org.leralix.lib.utils.config.ConfigUtil;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.api.internal.wrappers.TanPlayerWrapper;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.events.EventManager;
import org.leralix.tan.events.events.RegionCreatedInternalEvent;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.listeners.chat.ChatListenerEvent;
import org.leralix.tan.service.AsyncEconomyService;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.text.TanChatUtils;
public class CreateRegion extends ChatListenerEvent {
  private final int cost;
  public CreateRegion(int cost) {
    super();
    this.cost = cost;
  }
  @Override
  public boolean execute(Player player, String message) {
    createRegionAsync(player, message);
    return true;
  }

  private void createRegionAsync(Player player, String regionName) {
    // Step 1: Check player's balance via Vault/zEssentials
    AsyncEconomyService.getBalance(player)
        .thenCompose(balance -> {
          if (balance < cost) {
            double deficit = cost - balance;
            TanChatUtils.message(
                player,
                Lang.PLAYER_NOT_ENOUGH_MONEY_EXTENDED.get(player, Double.toString(deficit)));
            return CompletableFuture.completedFuture(null);
          }

          // Step 2: Load player and validate
          return PlayerDataStorage.getInstance().get(player).thenCompose(tanPlayer -> {
            TownData town = TownDataStorage.getInstance().getSync(tanPlayer);
            if (!town.isLeader(player)) {
              TanChatUtils.message(player, Lang.PLAYER_ONLY_LEADER_CAN_PERFORM_ACTION.get(tanPlayer));
              return CompletableFuture.completedFuture(null);
            }

            int maxSize = ConfigUtil.getCustomConfig(ConfigTag.MAIN).getInt("RegionNameSize");
            if (regionName.length() > maxSize) {
              TanChatUtils.message(player, Lang.MESSAGE_TOO_LONG.get(tanPlayer, Integer.toString(maxSize)));
              return CompletableFuture.completedFuture(null);
            }

            if (RegionDataStorage.getInstance().isNameUsed(regionName)) {
              TanChatUtils.message(player, Lang.NAME_ALREADY_USED.get(tanPlayer));
              return CompletableFuture.completedFuture(null);
            }

            // Step 3: Withdraw money from player (Vault/zEssentials)
            return AsyncEconomyService.withdraw(player, cost)
                .thenCompose(newBalance -> {
                  // Step 4: Create region after successful payment
                  return RegionDataStorage.getInstance().createNewRegion(regionName, town)
                      .thenApply(newRegion -> {
                        TownsAndNations.getPlugin().getLogger().info(
                            "[REGION-CREATION] Region '" + regionName + "' created by " + player.getName());
                        return newRegion;
                      })
                      .exceptionally(creationError -> {
                        // Refund on failure
                        TownsAndNations.getPlugin().getLogger().severe(
                            "[REGION-CREATION] Failed for " + player.getName() + ", refunding: " + creationError.getMessage());
                        AsyncEconomyService.deposit(player, cost);
                        TanChatUtils.message(player, "§cRegion creation failed. Your money has been refunded.");
                        return null;
                      });
                })
                .exceptionally(paymentError -> {
                  TanChatUtils.message(player, "§cPayment failed. Please try again.");
                  return null;
                });
          });
        })
        .thenAccept(newRegion -> {
          if (newRegion == null) return;

          org.leralix.tan.utils.FoliaScheduler.runTask(
              TownsAndNations.getPlugin(),
              () -> {
                PlayerDataStorage.getInstance()
                    .get(player)
                    .thenAccept(updatedPlayer -> {
                      EventManager.getInstance().callEvent(
                          new RegionCreatedInternalEvent((RegionData) newRegion, TanPlayerWrapper.of(updatedPlayer)));
                      openGui(p -> ((RegionData) newRegion).openMainMenu(player), player);
                    });
              });
        })
        .exceptionally(throwable -> {
          TanChatUtils.message(player, Lang.SYNTAX_ERROR.get(player));
          return null;
        });
  }
}
