package org.leralix.tan.commands.player;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.leralix.lib.commands.PlayerSubCommand;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.exception.EconomyException;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.service.AsyncEconomyService;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.commands.CommandExceptionHandler;
import org.leralix.tan.utils.commands.RateLimitRegistry;
import org.leralix.tan.utils.text.TanChatUtils;
public class PayCommand extends PlayerSubCommand {
  private final double maxPayDistance;
  public PayCommand(double maxPayDistance) {
    this.maxPayDistance = maxPayDistance;
  }
  @Override
  public String getName() {
    return "pay";
  }
  @Override
  public String getDescription() {
    return Lang.PAY_COMMAND_DESC.getDefault();
  }
  @Override
  public String getSyntax() {
    return "/ccn pay <player> <amount>";
  }
  public int getArguments() {
    return 3;
  }
  @Override
  public List<String> getTabCompleteSuggestions(Player player, String lowerCase, String[] args) {
    return payPlayerSuggestion(args);
  }
  @Override
  public void perform(Player player, String[] args) {
    var rateLimitResult = RateLimitRegistry.getInstance().canExecute(getName(), player.getUniqueId());
    if (!rateLimitResult.isAllowed()) {
      player.sendMessage("§cPlease wait " + rateLimitResult.getRemainingSeconds() + "s before using this command again.");
      return;
    }
    if (!CommandExceptionHandler.validateArgCount(player, args, 3, getSyntax())) {
      return;
    }
    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              FoliaScheduler.runTask(
                  TownsAndNations.getPlugin(),
                  () -> {
                    LangType langType = tanPlayer.getLang();
                    Player receiver = Bukkit.getServer().getPlayer(args[1]);
                    if (receiver == null) {
                      TanChatUtils.message(player, Lang.PLAYER_NOT_FOUND.get(langType));
                      return;
                    }
                    if (receiver.getUniqueId().equals(player.getUniqueId())) {
                      TanChatUtils.message(player, Lang.PAY_SELF_ERROR.get(langType));
                      return;
                    }
                    try {
                      Location senderLocation = player.getLocation();
                      Location receiverLocation = receiver.getLocation();
                      if (senderLocation.getWorld() != receiverLocation.getWorld()) {
                        TanChatUtils.message(
                            player, Lang.INTERACTION_TOO_FAR_ERROR.get(langType));
                        return;
                      }
                      if (senderLocation.distance(receiverLocation) > maxPayDistance) {
                        TanChatUtils.message(
                            player, Lang.INTERACTION_TOO_FAR_ERROR.get(langType));
                        return;
                      }
                    } catch (Exception e) {
                      TanChatUtils.message(player, Lang.SYNTAX_ERROR.get(langType));
                      CommandExceptionHandler.logCommandExecution(player, "pay", args);
                      return;
                    }
                    Optional<Integer> amountOpt =
                        CommandExceptionHandler.parseInt(player, args[2], "amount");
                    if (amountOpt.isEmpty()) {
                      return;
                    }
                    int amount = amountOpt.get();
                    if (amount < 1) {
                      TanChatUtils.message(player, Lang.PAY_MINIMUM_REQUIRED.get(langType));
                      return;
                    }

                    // Async balance check and payment
                    executePaymentAsync(player, receiver, amount, langType, args);
                  });
            })
        .exceptionally(
            throwable -> {
              TownsAndNations.getPlugin()
                  .getLogger()
                  .severe("PayCommand failed: " + throwable.getMessage());
              return null;
            });
  }

  /**
   * Executes payment asynchronously using AsyncEconomyService.
   *
   * <p>Flow:</p>
   * <ol>
   *   <li>Check sender's balance asynchronously</li>
   *   <li>If sufficient funds, withdraw from sender asynchronously</li>
   *   <li>Deposit to receiver asynchronously</li>
   *   <li>Send confirmation messages on region thread</li>
   * </ol>
   *
   * @param sender The player sending money
   * @param receiver The player receiving money
   * @param amount The amount to transfer
   * @param langType The language type for messages
   * @param args Original command args for logging
   */
  private void executePaymentAsync(
      Player sender,
      Player receiver,
      int amount,
      LangType langType,
      String[] args) {

    AsyncEconomyService.getBalance(sender)
        .thenAccept(senderBalance -> {
          if (senderBalance < amount) {
            // Insufficient funds
            TanChatUtils.message(
                sender,
                Lang.PLAYER_NOT_ENOUGH_MONEY_EXTENDED.get(
                    langType,
                    Double.toString(amount - senderBalance)));
            return;
          }

          // Sufficient funds - execute payment
          AsyncEconomyService.withdraw(sender, amount)
              .thenCompose(newBalance -> {
                // Withdraw successful, now deposit to receiver
                return AsyncEconomyService.deposit(receiver, amount);
              })
              .thenAccept(finalBalance -> {
                // Payment complete - send confirmations
                FoliaScheduler.runTask(
                    TownsAndNations.getPlugin(),
                    () -> {
                      TanChatUtils.message(
                          sender,
                          Lang.PAY_CONFIRMED_SENDER.get(
                              langType, Integer.toString(amount), receiver.getName()));
                      TanChatUtils.message(
                          receiver,
                          Lang.PAY_CONFIRMED_RECEIVER.get(
                              receiver, Integer.toString(amount), sender.getName()));
                    });
              })
              .exceptionally(throwable -> {
                // Payment failed
                TanChatUtils.message(sender, Lang.PLAYER_NOT_ENOUGH_MONEY.get(langType));
                CommandExceptionHandler.logCommandExecution(sender, "pay", args);
                TownsAndNations.getPlugin()
                    .getLogger()
                    .warning("Payment transaction failed: " + throwable.getMessage());
                return null;
              });
        })
        .exceptionally(throwable -> {
          // Balance check failed
          TownsAndNations.getPlugin()
              .getLogger()
              .warning("Failed to check balance for payment: " + throwable.getMessage());
          TanChatUtils.message(sender, Lang.SYNTAX_ERROR.get(langType));
          return null;
        });
  }
}