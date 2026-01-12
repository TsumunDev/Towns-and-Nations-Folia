package org.leralix.tan.storage.migration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.RankData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.storage.typeadapter.IconAdapter;
import org.leralix.tan.storage.typeadapter.OwnerDeserializer;
import org.leralix.tan.storage.typeadapter.PropertyDataDeserializer;
import org.leralix.tan.dataclass.property.AbstractOwner;

/**
 * Migrates legacy icon data (materialTypeName + customModelData) to new base64 format.
 *
 * <p>This utility ensures backward compatibility while transitioning from the old icon
 * storage format to the new base64 serialization format introduced in version 0.15.1.</p>
 *
 * <p><b>Migration Strategy:</b></p>
 * <ol>
 *   <li>Scan all JSON data files for legacy icon fields</li>
 *   <li>Load objects using custom Gson with legacy field support</li>
 *   <li>Trigger migration by calling getIcon() to populate base64Item</li>
 *   <li>Save objects back with new format (base64Item only)</li>
 *   <li>Backup original files before modification</li>
 * </ol>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * IconDataMigrator migrator = new IconDataMigrator(plugin);
 * migrator.migrateAll();
 * }</pre>
 *
 * @since 0.15.1
 */
public class IconDataMigrator {

  private final Plugin plugin;
  private final Gson legacyGson;
  private final Gson newGson;
  private int migratedCount = 0;
  private int errorCount = 0;

  /**
   * Creates a new IconDataMigrator with configured Gson instances.
   *
   * @param plugin The plugin instance
   */
  public IconDataMigrator(Plugin plugin) {
    this.plugin = plugin;

    // Gson that can read legacy format (materialTypeName + customModelData)
    this.legacyGson = new GsonBuilder()
        .registerTypeAdapter(AbstractOwner.class, new OwnerDeserializer())
        .registerTypeAdapter(org.leralix.tan.dataclass.territory.cosmetic.ICustomIcon.class, new IconAdapter())
        .setPrettyPrinting()
        .create();

    // Gson that only writes new format (base64Item)
    this.newGson = new GsonBuilder()
        .registerTypeAdapter(AbstractOwner.class, new OwnerDeserializer())
        .registerTypeAdapter(org.leralix.tan.dataclass.territory.cosmetic.ICustomIcon.class, new IconAdapter())
        .setPrettyPrinting()
        .create();
  }

  /**
   * Migrates all icon data in the plugin's data directory.
   *
   * <p>This method processes:
   * <ul>
   *   <li>Property data files (properties/)</li>
   *   <li>Town data files (towns/)</li>
   *   <li>Region data files (regions/)</li>
   *   <li>Rank data (ranks.json)</li>
   * </ul>
   *
   * @return true if migration completed without errors, false otherwise
   */
  public boolean migrateAll() {
    plugin.getLogger().info("Starting icon data migration...");
    long startTime = System.currentTimeMillis();

    try {
      // Migrate properties
      migrateDirectory(new File(plugin.getDataFolder(), "properties"), PropertyData.class);

      // Migrate towns (if they have custom icons)
      migrateDirectory(new File(plugin.getDataFolder(), "towns"), TerritoryData.class);

      // Migrate regions (if they have custom icons)
      migrateDirectory(new File(plugin.getDataFolder(), "regions"), TerritoryData.class);

      // Migrate ranks
      migrateRanks();

      long duration = System.currentTimeMillis() - startTime;
      plugin.getLogger().info(String.format(
          "Icon data migration completed: %d files migrated, %d errors (%.2fs)",
          migratedCount, errorCount, duration / 1000.0));

      return errorCount == 0;

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Fatal error during icon data migration", e);
      return false;
    }
  }

  /**
   * Migrates all JSON files in a directory.
   *
   * @param directory The directory to scan
   * @param type The type of objects to deserialize
   * @param <T> The type parameter
   */
  private <T> void migrateDirectory(File directory, Class<T> type) {
    if (!directory.exists() || !directory.isDirectory()) {
      plugin.getLogger().fine("Skipping migration for non-existent directory: " + directory.getPath());
      return;
    }

    File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      plugin.getLogger().fine("No JSON files found in: " + directory.getPath());
      return;
    }

    plugin.getLogger().info("Migrating " + files.length + " files in " + directory.getName() + "...");

    for (File file : files) {
      try {
        migrateFile(file, type);
      } catch (Exception e) {
        errorCount++;
        plugin.getLogger().log(Level.WARNING,
            "Error migrating file: " + file.getName(), e);
      }
    }
  }

  /**
   * Migrates a single JSON file.
   *
   * @param file The file to migrate
   * @param type The type of object to deserialize
   * @param <T> The type parameter
   * @throws IOException If file operations fail
   */
  private <T> void migrateFile(File file, Class<T> type) throws IOException {
    // Read raw JSON to check if migration is needed
    JsonObject jsonObject = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

    // Check if file has legacy icon fields
    boolean needsMigration = hasLegacyIconFields(jsonObject);

    if (!needsMigration) {
      plugin.getLogger().fine("Skipping " + file.getName() + " (already migrated or no icons)");
      return;
    }

    // Create backup
    createBackup(file);

    // Load object with legacy Gson (can read both old and new format)
    try (FileReader reader = new FileReader(file)) {
      T object = legacyGson.fromJson(reader, type);

      // Trigger migration by accessing icon
      if (object instanceof PropertyData propertyData) {
        if (propertyData.getIcon() != null) {
          propertyData.getIcon().getIcon(); // Triggers migration in getIcon()
        }
      } else if (object instanceof TerritoryData territoryData) {
        if (territoryData.getIcon() != null) {
          territoryData.getIcon().getIcon(); // Triggers migration
        }
      } else if (object instanceof RankData rankData) {
        if (rankData.getRankIcon() != null) {
          rankData.getRankIcon().getIcon(); // Triggers migration
        }
      }

      // Save with new Gson (writes only base64Item format)
      try (FileWriter writer = new FileWriter(file)) {
        newGson.toJson(object, writer);
      }

      migratedCount++;
      plugin.getLogger().fine("Migrated: " + file.getName());
    }
  }

  /**
   * Checks if a JSON object contains legacy icon fields.
   *
   * @param jsonObject The JSON object to check
   * @return true if legacy fields are present
   */
  private boolean hasLegacyIconFields(JsonObject jsonObject) {
    // Check for legacy icon fields at various nesting levels
    return hasNestedField(jsonObject, "materialTypeName")
        || hasNestedField(jsonObject, "customModelData");
  }

  /**
   * Recursively checks if a field exists anywhere in the JSON structure.
   *
   * @param element The JSON element to search
   * @param fieldName The field name to look for
   * @return true if the field is found
   */
  private boolean hasNestedField(com.google.gson.JsonElement element, String fieldName) {
    if (element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();
      if (obj.has(fieldName)) {
        return true;
      }
      for (String key : obj.keySet()) {
        if (hasNestedField(obj.get(key), fieldName)) {
          return true;
        }
      }
    } else if (element.isJsonArray()) {
      for (com.google.gson.JsonElement item : element.getAsJsonArray()) {
        if (hasNestedField(item, fieldName)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Migrates the ranks.json file.
   */
  private void migrateRanks() {
    File ranksFile = new File(plugin.getDataFolder(), "ranks.json");
    if (!ranksFile.exists()) {
      plugin.getLogger().fine("No ranks.json file found");
      return;
    }

    try {
      migrateFile(ranksFile, RankData.class);
    } catch (Exception e) {
      errorCount++;
      plugin.getLogger().log(Level.WARNING, "Error migrating ranks.json", e);
    }
  }

  /**
   * Creates a backup of a file before migration.
   *
   * @param file The file to backup
   * @throws IOException If backup fails
   */
  private void createBackup(File file) throws IOException {
    Path backupPath = Path.of(file.getPath() + ".backup." + System.currentTimeMillis());
    Files.copy(file.toPath(), backupPath);
    plugin.getLogger().fine("Created backup: " + backupPath.getFileName());
  }

  /**
   * Gets the number of successfully migrated files.
   *
   * @return The migration count
   */
  public int getMigratedCount() {
    return migratedCount;
  }

  /**
   * Gets the number of errors encountered during migration.
   *
   * @return The error count
   */
  public int getErrorCount() {
    return errorCount;
  }
}
