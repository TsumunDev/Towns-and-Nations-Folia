package org.leralix.tan.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;

public class EconomyUtil {

  private static AbstractTanEcon econ;

  public static void register(AbstractTanEcon newEcon) {
    econ = newEcon;
  }

  public static boolean isStandalone() {
    return econ instanceof TanEconomyStandalone;
  }

  public static AbstractTanEcon getEconInstance() {
    return econ;
  }

  /**
   * Gets the default currency ID from CurrencyConfig.
   * Returns null if not configured (will use economy default).
   */
  private static String getDefaultCurrencyId() {
    CurrencyConfig defaultCurrency = CurrencyConfig.getDefaultCurrency();
    return defaultCurrency != null ? defaultCurrency.getId() : null;
  }

  // ==================== STANDARD METHODS (use default currency) ====================

  public static double getBalance(OfflinePlayer offlinePlayer) {
    return getBalance(offlinePlayer, getDefaultCurrencyId());
  }

  public static double getBalance(ITanPlayer player) {
    return getBalance(player, getDefaultCurrencyId());
  }

  public static double getBalance(Player player) {
    return getBalance(player, getDefaultCurrencyId());
  }

  public static void removeFromBalance(ITanPlayer tanPlayer, double amount) {
    withdrawPlayer(tanPlayer, amount, getDefaultCurrencyId());
  }

  public static void removeFromBalance(OfflinePlayer offlinePlayer, double amount) {
    withdrawPlayer(PlayerDataStorage.getInstance().getSync(offlinePlayer), amount, getDefaultCurrencyId());
  }

  public static void removeFromBalance(Player player, double amount) {
    withdrawPlayer(PlayerDataStorage.getInstance().getSync(player), amount, getDefaultCurrencyId());
  }

  public static void addFromBalance(ITanPlayer player, double amount) {
    depositPlayer(player, amount, getDefaultCurrencyId());
  }

  public static void addFromBalance(Player player, double amount) {
    depositPlayer(PlayerDataStorage.getInstance().getSync(player), amount, getDefaultCurrencyId());
  }

  public static void addFromBalance(OfflinePlayer offlinePlayer, double amount) {
    depositPlayer(PlayerDataStorage.getInstance().getSync(offlinePlayer), amount, getDefaultCurrencyId());
  }

  public static String getMoneyIcon() {
    return econ.getMoneyIcon();
  }

  public static void setBalance(ITanPlayer target, double amount) {
    removeFromBalance(target, getBalance(target));
    addFromBalance(target, amount);
  }

  public static String formatMoney(double amount) {
    return econ.formatMoney(amount);
  }

  public static void addToBalance(Player player, double amount) {
    addFromBalance(player, amount);
  }

  public static void addToBalance(ITanPlayer tanPlayer, double amount) {
    addFromBalance(tanPlayer, amount);
  }

  public static void addToBalance(OfflinePlayer offlinePlayer, double amount) {
    addFromBalance(offlinePlayer, amount);
  }

  // ==================== MULTI-CURRENCY METHODS ====================

  /**
   * Gets balance for a specific currency.
   * @param currencyId The currency ID (null for economy default)
   */
  public static double getBalance(OfflinePlayer offlinePlayer, String currencyId) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(offlinePlayer);
    return getBalance(tanPlayer, currencyId);
  }

  public static double getBalance(ITanPlayer player, String currencyId) {
    if (econ instanceof TanEconomyExternal && currencyId != null) {
      return ((TanEconomyExternal) econ).getBalance(player, currencyId);
    }
    return econ.getBalance(player);
  }

  public static double getBalance(Player player, String currencyId) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    return getBalance(tanPlayer, currencyId);
  }

  /**
   * Withdraws a specific currency from player.
   */
  private static void withdrawPlayer(ITanPlayer tanPlayer, double amount, String currencyId) {
    if (econ instanceof TanEconomyExternal && currencyId != null) {
      ((TanEconomyExternal) econ).withdrawPlayer(tanPlayer, amount, currencyId);
    } else {
      econ.withdrawPlayer(tanPlayer, amount);
    }
  }

  /**
   * Deposits a specific currency to player.
   */
  private static void depositPlayer(ITanPlayer tanPlayer, double amount, String currencyId) {
    if (econ instanceof TanEconomyExternal && currencyId != null) {
      ((TanEconomyExternal) econ).depositPlayer(tanPlayer, amount, currencyId);
    } else {
      econ.depositPlayer(tanPlayer, amount);
    }
  }

  // ==================== CURRENCY-SPECIFIC PUBLIC METHODS ====================

  /**
   * Checks if player has enough of a specific currency.
   */
  public static boolean has(ITanPlayer tanPlayer, double amount, String currencyId) {
    return getBalance(tanPlayer, currencyId) >= amount;
  }

  /**
   * Withdraws a specific currency from player (public API).
   */
  public static void withdrawCurrency(ITanPlayer tanPlayer, double amount, String currencyId) {
    withdrawPlayer(tanPlayer, amount, currencyId);
  }

  /**
   * Deposits a specific currency to player (public API).
   */
  public static void depositCurrency(ITanPlayer tanPlayer, double amount, String currencyId) {
    depositPlayer(tanPlayer, amount, currencyId);
  }
}
