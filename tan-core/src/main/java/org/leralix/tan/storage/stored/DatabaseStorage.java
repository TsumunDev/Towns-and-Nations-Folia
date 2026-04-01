package org.leralix.tan.storage.stored;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.database.DatabaseHandler;
import org.leralix.tan.storage.exceptions.DatabaseNotReadyException;

/**
 * Abstract base class for database-backed storage with caching support.
 *
 * <p>This class provides a generic CRUD (Create, Read, Update, Delete) interface for
 * storing objects in a SQLite/MySQL database with JSON serialization and optional
 * in-memory caching. All database operations are asynchronous to prevent blocking
 * in Folia's regionized threading model.</p>
 *
 * <p><b>Type Parameters:</b><br>
 * <code>&lt;T&gt;</code> - The type of objects stored (e.g., ITanPlayer, TownData, RegionData)</p>
 *
 * <p><b>Architecture:</b></p>
 * <ul>
 *   <li>Storage: Objects are serialized to JSON and stored in SQLite/MySQL</li>
 *   <li>Cache: Optional in-memory cache using synchronized LRU LinkedHashMap (thread-safe with automatic eviction)</li>
 *   <li>Async: All operations return CompletableFuture for non-blocking access</li>
 *   <li>Thread-Safe: Safe for concurrent access from multiple regions</li>
 * </ul>
 *
 * <p><b>Cache Behavior:</b></p>
 * <ul>
 *   <li>First {@link #get} loads from database and caches result</li>
 *   <li>Subsequent {@link #get} calls return cached data (fast path)</li>
 *   <li>Cache is invalidated when data is modified via {@link #put}</li>
 *   <li>Use {@link #getAll()} to warm up cache for multiple items</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Get object asynchronously (recommended)
 * storage.get(playerId)
 *     .thenAccept(object -> {
 *         if (object != null) {
 *             // Process object
 *         }
 *     });
 *
 * // Save object (invalidates cache)
 * storage.putSync(objectId, object);
 *
 * // Delete object
 * storage.delete(object);
 * }</pre>
 *
 * <h3>Implementations:</h3>
 * <ul>
 *   <li>{@link PlayerDataStorage} - Player data storage</li>
 *   <li>{@link TownDataStorage} - Town data storage</li>
 *   <li>{@link RegionDataStorage} - Region data storage</li>
 * </ul>
 *
 * @param <T> The type of objects stored in this storage
 * @see PlayerDataStorage
 * @see TownDataStorage
 * @see RegionDataStorage
 * @since 0.15.0
 */
public abstract class DatabaseStorage<T> {
  /** SQL keywords that must not be allowed as table names */
  private static final java.util.Set<String> SQL_KEYWORDS = java.util.Set.of(
      "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
      "TABLE", "INDEX", "VIEW", "JOIN", "UNION", "OR", "AND", "NOT", "IN", "LIKE", "IS",
      "NULL", "TRUE", "FALSE", "CASE", "WHEN", "THEN", "ELSE", "END", "AS", "ORDER", "BY",
      "GROUP", "HAVING", "LIMIT", "OFFSET", "DISTINCT", "EXISTS", "BETWEEN", "VALUES",
      "SET", "INTO", "DESC", "ASC", "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "TRANSACTION"
  );

  protected final Gson gson;
  protected final String tableName;
  protected final Class<T> typeClass;
  protected final Type typeToken;
  protected final Map<String, T> cache;
  protected final int cacheSize;
  protected final boolean cacheEnabled;
  /** LRU cache wrapper for automatic eviction when cacheSize limit is reached */
  protected static class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;
    public LRUCache(int maxSize) {
      super(maxSize, 0.75f, true); // access-order for LRU
      this.maxSize = maxSize;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
      return size() > maxSize;
    }
  }

  /**
   * Validates table name to prevent SQL injection.
   * Only allows alphanumeric + underscore, 1-64 chars, not starting with digit,
   * and not a SQL keyword.
   */
  private static boolean isValidTableName(String tableName) {
    if (tableName == null || tableName.isEmpty() || tableName.length() > 64) {
      return false;
    }
    // Must start with letter or underscore, contain only alphanumeric/underscore
    if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
      return false;
    }
    // Must not be a SQL keyword (case-insensitive)
    String upperName = tableName.toUpperCase();
    if (SQL_KEYWORDS.contains(upperName)) {
      return false;
    }
    return true;
  }
  protected DatabaseStorage(String tableName, Class<T> typeClass, Gson gson) {
    this(tableName, typeClass, typeClass, gson, true);
  }
  protected DatabaseStorage(
      String tableName, Class<T> typeClass, Type typeToken, Gson gson, boolean enableCache) {
    this(
        tableName,
        typeClass,
        typeToken,
        gson,
        enableCache,
        TownsAndNations.getPlugin().getConfig().getInt("cache." + tableName, 1000));
  }
  protected DatabaseStorage(
      String tableName,
      Class<T> typeClass,
      Type typeToken,
      Gson gson,
      boolean enableCache,
      int cacheSize) {
    this.tableName = tableName;
    this.typeClass = typeClass;
    this.typeToken = typeToken;
    this.gson = gson;
    this.cacheEnabled = enableCache;
    this.cacheSize = cacheSize;
    // Use LRU cache with synchronized wrapper for thread-safety in Folia
    // LinkedHashMap with access-order provides LRU eviction via removeEldestEntry
    this.cache = enableCache
        ? java.util.Collections.synchronizedMap(new LRUCache<>(cacheSize))
        : null;
    // Validate tableName to prevent SQL injection
    if (!isValidTableName(tableName)) {
      throw new IllegalArgumentException(
          "Invalid table name: '" + tableName + "'. Must be 1-64 alphanumeric/underscore characters, "
              + "not starting with a digit, and not a SQL keyword.");
    }
    createTable();
    createIndexes();
  }
  protected DatabaseHandler getDatabase() {
    return TownsAndNations.getPlugin().getDatabaseHandler();
  }
  protected abstract void createTable();
  protected void createIndexes() {
  }
  protected void clearCache() {
    if (cacheEnabled && cache != null) {
      cache.clear();
    }
  }
  protected void invalidateCache(String id) {
    if (cacheEnabled && cache != null) {
      cache.remove(id);
    }
  }
  protected void invalidateCacheIf(java.util.function.Predicate<T> condition) {
    if (cacheEnabled && cache != null) {
      cache.entrySet().removeIf(entry -> condition.test(entry.getValue()));
    }
  }

  /**
   * Gets an object by its ID asynchronously.
   *
   * <p>This is the primary method for retrieving stored objects. It checks the cache first
   * (fast path) before loading from the database (slow path). The method is non-blocking
   * and safe to call from any thread in Folia.</p>
   *
   * <p><b>Cache Behavior:</b></p>
   * <ul>
   *   <li>First call: Loads from database, caches result</li>
   *   <li>Subsequent calls: Returns cached data (very fast)</li>
   *   <li>Cache invalidated on: {@link #putSync}</li>
   * </ul>
   *
   * @param id the unique identifier of the object to retrieve
   * @return CompletableFuture that completes with the object, or null if not found
   * @see #getSync(String) for blocking version
   * @see #getAll() to load all objects
   */
  public CompletableFuture<T> get(String id) {
    CompletableFuture<T> future = new CompletableFuture<>();
    if (id == null) {
      future.complete(null);
      return future;
    }
    if (cacheEnabled && cache != null) {
      T cached = cache.get(id);
      if (cached != null) {
        future.complete(cached);
        return future;
      }
    }
    runAsync(
        () -> {
          try {
            T object = loadFromDatabase(id);
            if (object != null && cacheEnabled && cache != null) {
              cache.put(id, object);
            }
            future.complete(object);
          } catch (DatabaseNotReadyException e) {
            future.completeExceptionally(e);
          } catch (Exception e) {
            future.completeExceptionally(e);
          }
        });
    return future;
  }
  protected void runAsync(Runnable task) {
    org.leralix.tan.utils.FoliaScheduler.runTaskAsynchronously(TownsAndNations.getPlugin(), task);
  }
  private T loadFromDatabase(String id) {
    String selectSQL = "SELECT data FROM " + tableName + " WHERE id = ?";
    try (Connection conn = getDatabase().getDataSource().getConnection()) {
      if (conn == null || conn.isClosed()) {
        String errorMsg = "Database connection is null or closed for " + typeClass.getSimpleName();
        TownsAndNations.getPlugin().getLogger().severe(errorMsg);
        throw new DatabaseNotReadyException(errorMsg);
      }
      try (PreparedStatement ps = conn.prepareStatement(selectSQL)) {
        ps.setString(1, id);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            String jsonData = rs.getString("data");
            if (typeToken.equals(ITanPlayer.class)) {
              com.google.gson.JsonElement jsonElement =
                  com.google.gson.JsonParser.parseString(jsonData);
              if (jsonElement.isJsonObject()) {
                com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
                jsonObject.addProperty("uuid", id);
                jsonData = jsonObject.toString();
              }
            }
            return gson.fromJson(jsonData, typeToken);
          }
          return null;
        }
      }
    } catch (SQLException e) {
      String errorMsg =
          "SQL error retrieving "
              + typeClass.getSimpleName()
              + " with ID "
              + id
              + ": "
              + e.getMessage();
      TownsAndNations.getPlugin().getLogger().severe(errorMsg);
      throw new DatabaseNotReadyException(errorMsg, e);
    } catch (JsonSyntaxException e) {
      String errorMsg =
          "JSON parsing error for "
              + typeClass.getSimpleName()
              + " with ID "
              + id
              + ": "
              + e.getMessage();
      TownsAndNations.getPlugin().getLogger().severe(errorMsg);
      throw new RuntimeException(errorMsg, e);
    }
  }
  @Deprecated
  public Map<String, T> getAll() {
    return getAllSync();
  }
  public Map<String, T> getAllSync() {
    Map<String, T> result = new LinkedHashMap<>();
    String selectSQL = "SELECT id, data FROM " + tableName;
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(selectSQL);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        String id = rs.getString("id");
        String jsonData = rs.getString("data");
        try {
          T object = gson.fromJson(jsonData, typeToken);
          if (object != null) {
            result.put(id, object);
          }
        } catch (JsonSyntaxException e) {
          TownsAndNations.getPlugin()
              .getLogger()
              .warning(
                  "Failed to deserialize "
                      + typeClass.getSimpleName()
                      + " with ID "
                      + id
                      + ": "
                      + e.getMessage());
        }
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe(
              "Error retrieving all " + typeClass.getSimpleName() + " objects: " + e.getMessage());
    }
    return result;
  }
  public CompletableFuture<Map<String, T>> getAllAsync() {
    CompletableFuture<Map<String, T>> future = new CompletableFuture<>();
    runAsync(
        () -> {
          Map<String, T> result = new LinkedHashMap<>();
          String selectSQL = "SELECT id, data FROM " + tableName;
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(selectSQL);
              ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              String id = rs.getString("id");
              String jsonData = rs.getString("data");
              try {
                T object = gson.fromJson(jsonData, typeToken);
                if (object != null) {
                  result.put(id, object);
                }
              } catch (JsonSyntaxException e) {
                TownsAndNations.getPlugin()
                    .getLogger()
                    .warning(
                        "Failed to deserialize "
                            + typeClass.getSimpleName()
                            + " with ID "
                            + id
                            + ": "
                            + e.getMessage());
              }
            }
            future.complete(result);
          } catch (SQLException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .severe(
                    "Error retrieving all "
                        + typeClass.getSimpleName()
                        + " objects: "
                        + e.getMessage());
            future.completeExceptionally(e);
          }
        });
    return future;
  }
  public List<String> getAllIds() {
    List<String> result = new ArrayList<>();
    String selectSQL = "SELECT id FROM " + tableName;
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(selectSQL);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        result.add(rs.getString("id"));
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe(
              "Error retrieving all IDs for " + typeClass.getSimpleName() + ": " + e.getMessage());
    }
    return result;
  }
  @Deprecated
  public void put(String id, T obj) {
    putSync(id, obj);
  }
  public void putSync(String id, T obj) {
    if (id == null || obj == null) {
      return;
    }
    String jsonData = gson.toJson(obj, typeToken);
    String upsertSQL = getDatabase().getUpsertSQL(tableName);
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(upsertSQL)) {
      ps.setString(1, id);
      ps.setString(2, jsonData);
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
  }
  public CompletableFuture<Void> putAsync(String id, T obj) {
    if (id == null || obj == null) {
      return CompletableFuture.completedFuture(null);
    }
    CompletableFuture<Void> future = new CompletableFuture<>();
    String jsonData = gson.toJson(obj, typeToken);
    String upsertSQL = getDatabase().getUpsertSQL(tableName);
    runAsync(
        () -> {
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(upsertSQL)) {
            ps.setString(1, id);
            ps.setString(2, jsonData);
            ps.executeUpdate();
            // Update cache ONLY after successful database write
            // This prevents cache corruption if SQL fails
            if (cacheEnabled && cache != null) {
              cache.put(id, obj);
            }
            future.complete(null);
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
            future.completeExceptionally(e);
          }
        });
    return future;
  }
  public void putAll(Map<String, T> objects) {
    if (objects == null || objects.isEmpty()) {
      return;
    }
    String upsertSQL = getDatabase().getUpsertSQL(tableName);
    // Use try-with-resources to ensure connection is always closed, even on error
    try (Connection conn = getDatabase().getDataSource().getConnection()) {
      boolean originalAutoCommit = conn.getAutoCommit();
      try {
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(upsertSQL)) {
          for (Map.Entry<String, T> entry : objects.entrySet()) {
            String id = entry.getKey();
            T obj = entry.getValue();
            if (id != null && obj != null) {
              String jsonData = gson.toJson(obj, typeToken);
              ps.setString(1, id);
              ps.setString(2, jsonData);
              ps.addBatch();
            }
          }
          ps.executeBatch();
          conn.commit();
          // Update cache ONLY after successful database commit
          if (cacheEnabled && cache != null) {
            cache.putAll(objects);
          }
        }
      } catch (SQLException e) {
        // Rollback on any SQL error
        try {
          conn.rollback();
        } catch (SQLException rollbackEx) {
          TownsAndNations.getPlugin()
              .getLogger()
              .severe(
                  "Error rolling back transaction for "
                      + typeClass.getSimpleName()
                      + ": "
                      + rollbackEx.getMessage());
        }
        throw e;
      } finally {
        // Always restore original autoCommit setting
        try {
          conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException e) {
          TownsAndNations.getPlugin()
              .getLogger()
              .warning("Error restoring autoCommit: " + e.getMessage());
        }
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe(
              "Error batch storing " + typeClass.getSimpleName() + " objects: " + e.getMessage());
    }
  }
  @Deprecated
  public void delete(String id) {
    if (id == null) {
      return;
    }
    String deleteSQL = "DELETE FROM " + tableName + " WHERE id = ?";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
      ps.setString(1, id);
      ps.executeUpdate();
      invalidateCache(id);
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe(
              "Error deleting "
                  + typeClass.getSimpleName()
                  + " with ID "
                  + id
                  + ": "
                  + e.getMessage());
    }
  }
  public CompletableFuture<Void> deleteAsync(String id) {
    if (id == null) {
      return CompletableFuture.completedFuture(null);
    }
    invalidateCache(id);
    CompletableFuture<Void> future = new CompletableFuture<>();
    String deleteSQL = "DELETE FROM " + tableName + " WHERE id = ?";
    runAsync(
        () -> {
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
            ps.setString(1, id);
            ps.executeUpdate();
            future.complete(null);
          } catch (SQLException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .severe(
                    "Error deleting "
                        + typeClass.getSimpleName()
                        + " with ID "
                        + id
                        + ": "
                        + e.getMessage());
            future.completeExceptionally(e);
          }
        });
    return future;
  }
  public void deleteAll(Collection<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    String deleteSQL = "DELETE FROM " + tableName + " WHERE id = ?";
    // Use try-with-resources to ensure connection is always closed, even on error
    try (Connection conn = getDatabase().getDataSource().getConnection()) {
      boolean originalAutoCommit = conn.getAutoCommit();
      try {
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
          for (String id : ids) {
            if (id != null) {
              ps.setString(1, id);
              ps.addBatch();
            }
          }
          ps.executeBatch();
          conn.commit();
          // Invalidate cache ONLY after successful database commit
          for (String id : ids) {
            if (id != null) {
              invalidateCache(id);
            }
          }
        }
      } catch (SQLException e) {
        // Rollback on any SQL error
        try {
          conn.rollback();
        } catch (SQLException rollbackEx) {
          TownsAndNations.getPlugin()
              .getLogger()
              .severe(
                  "Error rolling back transaction for "
                      + typeClass.getSimpleName()
                      + ": "
                      + rollbackEx.getMessage());
        }
        throw e;
      } finally {
        // Always restore original autoCommit setting
        try {
          conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException e) {
          TownsAndNations.getPlugin()
              .getLogger()
              .warning("Error restoring autoCommit: " + e.getMessage());
        }
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe(
              "Error batch deleting " + typeClass.getSimpleName() + " objects: " + e.getMessage());
    }
  }
  public boolean exists(String id) {
    if (cacheEnabled && cache != null) {
      // Synchronized LRUCache.containsKey is thread-safe
      if (cache.containsKey(id)) {
        return true;
      }
    }
    String selectSQL = "SELECT 1 FROM " + tableName + " WHERE id = ?";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(selectSQL)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe(
              "Error checking existence of "
                  + typeClass.getSimpleName()
                  + " with ID "
                  + id
                  + ": "
                  + e.getMessage());
    }
    return false;
  }
  public int count() {
    String countSQL = "SELECT COUNT(*) FROM " + tableName;
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(countSQL);
        ResultSet rs = ps.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe("Error counting " + typeClass.getSimpleName() + " objects: " + e.getMessage());
    }
    return 0;
  }
  public CompletableFuture<Map<String, T>> getBatch(Collection<String> ids) {
    CompletableFuture<Map<String, T>> future = new CompletableFuture<>();
    if (ids == null || ids.isEmpty()) {
      future.complete(new LinkedHashMap<>());
      return future;
    }
    Map<String, T> result = new LinkedHashMap<>();
    List<String> uncachedIds = new ArrayList<>();
    if (cacheEnabled && cache != null) {
      // Synchronized LRUCache.get is thread-safe
      for (String id : ids) {
        T cached = cache.get(id);
        if (cached != null) {
          result.put(id, cached);
        } else {
          uncachedIds.add(id);
        }
      }
    } else {
      uncachedIds.addAll(ids);
    }
    if (uncachedIds.isEmpty()) {
      future.complete(result);
      return future;
    }
    runAsync(
        () -> {
          String placeholders =
              String.join(",", java.util.Collections.nCopies(uncachedIds.size(), "?"));
          String selectSQL = "SELECT id, data FROM " + tableName + " WHERE id IN (" + placeholders + ")";
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(selectSQL)) {
            int idx = 1;
            for (String id : uncachedIds) {
              ps.setString(idx++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
              while (rs.next()) {
                String id = rs.getString("id");
                String jsonData = rs.getString("data");
                try {
                  T object = gson.fromJson(jsonData, typeToken);
                  if (object != null) {
                    result.put(id, object);
                    if (cacheEnabled && cache != null) {
                      cache.put(id, object);
                    }
                  }
                } catch (JsonSyntaxException e) {
                  TownsAndNations.getPlugin()
                      .getLogger()
                      .warning(
                          "Failed to deserialize "
                              + typeClass.getSimpleName()
                              + " with ID "
                              + id
                              + ": "
                              + e.getMessage());
                }
              }
            }
            future.complete(result);
          } catch (SQLException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .severe(
                    "Error batch retrieving "
                        + typeClass.getSimpleName()
                        + " objects: "
                        + e.getMessage());
            future.completeExceptionally(e);
          }
        });
    return future;
  }
  public Map<String, T> getBatchSync(Collection<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return new LinkedHashMap<>();
    }
    Map<String, T> result = new LinkedHashMap<>();
    List<String> uncachedIds = new ArrayList<>();
    if (cacheEnabled && cache != null) {
      // Synchronized LRUCache.get is thread-safe
      for (String id : ids) {
        T cached = cache.get(id);
        if (cached != null) {
          result.put(id, cached);
        } else {
          uncachedIds.add(id);
        }
      }
    } else {
      uncachedIds.addAll(ids);
    }
    if (uncachedIds.isEmpty()) {
      return result;
    }
    String placeholders =
        String.join(",", java.util.Collections.nCopies(uncachedIds.size(), "?"));
    String selectSQL = "SELECT id, data FROM " + tableName + " WHERE id IN (" + placeholders + ")";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(selectSQL)) {
      int idx = 1;
      for (String id : uncachedIds) {
        ps.setString(idx++, id);
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String id = rs.getString("id");
          String jsonData = rs.getString("data");
          try {
            T object = gson.fromJson(jsonData, typeToken);
            if (object != null) {
              result.put(id, object);
              if (cacheEnabled && cache != null) {
                cache.put(id, object);
              }
            }
          } catch (JsonSyntaxException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .warning(
                    "Failed to deserialize "
                        + typeClass.getSimpleName()
                        + " with ID "
                        + id
                        + ": "
                        + e.getMessage());
          }
        }
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe(
              "Error batch retrieving "
                  + typeClass.getSimpleName()
                  + " objects: "
                  + e.getMessage());
    }
    return result;
  }
  public CompletableFuture<Map<String, T>> getPaginated(int offset, int limit) {
    CompletableFuture<Map<String, T>> future = new CompletableFuture<>();
    runAsync(
        () -> {
          Map<String, T> result = new LinkedHashMap<>();
          String selectSQL = "SELECT id, data FROM " + tableName + " LIMIT ? OFFSET ?";
          try (Connection conn = getDatabase().getDataSource().getConnection();
              PreparedStatement ps = conn.prepareStatement(selectSQL)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
              while (rs.next()) {
                String id = rs.getString("id");
                String jsonData = rs.getString("data");
                try {
                  T object = gson.fromJson(jsonData, typeToken);
                  if (object != null) {
                    result.put(id, object);
                    if (cacheEnabled && cache != null) {
                      cache.put(id, object);
                    }
                  }
                } catch (JsonSyntaxException e) {
                  TownsAndNations.getPlugin()
                      .getLogger()
                      .warning(
                          "Failed to deserialize "
                              + typeClass.getSimpleName()
                              + " with ID "
                              + id
                              + ": "
                              + e.getMessage());
                }
              }
            }
            future.complete(result);
          } catch (SQLException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .severe(
                    "Error retrieving paginated "
                        + typeClass.getSimpleName()
                        + " objects: "
                        + e.getMessage());
            future.completeExceptionally(e);
          }
        });
    return future;
  }
  public CompletableFuture<Void> processBatches(
      int batchSize, java.util.function.Consumer<Map<String, T>> consumer) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    runAsync(
        () -> {
          try {
            int offset = 0;
            boolean hasMore = true;
            while (hasMore) {
              // Use synchronous DB load directly — we're already on an async thread,
              // so no need to schedule another async task via getPaginated().join().
              Map<String, T> batch = loadPaginatedSync(offset, batchSize);
              if (batch.isEmpty()) {
                hasMore = false;
              } else {
                consumer.accept(batch);
                offset += batchSize;
              }
            }
            future.complete(null);
          } catch (Exception e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .severe(
                    "Error processing batches for "
                        + typeClass.getSimpleName()
                        + ": "
                        + e.getMessage());
            future.completeExceptionally(e);
          }
        });
    return future;
  }

  /**
   * Loads a paginated batch synchronously — intended for use inside already-async contexts
   * (like processBatches) to avoid nested async scheduling and .join() blocking.
   */
  private Map<String, T> loadPaginatedSync(int offset, int limit) {
    Map<String, T> result = new LinkedHashMap<>();
    String selectSQL = "SELECT id, data FROM " + tableName + " LIMIT ? OFFSET ?";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(selectSQL)) {
      ps.setInt(1, limit);
      ps.setInt(2, offset);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String id = rs.getString("id");
          String jsonData = rs.getString("data");
          try {
            T object = gson.fromJson(jsonData, typeToken);
            if (object != null) {
              result.put(id, object);
            }
          } catch (JsonSyntaxException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .warning(
                    "Failed to deserialize "
                        + typeClass.getSimpleName()
                        + " with ID "
                        + id
                        + ": "
                        + e.getMessage());
          }
        }
      }
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe(
              "Error loading paginated "
                  + typeClass.getSimpleName()
                  + " objects: "
                  + e.getMessage());
    }
    return result;
  }

  public abstract void reset();
}