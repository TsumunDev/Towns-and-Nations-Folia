package org.leralix.tan.cache

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import kotlinx.coroutines.*
import org.leralix.tan.coroutines.TanCoroutines
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Generic Kotlin cache manager with Guava backend.
 * Provides coroutine-friendly API with automatic loading.
 * 
 * Usage:
 * ```kotlin
 * val playerCache = CacheManager.create<String, PlayerData>(
 *     name = "players",
 *     expireAfterWrite = 5,
 *     timeUnit = TimeUnit.MINUTES,
 *     maxSize = 10_000
 * ) { playerId ->
 *     // Suspend function to load from database
 *     PlayerDataStorage.getInstance().get(playerId).join()
 * }
 * 
 * // Get cached or load
 * val player = playerCache.get(playerId)
 * ```
 */
class CacheManager<K : Any, V : Any> private constructor(
    private val name: String,
    private val cache: LoadingCache<K, V?>,
    private val loader: suspend (K) -> V?
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(CacheManager::class.java)
        
        /**
         * Create a new cache manager with the specified configuration.
         * 
         * @param name Cache name for logging/debugging
         * @param expireAfterWrite Time before entries expire
         * @param timeUnit Time unit for expiration
         * @param maxSize Maximum number of entries
         * @param loader Suspend function to load values on cache miss
         */
        fun <K : Any, V : Any> create(
            name: String,
            expireAfterWrite: Long = 5,
            timeUnit: TimeUnit = TimeUnit.MINUTES,
            maxSize: Long = 10_000,
            recordStats: Boolean = true,
            loader: suspend (K) -> V?
        ): CacheManager<K, V> {
            val guavaCache = CacheBuilder.newBuilder()
                .expireAfterWrite(expireAfterWrite, timeUnit)
                .maximumSize(maxSize)
                .apply { if (recordStats) recordStats() }
                .build(object : CacheLoader<K, V?>() {
                    override fun load(key: K): V? {
                        // Blocking load for Guava - wrapped in runBlocking
                        return runBlocking { loader(key) }
                    }
                })
            
            return CacheManager(name, guavaCache, loader)
        }
        
        /**
         * Create a simple cache without auto-loading.
         */
        fun <K : Any, V : Any> createSimple(
            name: String,
            expireAfterWrite: Long = 5,
            timeUnit: TimeUnit = TimeUnit.MINUTES,
            maxSize: Long = 10_000
        ): SimpleCache<K, V> = SimpleCache(
            name,
            CacheBuilder.newBuilder()
                .expireAfterWrite(expireAfterWrite, timeUnit)
                .maximumSize(maxSize)
                .recordStats()
                .build()
        )
    }
    
    /**
     * Get value from cache, loading if necessary (suspend function)
     */
    suspend fun get(key: K): V? {
        // Try cache first (no blocking)
        val cached = cache.getIfPresent(key)
        if (cached != null) return cached
        
        // Load asynchronously and cache
        return withContext(Dispatchers.IO) {
            try {
                val value = loader(key)
                if (value != null) {
                    cache.put(key, value)
                }
                value
            } catch (e: Exception) {
                logger.warn("[$name] Failed to load value for key $key: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Get value if present in cache (no loading)
     */
    fun getIfPresent(key: K): V? = cache.getIfPresent(key)
    
    /**
     * Put value directly into cache
     */
    fun put(key: K, value: V) {
        cache.put(key, value)
    }
    
    /**
     * Invalidate a specific key
     */
    fun invalidate(key: K) {
        cache.invalidate(key)
    }
    
    /**
     * Invalidate all entries
     */
    fun invalidateAll() {
        cache.invalidateAll()
    }
    
    /**
     * Get cache statistics
     */
    fun stats(): CacheStats {
        val guavaStats = cache.stats()
        return CacheStats(
            hitCount = guavaStats.hitCount(),
            missCount = guavaStats.missCount(),
            loadSuccessCount = guavaStats.loadSuccessCount(),
            loadExceptionCount = guavaStats.loadExceptionCount(),
            totalLoadTime = guavaStats.totalLoadTime(),
            evictionCount = guavaStats.evictionCount()
        )
    }
    
    /**
     * Get current cache size
     */
    fun size(): Long = cache.size()
    
    // ============ Java-friendly CompletableFuture API ============
    
    @JvmName("getAsync")
    fun getAsync(key: K): CompletableFuture<V?> = TanCoroutines.asFuture { get(key) }
}

/**
 * Simple cache without auto-loading.
 */
class SimpleCache<K : Any, V : Any>(
    private val name: String,
    private val cache: com.google.common.cache.Cache<K, V>
) {
    
    fun get(key: K): V? = cache.getIfPresent(key)
    
    fun put(key: K, value: V) = cache.put(key, value)
    
    fun getOrPut(key: K, loader: () -> V?): V? {
        val cached = cache.getIfPresent(key)
        if (cached != null) return cached
        
        val loaded = loader()
        if (loaded != null) {
            cache.put(key, loaded)
        }
        return loaded
    }
    
    suspend fun getOrLoad(key: K, loader: suspend () -> V?): V? {
        val cached = cache.getIfPresent(key)
        if (cached != null) return cached
        
        val loaded = loader()
        if (loaded != null) {
            cache.put(key, loaded)
        }
        return loaded
    }
    
    fun invalidate(key: K) = cache.invalidate(key)
    
    fun invalidateAll() = cache.invalidateAll()
    
    fun size(): Long = cache.size()
}

/**
 * Cache statistics data class.
 */
data class CacheStats(
    val hitCount: Long,
    val missCount: Long,
    val loadSuccessCount: Long,
    val loadExceptionCount: Long,
    val totalLoadTime: Long,
    val evictionCount: Long
) {
    val hitRate: Double 
        get() = if (hitCount + missCount == 0L) 0.0 
                else hitCount.toDouble() / (hitCount + missCount)
    
    val missRate: Double 
        get() = 1.0 - hitRate
    
    override fun toString(): String = buildString {
        append("CacheStats(")
        append("hitRate=%.2f%%, ".format(hitRate * 100))
        append("hits=$hitCount, ")
        append("misses=$missCount, ")
        append("loads=$loadSuccessCount, ")
        append("evictions=$evictionCount")
        append(")")
    }
}
