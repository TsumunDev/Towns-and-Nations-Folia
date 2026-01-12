package org.leralix.tan.listeners.chat.events;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.leralix.lib.utils.config.ConfigTag;
import org.leralix.lib.utils.config.ConfigUtil;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.events.EventManager;
import org.leralix.tan.events.events.TownCreatedInternalEvent;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.listeners.chat.ChatListenerEvent;
import org.leralix.tan.service.AsyncEconomyService;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.file.FileUtil;
import org.leralix.tan.utils.graphic.TeamUtils;
import org.leralix.tan.utils.text.TanChatUtils;
public class CreateTown extends ChatListenerEvent {
  int cost;
  public CreateTown(int cost) {
    super();
    this.cost = cost;
  }
  @Override
  public boolean execute(Player player, String message) {
    // Start async town creation flow
    createTownAsync(player, message);
    return true;
  }

  /**
   * Creates a town asynchronously using AsyncEconomyService and async storage operations.
   *
   * <p>Flow:</p>
   * <ol>
   *   <li>Check player's balance asynchronously</li>
   *   <li>If sufficient funds, validate town name</li>
   *   <li>Create town data asynchronously</li>
   *   <li>Withdraw cost from player asynchronously</li>
   *   <li>Fire town creation event on region thread</li>
   *   <li>Open town management GUI</li>
   * </ol>
   *
   * @param player The player creating the town
   * @param townName The proposed town name
   */
  private void createTownAsync(Player player, String townName) {

    // Step 1: Check player's balance asynchronously
    AsyncEconomyService.getBalance(player)
        .thenAccept(balance -> {
          // Step 2: Validate balance (on region thread after balance check)
          if (balance < cost) {
            double deficit = cost - balance;
            TanChatUtils.message(
                player,
                Lang.PLAYER_NOT_ENOUGH_MONEY_EXTENDED.get(player, Double.toString(deficit)));
            return;
          }

          // Step 3: Validate town name (synchronous, fast operation)
          FileConfiguration config = ConfigUtil.getCustomConfig(ConfigTag.MAIN);
          int maxSize = config.getInt("TownNameSize", 45);
          if (townName.length() > maxSize) {
            TanChatUtils.message(
                player,
                Lang.MESSAGE_TOO_LONG.get(player, Integer.toString(maxSize)));
            return;
          }

          if (TownDataStorage.getInstance().isNameUsed(townName)) {
            TanChatUtils.message(player, Lang.NAME_ALREADY_USED.get(player));
            return;
          }

          // Step 4: Load player data and create town
          PlayerDataStorage.getInstance()
              .get(player)
              .thenCompose(tanPlayer -> {
                // Create the town (returns CompletableFuture<TownData>)
                return TownDataStorage.getInstance().newTown(townName, tanPlayer);
              })
              .thenCompose(newTown -> {
                // Step 5: Withdraw cost from player
                return AsyncEconomyService.withdraw(player, cost)
                    .thenApply(newBalance -> newTown); // Pass newTown through
              })
              .thenAccept(newTown -> {
                // Step 6: Town creation complete - fire event and update UI
                org.leralix.tan.utils.FoliaScheduler.runTask(
                    TownsAndNations.getPlugin(),
                    () -> {
                      // Reload player data to get updated state
                      PlayerDataStorage.getInstance()
                          .get(player)
                          .thenAccept(updatedPlayer -> {
                            // Fire town creation event
                            EventManager.getInstance().callEvent(
                                new TownCreatedInternalEvent(newTown, updatedPlayer));

                            // Log to history
                            FileUtil.addLineToHistory(
                                Lang.TOWN_CREATED_NEWSLETTER.get(
                                    player.getName(), newTown.getName()));

                            // Update scoreboard
                            TeamUtils.setIndividualScoreBoard(player);

                            // Open town management GUI
                            openGui(p -> newTown.openMainMenu(player), player);
                          })
                          .exceptionally(throwable -> {
                            TownsAndNations.getPlugin()
                                .getLogger()
                                .warning("Failed to reload player data after town creation: "
                                    + throwable.getMessage());
                            // Still open GUI even if player data reload failed
                            TeamUtils.setIndividualScoreBoard(player);
                            openGui(p -> newTown.openMainMenu(player), player);
                            return null;
                          });
                    });
              })
              .exceptionally(throwable -> {
                // Town creation failed
                TownsAndNations.getPlugin()
                    .getLogger()
                    .severe("Failed to create town '" + townName + "': " + throwable.getMessage());
                TanChatUtils.message(
                    player,
                    Lang.SYNTAX_ERROR.get(player));
                return null;
              });
        })
        .exceptionally(throwable -> {
          // Balance check failed
          TownsAndNations.getPlugin()
              .getLogger()
              .warning("Failed to check balance for town creation: " + throwable.getMessage());
          TanChatUtils.message(
              player,
              Lang.SYNTAX_ERROR.get(player));
          return null;
        });
  }
}