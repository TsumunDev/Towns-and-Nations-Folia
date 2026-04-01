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
import org.leralix.tan.validation.InputValidator;
import org.leralix.tan.validation.ValidationException;
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
   * @param finalTownName The proposed town name
   */
  private void createTownAsync(Player player, String townName) {
    java.util.logging.Logger logger = TownsAndNations.getPlugin().getLogger();
    String playerName = player.getName();

    logger.info("[TOWN-CREATION] " + playerName + " attempting to create town: " + townName +
        " (cost: " + cost + ")");

    // VALIDATION STEP: Validate town name format first (fast, synchronous check)
    String validatedTownName;
    try {
      validatedTownName = InputValidator.validateTownName(townName);
      logger.info("[TOWN-CREATION] " + playerName + " - town name validation passed: " + validatedTownName);
    } catch (ValidationException e) {
      logger.warning("[TOWN-CREATION] " + playerName + " - invalid town name: " + e.getLogMessage());
      TanChatUtils.message(player, "§c" + e.getUserMessage());
      return;
    }

    // Step 1: Check player's balance asynchronously
    final String finalTownName = validatedTownName;
    AsyncEconomyService.getBalance(player)
        .thenCompose(balance -> {
          // Step 2: Validate balance (on region thread after balance check)
          if (balance < cost) {
            double deficit = cost - balance;
            logger.info("[TOWN-CREATION] " + playerName + " has insufficient funds (" +
                balance + ", needs " + cost + ")");
            TanChatUtils.message(
                player,
                Lang.PLAYER_NOT_ENOUGH_MONEY_EXTENDED.get(player, Double.toString(deficit)));
            return java.util.concurrent.CompletableFuture.completedFuture(null);
          }

          // Step 3: Check if name is already used (must be checked in storage)
          // Note: format validation already done via InputValidator at method entry
          if (TownDataStorage.getInstance().isNameUsed(finalTownName)) {
            logger.info("[TOWN-CREATION] " + playerName + " attempted to use existing name: " +
                finalTownName);
            TanChatUtils.message(player, Lang.NAME_ALREADY_USED.get(player));
            return java.util.concurrent.CompletableFuture.completedFuture(null);
          }

          // Step 4: Load player data
          return PlayerDataStorage.getInstance().get(player)
              .thenCompose(tanPlayer -> {
                // CRITICAL CHECK: Player already has a town?
                if (tanPlayer.hasTown()) {
                  logger.info("[TOWN-CREATION] " + playerName +
                      " already belongs to a town (creation denied)");
                  TanChatUtils.message(player,
                      "§cYou already belong to a town! Leave it first.");
                  return java.util.concurrent.CompletableFuture.completedFuture(null);
                }

                // Step 5: WITHDRAW MONEY FIRST (before town creation)
                logger.info("[TOWN-CREATION] Withdrawing " + cost + " from " + playerName);
                return AsyncEconomyService.withdraw(player, cost)
                    .thenCompose(newBalance -> {
                      logger.info("[TOWN-CREATION] Payment successful! " + playerName +
                          " new balance: " + newBalance);

                      // Step 6: Create town AFTER successful payment
                      return TownDataStorage.getInstance().newTown(finalTownName, tanPlayer)
                          .thenApply(newTown -> {
                            logger.info("[TOWN-CREATION] Successfully created town '" +
                                finalTownName + "' for " + playerName);
                            return newTown;
                          })
                          .exceptionally(creationError -> {
                            // CRITICAL: Town creation failed - REFUND MONEY
                            logger.severe("[TOWN-CREATION] Town creation failed for " +
                                playerName + ", refunding " + cost + ": " +
                                    creationError.getMessage());

                            AsyncEconomyService.deposit(player, cost)
                                .thenAccept(refundSuccess -> {
                                  logger.info("[TOWN-CREATION] Refunded " + cost + " to " +
                                      playerName);
                                  TanChatUtils.message(player,
                                      "§cTown creation failed. Your money has been refunded.");
                                })
                                .exceptionally(refundError -> {
                                  logger.severe("[TOWN-CREATION] FAILED TO REFUND " + cost +
                                      " to " + playerName + ": " + refundError.getMessage());
                                  TanChatUtils.message(player,
                                      "§cTown creation failed. Contact admin for refund.");
                                  return null;
                                });

                            return null;
                          });
                    })
                    .exceptionally(paymentError -> {
                      // Payment failed - no town created, no money lost
                      logger.warning("[TOWN-CREATION] Payment failed for " + playerName +
                          ": " + paymentError.getMessage());
                      TanChatUtils.message(player,
                          "§cPayment failed. Please try again.");
                      return null;
                    });
              });
        })
        .thenAccept(newTown -> {
          // Step 7: Town creation complete - fire event and update UI
          if (newTown == null) {
            return; // Something failed earlier in the chain
          }

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
                      logger.warning("[TOWN-CREATION] Failed to reload player data after town creation: "
                          + throwable.getMessage());
                      // Still open GUI even if player data reload failed
                      TeamUtils.setIndividualScoreBoard(player);
                      openGui(p -> newTown.openMainMenu(player), player);
                      return null;
                    });
              });
        })
        .exceptionally(throwable -> {
          // Balance check failed
          logger.warning("[TOWN-CREATION] Failed to check balance for " + playerName +
              ": " + throwable.getMessage());
          TanChatUtils.message(
              player,
              Lang.SYNTAX_ERROR.get(player));
          return null;
        });
  }
}