package org.leralix.tan.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;

/**
 * Facade for economy operations in Towns and Nations.
 *
 * <p>This class provides a unified API for all economy operations, delegating to the
 * underlying economy implementation (standalone or external). It supports multiple
 * player object types for convenience:</p>
 * <ul>
 *   <li>{@link Player} - Online Bukkit players</li>
 *   <li>{@link OfflinePlayer} - Offline Bukkit players</li>
 *   <li>{@link ITanPlayer} - TAN's player data object</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Methods that use {@code getSync()} internally are
 * blocking. Avoid calling them on Folia region threads. Use
 * {@link org.leralix.tan.service.AsyncEconomyService} for non-blocking operations.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Check balance
 * double balance = EconomyUtil.getBalance(player);
 *
 * // Transfer money between players
 * if (EconomyUtil.getBalance(fromPlayer) >= amount) {
 *     EconomyUtil.removeFromBalance(fromPlayer, amount);
 *     EconomyUtil.addFromBalance(toPlayer, amount);
 * }
 *
 * // Format money for display
 * String formatted = EconomyUtil.formatMoney(1000.0);
 * }</pre>
 *
 * @see AbstractTanEcon
 * @see TanEconomyStandalone
 * @see org.leralix.tan.service.AsyncEconomyService
 * @since 0.15.0
 */
public class EconomyUtil {

  private static AbstractTanEcon econ;

  /**
   * Registers an economy implementation for use.
   *
   * <p>This method is called internally during plugin initialization to set
   * the active economy implementation. Typically not called by external code.</p>
   *
   * @param newEcon the economy implementation to register
   * @see #getEconInstance()
   */
  public static void register(AbstractTanEcon newEcon) {
    econ = newEcon;
  }

  /**
   * Checks if the current economy is standalone TAN economy.
   *
   * <p>Returns true if TAN is using its internal economy. Returns false if
   * TAN is delegating to an external economy plugin (via Vault).</p>
   *
   * @return true if using standalone TAN economy, false if using external economy
   * @see TanEconomyStandalone
   * @see TanEconomyExternal
   */
  public static boolean isStandalone() {
    return econ instanceof TanEconomyStandalone;
  }
  /**
   * Gets the underlying economy instance.
   *
   * <p><b>Internal use only:</b> This is exposed for {@link AsyncEconomyService}
   * and other advanced async operations. Prefer using {@link AsyncEconomyService}
   * for new code.</p>
   *
   * @return The economy instance
   */
  public static AbstractTanEcon getEconInstance() {
    return econ;
  }
  public static double getBalance(OfflinePlayer offlinePlayer) {
    return econ.getBalance(PlayerDataStorage.getInstance().getSync(offlinePlayer));
  }
  public static double getBalance(ITanPlayer player) {
    return econ.getBalance(player);
  }
  public static double getBalance(Player player) {
    return econ.getBalance(PlayerDataStorage.getInstance().getSync(player));
  }
  public static void removeFromBalance(ITanPlayer tanPlayer, double amount) {
    econ.withdrawPlayer(tanPlayer, amount);
  }
  public static void removeFromBalance(OfflinePlayer offlinePlayer, double amount) {
    econ.withdrawPlayer(PlayerDataStorage.getInstance().getSync(offlinePlayer), amount);
  }
  public static void removeFromBalance(Player player, double amount) {
    econ.withdrawPlayer(PlayerDataStorage.getInstance().getSync(player), amount);
  }
  public static void addFromBalance(ITanPlayer player, double amount) {
    econ.depositPlayer(player, amount);
  }
  public static void addFromBalance(Player player, double amount) {
    econ.depositPlayer(PlayerDataStorage.getInstance().getSync(player), amount);
  }
  public static void addFromBalance(OfflinePlayer offlinePlayer, double amount) {
    econ.depositPlayer(PlayerDataStorage.getInstance().getSync(offlinePlayer), amount);
  }
  public static String getMoneyIcon() {
    return econ.getMoneyIcon();
  }
  public static void setBalance(ITanPlayer target, double amount) {
    removeFromBalance(target, getBalance(target));
    addFromBalance(target, amount);
  }

  /**
   * Formats a money amount with the currency icon.
   *
   * @param amount The amount to format
   * @return Formatted string (e.g., "$100.00")
   */
  public static String formatMoney(double amount) {
    return econ.formatMoney(amount);
  }

  /**
   * Adds money to a player's balance (alias for addFromBalance).
   *
   * @param player The player to add money to
   * @param amount The amount to add
   */
  public static void addToBalance(Player player, double amount) {
    addFromBalance(player, amount);
  }

  /**
   * Adds money to an ITanPlayer's balance (alias for addFromBalance).
   *
   * @param tanPlayer The player to add money to
   * @param amount The amount to add
   */
  public static void addToBalance(ITanPlayer tanPlayer, double amount) {
    addFromBalance(tanPlayer, amount);
  }

  /**
   * Adds money to an offline player's balance (alias for addFromBalance).
   *
   * @param offlinePlayer The offline player to add money to
   * @param amount The amount to add
   */
  public static void addToBalance(OfflinePlayer offlinePlayer, double amount) {
    addFromBalance(offlinePlayer, amount);
  }
}