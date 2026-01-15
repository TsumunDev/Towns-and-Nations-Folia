package org.leralix.tan.economy;

import org.leralix.tan.dataclass.ITanPlayer;

/**
 * Abstract base class for economy implementations in Towns and Nations.
 *
 * <p>This class defines the economy API contract that all economy implementations must follow.
 * TAN provides several built-in implementations:</p>
 * <ul>
 *   <li>{@link TanEconomyStandalone} - Internal TAN economy (default)</li>
 *   <li>{@link TanEconomyExternal} - Wrapper for external Vault economies</li>
 *   <li>{@link TanEconomyVault} - Vault API provider for TAN's economy</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Economy operations are thread-safe. All implementations
 * handle concurrent access appropriately. However, for best performance in Folia,
 * consider using {@link org.leralix.tan.service.AsyncEconomyService} for non-blocking operations.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Get economy instance
 * AbstractTanEcon econ = EconomyUtil.getEconInstance();
 *
 * // Check balance
 * double balance = econ.getBalance(tanPlayer);
 *
 * // Check if player can afford something
 * if (econ.has(tanPlayer, 100.0)) {
 *     econ.withdrawPlayer(tanPlayer, 100.0);
 * }
 *
 * // Format money for display
 * String formatted = econ.formatMoney(1000.50); // "1000.50$"
 * }</pre>
 *
 * @see TanEconomyStandalone
 * @see TanEconomyExternal
 * @see EconomyUtil
 * @since 0.15.0
 */
public abstract class AbstractTanEcon {

  /**
   * Gets the current balance of a player.
   *
   * <p>Returns the player's current money balance. The balance is stored
   * in the player's ITanPlayer data and persists across server restarts.</p>
   *
   * <p><b>Thread Safety:</b> This method is thread-safe and can be called concurrently.</p>
   *
   * @param tanPlayer the player to query balance for
   * @return the player's current balance, may be negative
   * @see #has(ITanPlayer, double)
   * @see #withdrawPlayer(ITanPlayer, double)
   * @see #depositPlayer(ITanPlayer, double)
   */
  public abstract double getBalance(ITanPlayer tanPlayer);

  /**
   * Checks if a player has at least the specified amount.
   *
   * <p>This is a convenience method for checking if a player can afford a purchase
   * or payment. It uses strict greater-than comparison ({@code >}), not
   * greater-than-or-equal ({@code >=}).</p>
   *
   * <p><b>Note:</b> A player with exactly the specified amount will return {@code false}.
   * Use {@code getBalance(player) >= amount} for inclusive comparison.</p>
   *
   * @param tanPlayer the player to check
   * @param amount the amount to check for
   * @return true if the player's balance is greater than the specified amount
   * @see #getBalance(ITanPlayer)
   */
  public abstract boolean has(ITanPlayer tanPlayer, double amount);

  /**
   * Withdraws money from a player's balance.
   *
   * <p>Deducts the specified amount from the player's balance. This method
   * allows negative balances (overdraft) by default. Implementations may
   * choose to enforce minimum balance requirements.</p>
   *
   * <p><b>Thread Safety:</b> This method is thread-safe. Multiple concurrent
   * withdrawals will be serialized appropriately.</p>
   *
   * @param tanPlayer the player to withdraw from
   * @param amount the amount to withdraw (must be positive)
   * @see #depositPlayer(ITanPlayer, double)
   * @see #getBalance(ITanPlayer)
   */
  public abstract void withdrawPlayer(ITanPlayer tanPlayer, double amount);

  /**
   * Deposits money into a player's balance.
   *
   * <p>Adds the specified amount to the player's balance. The balance persists
   * automatically and will be available after server restart.</p>
   *
   * <p><b>Thread Safety:</b> This method is thread-safe. Multiple concurrent
   * deposits will be serialized appropriately.</p>
   *
   * @param tanPlayer the player to deposit to
   * @param amount the amount to deposit (must be positive)
   * @see #withdrawPlayer(ITanPlayer, double)
   * @see #getBalance(ITanPlayer)
   */
  public abstract void depositPlayer(ITanPlayer tanPlayer, double amount);

  /**
   * Gets the currency icon/symbol used by this economy.
   *
   * <p>Returns the currency symbol used for formatting money amounts.
   * For standalone TAN economy, this is typically "$" or a configured symbol.
   * For external economies, this returns the external plugin's currency name.</p>
   *
   * @return the currency icon or symbol, never null
   * @see #formatMoney(double)
   */
  public abstract String getMoneyIcon();

  /**
   * Formats a money amount with the currency icon.
   *
   * <p>Formats the specified amount as a string with the currency symbol appended.
   * The amount is formatted to 2 decimal places. Currency placement varies by
   * implementation (prefix or suffix).</p>
   *
   * <h3>Examples:</h3>
   * <ul>
   *   <li>Standalone: "1000.00$" (currency suffix)</li>
   *   <li>External: "1000.00 Coins" (external plugin format)</li>
   * </ul>
   *
   * @param amount The amount to format
   * @return Formatted string with currency symbol (e.g., "$100.00" or "100.00 Coins")
   * @see #getMoneyIcon()
   */
  public abstract String formatMoney(double amount);
}