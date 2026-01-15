package org.leralix.tan.redis;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;
import org.leralix.tan.dataclass.territory.TerritoryData;
public class TerritoryLazyLoader {
  private static final Logger logger = Logger.getLogger(TerritoryLazyLoader.class.getName());
  private static Cache<String, TerritoryData> territoryCache;
  private static final Set<String> loadingTerritories = java.util.concurrent.ConcurrentHashMap.newKeySet();
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
    synchronized (loadingTerritories) {
      if (loadingTerritories.contains(territoryId)) {
        logger.fine("[TaN-LazyLoader] Territory " + territoryId + " already loading, waiting...");
        while (loadingTerritories.contains(territoryId)) {
          try {
            loadingTerritories.wait(100);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
          }
        }
        return territoryCache.getIfPresent(territoryId);
      }
      loadingTerritories.add(territoryId);
    }
    try {
      long startTime = System.currentTimeMillis();
      TerritoryData territory = loadFunction.apply(territoryId);
      long loadTime = System.currentTimeMillis() - startTime;
      if (territory != null) {
        territoryCache.put(territoryId, territory);
        logger.fine("[TaN-LazyLoader] Loaded territory " + territoryId + " in " + loadTime + "ms");
      }
      return territory;
    } finally {
      synchronized (loadingTerritories) {
        loadingTerritories.remove(territoryId);
        loadingTerritories.notifyAll();
      }
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