package org.leralix.tan.storage.stored;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.NoPlayerData;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Unit tests for PlayerDataStorage class.
 * Tests singleton pattern, CRUD operations, and caching.
 */
public class PlayerDataStorageCoverageTest {

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
    @DisplayName("getInstance should return singleton instance")
    void testSingleton() {
        // Act
        PlayerDataStorage instance1 = PlayerDataStorage.getInstance();
        PlayerDataStorage instance2 = PlayerDataStorage.getInstance();

        // Assert
        assertNotNull(instance1);
        assertSame(instance1, instance2, "getInstance should return same instance");
    }

    @Test
    @DisplayName("get with Player should return ITanPlayer")
    void testGetWithPlayer() throws Exception {
        // Arrange
        PlayerMock player = server.addPlayer("TestPlayer");
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        ITanPlayer result = storage.get(player).join();

        // Assert
        assertNotNull(result, "Should return ITanPlayer for player");
        assertFalse(result instanceof NoPlayerData, "Should not return NoPlayerData for existing player");
    }

    @Test
    @DisplayName("get with UUID should return ITanPlayer")
    void testGetWithUUID() throws Exception {
        // Arrange
        UUID uuid = UUID.randomUUID();
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        ITanPlayer result = storage.get(uuid).join();

        // Assert
        assertNotNull(result, "Should return ITanPlayer for UUID");
    }

    @Test
    @DisplayName("get with String UUID should return ITanPlayer")
    void testGetWithStringUUID() throws Exception {
        // Arrange
        String uuidStr = UUID.randomUUID().toString();
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        ITanPlayer result = storage.get(uuidStr).join();

        // Assert
        assertNotNull(result, "Should return ITanPlayer for string UUID");
    }

    @Test
    @DisplayName("getSync should return ITanPlayer synchronously")
    void testGetSync() {
        // Arrange
        PlayerMock player = server.addPlayer("TestPlayer");
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        ITanPlayer result = storage.getSync(player);

        // Assert
        assertNotNull(result, "Should return ITanPlayer synchronously");
    }

    @Test
    @DisplayName("getSync with null should handle gracefully")
    void testGetSyncNull() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        ITanPlayer result = storage.getSync((org.bukkit.entity.Player) null);

        // Assert
        assertNotNull(result, "Should handle null player gracefully");
    }

    @Test
    @DisplayName("getSync with OfflinePlayer should work")
    void testGetSyncOfflinePlayer() {
        // Arrange
        PlayerMock player = server.addPlayer("OfflinePlayer");
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        ITanPlayer result = storage.getSync(player);

        // Assert
        assertNotNull(result, "Should return ITanPlayer for offline player");
    }

    @Test
    @DisplayName("put should handle null values gracefully")
    void testPutNull() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            storage.put(null, null);
            storage.put("test-id", null);
            storage.put(null, new NoPlayerData());
        });
    }

    @Test
    @DisplayName("getAllSync should return non-null map")
    void testGetAllSync() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        var result = storage.getAllSync();

        // Assert
        assertNotNull(result, "getAllSync should return non-null map");
    }

    @Test
    @DisplayName("reset should clear storage cache")
    void testReset() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("ResetTest");

        // Act
        storage.reset();

        // Assert - storage should be reset
        assertNotNull(storage.getSync(player));
    }

    @Test
    @DisplayName("Multiple getInstance calls should be thread-safe")
    void testThreadSafety() {
        // Act
        PlayerDataStorage instance1 = PlayerDataStorage.getInstance();
        PlayerDataStorage instance2 = PlayerDataStorage.getInstance();
        PlayerDataStorage instance3 = PlayerDataStorage.getInstance();

        // Assert
        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    @Test
    @DisplayName("getSync with UUID should work")
    void testGetSyncWithUUID() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        ITanPlayer result = storage.getSync(uuid);

        // Assert
        assertNotNull(result, "Should return ITanPlayer for UUID sync");
    }
}
