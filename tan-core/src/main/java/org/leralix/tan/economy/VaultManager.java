package org.leralix.tan.economy;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.constants.Constants;
public class VaultManager {
  private static final Logger LOGGER = Logger.getLogger(VaultManager.class.getName());

  public static void setupVault() {
    // Priority 1: Check if user wants standalone economy
    if (Constants.useStandaloneEconomy()) {
      TanEconomyVault tanEconomyVault = new TanEconomyVault();
      EconomyUtil.register(tanEconomyVault);
      Bukkit.getServicesManager()
          .register(
              Economy.class, tanEconomyVault, TownsAndNations.getPlugin(), ServicePriority.Normal);
      LOGGER.info("[CocoNation] Using standalone TAN economy (configured in config.yml)");
      CurrencyConfig.loadCurrencies();
      return;
    }

    // Priority 2: Try to hook Vault economy (ZEssentials, Essentials, etc.)
    // Deferred with retries — ZEssentials may register after CocoNation onEnable
    FoliaScheduler.runTaskLater(TownsAndNations.getPlugin(), () -> setupVaultWithRetry(5), 1L);
  }

  private static void setupVaultWithRetry(int attemptsLeft) {
    if (Constants.useStandaloneEconomy()) return;

    LOGGER.info("[CocoNation] Checking for Vault economy provider...");
    RegisteredServiceProvider<Economy> rsp =
        Bukkit.getServer().getServicesManager().getRegistration(Economy.class);

    if (rsp != null) {
      AbstractTanEcon tanEcon = new TanEconomyExternal(rsp.getProvider());
      EconomyUtil.register(tanEcon);
      LOGGER.info("[CocoNation] Economy provider found: " + rsp.getProvider().getName());
      CurrencyConfig.loadCurrencies();
      return;
    }

    if (attemptsLeft > 0) {
      LOGGER.warning("[CocoNation] Economy provider not found, retrying in 1 tick... (" + attemptsLeft + " attempts left)");
      FoliaScheduler.runTaskLater(TownsAndNations.getPlugin(), () -> setupVaultWithRetry(attemptsLeft - 1), 1L);
    } else {
      LOGGER.severe("[CocoNation] No Vault economy provider found after all retries! Is ZEssentials running?");
      LOGGER.severe("[CocoNation] Falling back to standalone economy. Town purchases may not work with external money.");
      EconomyUtil.register(new TanEconomyStandalone());
      CurrencyConfig.loadCurrencies();
    }
  }
}
