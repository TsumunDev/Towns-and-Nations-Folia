package org.leralix.tan.tasks;
import org.leralix.tan.TownsAndNations;
public class SaveStats {
  private SaveStats() {
    throw new IllegalStateException("Utility class");
  }
  public static void startSchedule() {
    TownsAndNations.getPlugin()
        .getLogger()
        .info(
            "SaveStats: Using DatabaseStorage - auto-save on every operation, no periodic save needed");
  }
  public static void saveAll() {
    TownsAndNations.getPlugin()
        .getLogger()
        .fine(
            "SaveStats.saveAll() called but not needed with DatabaseStorage (auto-saves on every operation)");
  }
}