package org.leralix.tan.storage.stored;
import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.property.AbstractOwner;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.cosmetic.ICustomIcon;
import org.leralix.tan.dataclass.territory.permission.RelationPermission;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.enums.permissions.ChunkPermissionType;
import org.leralix.tan.storage.typeadapter.EnumMapDeserializer;
import org.leralix.tan.storage.typeadapter.EnumMapKeyValueDeserializer;
import org.leralix.tan.storage.typeadapter.IconAdapter;
import org.leralix.tan.storage.typeadapter.OwnerDeserializer;
import org.leralix.tan.utils.FoliaScheduler;
public class TownDataStorage extends DatabaseStorage<TownData> {
  private static final String TABLE_NAME = "ccn_towns";
  private static TownDataStorage instance;
  private int newTownId;
  private TownDataStorage() {
    super(
        TABLE_NAME,
        TownData.class,
        new GsonBuilder()
            .registerTypeAdapter(
                new TypeToken<Map<ChunkPermissionType, RelationPermission>>() {}.getType(),
                new EnumMapKeyValueDeserializer<>(
                    ChunkPermissionType.class, RelationPermission.class))
            .registerTypeAdapter(
                new TypeToken<Map<TownRelation, List<String>>>() {}.getType(),
                new EnumMapDeserializer<>(
                    TownRelation.class, new TypeToken<List<String>>() {}.getType()))
            .registerTypeAdapter(
                new TypeToken<List<RelationPermission>>() {}.getType(),
                new EnumMapDeserializer<>(
                    RelationPermission.class, new TypeToken<List<String>>() {}.getType()))
            .registerTypeAdapter(AbstractOwner.class, new OwnerDeserializer())
            .registerTypeAdapter(ICustomIcon.class, new IconAdapter())
            .setPrettyPrinting()
            .create());
    loadNextTownId();
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
      try (ResultSet rs = conn.getMetaData().getColumns(null, null, TABLE_NAME, "town_name")) {
        if (!rs.next()) {
          stmt.executeUpdate(
              "ALTER TABLE %s ADD COLUMN town_name VARCHAR(255) NULL".formatted(TABLE_NAME));
          TownsAndNations.getPlugin().getLogger().info("Added town_name column to " + TABLE_NAME);
        }
      }
      try (ResultSet rs = conn.getMetaData().getColumns(null, null, TABLE_NAME, "creator_uuid")) {
        if (!rs.next()) {
          stmt.executeUpdate(
              "ALTER TABLE %s ADD COLUMN creator_uuid VARCHAR(255) NULL".formatted(TABLE_NAME));
          TownsAndNations.getPlugin()
              .getLogger()
              .info("Added creator_uuid column to " + TABLE_NAME);
        }
      }
      try (ResultSet rs = conn.getMetaData().getColumns(null, null, TABLE_NAME, "creator_name")) {
        if (!rs.next()) {
          stmt.executeUpdate(
              "ALTER TABLE %s ADD COLUMN creator_name VARCHAR(255) NULL".formatted(TABLE_NAME));
          TownsAndNations.getPlugin()
              .getLogger()
              .info("Added creator_name column to " + TABLE_NAME);
        }
      }
      try (ResultSet rs = conn.getMetaData().getColumns(null, null, TABLE_NAME, "creation_date")) {
        if (!rs.next()) {
          if (getDatabase().isMySQL()) {
            stmt.executeUpdate(
                "ALTER TABLE %s ADD COLUMN creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    .formatted(TABLE_NAME));
          } else {
            stmt.executeUpdate(
                "ALTER TABLE %s ADD COLUMN creation_date DATETIME DEFAULT CURRENT_TIMESTAMP"
                    .formatted(TABLE_NAME));
          }
          TownsAndNations.getPlugin()
              .getLogger()
              .info("Added creation_date column to " + TABLE_NAME);
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
        "CREATE INDEX IF NOT EXISTS idx_town_name ON " + TABLE_NAME + " (town_name)";
    String createCreatorUuidIndexSQL =
        "CREATE INDEX IF NOT EXISTS idx_town_creator_uuid ON " + TABLE_NAME + " (creator_uuid)";
    String createCreationDateIndexSQL =
        "CREATE INDEX IF NOT EXISTS idx_town_creation_date ON " + TABLE_NAME + " (creation_date)";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(createNameIndexSQL);
      stmt.execute(createCreatorUuidIndexSQL);
      stmt.execute(createCreationDateIndexSQL);
      TownsAndNations.getPlugin().getLogger().info("Created indexes on " + TABLE_NAME);
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("Error creating indexes for " + TABLE_NAME + ": " + e.getMessage());
    }
  }
  @Override
  public void put(String id, TownData obj) {
    if (id == null || obj == null) {
      return;
    }
    String jsonData = gson.toJson(obj, typeToken);

    // Check if new columns exist (v2.0 schema)
    boolean useNewSchema = columnExists("bank_balance");

    String upsertSQL;
    if (getDatabase().isMySQL()) {
      if (useNewSchema) {
        // New schema with all columns
        upsertSQL =
            "INSERT INTO "
                + tableName
                + " (id, town_name, creator_uuid, creator_name, leader_uuid, leader_name, nation_id, bank_balance, claims_count, members_count, is_open, data) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "town_name = VALUES(town_name), "
                + "creator_uuid = VALUES(creator_uuid), "
                + "creator_name = VALUES(creator_name), "
                + "leader_uuid = VALUES(leader_uuid), "
                + "leader_name = VALUES(leader_name), "
                + "nation_id = VALUES(nation_id), "
                + "bank_balance = VALUES(bank_balance), "
                + "claims_count = VALUES(claims_count), "
                + "members_count = VALUES(members_count), "
                + "is_open = VALUES(is_open), "
                + "data = VALUES(data)";
      } else {
        // Legacy schema (pre-v2.0)
        upsertSQL =
            "INSERT INTO "
                + tableName
                + " (id, town_name, creator_uuid, creator_name, data) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE town_name = VALUES(town_name), creator_uuid = VALUES(creator_uuid), creator_name = VALUES(creator_name), data = VALUES(data)";
      }
    } else {
      if (useNewSchema) {
        // New schema with all columns (SQLite)
        upsertSQL =
            "INSERT OR REPLACE INTO "
                + tableName
                + " (id, town_name, creator_uuid, creator_name, leader_uuid, leader_name, nation_id, bank_balance, claims_count, members_count, is_open, data) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      } else {
        // Legacy schema (pre-v2.0)
        upsertSQL =
            "INSERT OR REPLACE INTO "
                + tableName
                + " (id, town_name, creator_uuid, creator_name, data) VALUES (?, ?, ?, ?, ?)";
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
            ps.setString(paramIndex++, obj.getCreatorID());
            ps.setString(paramIndex++, obj.getCreatorName());

            ITanPlayer leaderData = obj.getLeaderData();
            String leaderName = (leaderData != null) ? leaderData.getNameStored() : null;
            ps.setString(paramIndex++, obj.getLeaderID());
            ps.setString(paramIndex++, leaderName);

            if (useNewSchema) {
              // Get additional data for new columns
              String nationId = null;
              try {
                org.leralix.tan.dataclass.territory.RegionData region = obj.getRegionSync();
                if (region != null) {
                  nationId = region.getID();
                }
              } catch (Exception e) {
                // Region not available
              }

              double bankBalance = obj.getBalance(); // TerritoryData.getBalance()
              int claimsCount = obj.getNumberOfClaimedChunk();
              int membersCount = obj.getPlayerIDList().size();
              boolean isOpen = obj.isRecruiting();

              ps.setString(paramIndex++, nationId);
              ps.setDouble(paramIndex++, bankBalance);
              ps.setInt(paramIndex++, claimsCount);
              ps.setInt(paramIndex++, membersCount);
              ps.setBoolean(paramIndex++, isOpen);
            }

            ps.setString(paramIndex, jsonData);
            ps.executeUpdate();

            if (cacheEnabled && cache != null) {
              synchronized (cache) {
                cache.put(id, obj);
              }
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
    try (Connection conn = getDatabase().getDataSource().getConnection()) {
      ResultSet rs = conn.getMetaData().getColumns(null, null, TABLE_NAME, columnName);
      boolean exists = rs.next();
      rs.close();
      return exists;
    } catch (SQLException e) {
      return false;
    }
  }
  private void loadNextTownId() {
    newTownId = getDatabase().getNextTownId();
  }
  @Override
  public void reset() {
    instance = null;
  }
  public static TownDataStorage getInstance() {
    if (instance == null) instance = new TownDataStorage();
    return instance;
  }
  public CompletableFuture<TownData> newTown(String townName, ITanPlayer tanPlayer) {
    String townId = getNextTownID();
    TownData newTown = new TownData(townId, townName, tanPlayer);
    put(townId, newTown);
    return CompletableFuture.completedFuture(newTown);
  }
  private @NotNull String getNextTownID() {
    String townId = "T" + newTownId;
    newTownId++;
    getDatabase().updateNextTownId(newTownId);
    return townId;
  }
  public CompletableFuture<TownData> newTown(String townName) {
    String townId = getNextTownID();
    TownData newTown = new TownData(townId, townName, null);
    put(townId, newTown);
    return CompletableFuture.completedFuture(newTown);
  }
  public void deleteTown(TownData townData) {
    delete(townData.getID());
  }
  public CompletableFuture<TownData> get(ITanPlayer tanPlayer) {
    return get(tanPlayer.getTownId());
  }
  public CompletableFuture<TownData> get(Player player) {
    return PlayerDataStorage.getInstance()
        .get(player)
        .thenCompose(
            tanPlayer -> {
              if (tanPlayer == null) {
                return CompletableFuture.completedFuture(null);
              }
              return get(tanPlayer.getTownId());
            });
  }
  public int getNumberOfTown() {
    return count();
  }
  public boolean isNameUsed(String townName) {
    if (townName == null) {
      return false;
    }
    String selectSQL =
        "SELECT 1 FROM " + TABLE_NAME + " WHERE json_extract(data, '$.name') = ? LIMIT 1";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(selectSQL)) {
      ps.setString(1, townName);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("json_extract not supported, falling back to full scan: " + e.getMessage());
      for (TownData town : getAll().values()) {
        if (townName.equals(town.getName())) return true;
      }
    }
    return false;
  }
  public TownData getSync(String id) {
    if (cacheEnabled && cache != null) {
      TownData cached = cache.get(id);
      if (cached != null) {
        return cached;
      }
    }
    get(id)
        .thenAccept(
            town -> {
              if (town != null && cacheEnabled && cache != null) {
                cache.put(id, town);
              }
            });
    return null;
  }
  public TownData getSync(ITanPlayer tanPlayer) {
    return getSync(tanPlayer.getTownId());
  }
  public TownData getSync(Player player) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    return getSync(tanPlayer.getTownId());
  }

  /**
   * Updates a town in storage (alias for put).
   *
   * @param town The town to update
   */
  public void update(TownData town) {
    if (town == null) {
      throw new IllegalArgumentException("Town cannot be null");
    }
    put(town.getID(), town);
  }

  /**
   * Updates a town in storage asynchronously (alias for putAsync).
   *
   * @param town The town to update
   * @return Future completing when update is done
   */
  public CompletableFuture<Void> updateAsync(TownData town) {
    if (town == null) {
      throw new IllegalArgumentException("Town cannot be null");
    }
    return putAsync(town.getID(), town);
  }
}