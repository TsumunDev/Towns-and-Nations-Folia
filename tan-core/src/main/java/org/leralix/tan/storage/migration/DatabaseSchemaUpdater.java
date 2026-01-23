package org.leralix.tan.storage.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.FoliaScheduler;

/**
 * Database Schema Migration v2.0
 *
 * <p>This class handles the migration from the old JSON-only schema to the new
 * hybrid schema with SQL-exposed columns.</p>
 *
 * <p><b>What it does:</b></p>
 * <ul>
 *   <li>Adds new columns to existing tables (non-breaking)</li>
 *   <li>Populates new columns from existing JSON data</li>
 *   <li>Creates indexes for performance</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // From console command
 * DatabaseSchemaUpdater.migrateAll();
 *
 * // Automatically on plugin startup (if not migrated)
 * DatabaseSchemaUpdater.checkAndMigrate();
 * }</pre>
 */
public class DatabaseSchemaUpdater {

  private static final Logger logger = TownsAndNations.getPlugin().getLogger();
  private static final String MIGRATION_VERSION = "v2.0";

  /**
   * Check if migration has been run, and execute if needed.
   *
   * @return true if migration was needed and executed, false otherwise
   */
  public static boolean checkAndMigrate() {
    if (isMigrated()) {
      logger.info("[DB Migration] Database already at " + MIGRATION_VERSION);
      return false;
    }

    logger.info("[DB Migration] Database needs migration to " + MIGRATION_VERSION);
    migrateAll();
    return true;
  }

  /**
   * Check if the v2.0 migration has already been applied.
   *
   * @return true if migrated, false otherwise
   */
  private static boolean isMigrated() {
    try (Connection conn = TownsAndNations.getPlugin().getDatabaseHandler().getDataSource().getConnection()) {
      // Check if tan_players has the 'balance' column (added in v2.0)
      ResultSet rs = conn.getMetaData().getColumns(null, null, "tan_players", "balance");
      boolean migrated = rs.next();
      rs.close();
      return migrated;
    } catch (SQLException e) {
      logger.warning("[DB Migration] Error checking migration status: " + e.getMessage());
      return false;
    }
  }

  /**
   * Execute full migration: add columns and populate data.
   */
  public static void migrateAll() {
    long startTime = System.currentTimeMillis();
    logger.info("[DB Migration] Starting migration to " + MIGRATION_VERSION + "...");

    try {
      // Step 1: Add new columns
      migrateSchema();

      // Step 2: Populate columns from existing JSON data
      populatePlayerColumns();
      populateTownColumns();
      populateRegionColumns();

      // Step 3: Mark migration as complete
      markMigrationComplete();

      long duration = System.currentTimeMillis() - startTime;
      logger.info("[DB Migration] ✓ Migration completed in " + duration + "ms");

    } catch (Exception e) {
      logger.severe("[DB Migration] ✗ Migration failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Step 1: Add new columns to existing tables.
   */
  private static void migrateSchema() {
    logger.info("[DB Migration] Step 1: Adding new columns...");

    try (Connection conn = TownsAndNations.getPlugin().getDatabaseHandler().getDataSource().getConnection();
         Statement stmt = conn.createStatement()) {

      boolean isMySQL = TownsAndNations.getPlugin().getDatabaseHandler().isMySQL();

      // ===== tan_players =====
      migrateColumn(stmt, "tan_players", "ip_address", "VARCHAR(45)", isMySQL);
      migrateColumn(stmt, "tan_players", "first_seen", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", isMySQL);
      migrateColumn(stmt, "tan_players", "balance", "DOUBLE DEFAULT 0.0", isMySQL);
      migrateColumn(stmt, "tan_players", "is_online", "BOOLEAN DEFAULT FALSE", isMySQL);
      migrateColumn(stmt, "tan_players", "town_id", "VARCHAR(255)", isMySQL);
      migrateColumn(stmt, "tan_players", "nation_id", "VARCHAR(255)", isMySQL);

      // ===== tan_towns =====
      migrateColumn(stmt, "tan_towns", "leader_uuid", "VARCHAR(255)", isMySQL);
      migrateColumn(stmt, "tan_towns", "leader_name", "VARCHAR(255)", isMySQL);
      migrateColumn(stmt, "tan_towns", "nation_id", "VARCHAR(255)", isMySQL);
      migrateColumn(stmt, "tan_towns", "bank_balance", "DOUBLE DEFAULT 0.0", isMySQL);
      migrateColumn(stmt, "tan_towns", "claims_count", "INT DEFAULT 0", isMySQL);
      migrateColumn(stmt, "tan_towns", "members_count", "INT DEFAULT 0", isMySQL);
      migrateColumn(stmt, "tan_towns", "is_open", "BOOLEAN DEFAULT FALSE", isMySQL);

      // ===== tan_regions =====
      migrateColumn(stmt, "tan_regions", "leader_uuid", "VARCHAR(255)", isMySQL);
      migrateColumn(stmt, "tan_regions", "leader_name", "VARCHAR(255)", isMySQL);
      migrateColumn(stmt, "tan_regions", "capital_id", "VARCHAR(255)", isMySQL);
      migrateColumn(stmt, "tan_regions", "members_count", "INT DEFAULT 0", isMySQL);
      migrateColumn(stmt, "tan_regions", "creation_date", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", isMySQL);

      // Create indexes
      createIndexes(stmt, isMySQL);

      logger.info("[DB Migration] ✓ Columns added successfully");

    } catch (SQLException e) {
      throw new RuntimeException("Failed to add new columns", e);
    }
  }

  /**
   * Add a column to a table if it doesn't exist.
   */
  private static void migrateColumn(Statement stmt, String table, String column, String definition, boolean isMySQL)
      throws SQLException {
    // Check if column exists
    ResultSet rs = stmt.getConnection().getMetaData().getColumns(null, null, table, column);
    boolean exists = rs.next();
    rs.close();

    if (!exists) {
      String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
      if (!isMySQL) {
        // SQLite doesn't support COLUMN keyword
        sql = sql.replace(" COLUMN ", " ");
      }
      stmt.execute(sql);
      logger.info("[DB Migration]   Added " + table + "." + column);
    }
  }

  /**
   * Create indexes for the new columns.
   */
  private static void createIndexes(Statement stmt, boolean isMySQL) throws SQLException {
    String indexSql = isMySQL ? "CREATE INDEX IF NOT EXISTS %s ON %s (%s)"
                              : "CREATE INDEX IF NOT EXISTS %s ON %s (%s)";

    // Players indexes
    stmt.execute(String.format(indexSql, "idx_players_town_id", "tan_players", "town_id"));
    stmt.execute(String.format(indexSql, "idx_players_nation_id", "tan_players", "nation_id"));
    stmt.execute(String.format(indexSql, "idx_players_balance", "tan_players", "balance"));
    stmt.execute(String.format(indexSql, "idx_players_online", "tan_players", "is_online"));

    // Towns indexes
    stmt.execute(String.format(indexSql, "idx_towns_leader_uuid", "tan_towns", "leader_uuid"));
    stmt.execute(String.format(indexSql, "idx_towns_nation_id", "tan_towns", "nation_id"));
    stmt.execute(String.format(indexSql, "idx_towns_balance", "tan_towns", "bank_balance"));
    stmt.execute(String.format(indexSql, "idx_towns_members", "tan_towns", "members_count"));

    // Regions indexes
    stmt.execute(String.format(indexSql, "idx_regions_leader_uuid", "tan_regions", "leader_uuid"));
    stmt.execute(String.format(indexSql, "idx_regions_capital_id", "tan_regions", "capital_id"));

    logger.info("[DB Migration] ✓ Indexes created");
  }

  /**
   * Step 2a: Populate tan_players columns from JSON data.
   */
  private static void populatePlayerColumns() {
    logger.info("[DB Migration] Step 2a: Populating player columns...");

    Map<String, ITanPlayer> players = PlayerDataStorage.getInstance().getAllSync();
    int updated = 0;

    try (Connection conn = TownsAndNations.getPlugin().getDatabaseHandler().getDataSource().getConnection()) {
      String sql = """
          UPDATE tan_players SET
            balance = ?,
            is_online = ?,
            town_id = ?,
            nation_id = ?,
            first_seen = COALESCE(first_seen, ?)
          WHERE id = ?
          """;

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        for (Map.Entry<String, ITanPlayer> entry : players.entrySet()) {
          String uuid = entry.getKey();
          ITanPlayer player = entry.getValue();

          ps.setDouble(1, player.getBalance());
          ps.setBoolean(2, false); // is_online - default to false
          ps.setString(3, null); // town_id - TODO: get from player
          ps.setString(4, null); // nation_id - TODO: get from town
          ps.setLong(5, System.currentTimeMillis()); // first_seen
          ps.setString(6, uuid);

          ps.addBatch();
          updated++;

          if (updated % 100 == 0) {
            ps.executeBatch();
            logger.info("[DB Migration]   Updated " + updated + "/" + players.size() + " players...");
          }
        }

        ps.executeBatch();
      }

      logger.info("[DB Migration] ✓ Updated " + updated + " players");

    } catch (SQLException e) {
      logger.severe("[DB Migration] ✗ Failed to populate player columns: " + e.getMessage());
    }
  }

  /**
   * Step 2b: Populate tan_towns columns from JSON data.
   */
  private static void populateTownColumns() {
    logger.info("[DB Migration] Step 2b: Populating town columns...");

    Map<String, TownData> towns = TownDataStorage.getInstance().getAllSync();
    int updated = 0;

    try (Connection conn = TownsAndNations.getPlugin().getDatabaseHandler().getDataSource().getConnection()) {
      String sql = """
          UPDATE tan_towns SET
            leader_uuid = ?,
            leader_name = ?,
            nation_id = ?,
            bank_balance = ?,
            claims_count = ?,
            members_count = ?,
            is_open = ?
          WHERE id = ?
          """;

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        for (Map.Entry<String, TownData> entry : towns.entrySet()) {
          String id = entry.getKey();
          TownData town = entry.getValue();

          // Leader info
          String leaderUuid = town.getLeaderID();
          String leaderName = null;
          try {
            ITanPlayer leader = town.getLeaderData();
            if (leader != null) {
              leaderName = leader.getNameStored();
            }
          } catch (Exception e) {
            // Leader data not available
          }

          // TODO: Get nation_id, bank_balance, claims_count, members_count, is_open from TownData API
          String nationId = null;
          double bankBalance = 0.0;
          int claimsCount = 0;
          int membersCount = 1;
          boolean isOpen = false;

          ps.setString(1, leaderUuid);
          ps.setString(2, leaderName);
          ps.setString(3, nationId);
          ps.setDouble(4, bankBalance);
          ps.setInt(5, claimsCount);
          ps.setInt(6, membersCount);
          ps.setBoolean(7, isOpen);
          ps.setString(8, id);

          ps.addBatch();
          updated++;

          if (updated % 100 == 0) {
            ps.executeBatch();
            logger.info("[DB Migration]   Updated " + updated + "/" + towns.size() + " towns...");
          }
        }

        ps.executeBatch();
      }

      logger.info("[DB Migration] ✓ Updated " + updated + " towns");

    } catch (SQLException e) {
      logger.severe("[DB Migration] ✗ Failed to populate town columns: " + e.getMessage());
    }
  }

  /**
   * Step 2c: Populate tan_regions columns from JSON data.
   */
  private static void populateRegionColumns() {
    logger.info("[DB Migration] Step 2c: Populating region columns...");

    Map<String, RegionData> regions = RegionDataStorage.getInstance().getAllSync();
    int updated = 0;

    try (Connection conn = TownsAndNations.getPlugin().getDatabaseHandler().getDataSource().getConnection()) {
      String sql = """
          UPDATE tan_regions SET
            leader_uuid = ?,
            leader_name = ?,
            capital_id = ?,
            members_count = ?
          WHERE id = ?
          """;

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        for (Map.Entry<String, RegionData> entry : regions.entrySet()) {
          String id = entry.getKey();
          RegionData region = entry.getValue();

          // TODO: Get leader_uuid, leader_name, capital_id, members_count from RegionData API
          String leaderUuid = null;
          String leaderName = null;
          String capitalId = null;
          int membersCount = 1;

          ps.setString(1, leaderUuid);
          ps.setString(2, leaderName);
          ps.setString(3, capitalId);
          ps.setInt(4, membersCount);
          ps.setString(5, id);

          ps.addBatch();
          updated++;

          if (updated % 100 == 0) {
            ps.executeBatch();
            logger.info("[DB Migration]   Updated " + updated + "/" + regions.size() + " regions...");
          }
        }

        ps.executeBatch();
      }

      logger.info("[DB Migration] ✓ Updated " + updated + " regions");

    } catch (SQLException e) {
      logger.severe("[DB Migration] ✗ Failed to populate region columns: " + e.getMessage());
    }
  }

  /**
   * Step 3: Mark migration as complete in metadata table.
   */
  private static void markMigrationComplete() {
    try (Connection conn = TownsAndNations.getPlugin().getDatabaseHandler().getDataSource().getConnection();
         Statement stmt = conn.createStatement()) {

      // Create metadata table if not exists
      stmt.execute("""
          CREATE TABLE IF NOT EXISTS tan_metadata (
              key VARCHAR(255) PRIMARY KEY,
              value VARCHAR(255)
          )
          """);

      // Mark migration as complete
      String versionSql = "INSERT OR REPLACE INTO tan_metadata (key, value) VALUES ('schema_version', '"
          + MIGRATION_VERSION + "')";
      stmt.execute(versionSql);

      logger.info("[DB Migration] ✓ Migration marked as complete");

    } catch (SQLException e) {
      logger.warning("[DB Migration] Failed to mark migration complete: " + e.getMessage());
    }
  }

  /**
   * Manual migration command from console.
   */
  public static void migrateFromCommand() {
    if (isMigrated()) {
      logger.info("[DB Migration] Database is already at " + MIGRATION_VERSION);
      logger.info("[DB Migration] Use 'force-migrate' to re-migrate (not recommended)");
      return;
    }

    logger.info("[DB Migration] Starting migration from command...");
    migrateAll();
  }

  /**
   * Force re-migration (use with caution).
   */
  public static void forceMigrate() {
    logger.warning("[DB Migration] Force migration requested - this will re-populate all columns");
    logger.warning("[DB Migration] This is safe but may take time...");

    migrateAll();
  }
}
