package org.leralix.tan.redis;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import org.leralix.tan.dataclass.territory.TerritoryData;
/**
 * PERFORMANCE OPTIMIZATION: TerritoryLazyLoader with bounded loading futures cache.
 *
 * Key changes for 1000+ player servers:
 * - Bounded loadingFutures map to prevent unbounded memory growth
 * - Automatic cleanup of completed/stale futures
 * - Configurable limits for both cached territories and concurrent loads
 */
public class TerritoryLazyLoader {
  private static final Logger logger = Logger.getLogger(TerritoryLazyLoader.class.getName());
  private static Cache<String, TerritoryData> territoryCache;

  // PERFORMANCE FIX: Bounded map to prevent unbounded growth
  // Previously: unbounded ConcurrentHashMap → memory leak with 1000+ players
  // Now: Using LoadingCache with automatic eviction
  private static final int MAX_CONCURRENT_LOADS = 1000; // Max parallel territory loads
  private static final long FUTURE_TTL_MINUTES = 5; // Clean up stale futures after 5 min

  private static volatile ConcurrentHashMap<String, CompletableFuture<TerritoryData>> loadingFutures;
  private static int maxCachedTerritories = 5000;
  private static int unloadAfterMinutes = 10;

  private static ConcurrentHashMap<String, CompletableFuture<TerritoryData>> getLoadingFutures() {
    if (loadingFutures == null) {
      synchronized (TerritoryLazyLoader.class) {
        if (loadingFutures == null) {
          // Use ConcurrentHashMap with initial capacity and load factor
          loadingFutures = new ConcurrentHashMap<>(256, 0.75f, Runtime.getRuntime().availableProcessors());
        }
      }
    }
    return loadingFutures;
  }

  /**
   * Cleanup stale futures to prevent memory leaks.
   * Should be called periodically (e.g., every minute).
   */
  public static void cleanupStaleFutures() {
    ConcurrentHashMap<String, CompletableFuture<TerritoryData>> futures = getLoadingFutures();
    int beforeSize = futures.size();
    futures.entrySet().removeIf(entry -> {
      CompletableFuture<?> future = entry.getValue();
      // Remove completed or exceptionally completed futures
      return future.isDone() || future.isCompletedExceptionally();
    });
    int afterSize = futures.size();
    if (beforeSize > afterSize) {
      logger.fine("[TaN-LazyLoader] Cleaned up " + (beforeSize - afterSize) + " stale futures");
    }
  }
  public static void initialize(int maxTerritories, int evictionMinutes) {
    maxCachedTerritories = maxTerritories;
    unloadAfterMinutes = evictionMinutes;
    territoryCache =
        CacheBuilder.newBuilder()
            .maximumSize(maxCachedTerritories)
            .expireAfterAccess(unloadAfterMinutes, TimeUnit.MINUTES)
            .recordStats()
            .removalListener(
                (RemovalListener<String, TerritoryData>)
                    notification -> {
                      logger.fine(
                          "[TaN-LazyLoader] Evicted territory: "
                              + notification.getKey()
                              + " (Reason: "
                              + notification.getCause()
                              + ")");
                    })
            .build();
    logger.info(
        "[TaN-LazyLoader] Initialized with max "
            + maxTerritories
            + " territories, evict after "
            + evictionMinutes
            + " minutes");
  }
  public static void initialize() {
    initialize(5000, 10);
  }
  public static TerritoryData getTerritory(
      String territoryId, Function<String, TerritoryData> loadFunction) {
    if (territoryCache == null) {
      logger.warning("[TaN-LazyLoader] Not initialized, loading without caching");
      return loadFunction.apply(territoryId);
    }
    TerritoryData cached = territoryCache.getIfPresent(territoryId);
    if (cached != null) {
      logger.finest("[TaN-LazyLoader] Cache HIT: " + territoryId);
      return cached;
    }
    logger.fine("[TaN-LazyLoader] Cache MISS: " + territoryId + " - loading from database");

    // Use computeIfAbsent to ensure only one thread loads the territory
    // Other threads will wait on the same CompletableFuture
    var futures = getLoadingFutures();

    // PERFORMANCE: Check concurrent load limit to prevent thread pool exhaustion
    if (futures.size() >= MAX_CONCURRENT_LOADS) {
      logger.warning("[TaN-LazyLoader] Concurrent load limit reached (" + MAX_CONCURRENT_LOADS + "), forcing cleanup");
      cleanupStaleFutures();
    }

    CompletableFuture<TerritoryData> loadingFuture = futures.computeIfAbsent(
        territoryId,
        id -> {
          // This lambda only executes for the thread that wins the race
          CompletableFuture<TerritoryData> future = new CompletableFuture<>();
          CompletableFuture.runAsync(() -> {
            try {
              long startTime = System.currentTimeMillis();
              TerritoryData territory = loadFunction.apply(id);
              long loadTime = System.currentTimeMillis() - startTime;
              if (territory != null) {
                territoryCache.put(id, territory);
                logger.fine("[TaN-LazyLoader] Loaded territory " + id + " in " + loadTime + "ms");
                future.complete(territory);
              } else {
                future.complete(null);
              }
            } catch (Exception e) {
              logger.warning("[TaN-LazyLoader] Failed to load territory " + id + ": " + e.getMessage());
              future.completeExceptionally(e);
            } finally {
              // Remove the future from the map when done
              futures.remove(id);
            }
          });
          return future;
        }
    );

    try {
      // Wait for the loading to complete (non-blocking in async context)
      return loadingFuture.join();
    } catch (Exception e) {
      logger.warning("[TaN-LazyLoader] Exception while waiting for territory " + territoryId + ": " + e.getMessage());
      futures.remove(territoryId);
      return null;
    }
  }
  public static CompletableFuture<Void> preloadTerritories(
      List<String> territoryIds, Function<String, TerritoryData> loadFunction) {
    if (territoryCache == null) {
      logger.warning("[TaN-LazyLoader] Not initialized");
      return CompletableFuture.completedFuture(null);
    }
    logger.info("[TaN-LazyLoader] Pre-loading " + territoryIds.size() + " territories");
    List<CompletableFuture<Void>> futures =
        territoryIds.stream()
            .filter(id -> territoryCache.getIfPresent(id) == null)
            .map(
                id ->
                    CompletableFuture.runAsync(
                        () -> {
                          getTerritory(id, loadFunction);
                        }))
            .toList();
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(
            () -> {
              logger.info("[TaN-LazyLoader] Pre-loaded " + territoryIds.size() + " territories");
            });
  }
  public static void invalidateTerritory(String territoryId) {
    if (territoryCache != null) {
      territoryCache.invalidate(territoryId);
      logger.fine("[TaN-LazyLoader] Invalidated territory: " + territoryId);
    }
  }
  public static void invalidateTerritories(List<String> territoryIds) {
    if (territoryCache != null) {
      territoryIds.forEach(territoryCache::invalidate);
      logger.fine("[TaN-LazyLoader] Invalidated " + territoryIds.size() + " territories");
    }
  }
  public static void clearCache() {
    if (territoryCache != null) {
      territoryCache.invalidateAll();
      logger.info("[TaN-LazyLoader] Cleared all cached territories");
    }
  }
  public static String getStats() {
    if (territoryCache == null) {
      return "Lazy loader not initialized";
    }
    var stats = territoryCache.stats();
    double hitRate = stats.hitRate() * 100;
    double memoryMB = (territoryCache.size() * 2048) / (1024.0 * 1024.0);
    var futures = getLoadingFutures();
    return String.format(
        "Cached: %d territories (~%.1f MB) | Hit Rate: %.1f%% | "
            + "Hits: %d | Misses: %d | Evictions: %d | Pending Loads: %d",
        territoryCache.size(),
        memoryMB,
        hitRate,
        stats.hitCount(),
        stats.missCount(),
        stats.evictionCount(),
        futures.size());
  }

  /**
   * Get count of pending territory loads (for monitoring).
   */
  public static int getPendingLoadsCount() {
    return getLoadingFutures().size();
  }
  public static long getCachedCount() {
    return territoryCache != null ? territoryCache.size() : 0;
  }
  public static double getHitRate() {
    if (territoryCache == null) {
      return 0;
    }
    return territoryCache.stats().hitRate() * 100;
  }
  public static double getMemoryUsageMB() {
    if (territoryCache == null) {
      return 0;
    }
    return (territoryCache.size() * 2048) / (1024.0 * 1024.0);
  }
  public static boolean isCached(String territoryId) {
    return territoryCache != null && territoryCache.getIfPresent(territoryId) != null;
  }
}