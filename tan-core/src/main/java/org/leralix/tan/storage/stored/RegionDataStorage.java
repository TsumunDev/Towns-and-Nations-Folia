package org.leralix.tan.storage.stored;
import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.cosmetic.ICustomIcon;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.typeadapter.EnumMapDeserializer;
import org.leralix.tan.storage.typeadapter.IconAdapter;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.file.FileUtil;
public class RegionDataStorage extends DatabaseStorage<RegionData> {
  private static final Set<String> knownColumns = ConcurrentHashMap.newKeySet();
  private static final String TABLE_NAME = "ccn_regions";
  private AtomicInteger nextID;
  private static volatile RegionDataStorage instance;
  public static RegionDataStorage getInstance() {
    if (instance == null) {
      synchronized (RegionDataStorage.class) {
        if (instance == null) {
          instance = new RegionDataStorage();
        }
      }
    }
    return instance;
  }
  private RegionDataStorage() {
    super(
        TABLE_NAME,
        RegionData.class,
        new GsonBuilder()
            .registerTypeAdapter(
                new TypeToken<Map<TownRelation, List<String>>>() {}.getType(),
                new EnumMapDeserializer<>(
                    TownRelation.class, new TypeToken<List<String>>() {}.getType()))
            .registerTypeAdapter(ICustomIcon.class, new IconAdapter())
            .setPrettyPrinting()
            .create());
    loadNextID();
  }
  @Override
  protected void createTable() {
    String createTableSQL =
        """
            CREATE TABLE IF NOT EXISTS %s (
                id VARCHAR(255) PRIMARY KEY,
                data TEXT NOT NULL
            )
        """
            .formatted(TABLE_NAME);
    try (Connection conn = getDatabase().getDataSource().getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(createTableSQL);
      try (ResultSet rs = conn.getMetaData().getColumns(null, null, TABLE_NAME, "region_name")) {
        if (!rs.next()) {
          stmt.executeUpdate(
              "ALTER TABLE %s ADD COLUMN region_name VARCHAR(255) NULL".formatted(TABLE_NAME));
          TownsAndNations.getPlugin().getLogger().info("Added region_name column to " + TABLE_NAME);
        }
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe("Error creating table " + TABLE_NAME + ": " + e.getMessage());
    }
  }
  @Override
  protected void createIndexes() {
    String createNameIndexSQL =
        "CREATE INDEX IF NOT EXISTS idx_region_name ON " + TABLE_NAME + " (region_name)";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(createNameIndexSQL);
      TownsAndNations.getPlugin()
          .getLogger()
          .info("Created index idx_region_name on " + TABLE_NAME);
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("Error creating indexes for " + TABLE_NAME + ": " + e.getMessage());
    }
  }
  @Override
  public void put(String id, RegionData obj) {
    if (id == null || obj == null) {
      return;
    }
    String jsonData = gson.toJson(obj, typeToken);

    // Check if new columns exist (v2.0 schema)
    boolean useNewSchema = columnExists("members_count");

    String upsertSQL;
    if (getDatabase().isMySQL()) {
      if (useNewSchema) {
        // New schema with all columns
        upsertSQL =
            "INSERT INTO "
                + tableName
                + " (id, region_name, leader_uuid, leader_name, capital_id, members_count, data) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "region_name = VALUES(region_name), "
                + "leader_uuid = VALUES(leader_uuid), "
                + "leader_name = VALUES(leader_name), "
                + "capital_id = VALUES(capital_id), "
                + "members_count = VALUES(members_count), "
                + "data = VALUES(data)";
      } else {
        // Legacy schema (pre-v2.0)
        upsertSQL =
            "INSERT INTO "
                + tableName
                + " (id, region_name, data) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), data = VALUES(data)";
      }
    } else {
      if (useNewSchema) {
        // New schema with all columns (SQLite)
        upsertSQL =
            "INSERT OR REPLACE INTO "
                + tableName
                + " (id, region_name, leader_uuid, leader_name, capital_id, members_count, data) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
      } else {
        // Legacy schema (pre-v2.0)
        upsertSQL =
            "INSERT OR REPLACE INTO " + tableName + " (id, region_name, data) VALUES (?, ?, ?)";
      }
    }

    FoliaScheduler.runTaskAsynchronously(
        TownsAndNations.getPlugin(),
        () -> {
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(upsertSQL)) {
            int paramIndex = 1;
            ps.setString(paramIndex++, id);
            ps.setString(paramIndex++, obj.getName());

            if (useNewSchema) {
              // Get additional data for new columns
              String leaderUuid = obj.getLeaderID();
              String leaderName = null;
              String capitalId = null;
              int membersCount = 1;

              try {
                // Get leader name
                org.leralix.tan.dataclass.ITanPlayer leader = obj.getLeaderData();
                if (leader != null) {
                  leaderName = leader.getNameStored();
                }

                // Get capital info
                org.leralix.tan.dataclass.territory.TerritoryData capital = obj.getCapital();
                if (capital != null) {
                  capitalId = capital.getID();
                }

                // Get members count (towns in region)
                membersCount = obj.getSubjects().size();
              } catch (Exception e) {
                // Data not available, use defaults
              }

              ps.setString(paramIndex++, leaderUuid);
              ps.setString(paramIndex++, leaderName);
              ps.setString(paramIndex++, capitalId);
              ps.setInt(paramIndex++, membersCount);
            }

            ps.setString(paramIndex, jsonData);
            ps.executeUpdate();

            if (cacheEnabled && cache != null) {
              // Thread-safe: ConcurrentHashMap provides lock-free writes
              cache.put(id, obj);
            }
          } catch (SQLException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .severe(
                    "Error storing "
                        + typeClass.getSimpleName()
                        + " with ID "
                        + id
                        + ": "
                        + e.getMessage());
          }
        });
  }

  /**
   * Check if a column exists in the table.
   */
  private boolean columnExists(String columnName) {
    if (knownColumns.contains(columnName)) {
      return true;
    }
    try (Connection conn = getDatabase().getDataSource().getConnection()) {
      ResultSet rs = conn.getMetaData().getColumns(null, null, TABLE_NAME, columnName);
      boolean exists = rs.next();
      rs.close();
      if (exists) {
        knownColumns.add(columnName);
      }
      return exists;
    } catch (SQLException e) {
      return false;
    }
  }
  private void loadNextID() {
    nextID = new AtomicInteger(getDatabase().getNextRegionId());
  }
  public CompletableFuture<RegionData> createNewRegion(String name, TownData capital) {
    ITanPlayer newLeader = capital.getLeaderData();
    String regionID = generateNextID();
    RegionData newRegion = new RegionData(regionID, name, newLeader);
    put(regionID, newRegion);
    capital.setOverlord(newRegion);
    FileUtil.addLineToHistory(Lang.REGION_CREATED_NEWSLETTER.get(newLeader.getNameStored(), name));
    return CompletableFuture.completedFuture(newRegion);
  }
  private @NotNull String generateNextID() {
    int id = nextID.getAndIncrement();
    getDatabase().updateNextRegionId(nextID.get());
    return "R" + id;
  }
  public CompletableFuture<RegionData> get(Player player) {
    return PlayerDataStorage.getInstance().get(player).thenCompose(this::get);
  }
  public CompletableFuture<RegionData> get(ITanPlayer tanPlayer) {
    return TownDataStorage.getInstance()
        .get(tanPlayer)
        .thenCompose(
            town -> {
              if (town == null) return CompletableFuture.completedFuture(null);
              return CompletableFuture.completedFuture(town.getRegionSync());
            });
  }
  @Deprecated
  public void deleteRegion(RegionData region) {
    deleteAsync(region.getID()).join();
  }

  /**
   * Deletes a region asynchronously.
   * @param region the region to delete
   * @return CompletableFuture that completes when the region is deleted
   */
  public CompletableFuture<Void> deleteRegionAsync(RegionData region) {
    return deleteAsync(region.getID());
  }
  public boolean isNameUsed(String name) {
    if (name == null) {
      return false;
    }
    String selectSQL =
        "SELECT 1 FROM " + TABLE_NAME + " WHERE json_extract(data, '$.name') = ? LIMIT 1";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(selectSQL)) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("json_extract not supported, falling back to full scan: " + e.getMessage());
      for (RegionData region : getAllSync().values()) {
        if (name.equals(region.getName())) return true;
      }
    }
    return false;
  }
  public RegionData getSync(Player player) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    if (tanPlayer == null) return null;
    return getSync(tanPlayer);
  }
  @Override
  public void reset() {
    synchronized (RegionDataStorage.class) {
      instance = null;
    }
  }
  public RegionData getSync(String id) {
    if (cacheEnabled && cache != null) {
      RegionData cached = cache.get(id);
      if (cached != null) {
        return cached;
      }
    }
    get(id)
        .thenAccept(
            region -> {
              if (region != null && cacheEnabled && cache != null) {
                cache.put(id, region);
              }
            });
    return null;
  }
  @Deprecated
  public RegionData getSync(ITanPlayer tanPlayer) {
    try {
      return get(tanPlayer).join();
    } catch (Exception e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("Error getting region data synchronously: " + e.getMessage());
      return null;
    }
  }

  /**
   * Gets a region by its name asynchronously using SQL query with index.
   * This is much more efficient than loading all regions and filtering.
   *
   * @param name The region name to search for (case-insensitive)
   * @return CompletableFuture that completes with the region, or null if not found
   */
  public CompletableFuture<RegionData> getByName(String name) {
    if (name == null || name.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    CompletableFuture<RegionData> future = new CompletableFuture<>();

    // First try to use the indexed region_name column
    String selectSQL = "SELECT data FROM " + TABLE_NAME + " WHERE region_name = ? LIMIT 1";
    runAsync(
        () -> {
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(selectSQL)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
              if (rs.next()) {
                String jsonData = rs.getString("data");
                RegionData region = gson.fromJson(jsonData, RegionData.class);
                if (region != null && cacheEnabled && cache != null) {
                  cache.put(region.getID(), region);
                }
                future.complete(region);
                return;
              }
            }
          } catch (SQLException e) {
            // Fall through to JSON extraction if column doesn't exist
          }

          // Fallback: Try json_extract on the data column
          String fallbackSQL = "SELECT data FROM " + TABLE_NAME + " WHERE json_extract(data, '$.name') = ? LIMIT 1";
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(fallbackSQL)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
              if (rs.next()) {
                String jsonData = rs.getString("data");
                RegionData region = gson.fromJson(jsonData, RegionData.class);
                if (region != null && cacheEnabled && cache != null) {
                  cache.put(region.getID(), region);
                }
                future.complete(region);
                return;
              }
            }
          } catch (SQLException e2) {
            // Both methods failed, log and continue to full scan
          }

          // Last resort: Full scan with case-insensitive comparison
          String fullScanSQL = "SELECT id, data FROM " + TABLE_NAME;
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(fullScanSQL);
              ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              String jsonData = rs.getString("data");
              try {
                RegionData region = gson.fromJson(jsonData, RegionData.class);
                if (region != null && region.getName().equalsIgnoreCase(name)) {
                  if (cacheEnabled && cache != null) {
                    cache.put(region.getID(), region);
                  }
                  future.complete(region);
                  return;
                }
              } catch (com.google.gson.JsonSyntaxException e) {
                // Skip invalid entries
              }
            }
            future.complete(null);
          } catch (SQLException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .warning("Error getting region by name: " + e.getMessage());
            future.complete(null);
          }
        });
    return future;
  }

  /**
   * Gets a region by its name synchronously (uses cache if available).
   *
   * @param name The region name to search for
   * @return The region, or null if not found
   */
  @Deprecated
  public RegionData getByNameSync(String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }
    // Check cache first
    if (cacheEnabled && cache != null) {
      for (RegionData region : cache.values()) {
        if (region.getName().equalsIgnoreCase(name)) {
          return region;
        }
      }
    }
    return getByName(name).join();
  }
}