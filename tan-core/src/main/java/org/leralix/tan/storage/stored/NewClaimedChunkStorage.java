package org.leralix.tan.storage.stored;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.chunk.*;
import org.leralix.tan.dataclass.territory.TerritoryData;
public class NewClaimedChunkStorage extends DatabaseStorage<ClaimedChunk2> {
  private static final String TABLE_NAME = "ccn_claimed_chunks";
  private static volatile NewClaimedChunkStorage instance;
  private NewClaimedChunkStorage() {
    super(TABLE_NAME, ClaimedChunk2.class, new GsonBuilder().setPrettyPrinting().create());
  }
  public static NewClaimedChunkStorage getInstance() {
    if (instance == null) {
      synchronized (NewClaimedChunkStorage.class) {
        if (instance == null) {
          instance = new NewClaimedChunkStorage();
        }
      }
    }
    return instance;
  }
  public ClaimedChunk2 getFromCacheOrNull(String id) {
    if (id == null) {
      return null;
    }
    if (cacheEnabled && cache != null) {
      // Thread-safe: ConcurrentHashMap provides lock-free reads
      ClaimedChunk2 cached = cache.get(id);
      if (cached != null) {
        return cached;
      }
    }
    get(id)
        .thenAccept(
            chunk -> {
            });
    return null;
  }
  @Deprecated
  public ClaimedChunk2 getSync(String id) {
    return null;
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
  @Override
  protected void createIndexes() {
    String createOwnerIndexSQL =
        "CREATE INDEX IF NOT EXISTS idx_chunk_owner ON "
            + TABLE_NAME
            + " ((CAST(JSON_EXTRACT(data, '$.ownerID') AS CHAR(255))))";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(createOwnerIndexSQL);
      TownsAndNations.getPlugin()
          .getLogger()
          .info("Created index idx_chunk_owner on " + TABLE_NAME);
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning(
              "Could not create functional index on "
                  + TABLE_NAME
                  + ", this is normal for older MySQL versions: "
                  + e.getMessage());
    }
  }
  private static String getChunkKey(Chunk chunk) {
    return getChunkKey(chunk.getX(), chunk.getZ(), chunk.getWorld().getUID().toString());
  }
  private static String getChunkKey(ClaimedChunk2 chunk) {
    return getChunkKey(chunk.getX(), chunk.getZ(), chunk.getWorldUUID());
  }
  private static String getChunkKey(int x, int z, String chunkWorldUID) {
    return x + "," + z + "," + chunkWorldUID;
  }
  /**
   * Get all claimed chunks asynchronously.
   * @return CompletableFuture with map of all claimed chunks
   */
  public CompletableFuture<Map<String, ClaimedChunk2>> getClaimedChunksMapAsync() {
    return getAllAsync();
  }

  /**
   * @deprecated Use {@link #getClaimedChunksMapAsync()} to avoid blocking Folia region threads
   */
  @Deprecated
  public Map<String, ClaimedChunk2> getClaimedChunksMap() {
    return new HashMap<>();
  }
  public boolean isChunkClaimed(Chunk chunk) {
    String key = getChunkKey(chunk);
    if (cacheEnabled && cache != null) {
      // Thread-safe: ConcurrentHashMap provides lock-free reads
      if (cache.containsKey(key)) {
        return true;
      }
    }
    return exists(key);
  }

  /**
   * Check if a chunk is claimed asynchronously.
   * Checks cache first (fast path), then database without blocking.
   *
   * @param chunk the chunk to check
   * @return CompletableFuture with true if the chunk is claimed
   */
  public CompletableFuture<Boolean> isChunkClaimedAsync(Chunk chunk) {
    String key = getChunkKey(chunk);
    if (cacheEnabled && cache != null) {
      if (cache.containsKey(key)) {
        return CompletableFuture.completedFuture(true);
      }
    }
    return get(key).thenApply(Objects::nonNull);
  }
  public Collection<TerritoryChunk> getAllChunkFrom(TerritoryData territoryData) {
    return getAllChunkFrom(territoryData.getID());
  }
  public Collection<TerritoryChunk> getAllChunkFrom(String territoryDataID) {
    List<TerritoryChunk> chunks = new ArrayList<>();
    String selectSQL =
        "SELECT id, data FROM " + TABLE_NAME + " WHERE json_extract(data, '$.ownerID') = ?";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(selectSQL)) {
      ps.setString(1, territoryDataID);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String jsonData = rs.getString("data");
          ClaimedChunk2 chunk = deserializeChunk(jsonData);
          if (chunk instanceof TerritoryChunk territoryChunk) {
            chunks.add(territoryChunk);
          }
        }
      }
      return Collections.unmodifiableCollection(chunks);
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning("Error optimized query for territory chunks: " + e.getMessage());
      return Collections.unmodifiableCollection(chunks);
    }
  }

  /**
   * Get all territory chunks for a given territory asynchronously.
   * Uses optimized SQL query with async fallback.
   *
   * @param territoryData the territory to get chunks for
   * @return CompletableFuture with the collection of territory chunks
   */
  public CompletableFuture<Collection<TerritoryChunk>> getAllChunkFromAsync(TerritoryData territoryData) {
    return getAllChunkFromAsync(territoryData.getID());
  }

  /**
   * Get all territory chunks for a given territory ID asynchronously.
   *
   * @param territoryDataID the territory ID to get chunks for
   * @return CompletableFuture with the collection of territory chunks
   */
  public CompletableFuture<Collection<TerritoryChunk>> getAllChunkFromAsync(String territoryDataID) {
    CompletableFuture<Collection<TerritoryChunk>> future = new CompletableFuture<>();
    runAsync(() -> {
      List<TerritoryChunk> chunks = new ArrayList<>();
      String selectSQL =
          "SELECT id, data FROM " + TABLE_NAME + " WHERE json_extract(data, '$.ownerID') = ?";
      try (Connection conn = getDatabase().getDataSource().getConnection();
          PreparedStatement ps = conn.prepareStatement(selectSQL)) {
        ps.setString(1, territoryDataID);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            String jsonData = rs.getString("data");
            ClaimedChunk2 chunk = deserializeChunk(jsonData);
            if (chunk instanceof TerritoryChunk territoryChunk) {
              chunks.add(territoryChunk);
            }
          }
        }
        future.complete(Collections.unmodifiableCollection(chunks));
      } catch (SQLException e) {
        TownsAndNations.getPlugin()
            .getLogger()
            .warning("Error optimized async query, falling back to full scan: " + e.getMessage());
        getAllAsync().thenAccept(allChunks -> {
          for (ClaimedChunk2 chunk : allChunks.values()) {
            if (chunk instanceof TerritoryChunk territoryChunk
                && territoryChunk.getOwnerID().equals(territoryDataID)) {
              chunks.add(territoryChunk);
            }
          }
          future.complete(Collections.unmodifiableCollection(chunks));
        }).exceptionally(ex -> {
          future.completeExceptionally(ex);
          return null;
        });
      }
    });
    return future;
  }
  private ClaimedChunk2 deserializeChunk(String jsonData) {
    JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
    JsonElement ownerIdElement = jsonObject.get("ownerID");
    if (ownerIdElement == null || ownerIdElement.isJsonNull()) {
      return null;
    }
    String ownerId = ownerIdElement.getAsString();
    if (ownerId.startsWith("T")) {
      return gson.fromJson(jsonData, TownClaimedChunk.class);
    } else if (ownerId.startsWith("R")) {
      return gson.fromJson(jsonData, RegionClaimedChunk.class);
    } else if (ownerId.startsWith("L")) {
      return gson.fromJson(jsonData, LandmarkClaimedChunk.class);
    }
    return null;
  }
  /**
   * Claim a chunk for a town (ASYNC for Folia performance).
   * PERFORMANCE: Returns CompletableFuture instead of blocking with .join()
   */
  public CompletableFuture<TownClaimedChunk> claimTownChunkAsync(Chunk chunk, String ownerID) {
    TownClaimedChunk townClaimedChunk = new TownClaimedChunk(chunk, ownerID);
    return putAsync(getChunkKey(chunk), townClaimedChunk)
        .thenApply(v -> {
          // Invalidate permission cache for this chunk
          org.leralix.tan.service.PermissionCache.getInstance().invalidateChunk(
              chunk.getX(), chunk.getZ(), chunk.getWorld().getUID().toString());
          return townClaimedChunk;
        });
  }

  /**
   * Claim a chunk for a town (SYNC - deprecated, use async version).
   * @deprecated Use {@link #claimTownChunkAsync(Chunk, String)} for Folia compatibility
   */
  @Deprecated
  public TownClaimedChunk claimTownChunk(Chunk chunk, String ownerID) {
    return claimTownChunkAsync(chunk, ownerID).join();
  }

  /**
   * Claim a chunk for a region (ASYNC for Folia performance).
   * PERFORMANCE: Returns CompletableFuture instead of blocking with .join()
   */
  public CompletableFuture<Void> claimRegionChunkAsync(Chunk chunk, String ownerID) {
    return putAsync(getChunkKey(chunk), new RegionClaimedChunk(chunk, ownerID))
        .thenAccept(v -> {
          // Invalidate permission cache for this chunk
          org.leralix.tan.service.PermissionCache.getInstance().invalidateChunk(
              chunk.getX(), chunk.getZ(), chunk.getWorld().getUID().toString());
        });
  }

  /**
   * Claim a chunk for a region (SYNC - deprecated, use async version).
   * @deprecated Use {@link #claimRegionChunkAsync(Chunk, String)} for Folia compatibility
   */
  @Deprecated
  public void claimRegionChunk(Chunk chunk, String ownerID) {
    claimRegionChunkAsync(chunk, ownerID);
  }

  /**
   * Claim a chunk for a landmark (ASYNC for Folia performance).
   * PERFORMANCE: Returns CompletableFuture instead of blocking with .join()
   */
  public CompletableFuture<Void> claimLandmarkChunkAsync(Chunk chunk, String ownerID) {
    return putAsync(getChunkKey(chunk), new LandmarkClaimedChunk(chunk, ownerID))
        .thenAccept(v -> {
          // Invalidate permission cache for this chunk
          org.leralix.tan.service.PermissionCache.getInstance().invalidateChunk(
              chunk.getX(), chunk.getZ(), chunk.getWorld().getUID().toString());
        });
  }

  /**
   * Claim a chunk for a landmark (SYNC - deprecated, use async version).
   * @deprecated Use {@link #claimLandmarkChunkAsync(Chunk, String)} for Folia compatibility
   */
  @Deprecated
  public void claimLandmarkChunk(Chunk chunk, String ownerID) {
    claimLandmarkChunkAsync(chunk, ownerID);
  }
  public CompletableFuture<Boolean> isAllAdjacentChunksClaimedBySameTerritoryAsync(
      Chunk chunk, String territoryID) {
    List<String> adjacentChunkKeys =
        Arrays.asList(
            getChunkKey(chunk.getX() + 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
            getChunkKey(chunk.getX() - 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
            getChunkKey(chunk.getX(), chunk.getZ() + 1, chunk.getWorld().getUID().toString()),
            getChunkKey(chunk.getX(), chunk.getZ() - 1, chunk.getWorld().getUID().toString()));
    List<CompletableFuture<ClaimedChunk2>> futures =
        adjacentChunkKeys.stream().map(this::get).toList();
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(
            v -> {
              for (CompletableFuture<ClaimedChunk2> future : futures) {
                ClaimedChunk2 adjacentClaimedChunk = future.join();
                if (adjacentClaimedChunk == null) {
                  return false;
                }
                if (adjacentClaimedChunk instanceof TerritoryChunk territoryChunk) {
                  if (!territoryChunk.getOccupierID().equals(territoryID)) {
                    return false;
                  }
                }
              }
              return true;
            });
  }
  @Deprecated
  public boolean isAllAdjacentChunksClaimedBySameTerritory(Chunk chunk, String territoryID) {
    return false;
  }
  public CompletableFuture<Boolean> isOneAdjacentChunkClaimedBySameTerritoryAsync(
      Chunk chunk, String townID) {
    List<String> adjacentChunkKeys =
        Arrays.asList(
            getChunkKey(chunk.getX() + 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
            getChunkKey(chunk.getX() - 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
            getChunkKey(chunk.getX(), chunk.getZ() + 1, chunk.getWorld().getUID().toString()),
            getChunkKey(chunk.getX(), chunk.getZ() - 1, chunk.getWorld().getUID().toString()));
    List<CompletableFuture<ClaimedChunk2>> futures =
        adjacentChunkKeys.stream().map(this::get).toList();
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(
            v -> {
              for (CompletableFuture<ClaimedChunk2> future : futures) {
                ClaimedChunk2 adjacentClaimedChunk = future.join();
                if (adjacentClaimedChunk != null
                    && adjacentClaimedChunk.getOwnerID().equals(townID)) {
                  return true;
                }
              }
              return false;
            });
  }
  @Deprecated
  public boolean isOneAdjacentChunkClaimedBySameTerritory(Chunk chunk, String townID) {
    return false;
  }
  public void unclaimChunkAndUpdate(ClaimedChunk2 claimedChunk) {
    unclaimChunk(claimedChunk);
    claimedChunk.notifyUpdate();
  }

  /**
   * Unclaim a chunk (ASYNC for Folia performance).
   * PERFORMANCE: Returns CompletableFuture instead of blocking with .join()
   */
  public CompletableFuture<Void> unclaimChunkAsync(ClaimedChunk2 claimedChunk) {
    // Invalidate permission cache first (fast, non-blocking)
    org.leralix.tan.service.PermissionCache.getInstance().invalidateChunk(
        claimedChunk.getX(), claimedChunk.getZ(), claimedChunk.getWorldUUID());
    // Then delete from database asynchronously
    return deleteAsync(getChunkKey(claimedChunk));
  }

  /**
   * Unclaim a chunk (SYNC - deprecated, use async version).
   * @deprecated Use {@link #unclaimChunkAsync(ClaimedChunk2)} for Folia compatibility
   */
  @Deprecated
  public void unclaimChunk(ClaimedChunk2 claimedChunk) {
    unclaimChunkAsync(claimedChunk);
  }

  /**
   * Unclaim a chunk by location (ASYNC for Folia performance).
   */
  public CompletableFuture<Void> unclaimChunkAsync(Chunk chunk) {
    ClaimedChunk2 claimedChunk = get(chunk);
    if (claimedChunk instanceof WildernessChunk) {
      return CompletableFuture.completedFuture(null);
    }
    return unclaimChunkAsync(claimedChunk);
  }

  /**
   * Unclaim a chunk by location (SYNC - deprecated, use async version).
   * @deprecated Use {@link #unclaimChunkAsync(Chunk)} for Folia compatibility
   */
  @Deprecated
  public void unclaimChunk(Chunk chunk) {
    unclaimChunkAsync(chunk);
  }
  public @NotNull List<ClaimedChunk2> getFourAjacentChunks(ClaimedChunk2 chunk) {
    return Arrays.asList(
        get(chunk.getX(), chunk.getZ() - 1, chunk.getWorld().getUID().toString()),
        get(chunk.getX() + 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
        get(chunk.getX(), chunk.getZ() + 1, chunk.getWorld().getUID().toString()),
        get(chunk.getX() - 1, chunk.getZ(), chunk.getWorld().getUID().toString())
        );
  }
  public @NotNull List<ClaimedChunk2> getEightAjacentChunks(ClaimedChunk2 chunk) {
    return Arrays.asList(
        get(chunk.getX(), chunk.getZ() - 1, chunk.getWorld().getUID().toString()),
        get(
            chunk.getX() + 1,
            chunk.getZ() - 1,
            chunk.getWorld().getUID().toString()),
        get(chunk.getX() + 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
        get(chunk.getX() + 1, chunk.getZ() + 1, chunk.getWorld().getUID().toString()),
        get(chunk.getX(), chunk.getZ() + 1, chunk.getWorld().getUID().toString()),
        get(chunk.getX() - 1, chunk.getZ() + 1, chunk.getWorld().getUID().toString()),
        get(chunk.getX() - 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
        get(chunk.getX() - 1, chunk.getZ() - 1, chunk.getWorld().getUID().toString())
        );
  }
  public void unclaimAllChunksFromTerritory(TerritoryData territoryData) {
    unclaimAllChunkFromID(territoryData.getID());
  }
  public void unclaimAllChunkFromID(String id) {
    String deleteSQL = "DELETE FROM " + TABLE_NAME + " WHERE json_extract(data, '$.ownerID') = ?";
    try (Connection conn = getDatabase().getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
      ps.setString(1, id);
      int deleted = ps.executeUpdate();
      TownsAndNations.getPlugin()
          .getLogger()
          .info("Deleted " + deleted + " chunks for territory " + id);
      invalidateCacheIf(chunk -> chunk.getOwnerID().equals(id));
    } catch (SQLException e) {
      TownsAndNations.getPlugin()
          .getLogger()
          .warning(
              "Error in optimized delete, falling back to individual deletes: " + e.getMessage());
      // Use async version for the fallback to avoid blocking
      unclaimAllChunkFromIDAsync(id).exceptionally(ex -> {
        TownsAndNations.getPlugin()
            .getLogger()
            .severe("Error in async fallback delete for territory " + id + ": " + ex.getMessage());
        return null;
      });
    }
  }

  /**
   * Unclaim all chunks for a territory asynchronously.
   *
   * @param id the territory ID whose chunks should be removed
   * @return CompletableFuture that completes when all chunks are removed
   */
  public CompletableFuture<Void> unclaimAllChunkFromIDAsync(String id) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    runAsync(() -> {
      String deleteSQL = "DELETE FROM " + TABLE_NAME + " WHERE json_extract(data, '$.ownerID') = ?";
      try (Connection conn = getDatabase().getDataSource().getConnection();
          PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
        ps.setString(1, id);
        int deleted = ps.executeUpdate();
        TownsAndNations.getPlugin()
            .getLogger()
            .info("Deleted " + deleted + " chunks for territory " + id);
        invalidateCacheIf(chunk -> chunk.getOwnerID().equals(id));
        future.complete(null);
      } catch (SQLException e) {
        TownsAndNations.getPlugin()
            .getLogger()
            .warning(
                "Error in async optimized delete, falling back to individual deletes: " + e.getMessage());
        getAllAsync().thenAccept(allChunks -> {
          List<String> toDelete = new ArrayList<>();
          for (Map.Entry<String, ClaimedChunk2> entry : allChunks.entrySet()) {
            ClaimedChunk2 chunk = entry.getValue();
            if (chunk.getOwnerID().equals(id)) {
              toDelete.add(entry.getKey());
            }
          }
          deleteAll(toDelete);
          future.complete(null);
        }).exceptionally(ex -> {
          future.completeExceptionally(ex);
          return null;
        });
      }
    });
    return future;
  }
  public ClaimedChunk2 get(int x, int z, String worldID) {
    ClaimedChunk2 claimedChunk = getFromCacheOrNull(getChunkKey(x, z, worldID));
    if (claimedChunk == null) {
      return new WildernessChunk(x, z, worldID);
    }
    return claimedChunk;
  }
  public @NotNull ClaimedChunk2 get(Chunk chunk) {
    ClaimedChunk2 claimedChunk = getFromCacheOrNull(getChunkKey(chunk));
    if (claimedChunk == null) {
      return new WildernessChunk(chunk);
    }
    return claimedChunk;
  }
  public CompletableFuture<Void> preloadChunksAsync(
      int centerX, int centerZ, String worldID, int radius) {
    List<CompletableFuture<ClaimedChunk2>> futures = new ArrayList<>();
    for (int x = centerX - radius; x <= centerX + radius; x++) {
      for (int z = centerZ - radius; z <= centerZ + radius; z++) {
        futures.add(get(getChunkKey(x, z, worldID)));
      }
    }
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
  }
  @Override
  public void reset() {
    instance = null;
  }
}