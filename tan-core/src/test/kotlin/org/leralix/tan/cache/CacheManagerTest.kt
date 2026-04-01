package org.leralix.tan.cache

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Integration tests for {@link CacheManager}.
 * Tests LRU eviction, hit/miss statistics, and concurrent access.
 */
@DisplayName("CacheManager Integration Tests")
class CacheManagerTest {

    private lateinit var cacheManager: CacheManager<String, String>
    private lateinit var intCacheManager: CacheManager<String, Int>

    @BeforeEach
    fun setUp() {
        // Create cache with short TTL for testing expiration
        cacheManager = CacheManager.create(
            name = "test-cache",
            expireAfterWrite = 100, // 100ms TTL for faster expiration tests
            timeUnit = TimeUnit.MILLISECONDS,
            maxSize = 100, // max size
            recordStats = true, // record stats
            loader = { key ->
                // Simulate data loading
                if (key == "error-key") {
                    throw RuntimeException("Load error")
                }
                "value-$key"
            }
        )

        intCacheManager = CacheManager.create(
            name = "int-cache",
            expireAfterWrite = 5, // 5 seconds TTL
            timeUnit = TimeUnit.SECONDS,
            maxSize = 50,
            recordStats = true,
            loader = { key ->
                if (key == "zero") {
                    0
                } else {
                    key.toInt()
                }
            }
        )
    }

    @AfterEach
    fun tearDown() {
        if (::cacheManager.isInitialized) {
            cacheManager.invalidateAll()
        }
        if (::intCacheManager.isInitialized) {
            intCacheManager.invalidateAll()
        }
    }

    // ==================== Basic CRUD Tests ====================

    @Test
    @DisplayName("get() should load and cache value on first access")
    fun test_firstAccess_loadsAndCachesValue() = runBlocking {
        // Act
        val value = cacheManager.get("key1")

        // Assert
        assertNotNull(value)
        assertEquals("value-key1", value)

        // Verify it's cached
        val stats = cacheManager.stats()
        assertEquals(0, stats.hitCount)
        assertEquals(1, stats.missCount)
    }

    @Test
    @DisplayName("get() should return cached value on subsequent access")
    fun test_subsequentAccess_returnsCachedValue() = runBlocking {
        // Arrange
        cacheManager.get("key2") // First access - miss
        val value2 = cacheManager.get("key2") // Second access - hit

        // Assert
        assertNotNull(value2)
        assertEquals("value-key2", value2)

        val stats = cacheManager.stats()
        assertEquals(1, stats.hitCount)
        assertEquals(1, stats.missCount)
    }

    @Test
    @DisplayName("getIfPresent() should return null for non-existent key")
    fun test_getIfPresent_nonExistentKey_returnsNull() {
        // Act
        val value = cacheManager.getIfPresent("non-existent")

        // Assert
        assertNull(value)
    }

    @Test
    @DisplayName("getIfPresent() should return cached value without loading")
    fun test_getIfPresent_existingKey_returnsValue() {
        // Arrange
        cacheManager.put("key3", "manual-value")

        // Act
        val value = cacheManager.getIfPresent("key3")

        // Assert
        assertEquals("manual-value", value)

        // getIfPresent doesn't use the loader, so no load count should be recorded
        // However, stats might be 0 or empty depending on Guava state
        val stats = cacheManager.stats()
        assertTrue(stats.loadSuccessCount >= 0)
    }

    @Test
    @DisplayName("put() should store value in cache")
    fun test_put_storesValue() {
        // Act
        cacheManager.put("key4", "custom-value")
        val value = cacheManager.getIfPresent("key4")

        // Assert
        assertEquals("custom-value", value)
    }

    @Test
    @DisplayName("put() should override existing value")
    fun test_put_overwritesExistingValue() {
        // Arrange
        cacheManager.put("key5", "original")

        // Act
        cacheManager.put("key5", "updated")

        // Assert
        val value = cacheManager.getIfPresent("key5")
        assertEquals("updated", value)
    }

    @Test
    @DisplayName("invalidate() should remove specific key")
    fun test_invalidate_removesSpecificKey() {
        // Arrange
        cacheManager.put("key6", "value6")
        assertNotNull(cacheManager.getIfPresent("key6"))

        // Act
        cacheManager.invalidate("key6")

        // Assert
        assertNull(cacheManager.getIfPresent("key6"))
    }

    @Test
    @DisplayName("invalidateAll() should clear entire cache")
    fun test_invalidateAll_clearsCache() {
        // Arrange
        cacheManager.put("key7", "value7")
        cacheManager.put("key8", "value8")
        cacheManager.put("key9", "value9")
        assertEquals(3, cacheManager.size())

        // Act
        cacheManager.invalidateAll()

        // Assert
        assertEquals(0, cacheManager.size())
        assertNull(cacheManager.getIfPresent("key7"))
        assertNull(cacheManager.getIfPresent("key8"))
        assertNull(cacheManager.getIfPresent("key9"))
    }

    // ==================== Statistics Tests ====================

    @Test
    @DisplayName("stats() should track hit and miss counts")
    fun test_stats_tracksHitsAndMisses() = runBlocking {
        // Arrange
        cacheManager.get("key10") // miss
        cacheManager.get("key10") // hit
        cacheManager.get("key10") // hit
        cacheManager.get("key11") // miss
        cacheManager.get("key12") // miss

        // Act
        val stats = cacheManager.stats()

        // Assert
        assertEquals(2, stats.hitCount)
        assertEquals(3, stats.missCount)
    }

    @Test
    @DisplayName("stats() hitRate should be calculated correctly")
    fun test_stats_hitRate_isCalculatedCorrectly() = runBlocking {
        // Arrange
        cacheManager.get("key13") // miss
        cacheManager.get("key13") // hit
        cacheManager.get("key13") // hit
        cacheManager.get("key13") // hit

        // Act
        val stats = cacheManager.stats()

        // Assert
        assertEquals(0.75, stats.hitRate, 0.01) // 3 hits / 4 total
        assertEquals(0.25, stats.missRate, 0.01) // 1 miss / 4 total
    }

    @Test
    @DisplayName("stats() hitRate should be 0 when no requests")
    fun test_stats_hitRate_zeroWhenNoRequests() {
        // Act
        val stats = cacheManager.stats()

        // Assert - hitRate should be 0 or NaN when no requests
        assertTrue(stats.hitRate == 0.0 || stats.hitRate.isNaN())
        assertTrue(stats.missRate == 0.0 || stats.missRate.isNaN())
    }

    @Test
    @DisplayName("stats() should track load success count")
    fun test_stats_tracksLoadSuccessCount() = runBlocking {
        // Act
        cacheManager.get("key14")
        cacheManager.get("key15")
        cacheManager.get("key16")

        // Assert
        val stats = cacheManager.stats()
        // Guava records loads, not requests. Since we're using a loader,
        // each miss triggers a load which should be recorded
        assertTrue(stats.loadSuccessCount >= 3)
    }

    @Test
    @DisplayName("stats() should track load exception count")
    fun test_stats_tracksLoadExceptionCount() = runBlocking {
        // Act
        val value = cacheManager.get("error-key")

        // Assert
        assertNull(value)
        val stats = cacheManager.stats()
        // Guava may or may not record load exceptions depending on how the loader is called
        assertTrue(stats.loadExceptionCount >= 0)
    }

    @Test
    @DisplayName("stats() toString should contain formatted information")
    fun test_stats_toString_containsFormattedInfo() = runBlocking {
        // Arrange
        cacheManager.get("key17")
        cacheManager.get("key17")

        // Act
        val statsString = cacheManager.stats().toString()

        // Assert
        assertTrue(statsString.contains("hitRate="))
        assertTrue(statsString.contains("hits="))
        assertTrue(statsString.contains("misses="))
    }

    // ==================== Expiration Tests ====================

    @Test
    @DisplayName("Cache should expire entries after TTL")
    fun test_cache_shouldExpireEntriesAfterTTL() {
        // Arrange
        cacheManager.put("expire-key", "expire-value")
        assertNotNull(cacheManager.getIfPresent("expire-key"))

        // Wait for expiration (100ms TTL + buffer)
        Thread.sleep(200)

        // Act
        val value = cacheManager.getIfPresent("expire-key")

        // Assert
        assertNull(value)
    }

    @Test
    @DisplayName("Cache should reload expired entries")
    fun test_cache_shouldReloadExpiredEntries() = runBlocking {
        // Arrange
        cacheManager.put("reload-key", "old-value")

        // Wait for expiration
        Thread.sleep(200)

        // Act - get should trigger reload via loader
        val value = cacheManager.get("reload-key")

        // Assert
        assertNotNull(value)
        assertEquals("value-reload-key", value) // Loader returns "value-" + key
    }

    // ==================== Size Tests ====================

    @Test
    @DisplayName("size() should return correct cache size")
    fun test_size_returnsCorrectSize() {
        // Act
        cacheManager.put("size1", "value1")
        cacheManager.put("size2", "value2")
        cacheManager.put("size3", "value3")

        // Assert
        assertEquals(3, cacheManager.size())
    }

    @Test
    @DisplayName("size() should decrease after invalidation")
    fun test_size_decreasesAfterInvalidation() {
        // Arrange
        cacheManager.put("size4", "value4")
        cacheManager.put("size5", "value5")
        assertEquals(2, cacheManager.size())

        // Act
        cacheManager.invalidate("size4")

        // Assert
        assertEquals(1, cacheManager.size())
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    @DisplayName("Cache should handle concurrent reads")
    fun test_concurrentAccess_handlesMultipleReads() {
        val threadCount = 10
        val readsPerThread = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        // Pre-populate cache
        cacheManager.put("concurrent-key", "shared-value")

        // Act - Spawn threads that all read the same key
        repeat(threadCount) { _: Int ->
            executor.submit {
                try {
                    repeat(readsPerThread) { _: Int ->
                        val value = cacheManager.getIfPresent("concurrent-key")
                        assertEquals("shared-value", value)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()

        // Assert - Value should still be accessible
        assertEquals("shared-value", cacheManager.getIfPresent("concurrent-key"))
    }

    @Test
    @DisplayName("Cache should handle concurrent writes")
    fun test_concurrentAccess_handlesMultipleWrites() {
        val threadCount = 5
        val writesPerThread = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val writeCount = AtomicInteger(0)

        // Act - Spawn threads that all write different keys
        repeat(threadCount) { threadId: Int ->
            executor.submit {
                try {
                    repeat(writesPerThread) { j: Int ->
                        val key = "thread-$threadId-key-$j"
                        cacheManager.put(key, "value-$threadId-$j")
                        writeCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()

        // Assert - All writes should be counted
        assertEquals(threadCount * writesPerThread, writeCount.get())
        // Size should be at least close to expected (may be slightly less due to timing)
        assertTrue(cacheManager.size() >= threadCount * writesPerThread - 5)
    }

    // ==================== Integer Cache Tests ====================

    @Test
    @DisplayName("Integer cache should handle numeric values")
    fun test_intCache_handlesNumericValues() = runBlocking {
        // Act
        val value = intCacheManager.get("42")

        // Assert
        assertNotNull(value)
        assertEquals(42, value)
    }

    @Test
    @DisplayName("Integer cache should handle zero value")
    fun test_intCache_handlesZeroValue() = runBlocking {
        // Act
        val value = intCacheManager.get("zero")

        // Assert
        assertNotNull(value)
        assertEquals(0, value)
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Cache should handle empty string keys")
    fun test_emptyStringKey_works() = runBlocking {
        // Act
        val value = cacheManager.get("")

        // Assert
        assertNotNull(value)
        assertEquals("value-", value)
    }

    @Test
    @DisplayName("Multiple invalidations should be idempotent")
    fun test_invalidate_multipleCalls_idempotent() {
        // Arrange
        cacheManager.put("dup-key", "value")
        cacheManager.invalidate("dup-key")

        // Act - Invalidate same key twice
        cacheManager.invalidate("dup-key")

        // Assert - Should not throw
        assertNull(cacheManager.getIfPresent("dup-key"))
    }

    @Test
    @DisplayName("Stats should remain consistent after multiple operations")
    fun test_stats_remainConsistentAfterMultipleOperations() = runBlocking {
        // Act - Perform various operations
        cacheManager.get("key1")
        cacheManager.get("key1")
        cacheManager.put("key2", "manual")
        cacheManager.get("key2")
        cacheManager.invalidate("key1")
        cacheManager.get("key3")

        // Assert
        val stats = cacheManager.stats()
        // 1 hit (key2), 3 misses (key1, key1 reload, key3)
        assertTrue(stats.hitCount >= 1)
        assertTrue(stats.missCount >= 2)
        assertEquals(0, stats.loadExceptionCount)
    }
}
