package org.leralix.tan.tasks;

import org.leralix.tan.TownsAndNations;

public class SaveStats {

  private SaveStats() {
    throw new IllegalStateException("Utility class");
  }

  public static void startSchedule() {
    // Note: With DatabaseStorage, auto-save is no longer needed
    // Each put() operation writes directly to the database
    // This schedule is kept for compatibility but does nothing
    TownsAndNations.getPlugin()
        .getLogger()
        .info(
            "SaveStats: Using DatabaseStorage - auto-save on every operation, no periodic save needed");
  }

  public static void saveAll() {
<<<<<<< Updated upstream
    // Note: With DatabaseStorage, this method is obsolete
    // All data is already persisted in the database on every put() call
    // No action needed - keeping method for backward compatibility
    TownsAndNations.getPlugin()
        .getLogger()
        .fine(
            "SaveStats.saveAll() called but not needed with DatabaseStorage (auto-saves on every operation)");
=======
    TownsAndNations plugin = TownsAndNations.getPlugin();
    long startTime = System.currentTimeMillis();

    plugin.getLogger().info("[TaN-AutoSave] Starting automatic save of all data...");

    try {
      int townCount = saveTowns();

      int regionCount = saveRegions();

      int playerCount = savePlayers();

      long duration = System.currentTimeMillis() - startTime;

      plugin
          .getLogger()
          .info(
              String.format(
                  "[TaN-AutoSave] ✓ Saved %d towns, %d regions, %d players in %dms",
                  townCount, regionCount, playerCount, duration));
    } catch (Exception e) {
      plugin.getLogger().severe("[TaN-AutoSave] ✗ Error during auto-save: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static int saveTowns() {
    try {
      TownDataStorage storage = TownDataStorage.getInstance();
      java.util.Map<String, TownData> allTowns = storage.getAllSync();

      for (java.util.Map.Entry<String, TownData> entry : allTowns.entrySet()) {
        storage.putAsync(entry.getKey(), entry.getValue());
      }

      return allTowns.size();
    } catch (Exception e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("[TaN-AutoSave] Error saving towns: " + e.getMessage());
      return 0;
    }
  }

  private static int saveRegions() {
    try {
      RegionDataStorage storage = RegionDataStorage.getInstance();
      java.util.Map<String, RegionData> allRegions = storage.getAllSync();

      for (java.util.Map.Entry<String, RegionData> entry : allRegions.entrySet()) {
        storage.putAsync(entry.getKey(), entry.getValue());
      }

      return allRegions.size();
    } catch (Exception e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("[TaN-AutoSave] Error saving regions: " + e.getMessage());
      return 0;
    }
  }

  private static int savePlayers() {
    try {
      PlayerDataStorage storage = PlayerDataStorage.getInstance();
      int saved = 0;

      for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
        try {
          String uuid = player.getUniqueId().toString();
          ITanPlayer tanPlayer = storage.get(uuid).join();
          if (tanPlayer != null) {
            storage.putAsync(uuid, tanPlayer);
            saved++;
          }
        } catch (Exception e) {
        }
      }

      return saved;
    } catch (Exception e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("[TaN-AutoSave] Error saving players: " + e.getMessage());
      return 0;
    }
>>>>>>> Stashed changes
  }
}
