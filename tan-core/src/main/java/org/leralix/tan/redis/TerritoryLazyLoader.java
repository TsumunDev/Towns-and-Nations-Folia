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
public class TerritoryLazyLoader {
  private static final Logger logger = Logger.getLogger(TerritoryLazyLoader.class.getName());
  private static Cache<String, TerritoryData> territoryCache;
  // Map of territory IDs to pending futures - threads can wait on the same future
  private static final ConcurrentHashMap<String, CompletableFuture<TerritoryData>> loadingFutures = new ConcurrentHashMap<>();
  private static int maxCachedTerritories = 5000;
  private static int unloadAfterMinutes = 10;
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
    CompletableFuture<TerritoryData> loadingFuture = loadingFutures.computeIfAbsent(
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
              loadingFutures.remove(id);
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
      loadingFutures.remove(territoryId);
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
    return String.format(
        "Cached: %d territories (~%.1f MB) | Hit Rate: %.1f%% | "
            + "Hits: %d | Misses: %d | Evictions: %d",
        territoryCache.size(),
        memoryMB,
        hitRate,
        stats.hitCount(),
        stats.missCount(),
        stats.evictionCount());
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