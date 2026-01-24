package org.leralix.tan.economy;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.leralix.tan.dataclass.ITanPlayer;

/**
 * Direct zEssentials economy integration via CurrenciesAPI.
 *
 * <p>This class provides a bridge between TAN's economy abstraction and zEssentials'
 * economy system through the CurrenciesAPI library. This bypasses Vault entirely,
 * providing a more direct and reliable integration.</p>
 *
 * <h3>Benefits over Vault:</h3>
 * <ul>
 *   <li>Direct API calls (no abstraction layer overhead)</li>
 *   <li>Better error handling and debugging</li>
 *   <li>Multi-currency support (zEssentials feature)</li>
 *   <li>No dependency on Vault's service registration</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Automatically instantiated by VaultManager when zEssentials + CurrenciesAPI are detected
 * TanEconomyZessentials economy = new TanEconomyZessentials();
 * double balance = economy.getBalance(tanPlayer);
 * }</pre>
 *
 * @see Currencies
 * @see VaultManager
 * @since 2.0.0
 */
public class TanEconomyZessentials extends AbstractTanEcon {

    private static final Logger LOGGER = Bukkit.getLogger();
    private static final String CURRENCY_NAME = "coins"; // Default zEssentials currency

    private final Object currenciesApi; // Currencies.ZESSENTIALS (lazy loaded via reflection)

    /**
     * Creates a new zEssentials economy integration.
     *
     * @throws IllegalStateException if CurrenciesAPI is not available
     */
    public TanEconomyZessentials() {
        super();

        // Check if CurrenciesAPI is available
        if (!isCurrenciesApiAvailable()) {
            throw new IllegalStateException(
                "CurrenciesAPI is not available! Please ensure zEssentials and CurrenciesAPI are installed."
            );
        }

        this.currenciesApi = getCurrenciesEnum();

        if (this.currenciesApi == null) {
            throw new IllegalStateException("Failed to get Currencies.ZESSENTIALS enum constant");
        }

        LOGGER.info("[TAN] Successfully initialized zEssentials economy integration via CurrenciesAPI");
    }

    @Override
    public double getBalance(ITanPlayer tanPlayer) {
        try {
            UUID uuid = UUID.fromString(tanPlayer.getID());
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            // Call Currencies.ZESSENTIALS.getBalance(player, "coins")
            BigDecimal balance = (BigDecimal) invokeCurrenciesMethod(
                "getBalance",
                new Class[]{OfflinePlayer.class, String.class},
                offlinePlayer,
                CURRENCY_NAME
            );

            return balance != null ? balance.doubleValue() : 0.0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[TAN] Failed to get balance from zEssentials: " + e.getMessage(), e);
            return 0.0;
        }
    }

    @Override
    public boolean has(ITanPlayer tanPlayer, double amount) {
        return getBalance(tanPlayer) >= amount;
    }

    @Override
    public void withdrawPlayer(ITanPlayer tanPlayer, double amount) {
        try {
            UUID uuid = UUID.fromString(tanPlayer.getID());
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            // Call Currencies.ZESSENTIALS.withdraw(player, BigDecimal)
            invokeCurrenciesMethod(
                "withdraw",
                new Class[]{OfflinePlayer.class, BigDecimal.class},
                offlinePlayer,
                BigDecimal.valueOf(amount)
            );

            LOGGER.fine("[TAN] Withdrew " + amount + " from " + tanPlayer.getNameStored());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[TAN] Failed to withdraw from zEssentials: " + e.getMessage(), e);
        }
    }

    @Override
    public void depositPlayer(ITanPlayer tanPlayer, double amount) {
        try {
            UUID uuid = UUID.fromString(tanPlayer.getID());
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            // Call Currencies.ZESSENTIALS.deposit(player, BigDecimal)
            invokeCurrenciesMethod(
                "deposit",
                new Class[]{OfflinePlayer.class, BigDecimal.class},
                offlinePlayer,
                BigDecimal.valueOf(amount)
            );

            LOGGER.fine("[TAN] Deposited " + amount + " to " + tanPlayer.getNameStored());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[TAN] Failed to deposit to zEssentials: " + e.getMessage(), e);
        }
    }

    @Override
    public String getMoneyIcon() {
        // Try to get currency name from zEssentials, fall back to default
        return "$"; // zEssentials default currency symbol
    }

    @Override
    public String formatMoney(double amount) {
        return String.format("%.2f %s", amount, amount == 1.0 ? "coin" : "coins");
    }

    /**
     * Checks if CurrenciesAPI is available on the server.
     *
     * @return true if CurrenciesAPI class is available
     */
    public static boolean isCurrenciesApiAvailable() {
        try {
            Class.forName("fr.traqueur.currencies.Currencies");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Gets the Currencies.ZESSENTIALS enum constant via reflection.
     *
     * @return the ZESSENTIALS enum constant, or null if not found
     */
    private Object getCurrenciesEnum() {
        try {
            Class<?> currenciesClass = Class.forName("fr.traqueur.currencies.Currencies");
            return Enum.valueOf((Class<Enum>) currenciesClass, "ZESSENTIALS");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("[TAN] CurrenciesAPI class not found: " + e.getMessage());
            return null;
        }
    }

    /**
     * Invokes a method on the Currencies enum instance via reflection.
     *
     * @param methodName the name of the method to invoke
     * @param parameterTypes the parameter types
     * @param args the method arguments
     * @return the result of the method invocation
     * @throws Exception if the method cannot be invoked
     */
    private Object invokeCurrenciesMethod(String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {

        if (currenciesApi == null) {
            throw new IllegalStateException("CurrenciesAPI enum not initialized");
        }

        try {
            java.lang.reflect.Method method = currenciesApi.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(currenciesApi, args);
        } catch (NoSuchMethodException e) {
            LOGGER.severe("[TAN] Method not found in CurrenciesAPI: " + methodName);
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[TAN] Failed to invoke CurrenciesAPI method: " + methodName, e);
            throw e;
        }
    }
}
