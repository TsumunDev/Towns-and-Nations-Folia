package org.leralix.tan.storage.database;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.leralix.tan.TownsAndNations;
public class DatabaseTransaction {
  public static CompletableFuture<Void> executeInTransaction(
      Consumer<Connection> operation) {
    return executeInTransactionWithResult(conn -> {
      operation.accept(conn);
      return null;
    });
  }
  public static <T> CompletableFuture<T> executeInTransactionWithResult(
      Function<Connection, T> operation) {
    return org.leralix.tan.async.VirtualThreadExecutor.supplyAsync(() -> {
      Connection conn = null;
      boolean originalAutoCommit = true;
      try {
        conn = TownsAndNations.getPlugin().getDatabaseHandler().getDataSource().getConnection();
        originalAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        T result = operation.apply(conn);
        conn.commit();
        TownsAndNations.getPlugin()
            .getLogger()
            .fine("[TaN-Transaction] Transaction committed successfully");
        return result;
      } catch (Exception e) {
        if (conn != null) {
          try {
            conn.rollback();
            TownsAndNations.getPlugin()
                .getLogger()
                .warning("[TaN-Transaction] Transaction rolled back due to error: "
                    + e.getMessage());
          } catch (SQLException rollbackEx) {
            TownsAndNations.getPlugin()
                .getLogger()
                .severe("[TaN-Transaction] Rollback failed: " + rollbackEx.getMessage());
            rollbackEx.printStackTrace();
          }
        }
        throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
      } finally {
        if (conn != null) {
          try {
            conn.setAutoCommit(originalAutoCommit);
            conn.close();
          } catch (SQLException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .severe("[TaN-Transaction] Failed to cleanup connection: " + e.getMessage());
            e.printStackTrace();
          }
        }
      }
    });
  }
}