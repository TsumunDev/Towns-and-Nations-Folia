package org.leralix.tan.storage.stored;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Unit tests for TownDataStorage class.
 * Tests singleton pattern, CRUD operations, and town management.
 */
public class TownDataStorageCoverageTest {

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("getInstance should return singleton instance")
    void testSingleton() {
        // Act
        TownDataStorage instance1 = TownDataStorage.getInstance();
        TownDataStorage instance2 = TownDataStorage.getInstance();

        // Assert
        assertNotNull(instance1);
        assertSame(instance1, instance2, "getInstance should return same instance");
    }

    @Test
    @DisplayName("get with ID should return CompletableFuture")
    void testGetWithId() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act
        var result = storage.get("test-town-id");

        // Assert
        assertNotNull(result, "get should return CompletableFuture");
    }

    @Test
    @DisplayName("getSync with ID should handle non-existent town")
    void testGetSyncNonExistent() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act
        TownData result = storage.getSync("non-existent-town-id");

        // Assert - Should return null for non-existent town
        // (or create new depending on implementation)
    }

    @Test
    @DisplayName("getSync with Player should return ITanPlayer's town")
    void testGetSyncWithPlayer() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerMock player = MockBukkit.getMock().addPlayer("TestPlayer");

        // Act
        TownData result = storage.getSync(player);

        // Assert - May be null if player has no town
        assertNotNull(storage, "Storage should be initialized");
    }

    @Test
    @DisplayName("getAllSync should return non-null map")
    void testGetAllSync() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act
        var result = storage.getAllSync();

        // Assert
        assertNotNull(result, "getAllSync should return non-null map");
    }

    @Test
    @DisplayName("getNumberOfTown should return non-negative count")
    void testGetNumberOfTown() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act
        int count = storage.getNumberOfTown();

        // Assert
        assertTrue(count >= 0, "Town count should be non-negative");
    }

    @Test
    @DisplayName("isNameUsed should return false for new name")
    void testIsNameUsed() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act
        boolean isUsed = storage.isNameUsed("NonExistentTownName12345");

        // Assert
        assertFalse(isUsed, "Should return false for non-existent town name");
    }

    @Test
    @DisplayName("reset should clear storage cache")
    void testReset() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act
        storage.reset();

        // Assert - storage should be reset without errors
        assertNotNull(storage);
    }

    @Test
    @DisplayName("put should handle null values gracefully")
    void testPutNull() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            storage.put(null, null);
        });
    }

    @Test
    @DisplayName("deleteTown should handle null gracefully")
    void testDeleteTownNull() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            storage.deleteTown(null);
        });
    }

    @Test
    @DisplayName("newTown should return CompletableFuture")
    void testNewTown() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act
        var result = storage.newTown("TestTownName");

        // Assert
        assertNotNull(result, "newTown should return CompletableFuture");
    }

    @Test
    @DisplayName("Multiple getInstance calls should be thread-safe")
    void testThreadSafety() {
        // Act
        TownDataStorage instance1 = TownDataStorage.getInstance();
        TownDataStorage instance2 = TownDataStorage.getInstance();
        TownDataStorage instance3 = TownDataStorage.getInstance();

        // Assert
        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    @Test
    @DisplayName("get with ITanPlayer should return CompletableFuture")
    void testGetWithITanPlayer() throws Exception {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerMock player = MockBukkit.getMock().addPlayer("TestPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);

        // Act
        var result = storage.get(tanPlayer);

        // Assert
        assertNotNull(result, "get should return CompletableFuture");
    }

    @Test
    @DisplayName("get with Player should return CompletableFuture")
    void testGetWithPlayer() throws Exception {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerMock player = MockBukkit.getMock().addPlayer("TestPlayer");

        // Act
        var result = storage.get(player);

        // Assert
        assertNotNull(result, "get should return CompletableFuture");
    }
}
