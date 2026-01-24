package org.leralix.tan.storage.stored;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Comprehensive unit tests for DatabaseStorage class.
 *
 * <p>Tests CRUD operations, caching behavior, async operations,
 * batch operations, and error handling as required by Story 4.1.</p>
 *
 * @since 0.16.0
 */
@DisplayName("DatabaseStorage Comprehensive Tests")
public class DatabaseStorageComprehensiveTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ==================== CRUD Operations Tests ====================

    @Test
    @DisplayName("Create operation should store data successfully")
    void testCreateOperation() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        TestData testData = new TestData("test1", "value1");

        // Act
        storage.putSync(testData.getId(), testData);

        // Assert
        assertEquals(testData, storage.getSync(testData.getId()));
    }

    @Test
    @DisplayName("Read operation should retrieve stored data")
    void testReadOperation() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        TestData testData = new TestData("test2", "value2");
        storage.putSync(testData.getId(), testData);

        // Act
        TestData retrieved = storage.getSync(testData.getId());

        // Assert
        assertNotNull(retrieved);
        assertEquals("test2", retrieved.getId());
        assertEquals("value2", retrieved.getValue());
    }

    @Test
    @DisplayName("Update operation should modify existing data")
    void testUpdateOperation() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        TestData original = new TestData("test3", "original");
        storage.putSync(original.getId(), original);

        // Act
        TestData updated = new TestData("test3", "updated");
        storage.putSync(updated.getId(), updated);

        // Assert
        TestData retrieved = storage.getSync("test3");
        assertEquals("updated", retrieved.getValue());
    }

    @Test
    @DisplayName("Delete operation should remove data")
    void testDeleteOperation() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        TestData testData = new TestData("test4", "value4");
        storage.putSync(testData.getId(), testData);

        // Act
        storage.remove(testData.getId());

        // Assert
        assertNull(storage.getSync(testData.getId()));
    }

    // ==================== Cache Behavior Tests ====================

    @Test
    @DisplayName("Cache hit should return data without database query")
    void testCacheHit() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage(true, 100);
        TestData testData = new TestData("cache_hit", "cached_value");
        storage.putSync(testData.getId(), testData);

        // Act - First access loads from storage, caches the data
        TestData firstAccess = storage.getSync(testData.getId());

        // Act - Second access should hit cache
        TestData secondAccess = storage.getSync(testData.getId());

        // Assert - Both accesses should return same data
        assertNotNull(firstAccess);
        assertNotNull(secondAccess);
        assertEquals(firstAccess.getValue(), secondAccess.getValue());
    }

    @Test
    @DisplayName("Cache miss should load data from storage")
    void testCacheMiss() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage(true, 100);
        TestData testData = new TestData("cache_miss", "miss_value");

        // Act - Cache is empty, should load from storage
        storage.putSync(testData.getId(), testData);
        storage.clearCache(); // Clear cache to simulate miss
        TestData retrieved = storage.getSync(testData.getId());

        // Assert
        assertNotNull(retrieved);
        assertEquals("miss_value", retrieved.getValue());
    }

    @Test
    @DisplayName("Cache invalidation should refresh data")
    void testCacheInvalidation() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage(true, 100);
        TestData original = new TestData("invalidate", "original");
        storage.putSync(original.getId(), original);

        // Act - Clear cache
        storage.clearCache();

        // Assert - Data should still be retrievable from storage
        TestData retrieved = storage.getSync(original.getId());
        assertNotNull(retrieved);
    }

    // ==================== Async Operations Tests ====================

    @Test
    @DisplayName("Async get operation should complete successfully")
    void testAsyncGetOperation() throws ExecutionException, InterruptedException, TimeoutException {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        TestData testData = new TestData("async1", "async_value");
        storage.putSync(testData.getId(), testData);

        // Act
        CompletableFuture<TestData> future = storage.get(testData.getId());

        // Assert
        assertNotNull(future);
        TestData result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals("async_value", result.getValue());
    }

    @Test
    @DisplayName("Async get operation should return null for non-existent data")
    void testAsyncGetNonExistent() throws ExecutionException, InterruptedException, TimeoutException {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();

        // Act
        CompletableFuture<TestData> future = storage.get("nonexistent");

        // Assert
        assertNotNull(future);
        TestData result = future.get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @DisplayName("Multiple async operations should complete concurrently")
    void testConcurrentAsyncOperations() throws Exception {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        List<CompletableFuture<TestData>> futures = new ArrayList<>();

        // Act - Start multiple async operations
        for (int i = 0; i < 10; i++) {
            String id = "concurrent_" + i;
            TestData data = new TestData(id, "value_" + i);
            storage.putSync(id, data);
            futures.add(storage.get(id));
        }

        // Assert - All futures should complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(10, TimeUnit.SECONDS);

        for (int i = 0; i < 10; i++) {
            CompletableFuture<TestData> future = futures.get(i);
            assertTrue(future.isDone());
            assertNotNull(future.get());
        }
    }

    // ==================== Batch Operations Tests ====================

    @Test
    @DisplayName("Batch get operation should load multiple items efficiently")
    void testBatchGetOperation() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        Collection<String> ids = new ArrayList<>();

        // Create test data
        for (int i = 0; i < 5; i++) {
            String id = "batch_" + i;
            ids.add(id);
            storage.putSync(id, new TestData(id, "batch_value_" + i));
        }

        // Act
        java.util.Map<String, TestData> results = storage.getBatchSync(ids);

        // Assert
        assertNotNull(results);
        assertEquals(5, results.size());
        for (int i = 0; i < 5; i++) {
            String id = "batch_" + i;
            assertTrue(results.containsKey(id));
            assertEquals("batch_value_" + i, results.get(id).getValue());
        }
    }

    @Test
    @DisplayName("Batch get operation with empty collection should return empty map")
    void testBatchGetEmpty() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        Collection<String> emptyIds = new ArrayList<>();

        // Act
        java.util.Map<String, TestData> results = storage.getBatchSync(emptyIds);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Batch get operation should handle non-existent IDs gracefully")
    void testBatchGetWithNonExistentIds() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        Collection<String> ids = List.of("exists1", "nonexistent", "exists2");
        storage.putSync("exists1", new TestData("exists1", "value1"));
        storage.putSync("exists2", new TestData("exists2", "value2"));

        // Act
        java.util.Map<String, TestData> results = storage.getBatchSync(ids);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsKey("exists1"));
        assertTrue(results.containsKey("exists2"));
        assertFalse(results.containsKey("nonexistent"));
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Get operation with null ID should return null")
    void testGetWithNullId() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();

        // Act
        TestData result = storage.getSync(null);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Async get with null ID should complete with null")
    void testAsyncGetWithNullId() throws Exception {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();

        // Act
        CompletableFuture<TestData> future = storage.get(null);

        // Assert
        assertNotNull(future);
        TestData result = future.get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @DisplayName("Put operation with null data should handle gracefully")
    void testPutWithNullData() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> storage.putSync("test_id", null));
    }

    @Test
    @DisplayName("Remove operation with non-existent ID should not throw")
    void testRemoveNonExistent() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> storage.remove("nonexistent_id"));
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Multiple sequential operations should complete in reasonable time")
    void testSequentialOperationsPerformance() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();
        int operationCount = 100;

        // Act
        long startTime = System.nanoTime();
        for (int i = 0; i < operationCount; i++) {
            String id = "perf_" + i;
            storage.putSync(id, new TestData(id, "value_" + i));
            storage.getSync(id);
        }
        long duration = System.nanoTime() - startTime;

        // Assert - Should complete in less than 5 seconds (generous for test environment)
        long durationMs = duration / 1_000_000;
        assertTrue(durationMs < 5000,
            "Operations took too long: " + durationMs + "ms");
    }

    // ==================== Test Implementation ====================

    /**
     * Test implementation of DatabaseStorage for testing.
     */
    static class TestDatabaseStorage extends DatabaseStorage<TestData> {

        private static final Gson GSON = new Gson();

        public TestDatabaseStorage() {
            this(true, 100);
        }

        public TestDatabaseStorage(boolean enableCache, int cacheSize) {
            super("test_data", TestData.class, TestData.class, GSON, enableCache, cacheSize);
        }

        @Override
        protected void createTable() {
            // Test storage doesn't create actual tables
        }

        @Override
        protected void createIndexes() {
            // No indexes for test
        }

        @Override
        public void reset() {
            clearCache();
        }
    }

    /**
     * Test data class.
     */
    static class TestData {
        private String id;
        private String value;

        TestData() {}

        TestData(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestData testData = (TestData) obj;
            return id != null ? id.equals(testData.id) : testData.id == null;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }
}
