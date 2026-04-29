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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.NationData;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.cosmetic.ICustomIcon;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.typeadapter.EnumMapDeserializer;
import org.leralix.tan.storage.typeadapter.IconAdapter;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.file.FileUtil;
public class NationDataStorage extends DatabaseStorage<NationData> {
  private static final Set<String> knownColumns = ConcurrentHashMap.newKeySet();
  private static final String TABLE_NAME = "ccn_nations";
  private AtomicInteger nextID;
  private static volatile NationDataStorage instance;
  public static NationDataStorage getInstance() {
    if (instance == null) {
      synchronized (NationDataStorage.class) {
        if (instance == null) {
          instance = new NationDataStorage();
        }
      }
    }
    return instance;
  }
  private NationDataStorage() {
    super(
        TABLE_NAME,
        NationData.class,
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
      try (ResultSet rs = conn.getMetaData().getColumns(null, null, TABLE_NAME, "nation_name")) {
        if (!rs.next()) {
          stmt.executeUpdate(
              "ALTER TABLE %s ADD COLUMN nation_name VARCHAR(255) NULL".formatted(TABLE_NAME));
          TownsAndNations.getPlugin().getLogger().info("Added nation_name column to " + TABLE_NAME);
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
        "CREATE INDEX IF NOT EXISTS idx_nation_name ON " + TABLE_NAME + " (nation_name)";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(createNameIndexSQL);
      TownsAndNations.getPlugin()
          .getLogger()
          .info("Created index idx_nation_name on " + TABLE_NAME);
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("Error creating indexes for " + TABLE_NAME + ": " + e.getMessage());
    }
  }
  @Override
  public void put(String id, NationData obj) {
    if (id == null || obj == null) {
      return;
    }
    String jsonData = gson.toJson(obj, typeToken);
    boolean useNewSchema = columnExists("members_count");
    String upsertSQL = buildUpsertSQL(useNewSchema);

    if (useNewSchema) {
      String leaderUuid = obj.getLeaderID();
      String capitalId = obj.getCapitalID();
      int membersCount = obj.getRegionsInNation().size();
      obj.getLeaderDataAsync()
          .thenAccept(
              leader -> {
                String leaderName = (leader != null) ? leader.getNameStored() : null;
                executeUpsert(id, obj.getName(), jsonData, upsertSQL, true,
                    leaderUuid, leaderName, capitalId, membersCount, obj);
              })
          .exceptionally(
              ex -> {
                executeUpsert(id, obj.getName(), jsonData, upsertSQL, true,
                    leaderUuid, null, capitalId, membersCount, obj);
                return null;
              });
    } else {
      executeUpsert(id, obj.getName(), jsonData, upsertSQL, false,
          null, null, null, 0, obj);
    }
  }

  private String buildUpsertSQL(boolean useNewSchema) {
    if (getDatabase().isMySQL()) {
      if (useNewSchema) {
        return "INSERT INTO "
            + tableName
            + " (id, nation_name, leader_uuid, leader_name, capital_id, members_count, data) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "nation_name = VALUES(nation_name), "
            + "leader_uuid = VALUES(leader_uuid), "
            + "leader_name = VALUES(leader_name), "
            + "capital_id = VALUES(capital_id), "
            + "members_count = VALUES(members_count), "
            + "data = VALUES(data)";
      }
      return "INSERT INTO "
          + tableName
          + " (id, nation_name, data) VALUES (?, ?, ?) "
          + "ON DUPLICATE KEY UPDATE nation_name = VALUES(nation_name), data = VALUES(data)";
    }
    if (useNewSchema) {
      return "INSERT OR REPLACE INTO "
          + tableName
          + " (id, nation_name, leader_uuid, leader_name, capital_id, members_count, data) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }
    return "INSERT OR REPLACE INTO " + tableName + " (id, nation_name, data) VALUES (?, ?, ?)";
  }

  private void executeUpsert(String id, String name, String jsonData, String upsertSQL,
      boolean useNewSchema, String leaderUuid, String leaderName,
      String capitalId, int membersCount, NationData obj) {
    FoliaScheduler.runTaskAsynchronously(
        TownsAndNations.getPlugin(),
        () -> {
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(upsertSQL)) {
            int paramIndex = 1;
            ps.setString(paramIndex++, id);
            ps.setString(paramIndex++, name);
            if (useNewSchema) {
              ps.setString(paramIndex++, leaderUuid);
              ps.setString(paramIndex++, leaderName);
              ps.setString(paramIndex++, capitalId);
              ps.setInt(paramIndex++, membersCount);
            }
            ps.setString(paramIndex, jsonData);
            ps.executeUpdate();
            if (cacheEnabled && cache != null) {
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
    nextID = new AtomicInteger(getDatabase().getNextNationId());
  }
  public CompletableFuture<NationData> createNewNation(String name, RegionData capital, ITanPlayer leader) {
    String nationID = generateNextID();
    NationData newNation = new NationData(nationID, name, leader, capital.getID());
    put(nationID, newNation);
    capital.setOverlord(newNation);
    FileUtil.addLineToHistory(Lang.NATION_CREATED_NEWSLETTER.get(leader.getNameStored(), name));
    return CompletableFuture.completedFuture(newNation);
  }
  private @NotNull String generateNextID() {
    int id = nextID.getAndIncrement();
    getDatabase().updateNextNationId(nextID.get());
    return "N" + id;
  }
  public CompletableFuture<NationData> get(Player player) {
    return PlayerDataStorage.getInstance().get(player).thenCompose(this::get);
  }
  public CompletableFuture<NationData> get(ITanPlayer tanPlayer) {
    return RegionDataStorage.getInstance()
        .get(tanPlayer)
        .thenCompose(
            region -> {
              if (region == null) return CompletableFuture.completedFuture(null);
              return region.getNationAsync();
            });
  }
  @Deprecated
  public void deleteNation(NationData nation) {
    deleteAsync(nation.getID());
  }
  public CompletableFuture<Void> deleteNationAsync(NationData nation) {
    return deleteAsync(nation.getID());
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
      for (NationData nation : getAllSync().values()) {
        if (name.equals(nation.getName())) return true;
      }
    }
    return false;
  }
  public NationData getSync(Player player) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    if (tanPlayer == null) return null;
    return getSync(tanPlayer);
  }
  @Override
  public void reset() {
    synchronized (NationDataStorage.class) {
      instance = null;
    }
  }
  public NationData getSync(String id) {
    if (cacheEnabled && cache != null) {
      NationData cached = cache.get(id);
      if (cached != null) {
        return cached;
      }
    }
    get(id)
        .thenAccept(
            nation -> {
              if (nation != null && cacheEnabled && cache != null) {
                cache.put(id, nation);
              }
            });
    return null;
  }
  @Deprecated
  public NationData getSync(ITanPlayer tanPlayer) {
    return null;
  }
  public CompletableFuture<NationData> getByName(String name) {
    if (name == null || name.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    CompletableFuture<NationData> future = new CompletableFuture<>();
    String selectSQL = "SELECT data FROM " + TABLE_NAME + " WHERE nation_name = ? LIMIT 1";
    runAsync(
        () -> {
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(selectSQL)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
              if (rs.next()) {
                String jsonData = rs.getString("data");
                NationData nation = gson.fromJson(jsonData, NationData.class);
                if (nation != null && cacheEnabled && cache != null) {
                  cache.put(nation.getID(), nation);
                }
                future.complete(nation);
                return;
              }
            }
          } catch (SQLException e) {
            // Fall through to JSON extraction
          }
          String fallbackSQL = "SELECT data FROM " + TABLE_NAME + " WHERE json_extract(data, '$.name') = ? LIMIT 1";
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(fallbackSQL)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
              if (rs.next()) {
                String jsonData = rs.getString("data");
                NationData nation = gson.fromJson(jsonData, NationData.class);
                if (nation != null && cacheEnabled && cache != null) {
                  cache.put(nation.getID(), nation);
                }
                future.complete(nation);
                return;
              }
            }
          } catch (SQLException e2) {
            // Both methods failed
          }
          String fullScanSQL = "SELECT id, data FROM " + TABLE_NAME;
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(fullScanSQL);
              ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              String jsonData = rs.getString("data");
              try {
                NationData nation = gson.fromJson(jsonData, NationData.class);
                if (nation != null && nation.getName().equalsIgnoreCase(name)) {
                  if (cacheEnabled && cache != null) {
                    cache.put(nation.getID(), nation);
                  }
                  future.complete(nation);
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
                .warning("Error getting nation by name: " + e.getMessage());
            future.complete(null);
          }
        });
    return future;
  }
  @Deprecated
  public NationData getByNameSync(String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }
    if (cacheEnabled && cache != null) {
      for (NationData nation : cache.values()) {
        if (nation.getName().equalsIgnoreCase(name)) {
          return nation;
        }
      }
    }
    return null;
  }
}
