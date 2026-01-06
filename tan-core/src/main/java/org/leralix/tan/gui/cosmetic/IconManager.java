package org.leralix.tan.gui.cosmetic;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.leralix.lib.utils.config.ConfigUtil;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.gui.cosmetic.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class IconManager {
  private static final Logger logger = LoggerFactory.getLogger(IconManager.class);
  private static volatile IconManager instance;
  Map<IconKey, IconType> iconMap;
  private IconManager() {
    logger.info("[IconManager] Initializing IconManager...");
    this.iconMap = new EnumMap<>(IconKey.class);
    Plugin plugin = TownsAndNations.getPlugin();
    logger.info("[IconManager] Plugin instance obtained");
    ConfigUtil.saveAndUpdateResource(plugin, "menu/icons.yml");
    YamlConfiguration config =
        YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "menu/icons.yml"));
    logger.info("[IconManager] icons.yml loaded, {} keys found", config.getKeys(false).size());
    int successCount = 0;
    int errorCount = 0;
    int skippedCount = 0;
    for (String key : config.getKeys(false)) {
      IconKey iconKey = null;
      try {
        iconKey = IconKey.valueOf(key);
      } catch (IllegalArgumentException e) {
        plugin.getLogger().warning("Unknown IconKey: " + key + " - skipping");
        skippedCount++;
        continue;
      }
      try {
        String value = config.getString(key);
        logger.debug("[IconManager] Loading icon '{}': {}", key, value);
        IconType menuIcon = chooseIconBuilderType(value);
        iconMap.put(iconKey, menuIcon);
        successCount++;
        logger.debug("[IconManager] Successfully loaded icon: {}", key);
      } catch (Exception e) {
        errorCount++;
        plugin.getLogger().log(Level.SEVERE, "Error creating icon for key '" + key + "' (" + config.getString(key) + "), using BARRIER fallback", e);
        iconMap.put(iconKey, new ItemIconBuilder(Material.BARRIER));
      }
    }
    logger.info("[IconManager] Initialization complete: {} success, {} errors, {} skipped", successCount, errorCount, skippedCount);
  }
  public static IconManager getInstance() {
    IconManager localInstance = instance;
    if (localInstance == null) {
      synchronized (IconManager.class) {
        localInstance = instance;
        if (localInstance == null) {
          instance = localInstance = new IconManager();
        }
      }
    }
    return localInstance;
  }
  IconType chooseIconBuilderType(String value) {
    if (value.startsWith("http")) {
      return new UrlHeadIconType(value);
    }
    if (value.startsWith("nexo:")) {
      NexoIconType nexoIcon = NexoIconType.parse(value);
      if (nexoIcon != null) {
        return nexoIcon;
      }
      TownsAndNations.getPlugin()
          .getLogger()
          .log(Level.WARNING, "Invalid Nexo icon format: {0}", value);
      return new ItemIconBuilder(Material.BARRIER);
    }
    if (value.startsWith("minecraft:")) {
      String[] args = value.split(":");
      if (args.length <= 1) {
        TownsAndNations.getPlugin()
            .getLogger()
            .log(Level.WARNING, "Invalid name for item : {0}", value);
        return new ItemIconBuilder(Material.BARRIER);
      }
      Material iconMaterial;
      try {
        iconMaterial = Material.valueOf(args[1]);
      } catch (IllegalArgumentException e) {
        TownsAndNations.getPlugin()
            .getLogger()
            .log(Level.WARNING, "Invalid material in config : {0}", args[1]);
        iconMaterial = Material.BARRIER;
      }
      if (args.length >= 3) {
        int customModelData;
        try {
          customModelData = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
          TownsAndNations.getPlugin()
              .getLogger()
              .log(Level.WARNING, "Invalid custom model data for menu icon : {0}", args[2]);
          customModelData = 0;
        }
        return new CustomMaterialIcon(iconMaterial, customModelData);
      } else {
        return new ItemIconBuilder(iconMaterial);
      }
    }
    return switch (value) {
      case "PLAYER_HEAD" -> new PlayerHeadIconType();
      case "TOWN_HEAD" -> new TownIconType();
      case "PLAYER_LANGUAGE_HEAD" -> new PlayerLanguageIconType();
      case "NO_ICON" -> new NoIconType();
      default -> new UrlHeadIconType("");
    };
  }
  public IconBuilder get(IconKey key) {
    IconType iconType = iconMap.get(key);
    if (iconType == null) {
      logger.error("[IconManager] IconKey '{}' not found in iconMap! Available keys: {}", key, iconMap.keySet());
      return new IconBuilder(new ItemIconBuilder(Material.BARRIER));
    }
    logger.debug("[IconManager] Returning icon for key: {}", key);
    return new IconBuilder(iconType);
  }
  public IconBuilder get(ItemStack icon) {
    return new IconBuilder(new ItemIconType(icon));
  }
  public IconBuilder get(Material material) {
    return get(new ItemStack(material));
  }
  public static void reload() {
    logger.info("[IconManager] Reloading icons configuration...");
    synchronized (IconManager.class) {
      try {
        Class<?> nexoIntegrationClass = Class.forName("org.leralix.tan.integration.nexo.NexoIntegration");
        java.lang.reflect.Method clearCacheMethod = nexoIntegrationClass.getMethod("clearCache");
        clearCacheMethod.invoke(null);
        logger.info("[IconManager] Nexo cache cleared");
      } catch (Exception e) {
        logger.debug("[IconManager] Could not clear Nexo cache (integration may not be enabled): {}", e.getMessage());
      }
      instance = new IconManager();
      logger.info("[IconManager] Reload complete!");
    }
  }
}