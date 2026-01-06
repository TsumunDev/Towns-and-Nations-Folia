package org.leralix.tan.redis;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
public class QueryCacheManager {
  private static final Logger logger = Logger.getLogger(QueryCacheManager.class.getName());
  private static Cache<String, Object> localCache;
  private static RedissonClient redisClient;
  public static void initialize(RedissonClient redissonClient) {
    redisClient = redissonClient;
    localCache =
        CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .recordStats()
            .build();
    logger.info("[TaN-QueryCache] Initialized with L1 (local) + L2 (Redis) caching");
  }
  public static CompletableFuture<List<Object>> getTransactionHistoryCached(
      TerritoryData territory,
      String transactionType,
      Function<TerritoryData, List<Object>> fetchFunction) {
    String cacheKey = "tan:cache:trans_history:" + territory.getID() + ":" + transactionType;
    return CompletableFuture.supplyAsync(
        () -> {
          @SuppressWarnings("unchecked")
          List<Object> cached = (List<Object>) localCache.getIfPresent(cacheKey);
          if (cached != null) {
            logger.fine("[TaN-QueryCache] L1 HIT: " + cacheKey);
            return cached;
          }
          if (redisClient != null) {
            RMapCache<String, List<Object>> redisCache = redisClient.getMapCache("tan:query_cache");
            cached = redisCache.get(cacheKey);
            if (cached != null) {
              logger.fine("[TaN-QueryCache] L2 HIT: " + cacheKey);
              localCache.put(cacheKey, cached);
              return cached;
            }
          }
          logger.fine("[TaN-QueryCache] MISS: " + cacheKey);
          List<Object> result = fetchFunction.apply(territory);
          localCache.put(cacheKey, result);
          if (redisClient != null) {
            RMapCache<String, List<Object>> redisCache = redisClient.getMapCache("tan:query_cache");
            int ttlMinutes = transactionType.equals("TAXATION") ? 30 : 5;
            redisCache.put(cacheKey, result, ttlMinutes, TimeUnit.MINUTES);
          }
          return result;
        });
  }
  public static int getPlayerBalance(UUID playerUUID, Function<UUID, Integer> fetchFunction) {
    String cacheKey = "tan:cache:balance:" + playerUUID;
    Integer cached = (Integer) localCache.getIfPresent(cacheKey);
    if (cached != null) {
      logger.fine("[TaN-QueryCache] L1 HIT: " + cacheKey);
      return cached;
    }
    if (redisClient != null) {
      RMapCache<String, Integer> redisCache = redisClient.getMapCache("tan:query_cache");
      cached = redisCache.get(cacheKey);
      if (cached != null) {
        logger.fine("[TaN-QueryCache] L2 HIT: " + cacheKey);
        localCache.put(cacheKey, cached);
        return cached;
      }
    }
    logger.fine("[TaN-QueryCache] MISS: " + cacheKey);
    int balance = fetchFunction.apply(playerUUID);
    localCache.put(cacheKey, balance);
    if (redisClient != null) {
      RMapCache<String, Integer> redisCache = redisClient.getMapCache("tan:query_cache");
      redisCache.put(cacheKey, balance, 1, TimeUnit.MINUTES);
    }
    return balance;
  }
  public static TerritoryData getTerritoryData(
      String territoryId, Function<String, TerritoryData> fetchFunction) {
    String cacheKey = "tan:cache:territory:" + territoryId;
    TerritoryData cached = (TerritoryData) localCache.getIfPresent(cacheKey);
    if (cached != null) {
      logger.fine("[TaN-QueryCache] L1 HIT: " + cacheKey);
      return cached;
    }
    if (redisClient != null) {
      RMapCache<String, TerritoryData> redisCache = redisClient.getMapCache("tan:query_cache");
      cached = redisCache.get(cacheKey);
      if (cached != null) {
        logger.fine("[TaN-QueryCache] L2 HIT: " + cacheKey);
        localCache.put(cacheKey, cached);
        return cached;
      }
    }
    logger.fine("[TaN-QueryCache] MISS: " + cacheKey);
    TerritoryData territory = fetchFunction.apply(territoryId);
    localCache.put(cacheKey, territory);
    if (redisClient != null) {
      RMapCache<String, TerritoryData> redisCache = redisClient.getMapCache("tan:query_cache");
      redisCache.put(cacheKey, territory, 10, TimeUnit.MINUTES);
    }
    return territory;
  }
  public static void invalidateTransactionHistory(String territoryId) {
    String pattern = "tan:cache:trans_history:" + territoryId + ":*";
    localCache
        .asMap()
        .keySet()
        .removeIf(key -> key.startsWith("tan:cache:trans_history:" + territoryId));
    if (redisClient != null) {
      RMapCache<String, Object> redisCache = redisClient.getMapCache("tan:query_cache");
      redisCache.keySet().stream()
          .filter(key -> key.startsWith("tan:cache:trans_history:" + territoryId))
          .forEach(redisCache::remove);
    }
    logger.fine("[TaN-QueryCache] Invalidated transaction history for territory: " + territoryId);
  }
  public static void invalidatePlayerBalance(UUID playerUUID) {
    String cacheKey = "tan:cache:balance:" + playerUUID;
    localCache.invalidate(cacheKey);
    if (redisClient != null) {
      RMapCache<String, Object> redisCache = redisClient.getMapCache("tan:query_cache");
      redisCache.remove(cacheKey);
    }
    logger.fine("[TaN-QueryCache] Invalidated balance for player: " + playerUUID);
  }
  public static void invalidateTerritory(String territoryId) {
    String cacheKey = "tan:cache:territory:" + territoryId;
    localCache.invalidate(cacheKey);
    if (redisClient != null) {
      RMapCache<String, Object> redisCache = redisClient.getMapCache("tan:query_cache");
      redisCache.remove(cacheKey);
    }
    logger.fine("[TaN-QueryCache] Invalidated territory: " + territoryId);
  }
  public static void clearAllCaches() {
    localCache.invalidateAll();
    if (redisClient != null) {
      RMapCache<String, Object> redisCache = redisClient.getMapCache("tan:query_cache");
      redisCache.clear();
    }
    logger.info("[TaN-QueryCache] Cleared all caches");
  }
  public static String getCacheStats() {
    if (localCache == null) {
      return "Cache not initialized";
    }
    var stats = localCache.stats();
    double hitRate = stats.hitRate() * 100;
    return String.format(
        "L1 Cache - Hits: %d | Misses: %d | Hit Rate: %.1f%% | Size: %d",
        stats.hitCount(), stats.missCount(), hitRate, localCache.size());
  }
}