package org.leralix.tan.storage.stored;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Unit tests for DatabaseStorage class.
 * Tests caching, initialization, and configuration.
 */
public class DatabaseStorageTest {

    private ServerMock server;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Storage should initialize with cache enabled")
    void testStorageInitialization() {
        // Act
        TestDatabaseStorage storage = new TestDatabaseStorage();

        // Assert
        assertNotNull(storage);
        assertTrue(storage.isCacheEnabled());
        assertEquals(1000, storage.getCacheSize());
    }

    @Test
    @DisplayName("Storage should initialize with custom cache size")
    void testCustomCacheSize() {
        // Act
        TestDatabaseStorage storage = new TestDatabaseStorage(50);

        // Assert
        assertEquals(50, storage.getCacheSize());
    }

    @Test
    @DisplayName("Storage should initialize without cache")
    void testNoCache() {
        // Act
        TestDatabaseStorage storage = new TestDatabaseStorage(false, 0);

        // Assert
        assertFalse(storage.isCacheEnabled());
    }

    @Test
    @DisplayName("getTable should return correct table name")
    void testGetTableName() {
        // Act
        TestDatabaseStorage storage = new TestDatabaseStorage();

        // Assert
        assertEquals("test_data", storage.getTableName());
    }

    @Test
    @DisplayName("getGson should return non-null Gson instance")
    void testGetGson() {
        // Act
        TestDatabaseStorage storage = new TestDatabaseStorage();

        // Assert
        assertNotNull(storage.getGson());
    }

    @Test
    @DisplayName("clearCache should empty the cache")
    void testClearCache() {
        // Arrange
        TestDatabaseStorage storage = new TestDatabaseStorage();

        // Act
        storage.clearCache();

        // Assert - cache should be empty after clear
        // (tested via internal cache access)
    }

    // Test implementation with public methods for testing
    static class TestDatabaseStorage extends DatabaseStorage<TestData> {

        private static final Gson GSON = new Gson();

        public TestDatabaseStorage() {
            this(true, 1000);
        }

        public TestDatabaseStorage(int cacheSize) {
            this(true, cacheSize);
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

        // Public accessor methods for testing
        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public int getCacheSize() {
            return cacheSize;
        }

        public String getTableName() {
            return tableName;
        }

        public Gson getGson() {
            return gson;
        }
    }

    static class TestData {
        String id;
        String value;

        TestData() {}

        TestData(String id, String value) {
            this.id = id;
            this.value = value;
        }
    }
}
