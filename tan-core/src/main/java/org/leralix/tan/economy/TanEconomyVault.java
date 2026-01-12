package org.leralix.tan.economy;
import java.util.Collections;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.constants.Constants;
/**
 * Vault economy integration for Towns and Nations.
 *
 * <p><b>IMPORTANT - Blocking I/O Warning:</b></p>
 * <p>This implementation uses blocking {@code getSync()} calls internally to comply with
 * Vault's synchronous API contract. These methods will block the calling thread:</p>
 * <ul>
 *   <li>{@link #getBalance(OfflinePlayer)} - Blocks to load player data</li>
 *   <li>{@link #withdrawPlayer(OfflinePlayer, double)} - Blocks to load player data</li>
 *   <li>{@link #depositPlayer(OfflinePlayer, double)} - Blocks to load player data</li>
 * </ul>
 *
 * <p><b>Recommendations:</b></p>
 * <ul>
 *   <li>Avoid calling Vault methods on region threads in Folia</li>
 *   <li>Use {@link org.leralix.tan.service.AsyncEconomyService} for new code instead</li>
 *   <li>Consider caching balance values if frequent access is needed</li>
 * </ul>
 *
 * <p><b>Why Not Async?</b></p>
 * <p>Vault's API interface ({@link Economy}) is synchronous by definition. All methods must
 * return immediately (not {@code CompletableFuture}), so we cannot make them truly async
 * without breaking the Vault API contract and compatibility with other plugins.</p>
 *
 * @see org.leralix.tan.service.AsyncEconomyService for non-blocking economy operations
 */
public class TanEconomyVault extends TanEconomyStandalone implements Economy {
  public TanEconomyVault() {
    super();
  }
  @Override
  public boolean isEnabled() {
    return true;
  }
  @Override
  public String getName() {
    return "Towns and Nations Economy";
  }
  @Override
  public boolean hasBankSupport() {
    return false;
  }
  @Override
  public int fractionalDigits() {
    return Constants.getNbDigits();
  }
  @Override
  public String format(double v) {
    return String.valueOf(v);
  }
  @Override
  public String currencyNamePlural() {
    return Constants.getBaseCurrencyChar();
  }
  @Override
  public String currencyNameSingular() {
    return Constants.getBaseCurrencyChar();
  }
  @Override
  public boolean hasAccount(String s) {
    return true;
  }
  @Override
  public boolean hasAccount(OfflinePlayer offlinePlayer) {
    return true;
  }
  @Override
  public boolean hasAccount(String s, String s1) {
    return true;
  }
  @Override
  public boolean hasAccount(OfflinePlayer offlinePlayer, String s) {
    return true;
  }
  @Override
  public double getBalance(String s) {
    return getBalance(Bukkit.getOfflinePlayer(s));
  }
  /**
   * Gets a player's balance (BLOCKING - loads from storage).
   *
   * <p><b>Warning:</b> This method blocks the calling thread while loading player data from storage.
   * Avoid calling on Folia region threads. Use {@code AsyncEconomyService.getBalance()} instead.</p>
   *
   * @param offlinePlayer The player to query
   * @return The player's balance
   */
  @Override
  @SuppressWarnings("deprecation") // Using getSync() is necessary for Vault API compatibility
  public double getBalance(OfflinePlayer offlinePlayer) {
    return super.getBalance(PlayerDataStorage.getInstance().getSync(offlinePlayer));
  }
  @Override
  public double getBalance(String s, String s1) {
    return getBalance(s);
  }
  @Override
  public double getBalance(OfflinePlayer offlinePlayer, String s) {
    return getBalance(offlinePlayer);
  }
  @Override
  public boolean has(String s, double v) {
    return getBalance(s) >= v;
  }
  @Override
  public boolean has(OfflinePlayer offlinePlayer, double v) {
    return getBalance(offlinePlayer) > v;
  }
  @Override
  public boolean has(String s, String s1, double v) {
    return getBalance(s) > v;
  }
  @Override
  public boolean has(OfflinePlayer offlinePlayer, String s, double v) {
    return getBalance(offlinePlayer) > v;
  }
  @Override
  public EconomyResponse withdrawPlayer(String s, double v) {
    return withdrawPlayer(Bukkit.getOfflinePlayer(s), v);
  }
  /**
   * Withdraws funds from a player's balance (BLOCKING - loads from storage).
   *
   * <p><b>Warning:</b> This method blocks the calling thread while loading player data from storage.
   * Avoid calling on Folia region threads. Use {@code AsyncEconomyService.withdraw()} instead.</p>
   *
   * @param offlinePlayer The player to withdraw from
   * @param v The amount to withdraw
   * @return EconomyResponse indicating success or failure
   */
  @Override
  @SuppressWarnings("deprecation") // Using getSync() is necessary for Vault API compatibility
  public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double v) {
    if (v < 0)
      return new EconomyResponse(
          0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds");
    if (!has(offlinePlayer, v))
      return new EconomyResponse(
          v,
          PlayerDataStorage.getInstance().getSync(offlinePlayer).getBalance(),
          EconomyResponse.ResponseType.FAILURE,
          "Player does not have enough money");
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(offlinePlayer);
    tanPlayer.removeFromBalance((int) v);
    return new EconomyResponse(v, tanPlayer.getBalance(), EconomyResponse.ResponseType.SUCCESS, "");
  }
  @Override
  public EconomyResponse withdrawPlayer(String s, String s1, double v) {
    return withdrawPlayer(Bukkit.getOfflinePlayer(s), v);
  }
  @Override
  public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, double v) {
    return withdrawPlayer(offlinePlayer, v);
  }
  @Override
  public EconomyResponse depositPlayer(String s, double v) {
    return depositPlayer(Bukkit.getOfflinePlayer(s), v);
  }
  /**
   * Deposits funds to a player's balance (BLOCKING - loads from storage).
   *
   * <p><b>Warning:</b> This method blocks the calling thread while loading player data from storage.
   * Avoid calling on Folia region threads. Use {@code AsyncEconomyService.deposit()} instead.</p>
   *
   * @param offlinePlayer The player to deposit to
   * @param v The amount to deposit
   * @return EconomyResponse indicating success
   */
  @Override
  @SuppressWarnings("deprecation") // Using getSync() is necessary for Vault API compatibility
  public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double v) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(offlinePlayer);
    tanPlayer.addToBalance((int) v);
    return new EconomyResponse(v, tanPlayer.getBalance(), EconomyResponse.ResponseType.SUCCESS, "");
  }
  @Override
  public EconomyResponse depositPlayer(String s, String s1, double v) {
    return depositPlayer(Bukkit.getOfflinePlayer(s), v);
  }
  @Override
  public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double v) {
    return depositPlayer(offlinePlayer, v);
  }
  @Override
  public EconomyResponse createBank(String s, String s1) {
    return null;
  }
  @Override
  public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
    return null;
  }
  @Override
  public EconomyResponse deleteBank(String s) {
    return null;
  }
  @Override
  public EconomyResponse bankBalance(String s) {
    return null;
  }
  @Override
  public EconomyResponse bankHas(String s, double v) {
    return null;
  }
  @Override
  public EconomyResponse bankWithdraw(String s, double v) {
    return null;
  }
  @Override
  public EconomyResponse bankDeposit(String s, double v) {
    return null;
  }
  @Override
  public EconomyResponse isBankOwner(String s, String s1) {
    return null;
  }
  @Override
  public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
    return null;
  }
  @Override
  public EconomyResponse isBankMember(String s, String s1) {
    return null;
  }
  @Override
  public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
    return null;
  }
  @Override
  public List<String> getBanks() {
    return Collections.emptyList();
  }
  @Override
  public boolean createPlayerAccount(String s) {
    return false;
  }
  @Override
  public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
    return false;
  }
  @Override
  public boolean createPlayerAccount(String s, String s1) {
    return false;
  }
  @Override
  public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s) {
    return false;
  }
}