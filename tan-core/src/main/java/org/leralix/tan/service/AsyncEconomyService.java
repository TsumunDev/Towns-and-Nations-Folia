package org.leralix.tan.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.economy.AbstractTanEcon;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.FoliaAsyncHelper;

/**
 * Async economy service for Folia regionalized threading.
 *
 * <p>This service provides async alternatives to the blocking {@link EconomyUtil} methods.
 * All database/storage operations are performed off the region thread, and results are
 * delivered back to the appropriate region/entity thread for continued processing.</p>
 *
 * <h3>Migration Path:</h3>
 * <ul>
 *   <li>Old (blocking): {@code double balance = EconomyUtil.getBalance(player);}</li>
 *   <li>New (async):
 *     <pre>{@code
 *     AsyncEconomyService.getBalance(player).thenAccept(balance -> {
 *         player.sendMessage("Balance: " + balance);
 *     });
 *     }</pre>
 *   </li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li>All storage operations use {@link PlayerDataStorage#get(UUID)} (async)</li>
 *   <li>Results delivered via {@link FoliaAsyncHelper} to region/entity thread</li>
 *   <li>Economy operations delegated to {@link AbstractTanEcon} (in-memory, thread-safe)</li>
 *   <li>Backward compatible with existing sync API via {@link EconomyUtil}</li>
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 * <ul>
 *   <li>Storage load: Async thread (via PlayerDataStorage.get)</li>
 *   <li>Economy ops: Can be async (in-memory cache)</li>
 *   <li>Bukkit API access: Region/entity thread (via FoliaAsyncHelper)</li>
 * </ul>
 *
 * @since 0.16.0
 * @see EconomyUtil for synchronous (deprecated) methods
 * @see FoliaAsyncHelper for async execution patterns
 */
public class AsyncEconomyService {

  private static final Plugin PLUGIN = TownsAndNations.getPlugin();

  private AsyncEconomyService() {
    throw new IllegalStateException("Utility class");
  }

  private static AbstractTanEcon econ() {
    return EconomyUtil.getEconInstance();
  }

  // ========== Query Operations ==========

  /**
   * Gets a player's balance asynchronously.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   * AsyncEconomyService.getBalance(player)
   *     .thenAccept(balance -> {
   *         player.sendMessage("Your balance: " + balance);
   *     });
   * }</pre>
   *
   * @param offlinePlayer The offline player to query
   * @return CompletableFuture containing the balance
   */
  public static CompletableFuture<Double> getBalance(OfflinePlayer offlinePlayer) {
    UUID uuid = offlinePlayer.getUniqueId();

    return PlayerDataStorage.getInstance().get(uuid)
        .thenApply(econ()::getBalance);
  }

  /**
   * Gets a player's balance asynchronously.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   * AsyncEconomyService.getBalance(player)
   *     .thenAccept(balance -> {
   *         player.sendMessage("Your balance: " + balance);
   *     });
   * }</pre>
   *
   * @param player The player to query
   * @return CompletableFuture containing the balance
   */
  public static CompletableFuture<Double> getBalance(Player player) {
    UUID uuid = player.getUniqueId();

    return PlayerDataStorage.getInstance().get(uuid)
        .thenApply(econ()::getBalance);
  }

  /**
   * Gets a player's balance synchronously (ITanPlayer already loaded).
   *
   * <p>This method is safe to call when you already have an ITanPlayer instance
   * and don't need to load from storage.</p>
   *
   * @param tanPlayer The player data (already loaded)
   * @return The balance
   */
  public static double getBalance(ITanPlayer tanPlayer) {
    return econ().getBalance(tanPlayer);
  }

  // ========== Modification Operations ==========

  /**
   * Withdraws money from a player's balance asynchronously.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   * AsyncEconomyService.withdraw(player, 100.0)
   *     .thenRun(() -> {
   *         player.sendMessage("Withdrew $100.00");
   *     });
   * }</pre>
   *
   * @param offlinePlayer The offline player
   * @param amount The amount to withdraw
   * @return CompletableFuture that completes when the withdrawal is done
   */
  public static CompletableFuture<Void> withdraw(OfflinePlayer offlinePlayer, double amount) {
    UUID uuid = offlinePlayer.getUniqueId();

    return PlayerDataStorage.getInstance().get(uuid)
        .thenAccept(tanPlayer -> {
          econ().withdrawPlayer(tanPlayer, amount);
        });
  }

  /**
   * Withdraws money from a player's balance asynchronously.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   * AsyncEconomyService.withdraw(player, 100.0)
   *     .thenAccept(newBalance -> {
   *         player.sendMessage("Withdrew $100.00. New balance: " + newBalance);
   *     });
   * }</pre>
   *
   * @param player The player
   * @param amount The amount to withdraw
   * @return CompletableFuture containing the new balance
   */
  public static CompletableFuture<Double> withdraw(Player player, double amount) {
    UUID uuid = player.getUniqueId();

    return PlayerDataStorage.getInstance().get(uuid)
        .thenApply(tanPlayer -> {
          econ().withdrawPlayer(tanPlayer, amount);
          return econ().getBalance(tanPlayer);
        });
  }

  /**
   * Withdraws money from a player's balance synchronously.
   *
   * @param tanPlayer The player data (already loaded)
   * @param amount The amount to withdraw
   */
  public static void withdraw(ITanPlayer tanPlayer, double amount) {
    econ().withdrawPlayer(tanPlayer, amount);
  }

  /**
   * Deposits money to a player's balance asynchronously.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   * AsyncEconomyService.deposit(player, 100.0)
   *     .thenAccept(newBalance -> {
   *         player.sendMessage("Deposited $100.00. New balance: " + newBalance);
   *     });
   * }</pre>
   *
   * @param offlinePlayer The offline player
   * @param amount The amount to deposit
   * @return CompletableFuture that completes when the deposit is done
   */
  public static CompletableFuture<Void> deposit(OfflinePlayer offlinePlayer, double amount) {
    UUID uuid = offlinePlayer.getUniqueId();

    return PlayerDataStorage.getInstance().get(uuid)
        .thenAccept(tanPlayer -> {
          econ().depositPlayer(tanPlayer, amount);
        });
  }

  /**
   * Deposits money to a player's balance asynchronously.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   * AsyncEconomyService.deposit(player, 100.0)
   *     .thenAccept(newBalance -> {
   *         player.sendMessage("Deposited $100.00. New balance: " + newBalance);
   *     });
   * }</pre>
   *
   * @param player The player
   * @param amount The amount to deposit
   * @return CompletableFuture containing the new balance
   */
  public static CompletableFuture<Double> deposit(Player player, double amount) {
    UUID uuid = player.getUniqueId();

    return PlayerDataStorage.getInstance().get(uuid)
        .thenApply(tanPlayer -> {
          econ().depositPlayer(tanPlayer, amount);
          return econ().getBalance(tanPlayer);
        });
  }

  /**
   * Deposits money to a player's balance synchronously.
   *
   * @param tanPlayer The player data (already loaded)
   * @param amount The amount to deposit
   */
  public static void deposit(ITanPlayer tanPlayer, double amount) {
    econ().depositPlayer(tanPlayer, amount);
  }

  /**
   * Sets a player's balance to a specific amount asynchronously.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   * AsyncEconomyService.setBalance(player, 1000.0)
   *     .thenRun(() -> {
   *         player.sendMessage("Balance set to $1000.00");
   *     });
   * }</pre>
   *
   * @param tanPlayer The player data
   * @param amount The new balance
   * @return CompletableFuture that completes when the balance is set
   */
  public static CompletableFuture<Void> setBalance(ITanPlayer tanPlayer, double amount) {
    return CompletableFuture.runAsync(() -> {
      double currentBalance = econ().getBalance(tanPlayer);
      if (currentBalance > amount) {
        econ().withdrawPlayer(tanPlayer, currentBalance - amount);
      } else if (currentBalance < amount) {
        econ().depositPlayer(tanPlayer, amount - currentBalance);
      }
    });
  }

  // ========== Utility Operations ==========

  /**
   * Formats a monetary amount with the currency icon.
   *
   * @param amount The amount to format
   * @return Formatted string (e.g., "$100.00")
   */
  public static String formatMoney(double amount) {
    return econ().formatMoney(amount);
  }

  /**
   * Gets the currency icon/symbol.
   *
   * @return The currency icon (e.g., "$")
   */
  public static String getMoneyIcon() {
    return econ().getMoneyIcon();
  }

  /**
   * Checks if the economy is in standalone mode.
   *
   * @return true if standalone, false if using Vault
   */
  public static boolean isStandalone() {
    return EconomyUtil.isStandalone();
  }
}
