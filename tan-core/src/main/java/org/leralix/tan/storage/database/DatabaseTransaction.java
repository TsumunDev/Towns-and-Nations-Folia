package org.leralix.tan.storage.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.leralix.tan.TownsAndNations;

/**
 * Manages MySQL transaction boundaries for ACID operations.
 * 
 * <p>Use this class to wrap multiple database operations in a single atomic transaction.
 * If any operation fails, all changes are rolled back to maintain data consistency.
 * 
 * <p><b>Example usage:</b>
 * <pre>{@code
 * DatabaseTransaction.executeInTransaction(conn -> {
 *     // All these operations are atomic - either all succeed or all rollback
 *     updateTownBalance(conn, townId, -cost);
 *     updateTownLevel(conn, townId, newLevel);
 *     updateChunkLimit(conn, townId, newLimit);
 * });
 * }</pre>
 * 
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Prevents partial upgrades (e.g., money deducted but level not increased)</li>
 *   <li>Automatic rollback on any exception</li>
 *   <li>Connection management handled automatically</li>
 *   <li>Thread-safe via DatabaseHandler connection pool</li>
 * </ul>
 * 
 * @see DatabaseHandler
 */
public class DatabaseTransaction {

  /**
   * Executes a database operation within a transaction boundary.
   * 
   * <p>This method:
   * <ol>
   *   <li>Acquires a connection from the pool</li>
   *   <li>Disables auto-commit (starts transaction)</li>
   *   <li>Executes the provided operation</li>
   *   <li>Commits if successful, rolls back on exception</li>
   *   <li>Releases the connection back to the pool</li>
   * </ol>
   * 
   * @param operation The database operations to execute atomically. Receives a Connection object.
   * @return CompletableFuture that completes when transaction finishes
   * @throws RuntimeException if transaction fails (wraps SQLException)
   */
  public static CompletableFuture<Void> executeInTransaction(
      Consumer<Connection> operation) {
    return executeInTransactionWithResult(conn -> {
      operation.accept(conn);
      return null;
    });
  }

  /**
   * Executes a database operation within a transaction and returns a result.
   * 
   * <p>Same as {@link #executeInTransaction(Consumer)} but allows returning a value
   * from the transaction.
   * 
   * @param <T> The type of result returned by the operation
   * @param operation The database operations to execute atomically
   * @return CompletableFuture containing the operation result
   * @throws RuntimeException if transaction fails (wraps SQLException)
   */
  public static <T> CompletableFuture<T> executeInTransactionWithResult(
      Function<Connection, T> operation) {
    return org.leralix.tan.async.VirtualThreadExecutor.supplyAsync(() -> {
      Connection conn = null;
      boolean originalAutoCommit = true;
      
      try {
        // Step 1: Get connection from pool
        conn = TownsAndNations.getPlugin().getDatabaseHandler().getDataSource().getConnection();
        originalAutoCommit = conn.getAutoCommit();
        
        // Step 2: Start transaction (disable auto-commit)
        conn.setAutoCommit(false);
        
        // Step 3: Execute operations
        T result = operation.apply(conn);
        
        // Step 4: Commit transaction
        conn.commit();
        TownsAndNations.getPlugin()
            .getLogger()
            .fine("[TaN-Transaction] Transaction committed successfully");
        
        return result;
        
      } catch (Exception e) {
        // Step 5: Rollback on any error
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
        
        // Wrap exception for caller
        throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
        
      } finally {
        // Step 6: Restore auto-commit and release connection
        if (conn != null) {
          try {
            conn.setAutoCommit(originalAutoCommit);
            conn.close(); // Returns to pool (HikariCP)
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
