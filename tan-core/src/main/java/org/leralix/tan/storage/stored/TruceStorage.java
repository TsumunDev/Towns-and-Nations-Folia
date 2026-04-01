package org.leralix.tan.storage.stored;
import com.google.gson.GsonBuilder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ActiveTruce;
import org.leralix.tan.dataclass.territory.TerritoryData;
public class TruceStorage extends DatabaseStorage<HashMap<String, ActiveTruce>> {
  private static final String TABLE_NAME = "ccn_truces";
  private static volatile TruceStorage instance;
  protected TruceStorage() {
    super(
        TABLE_NAME,
        (Class<HashMap<String, ActiveTruce>>) (Class<?>) HashMap.class,
        new GsonBuilder().setPrettyPrinting().create());
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
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe("Error creating table " + TABLE_NAME + ": " + e.getMessage());
    }
  }
  public static TruceStorage getInstance() {
    if (instance == null) {
      synchronized (TruceStorage.class) {
        if (instance == null) {
          instance = new TruceStorage();
        }
      }
    }
    return instance;
  }
  @Override
  public void reset() {
    synchronized (TruceStorage.class) {
      instance = null;
    }
  }
  @Deprecated
  public void add(ActiveTruce activeTruce) {
    String id1 = activeTruce.getTerritoryID1();
    String id2 = activeTruce.getTerritoryID2();
    HashMap<String, ActiveTruce> map1 = get(id1).join();
    if (map1 == null) {
      map1 = new HashMap<>();
    }
    map1.put(id2, activeTruce);
    putSync(id1, map1);
    HashMap<String, ActiveTruce> map2 = get(id2).join();
    if (map2 == null) {
      map2 = new HashMap<>();
    }
    map2.put(id1, activeTruce);
    putSync(id2, map2);
  }

  /**
   * Adds a truce asynchronously without blocking the calling thread.
   * Chains get → modify → put for both territories.
   *
   * @param activeTruce the truce to add
   * @return CompletableFuture that completes when both sides are stored
   */
  public CompletableFuture<Void> addAsync(ActiveTruce activeTruce) {
    String id1 = activeTruce.getTerritoryID1();
    String id2 = activeTruce.getTerritoryID2();

    CompletableFuture<HashMap<String, ActiveTruce>> getMap1 = get(id1);
    CompletableFuture<HashMap<String, ActiveTruce>> getMap2 = get(id2);

    return getMap1.thenCombine(getMap2, (map1, map2) -> {
      // Mutate maps and return pair for storage
      HashMap<String, ActiveTruce> resolved1 = map1 != null ? map1 : new HashMap<>();
      HashMap<String, ActiveTruce> resolved2 = map2 != null ? map2 : new HashMap<>();
      resolved1.put(id2, activeTruce);
      resolved2.put(id1, activeTruce);
      return new HashMap[]{resolved1, resolved2};
    }).thenCompose(maps -> {
      CompletableFuture<Void> put1 = putAsync(id1, maps[0]);
      CompletableFuture<Void> put2 = putAsync(id2, maps[1]);
      return CompletableFuture.allOf(put1, put2);
    });
  }

  /**
   * Gets the remaining truce duration in hours asynchronously.
   *
   * @param territoryData1 first territory
   * @param territoryData2 second territory
   * @return CompletableFuture with remaining hours (0 if no truce)
   */
  public CompletableFuture<Long> getRemainingTruceAsync(TerritoryData territoryData1, TerritoryData territoryData2) {
    String id1 = territoryData1.getID();
    String id2 = territoryData2.getID();
    return get(id1).thenApply(truceMap -> {
      if (truceMap == null) {
        return 0L;
      }
      ActiveTruce truce = truceMap.get(id2);
      if (truce == null) {
        return 0L;
      }
      long remaining = truce.getEndOfTruce() - Instant.now().toEpochMilli();
      long remainingInHours = remaining / 1000 / 60 / 60;
      return Math.max(0, remainingInHours);
    });
  }
  public long getRemainingTruce(TerritoryData territoryData1, TerritoryData territoryData2) {
    String id1 = territoryData1.getID();
    String id2 = territoryData2.getID();
    HashMap<String, ActiveTruce> truceMap = get(id1).join();
    if (truceMap == null) {
      return 0;
    }
    ActiveTruce truce = truceMap.get(id2);
    if (truce == null) {
      return 0;
    }
    long remaining = truce.getEndOfTruce() - Instant.now().toEpochMilli();
    long remainingInHours = remaining / 1000 / 60 / 60;
    return Math.max(0, remainingInHours);
  }
}