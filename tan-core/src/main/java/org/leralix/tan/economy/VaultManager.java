package org.leralix.tan.economy;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.utils.constants.Constants;
public class VaultManager {
  public static void setupVault() {
    AbstractTanEcon tanEcon;
    Logger logger = Bukkit.getLogger();

    // Priority 1: Check if user wants standalone economy
    if (Constants.useStandaloneEconomy()) {
      TanEconomyVault tanEconomyVault = new TanEconomyVault();
      EconomyUtil.register(tanEconomyVault);
      Bukkit.getServicesManager()
          .register(
              Economy.class, tanEconomyVault, TownsAndNations.getPlugin(), ServicePriority.Normal);
      logger.log(Level.INFO, "[TaN] Using standalone TAN economy (configured in config.yml)");
      CurrencyConfig.loadCurrencies();
      return;
    }

    // Priority 2: Check for Vault economy (ZEssentials, Essentials, etc.)
    logger.log(Level.INFO, "[TaN] Checking for Vault economy provider...");
    RegisteredServiceProvider<Economy> rsp =
        Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp != null) {
      tanEcon = new TanEconomyExternal(rsp.getProvider());
      EconomyUtil.register(tanEcon);
      logger.log(Level.INFO, "[TaN] Vault economy detected: {0}", rsp.getProvider().getName());
      logger.log(Level.INFO, "[TaN] Initializing multi-currency configuration...");
      CurrencyConfig.loadCurrencies();
      logger.log(Level.INFO, "[TaN] Economy initialization complete");
      return;
    }

    // Priority 3: No economy found, use standalone
    logger.log(Level.INFO, "[TaN] No Vault economy found. Using standalone TAN economy.");
    EconomyUtil.register(new TanEconomyStandalone());
    CurrencyConfig.loadCurrencies();
  }
}
