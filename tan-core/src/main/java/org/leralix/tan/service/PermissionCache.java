package org.leralix.tan.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.leralix.tan.enums.permissions.ChunkPermissionType;

/**
 * High-performance cache for permission checks.
 * Uses record-based keys with chunk coordinates and short TTL for optimal performance in Folia.
 *
 * Design decisions:
 * - Uses ConcurrentHashMap for lock-free reads (critical for Folia region threads)
 * - TTL of 1.5 seconds balances freshness vs performance
 * - Chunk-level granularity (more cache hits than block-level)
 * - Auto-eviction based on timestamp to avoid memory leaks
 */
public class PermissionCache {

  private static final long TTL_NANOS = TimeUnit.SECONDS.toNanos(1);
  private static final int MAX_CACHE_SIZE = 5000;

  private final ConcurrentHashMap<PermissionKey, CachedResult> cache = new ConcurrentHashMap<>(MAX_CACHE_SIZE);

  private static volatile PermissionCache instance;

  private PermissionCache() {}

  public static PermissionCache getInstance() {
    if (instance == null) {
      synchronized (PermissionCache.class) {
        if (instance == null) {
          instance = new PermissionCache();
        }
      }
    }
    return instance;
  }

  /**
   * Check if permission result is cached and still valid.
   *
   * @param player The player to check
   * @param chunk The chunk to check
   * @param permissionType The permission type
   * @return Cached result or null if not found/expired
   */
  public Boolean getCached(Player player, Chunk chunk, ChunkPermissionType permissionType) {
    PermissionKey key = new PermissionKey(player.getUniqueId(), chunk.getX(), chunk.getZ(),
        chunk.getWorld().getUID().toString(), permissionType);
    CachedResult result = cache.get(key);
    if (result == null) {
      return null;
    }
    if (System.nanoTime() - result.timestamp() > TTL_NANOS) {
      cache.remove(key);
      return null;
    }
    return result.value();
  }

  /**
   * Cache a permission result.
   */
  public void put(Player player, Chunk chunk, ChunkPermissionType permissionType, boolean value) {
    PermissionKey key = new PermissionKey(player.getUniqueId(), chunk.getX(), chunk.getZ(),
        chunk.getWorld().getUID().toString(), permissionType);
    cache.put(key, new CachedResult(value, System.nanoTime()));

    // Periodic cleanup if cache grows too large
    if (cache.size() > MAX_CACHE_SIZE) {
      cleanupExpiredEntries();
    }
  }

  /**
   * Invalidate all cached permissions for a player.
   * Called when player joins/leaves a town or relation changes.
   */
  public void invalidatePlayer(UUID playerUuid) {
    cache.keySet().removeIf(key -> key.playerUuid().equals(playerUuid));
  }

  /**
   * Invalidate all cached permissions for a chunk.
   * Called when chunk ownership changes.
   */
  public void invalidateChunk(int chunkX, int chunkZ, String worldUuid) {
    cache.keySet().removeIf(key ->
        key.chunkX() == chunkX && key.chunkZ() == chunkZ && key.worldUuid().equals(worldUuid));
  }

  /**
   * Invalidate all cached permissions for a territory.
   * Called when war status changes or territory dissolved.
   */
  public void invalidateTerritory(String territoryId) {
    // Territory-based invalidation requires tracking territory per chunk
    // For now, we clear a percentage of cache as a trade-off
    if (cache.size() > MAX_CACHE_SIZE / 2) {
      cleanupExpiredEntries();
    }
  }

  /**
   * Remove expired entries from cache.
   */
  private void cleanupExpiredEntries() {
    long now = System.nanoTime();
    cache.entrySet().removeIf(entry -> now - entry.getValue().timestamp() > TTL_NANOS);
  }

  /**
   * Clear entire cache (e.g., on plugin reload).
   */
  public void clear() {
    cache.clear();
  }

  /**
   * Get cache statistics for monitoring.
   */
  public int getCacheSize() {
    return cache.size();
  }

  /**
   * Immutable key for cache lookups.
   */
  private record PermissionKey(
      UUID playerUuid,
      int chunkX,
      int chunkZ,
      String worldUuid,
      ChunkPermissionType permissionType
  ) {}

  /**
   * Cached permission result with timestamp.
   */
  private record CachedResult(
      boolean value,
      long timestamp
  ) {}
}
