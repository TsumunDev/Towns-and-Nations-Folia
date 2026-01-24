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
      logger.log(Level.INFO, "[TaN] -Vault is detected, registering TaN Economy");
      return;
    }

    // Priority 2: Check for CurrenciesAPI + zEssentials (direct integration, bypasses Vault)
    if (TanEconomyZessentials.isCurrenciesApiAvailable()) {
      try {
        tanEcon = new TanEconomyZessentials();
        EconomyUtil.register(tanEcon);
        logger.log(Level.INFO, "[TaN] -CurrenciesAPI detected, using zEssentials economy directly");
        return;
      } catch (Exception e) {
        logger.log(
            Level.WARNING,
            "[TaN] -CurrenciesAPI available but zEssentials integration failed: {0}. Falling back to Vault.",
            e.getMessage());
      }
    }

    // Priority 3: Check for Vault economy (traditional integration)
    RegisteredServiceProvider<Economy> rsp =
        Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp != null) {
      tanEcon = new TanEconomyExternal(rsp.getProvider());
      EconomyUtil.register(tanEcon);
      logger.log(
          Level.INFO,
          "[TaN] -Vault is detected, using {0} as economy",
          rsp.getProvider().getName());
      return;
    }

    // Priority 4: No economy found, use standalone
    logger.log(
        Level.INFO,
        "[TaN] -No active vault economy. Running standalone and waiting for potential update");
    EconomyUtil.register(new TanEconomyStandalone());
  }
}