package org.leralix.tan.economy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.leralix.tan.dataclass.ITanPlayer;

public class TanEconomyExternal extends AbstractTanEcon {

    private static final Logger LOGGER = Logger.getLogger(TanEconomyExternal.class.getName());

    private final Economy externalEconomy;

    private Method getBalanceWithWorldMethod;
    private Method withdrawWithWorldMethod;
    private Method depositWithWorldMethod;

    private boolean useMultiCurrency = false;

    public TanEconomyExternal(Economy externalEconomy) {
        super();
        this.externalEconomy = externalEconomy;

        LOGGER.info("[TAN-ECON] Initializing external economy via Vault: " + externalEconomy.getName());

        try {
            getBalanceWithWorldMethod = Economy.class.getMethod(
                "getBalance",
                OfflinePlayer.class,
                String.class
            );
            withdrawWithWorldMethod = Economy.class.getMethod(
                "withdrawPlayer",
                OfflinePlayer.class,
                double.class,
                String.class
            );
            depositWithWorldMethod = Economy.class.getMethod(
                "depositPlayer",
                OfflinePlayer.class,
                double.class,
                String.class
            );

            useMultiCurrency = true;
            LOGGER.info("[TAN-ECON] Multi-currency support ENABLED via Vault reflection");
        } catch (NoSuchMethodException e) {
            useMultiCurrency = false;
            LOGGER.info("[TAN-ECON] Multi-currency methods not available, using standard Vault API");
        }

        LOGGER.info("[TAN-ECON] Currency: " + externalEconomy.currencyNamePlural());
    }

    private String getCurrencyName(String currencyId) {
        if (currencyId == null) {
            CurrencyConfig defaultCurrency = CurrencyConfig.getDefaultCurrency();
            return defaultCurrency != null ? defaultCurrency.getZessentialsCurrency() : "default";
        }

        CurrencyConfig currency = CurrencyConfig.getCurrency(currencyId);
        if (currency == null) {
            LOGGER.warning("[TAN-ECON] Currency not found: " + currencyId + ", using default");
            CurrencyConfig defaultCurrency = CurrencyConfig.getDefaultCurrency();
            return defaultCurrency != null ? defaultCurrency.getZessentialsCurrency() : "default";
        }

        return currency.getZessentialsCurrency();
    }

    @Override
    public double getBalance(ITanPlayer tanPlayer) {
        return getBalance(tanPlayer, null);
    }

    public double getBalance(ITanPlayer tanPlayer, String currencyId) {
        UUID uuid = UUID.fromString(tanPlayer.getID());
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

        if (useMultiCurrency && currencyId != null) {
            try {
                String currencyName = getCurrencyName(currencyId);
                Double balance = (Double) getBalanceWithWorldMethod.invoke(
                    externalEconomy,
                    offlinePlayer,
                    currencyName
                );
                LOGGER.fine("[TAN-ECON] getBalance(" + tanPlayer.getNameStored() +
                           ", currency=" + currencyId + " -> " + currencyName + ") = " + balance);
                return balance;
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.log(Level.WARNING,
                    "[TAN-ECON] Failed to get balance with currency, falling back to default", e);
            }
        }

        return externalEconomy.getBalance(offlinePlayer);
    }

    @Override
    public boolean has(ITanPlayer tanPlayer, double amount) {
        return has(tanPlayer, amount, null);
    }

    public boolean has(ITanPlayer tanPlayer, double amount, String currencyId) {
        return getBalance(tanPlayer, currencyId) >= amount;
    }

    @Override
    public void withdrawPlayer(ITanPlayer tanPlayer, double amount) {
        withdrawPlayer(tanPlayer, amount, null);
    }

    public void withdrawPlayer(ITanPlayer tanPlayer, double amount, String currencyId) {
        UUID uuid = UUID.fromString(tanPlayer.getID());
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

        if (useMultiCurrency && currencyId != null) {
            try {
                String currencyName = getCurrencyName(currencyId);
                withdrawWithWorldMethod.invoke(
                    externalEconomy,
                    offlinePlayer,
                    amount,
                    currencyName
                );
                LOGGER.fine("[TAN-ECON] withdraw(" + tanPlayer.getNameStored() +
                           ", amount=" + amount + ", currency=" + currencyId + " -> " + currencyName + ")");
                return;
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.log(Level.WARNING,
                    "[TAN-ECON] Failed to withdraw with currency, falling back to default", e);
            }
        }

        externalEconomy.withdrawPlayer(offlinePlayer, amount);
    }

    @Override
    public void depositPlayer(ITanPlayer tanPlayer, double amount) {
        depositPlayer(tanPlayer, amount, null);
    }

    public void depositPlayer(ITanPlayer tanPlayer, double amount, String currencyId) {
        UUID uuid = UUID.fromString(tanPlayer.getID());
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

        if (useMultiCurrency && currencyId != null) {
            try {
                String currencyName = getCurrencyName(currencyId);
                depositWithWorldMethod.invoke(
                    externalEconomy,
                    offlinePlayer,
                    amount,
                    currencyName
                );
                LOGGER.fine("[TAN-ECON] deposit(" + tanPlayer.getNameStored() +
                           ", amount=" + amount + ", currency=" + currencyId + " -> " + currencyName + ")");
                return;
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.log(Level.WARNING,
                    "[TAN-ECON] Failed to deposit with currency, falling back to default", e);
            }
        }

        externalEconomy.depositPlayer(offlinePlayer, amount);
    }

    @Override
    public String getMoneyIcon() {
        return externalEconomy.currencyNameSingular();
    }

    @Override
    public String formatMoney(double amount) {
        return String.format("%.2f %s", amount, externalEconomy.currencyNamePlural());
    }
}
