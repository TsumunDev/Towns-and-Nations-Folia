package org.leralix.tan.storage.stored;
import com.google.gson.GsonBuilder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.storage.typeadapter.WargoalTypeAdapter;
import org.leralix.tan.wars.PlannedAttack;
import org.leralix.tan.wars.War;
import org.leralix.tan.wars.legacy.CreateAttackData;
import org.leralix.tan.wars.legacy.wargoals.WarGoal;
public class PlannedAttackStorage extends DatabaseStorage<PlannedAttack> {
  private static final String TABLE_NAME = "ccn_planned_attacks";
  private static volatile PlannedAttackStorage instance;
  protected PlannedAttackStorage() {
    super(
        TABLE_NAME,
        PlannedAttack.class,
        new GsonBuilder()
            .registerTypeAdapter(WarGoal.class, new WargoalTypeAdapter())
            .setPrettyPrinting()
            .create());
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
  public static PlannedAttackStorage getInstance() {
    if (instance == null) {
      synchronized (PlannedAttackStorage.class) {
        if (instance == null) {
          instance = new PlannedAttackStorage();
        }
      }
    }
    return instance;
  }
  public PlannedAttack newAttack(CreateAttackData createAttackData) {
    String newID = getNewID();
    PlannedAttack plannedAttack = new PlannedAttack(newID, createAttackData);
    putSync(newID, plannedAttack);
    return plannedAttack;
  }
  private void setupAllAttacks() {
    for (PlannedAttack plannedAttack : getAllSync().values()) {
      plannedAttack.setUpStartOfAttack();
    }
  }
  private String getNewID() {
    int ID = 0;
    while (exists("W" + ID)) {
      ID++;
    }
    return "W" + ID;
  }
  /**
   * Handle territory deletion by ending all planned attacks involving this territory.
   * Now fully async to avoid blocking Folia region threads.
   *
   * @param territoryData The territory being deleted
   * @return CompletableFuture that completes when all planned attacks are ended
   */
  public CompletableFuture<Void> territoryDeleted(TerritoryData territoryData) {
    return getAllAsync().thenCompose(plannedAttacks -> {
      List<CompletableFuture<Void>> endFutures = plannedAttacks.values().stream()
          .filter(plannedAttack -> {
            War war = plannedAttack.getWar();
            return war != null && (war.isMainAttacker(territoryData) || war.isMainDefender(territoryData));
          })
          .map(plannedAttack -> CompletableFuture.runAsync(() -> plannedAttack.end()))
          .toList();

      if (endFutures.isEmpty()) {
        return CompletableFuture.completedFuture(null);
      }

      return CompletableFuture.allOf(endFutures.toArray(new CompletableFuture[0]));
    });
  }
  public void delete(PlannedAttack plannedAttack) {
    deleteAsync(plannedAttack.getID()).join();
  }
  @Override
  public void reset() {
    synchronized (PlannedAttackStorage.class) {
      instance = null;
    }
  }
}