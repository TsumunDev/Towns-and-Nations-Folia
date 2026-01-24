package org.leralix.tan.economy;

import org.bukkit.configuration.ConfigurationSection;
import org.leralix.tan.utils.constants.Constants;

import java.util.*;
import java.util.logging.Logger;

/**
 * Configuration for multi-currency support in Towns and Nations.
 * 
 * <p>This class manages the mapping between TAN internal currencies and
 * ZEssentials currencies through the Vault economy API.</p>
 */
public class CurrencyConfig {
    
    private static final Logger LOGGER = Logger.getLogger(CurrencyConfig.class.getName());
    
    private final String id;
    private final String displayName;
    private final String zessentialsCurrency;
    private final boolean isDefault;
    
    private CurrencyConfig(String id, String displayName, String zessentialsCurrency, boolean isDefault) {
        this.id = id;
        this.displayName = displayName;
        this.zessentialsCurrency = zessentialsCurrency;
        this.isDefault = isDefault;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getZessentialsCurrency() {
        return zessentialsCurrency;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    // Singleton instance
    private static Map<String, CurrencyConfig> currencies = new HashMap<>();
    private static CurrencyConfig defaultCurrency;
    
    /**
     * Loads currency configuration from the main config.yml.
     */
    public static void loadCurrencies() {
        currencies.clear();
        
        ConfigurationSection currencySection = Constants.getMainConfig()
            .getConfigurationSection("currencies");
        
        if (currencySection == null) {
            LOGGER.warning("[TAN-CURRENCY] No 'currencies' section found in config.yml. Using default configuration.");
            loadDefaultCurrency();
            return;
        }
        
        for (String currencyId : currencySection.getKeys(false)) {
            ConfigurationSection currencyConfig = currencySection.getConfigurationSection(currencyId);
            
            String displayName = currencyConfig.getString("display-name", currencyId);
            String zessentialsCurrency = currencyConfig.getString("zessentials-currency", currencyId);
            boolean isDefault = currencyConfig.getBoolean("default", false);
            
            CurrencyConfig currency = new CurrencyConfig(
                currencyId,
                displayName,
                zessentialsCurrency,
                isDefault
            );
            
            currencies.put(currencyId.toLowerCase(), currency);
            
            if (isDefault) {
                if (defaultCurrency != null) {
                    LOGGER.warning("[TAN-CURRENCY] Multiple default currencies defined! Using: " + currencyId);
                }
                defaultCurrency = currency;
            }
            
            LOGGER.info("[TAN-CURRENCY] Loaded currency: " + currencyId + 
                       " → ZEssentials: " + zessentialsCurrency + 
                       (isDefault ? " (DEFAULT)" : ""));
        }
        
        if (defaultCurrency == null) {
            LOGGER.warning("[TAN-CURRENCY] No default currency defined! Using first currency.");
            if (!currencies.isEmpty()) {
                defaultCurrency = currencies.values().iterator().next();
            }
        }
        
        LOGGER.info("[TAN-CURRENCY] Loaded " + currencies.size() + " currencies. Default: " + 
                   (defaultCurrency != null ? defaultCurrency.getId() : "NONE"));
    }
    
    /**
     * Loads a default currency configuration when none is defined.
     */
    private static void loadDefaultCurrency() {
        CurrencyConfig defaultCur = new CurrencyConfig(
            "cocos",
            "Cocos",
            "default",
            true
        );
        
        currencies.put("cocos", defaultCur);
        defaultCurrency = defaultCur;
        
        LOGGER.info("[TAN-CURRENCY] Loaded default currency: cocos → ZEssentials: default");
    }
    
    /**
     * Gets a currency by ID.
     * 
     * @param currencyId The currency ID (case-insensitive)
     * @return The currency configuration, or null if not found
     */
    public static CurrencyConfig getCurrency(String currencyId) {
        if (currencyId == null) {
            return defaultCurrency;
        }
        return currencies.get(currencyId.toLowerCase());
    }
    
    /**
     * Gets the default currency.
     * 
     * @return The default currency configuration
     */
    public static CurrencyConfig getDefaultCurrency() {
        return defaultCurrency;
    }
    
    /**
     * Gets all registered currencies.
     * 
     * @return Unmodifiable map of currencies
     */
    public static Map<String, CurrencyConfig> getAllCurrencies() {
        return Collections.unmodifiableMap(currencies);
    }
    
    /**
     * Checks if a currency exists.
     * 
     * @param currencyId The currency ID to check
     * @return true if the currency exists
     */
    public static boolean hasCurrency(String currencyId) {
        return currencies.containsKey(currencyId.toLowerCase());
    }
}
