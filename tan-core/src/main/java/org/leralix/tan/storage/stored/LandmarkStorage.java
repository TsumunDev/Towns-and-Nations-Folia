package org.leralix.tan.storage.stored;
import com.google.gson.GsonBuilder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.leralix.lib.position.Vector3D;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.Landmark;
import org.leralix.tan.dataclass.territory.TerritoryData;
public class LandmarkStorage extends DatabaseStorage<Landmark> {
  private static final String TABLE_NAME = "ccn_landmarks";
  private AtomicInteger newLandmarkID;
  private static volatile LandmarkStorage instance;
  private LandmarkStorage() {
    super(TABLE_NAME, Landmark.class, new GsonBuilder().setPrettyPrinting().create());
    newLandmarkID = new AtomicInteger(0);
    loadNextLandmarkIDAsync();
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
  private void loadNextLandmarkID() {
    int ID = 0;
    for (String ids : getAllAsync().join().keySet()) {
      int newID = Integer.parseInt(ids.substring(1));
      if (newID > ID) ID = newID;
    }
    newLandmarkID = new AtomicInteger(ID + 1);
  }

  /**
   * Loads the next landmark ID asynchronously — does not block the startup thread.
   * The ID counter starts at 0 and is updated once the DB query completes.
   */
  private void loadNextLandmarkIDAsync() {
    getAllAsync().thenAccept(allLandmarks -> {
      int maxID = 0;
      for (String ids : allLandmarks.keySet()) {
        try {
          int parsed = Integer.parseInt(ids.substring(1));
          if (parsed > maxID) maxID = parsed;
        } catch (NumberFormatException ignored) {
          // Skip non-numeric IDs
        }
      }
      newLandmarkID.set(maxID + 1);
    }).exceptionally(ex -> {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("Failed to load landmark IDs asynchronously: " + ex.getMessage());
      return null;
    });
  }
  public static LandmarkStorage getInstance() {
    if (instance == null) {
      synchronized (LandmarkStorage.class) {
        if (instance == null) {
          instance = new LandmarkStorage();
        }
      }
    }
    return instance;
  }
  public static void setInstance(LandmarkStorage mockLandmarkStorage) {
    synchronized (LandmarkStorage.class) {
      instance = mockLandmarkStorage;
    }
  }
  @Deprecated
  public Landmark getSync(String id) {
    try {
      return get(id).join();
    } catch (Exception e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("Error getting landmark data synchronously: " + e.getMessage());
      return null;
    }
  }
  public Landmark addLandmark(Location position) {
    Vector3D vector3D = new Vector3D(position);
    int id = newLandmarkID.getAndIncrement();
    String landmarkID = "L" + id;
    Landmark landmark = new Landmark(landmarkID, vector3D);
    putSync(landmarkID, landmark);
    NewClaimedChunkStorage.getInstance().claimLandmarkChunk(position.getChunk(), landmarkID);
    return landmark;
  }
  @Deprecated
  public List<Landmark> getLandmarkOf(TerritoryData territoryData) {
    return getAllAsync().join().values().stream()
        .filter(landmark -> landmark.isOwnedBy(territoryData))
        .toList();
  }

  /**
   * Gets all landmarks owned by a territory asynchronously.
   * @param territoryData the territory to filter by
   * @return CompletableFuture with the list of matching landmarks
   */
  public CompletableFuture<List<Landmark>> getLandmarkOfAsync(TerritoryData territoryData) {
    return getAllAsync().thenApply(map -> map.values().stream()
        .filter(landmark -> landmark.isOwnedBy(territoryData))
        .toList());
  }
  @Deprecated
  public void generateAllResources() {
    for (Landmark landmark : getAllAsync().join().values()) {
      landmark.generateResources();
    }
  }

  /**
   * Generates resources for all landmarks asynchronously.
   * @return CompletableFuture that completes when all resources are generated
   */
  public CompletableFuture<Void> generateAllResourcesAsync() {
    return getAllAsync().thenAccept(map -> {
      for (Landmark landmark : map.values()) {
        landmark.generateResources();
      }
    });
  }
  @Override
  public void reset() {
    synchronized (LandmarkStorage.class) {
      instance = null;
    }
  }
}