package org.leralix.tan.profiling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance profiler for identifying bottlenecks and N+1 query problems.
 *
 * <p>This profiler tracks method call counts, execution times, and database query patterns
 * to help identify performance issues such as:</p>
 * <ul>
 *   <li>N+1 query problems (multiple queries in a loop)</li>
 *   <li>Frequent database calls</li>
 *   <li>Slow methods</li>
 *   <li>Cache miss patterns</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // Profile a method
 * PerformanceProfiler.profile("loadPlayerData", () -> loadPlayer(playerId));
 *
 * // Track a database query
 * PerformanceProfiler.trackQuery("players", "SELECT * FROM players WHERE id = ?", 5);
 *
 * // Get report
 * PerformanceProfiler.report();
 * }</pre>
 */
public class PerformanceProfiler {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceProfiler.class);

    private static final Map<String, MethodStats> methodStats = new ConcurrentHashMap<>();
    private static final Map<String, QueryStats> queryStats = new ConcurrentHashMap<>();
    private static final Map<String, CacheStats> cacheStats = new ConcurrentHashMap<>();

    private static final ThreadLocal<Map<String, Integer>> callDepth = ThreadLocal.withInitial(HashMap::new);
    private static boolean enabled = true;

    private PerformanceProfiler() {}

    /**
     * Enables or disables the profiler.
     *
     * @param enabled true to enable, false to disable
     */
    public static void setEnabled(boolean enabled) {
        PerformanceProfiler.enabled = enabled;
        logger.info("[Profiler] Enabled: {}", enabled);
    }

    /**
     * Checks if the profiler is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Profiles a method execution and records statistics.
     *
     * @param methodName the name of the method being profiled
     * @param runnable the method to profile
     */
    public static void profile(String methodName, Runnable runnable) {
        if (!enabled) {
            runnable.run();
            return;
        }

        profile(methodName, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Profiles a method execution and returns the result.
     *
     * @param <T> the return type
     * @param methodName the name of the method being profiled
     * @param supplier the method to profile
     * @return the result of the method
     */
    public static <T> T profile(String methodName, java.util.function.Supplier<T> supplier) {
        if (!enabled) {
            return supplier.get();
        }

        long start = System.nanoTime();
        int depth = callDepth.get().getOrDefault(methodName, 0);

        try {
            callDepth.get().put(methodName, depth + 1);
            T result = supplier.get();
            return result;
        } finally {
            long duration = System.nanoTime() - start;
            callDepth.get().put(methodName, depth);

            recordMethodExecution(methodName, duration);
        }
    }

    /**
     * Records a method execution.
     *
     * @param methodName the method name
     * @param durationNanos the execution duration in nanoseconds
     */
    private static void recordMethodExecution(String methodName, long durationNanos) {
        methodStats.computeIfAbsent(methodName, MethodStats::new)
                   .recordExecution(durationNanos);
    }

    /**
     * Tracks a database query execution.
     *
     * @param table the table being queried
     * @param queryPattern the query pattern (e.g., "SELECT * FROM players WHERE id = ?")
     * @param resultCount the number of results returned
     */
    public static void trackQuery(String table, String queryPattern, int resultCount) {
        if (!enabled) {
            return;
        }

        String key = table + ":" + queryPattern;
        queryStats.computeIfAbsent(key, k -> new QueryStats(table, queryPattern))
                  .recordQuery(resultCount);
    }

    /**
     * Tracks a cache hit.
     *
     * @param cacheName the name of the cache (e.g., "PlayerDataStorage")
     */
    public static void trackCacheHit(String cacheName) {
        if (!enabled) {
            return;
        }

        cacheStats.computeIfAbsent(cacheName, CacheStats::new)
                  .recordHit();
    }

    /**
     * Tracks a cache miss.
     *
     * @param cacheName the name of the cache (e.g., "PlayerDataStorage")
     */
    public static void trackCacheMiss(String cacheName) {
        if (!enabled) {
            return;
        }

        cacheStats.computeIfAbsent(cacheName, CacheStats::new)
                  .recordMiss();
    }

    /**
     * Gets all method statistics.
     *
     * @return a map of method names to statistics
     */
    public static Map<String, MethodStats> getMethodStats() {
        return new LinkedHashMap<>(methodStats);
    }

    /**
     * Gets all query statistics.
     *
     * @return a map of query keys to statistics
     */
    public static Map<String, QueryStats> getQueryStats() {
        return new LinkedHashMap<>(queryStats);
    }

    /**
     * Gets all cache statistics.
     *
     * @return a map of cache names to statistics
     */
    public static Map<String, CacheStats> getCacheStats() {
        return new LinkedHashMap<>(cacheStats);
    }

    /**
     * Clears all profiling data.
     */
    public static void clear() {
        methodStats.clear();
        queryStats.clear();
        cacheStats.clear();
        logger.info("[Profiler] All profiling data cleared");
    }

    /**
     * Generates and prints a profiling report to the console.
     */
    public static void report() {
        if (!enabled) {
            logger.warn("[Profiler] Profiler is disabled. Enable with setEnabled(true)");
            return;
        }

        logger.info("\n" + "=".repeat(80));
        logger.info("PERFORMANCE PROFILING REPORT");
        logger.info("=".repeat(80) + "\n");

        reportSlowMethods();
        reportQueryPatterns();
        reportCacheStatistics();
        reportN1Problems();

        logger.info("\n" + "=".repeat(80));
    }

    /**
     * Reports the slowest methods.
     */
    private static void reportSlowMethods() {
        logger.info("SLOWEST METHODS (by average time)\n");

        List<MethodStats> sorted = new ArrayList<>(methodStats.values());
        sorted.sort((a, b) -> Long.compare(b.getAverageDurationNanos(), a.getAverageDurationNanos()));

        int count = Math.min(10, sorted.size());
        for (int i = 0; i < count; i++) {
            MethodStats stats = sorted.get(i);
            logger.info(String.format("%2d. %-60s | Avg: %6.2fms | Calls: %6d",
                i + 1,
                stats.getMethodName(),
                stats.getAverageDurationMs(),
                stats.getCallCount()));
        }

        logger.info("");
    }

    /**
     * Reports query patterns.
     */
    private static void reportQueryPatterns() {
        logger.info("DATABASE QUERY PATTERNS\n");

        List<QueryStats> sorted = new ArrayList<>(queryStats.values());
        sorted.sort((a, b) -> Integer.compare(b.getTotalCalls(), a.getTotalCalls()));

        for (QueryStats stats : sorted) {
            logger.info(String.format("Table: %-20s | Calls: %6d | Avg Results: %6.1f",
                stats.getTable(),
                stats.getTotalCalls(),
                stats.getAverageResults()));
        }

        logger.info("");
    }

    /**
     * Reports cache statistics.
     */
    private static void reportCacheStatistics() {
        logger.info("CACHE STATISTICS\n");

        for (CacheStats stats : cacheStats.values()) {
            double hitRate = stats.getHitRate();
            logger.info(String.format("Cache: %-30s | Hits: %6d | Misses: %6d | Hit Rate: %5.1f%%",
                stats.getCacheName(),
                stats.getHits(),
                stats.getMisses(),
                hitRate));
        }

        logger.info("");
    }

    /**
     * Reports potential N+1 query problems.
     */
    private static void reportN1Problems() {
        logger.info("POTENTIAL N+1 QUERY PROBLEMS\n");

        boolean found = false;

        // Check for queries called multiple times with same pattern
        for (QueryStats stats : queryStats.values()) {
            if (stats.getTotalCalls() > 10 && stats.getAverageResults() <= 1) {
                found = true;
                logger.warn(String.format(
                    "⚠ Potential N+1: Table '%s' queried %d times with avg %.1f results per call",
                    stats.getTable(),
                    stats.getTotalCalls(),
                    stats.getAverageResults()));
            }
        }

        if (!found) {
            logger.info("No obvious N+1 query problems detected.");
        }

        logger.info("");
    }

    /**
     * Statistics for a method.
     */
    public static class MethodStats {
        private final String methodName;
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AtomicLong totalDurationNanos = new AtomicLong(0);
        private volatile long minDurationNanos = Long.MAX_VALUE;
        private volatile long maxDurationNanos = 0;

        public MethodStats(String methodName) {
            this.methodName = methodName;
        }

        void recordExecution(long durationNanos) {
            callCount.incrementAndGet();
            totalDurationNanos.addAndGet(durationNanos);

            long oldMin = minDurationNanos;
            if (durationNanos < oldMin) {
                synchronized (this) {
                    if (durationNanos < minDurationNanos) {
                        minDurationNanos = durationNanos;
                    }
                }
            }

            if (durationNanos > maxDurationNanos) {
                synchronized (this) {
                    if (durationNanos > maxDurationNanos) {
                        maxDurationNanos = durationNanos;
                    }
                }
            }
        }

        public String getMethodName() {
            return methodName;
        }

        public int getCallCount() {
            return callCount.get();
        }

        public long getAverageDurationNanos() {
            int count = callCount.get();
            return count > 0 ? totalDurationNanos.get() / count : 0;
        }

        public double getAverageDurationMs() {
            return getAverageDurationNanos() / 1_000_000.0;
        }

        public long getMinDurationNanos() {
            return minDurationNanos;
        }

        public long getMaxDurationNanos() {
            return maxDurationNanos;
        }
    }

    /**
     * Statistics for a database query.
     */
    public static class QueryStats {
        private final String table;
        private final String queryPattern;
        private final AtomicInteger totalCalls = new AtomicInteger(0);
        private final AtomicInteger totalResults = new AtomicInteger(0);

        public QueryStats(String table, String queryPattern) {
            this.table = table;
            this.queryPattern = queryPattern;
        }

        void recordQuery(int resultCount) {
            totalCalls.incrementAndGet();
            totalResults.addAndGet(resultCount);
        }

        public String getTable() {
            return table;
        }

        public String getQueryPattern() {
            return queryPattern;
        }

        public int getTotalCalls() {
            return totalCalls.get();
        }

        public int getTotalResults() {
            return totalResults.get();
        }

        public double getAverageResults() {
            int calls = totalCalls.get();
            return calls > 0 ? (double) totalResults.get() / calls : 0;
        }
    }

    /**
     * Statistics for a cache.
     */
    public static class CacheStats {
        private final String cacheName;
        private final AtomicInteger hits = new AtomicInteger(0);
        private final AtomicInteger misses = new AtomicInteger(0);

        public CacheStats(String cacheName) {
            this.cacheName = cacheName;
        }

        void recordHit() {
            hits.incrementAndGet();
        }

        void recordMiss() {
            misses.incrementAndGet();
        }

        public String getCacheName() {
            return cacheName;
        }

        public int getHits() {
            return hits.get();
        }

        public int getMisses() {
            return misses.get();
        }

        public double getHitRate() {
            int total = hits.get() + misses.get();
            return total > 0 ? (100.0 * hits.get()) / total : 0;
        }
    }
}
