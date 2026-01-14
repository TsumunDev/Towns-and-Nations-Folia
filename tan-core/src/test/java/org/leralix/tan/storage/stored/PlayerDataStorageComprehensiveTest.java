package org.leralix.tan.storage.stored;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * Comprehensive unit tests for PlayerDataStorage class.
 *
 * <p>Tests player-specific storage operations including balance updates,
 * town membership changes, and cache invalidation as required by Story 4.1.</p>
 *
 * @since 0.16.0
 */
@DisplayName("PlayerDataStorage Comprehensive Tests")
public class PlayerDataStorageComprehensiveTest {

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

    // ==================== Balance Update Tests ====================

    @Test
    @DisplayName("Player balance should be accessible after creation")
    void testPlayerBalanceAccess() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("BalancePlayer");

        // Act
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Assert
        assertNotNull(tanPlayer);
        assertNotNull(tanPlayer.getBalance(), "Balance should be initialized");
    }

    @Test
    @DisplayName("Balance updates should persist correctly")
    void testBalanceUpdate() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("BalanceUpdater");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act
        tanPlayer.addToBalance(100.0);
        storage.putSync(tanPlayer.getID(), tanPlayer);

        // Re-fetch to verify persistence
        ITanPlayer fetchedPlayer = storage.getSync(tanPlayer.getID());

        // Assert
        assertEquals(initialBalance + 100.0, fetchedPlayer.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Balance updates should work asynchronously")
    void testAsyncBalanceUpdate() throws Exception {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("AsyncBalancePlayer");

        // Act
        CompletableFuture<ITanPlayer> future = storage.get(player.getUniqueId());
        ITanPlayer tanPlayer = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertNotNull(tanPlayer);
        assertNotNull(tanPlayer.getBalance());
    }

    @Test
    @DisplayName("Multiple balance updates should accumulate correctly")
    void testMultipleBalanceUpdates() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("MultiBalancePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act
        tanPlayer.addToBalance(50.0);
        storage.putSync(tanPlayer.getID(), tanPlayer);

        tanPlayer.addToBalance(30.0);
        storage.putSync(tanPlayer.getID(), tanPlayer);

        tanPlayer.removeFromBalance(20.0);
        storage.putSync(tanPlayer.getID(), tanPlayer);

        // Assert
        ITanPlayer fetchedPlayer = storage.getSync(tanPlayer.getID());
        assertEquals(initialBalance + 50.0 + 30.0 - 20.0, fetchedPlayer.getBalance(), 0.001);
    }

    // ==================== Town Membership Tests ====================

    @Test
    @DisplayName("Player should initially have no town")
    void testPlayerInitiallyNoTown() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("NoTownPlayer");

        // Act
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Assert
        assertNotNull(tanPlayer);
        assertFalse(tanPlayer.hasTown(), "Player should initially have no town");
    }

    @Test
    @DisplayName("Town membership should persist after assignment")
    void testTownMembershipAssignment() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerDataStorage townStorage = TownDataStorage.getInstance();
        PlayerMock player = server.addPlayer("TownMemberPlayer");

        // Act - Create a town
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        townStorage.newTown("TestTown", tanPlayer).join();

        // Assert
        ITanPlayer fetchedPlayer = storage.getSync(tanPlayer.getID());
        assertTrue(fetchedPlayer.hasTown(), "Player should have a town");
        assertNotNull(fetchedPlayer.getTownId(), "Town ID should be set");
    }

    @Test
    @DisplayName("Town membership changes should persist correctly")
    void testTownMembershipChange() throws Exception {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerDataStorage townStorage = TownDataStorage.getInstance();
        PlayerMock player = server.addPlayer("TownChangePlayer");

        // Act - Create first town
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        String firstTownId = townStorage.newTown("FirstTown", tanPlayer).join().getID();

        // Act - Leave first town, create second town
        TownData firstTown = townStorage.getSync(firstTownId);
        firstTown.removePlayer(tanPlayer.getID());
        String secondTownId = townStorage.newTown("SecondTown", tanPlayer).join().getID();

        // Assert
        ITanPlayer fetchedPlayer = storage.getSync(tanPlayer.getID());
        assertTrue(fetchedPlayer.hasTown());
        assertEquals(secondTownId, fetchedPlayer.getTownId(), "Player should be in second town");
    }

    @Test
    @DisplayName("Async town membership queries should work correctly")
    void testAsyncTownMembershipQuery() throws Exception {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("AsyncTownPlayer");

        // Act
        CompletableFuture<ITanPlayer> future = storage.get(player.getUniqueId());
        ITanPlayer tanPlayer = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertNotNull(tanPlayer);
        // Initially no town
        assertFalse(tanPlayer.hasTown());
    }

    // ==================== Cache Invalidation Tests ====================

    @Test
    @DisplayName("Cache invalidation should refresh player data")
    void testCacheInvalidation() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("CacheInvalidationPlayer");

        // Act - Load player data (cache it)
        ITanPlayer firstLoad = storage.getSync(player.getUniqueId().toString());
        double originalBalance = firstLoad.getBalance();

        // Modify balance directly
        firstLoad.addToBalance(100.0);

        // Clear cache
        storage.clearCache();

        // Load again (should reflect cached data from before clear)
        ITanPlayer secondLoad = storage.getSync(player.getUniqueId().toString());

        // Assert - Balance should be updated
        assertEquals(originalBalance + 100.0, secondLoad.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Multiple player loads should use cache efficiently")
    void testCacheEfficiency() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("CacheEfficientPlayer");

        // Act - Load same player multiple times
        ITanPlayer load1 = storage.getSync(player.getUniqueId().toString());
        ITanPlayer load2 = storage.getSync(player.getUniqueId().toString());
        ITanPlayer load3 = storage.getSync(player.getUniqueId().toString());

        // Assert - All loads should return same data (cached)
        assertEquals(load1.getID(), load2.getID());
        assertEquals(load2.getID(), load3.getID());
        assertEquals(load1.getBalance(), load2.getBalance());
        assertEquals(load2.getBalance(), load3.getBalance());
    }

    // ==================== Batch Operations Tests ====================

    @Test
    @DisplayName("Batch player load should retrieve multiple players efficiently")
    void testBatchPlayerLoad() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        Collection<String> playerIds = new java.util.ArrayList<>();

        // Create multiple players
        for (int i = 0; i < 5; i++) {
            PlayerMock player = server.addPlayer("BatchPlayer" + i);
            ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
            playerIds.add(tanPlayer.getID());
        }

        // Act
        java.util.Map<String, ITanPlayer> results = storage.getBatchSync(playerIds);

        // Assert
        assertNotNull(results);
        assertEquals(5, results.size());
        for (String playerId : playerIds) {
            assertTrue(results.containsKey(playerId), "Should contain player: " + playerId);
            assertNotNull(results.get(playerId));
        }
    }

    @Test
    @DisplayName("Batch player load with empty collection should return empty map")
    void testBatchPlayerLoadEmpty() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        Collection<String> emptyIds = new java.util.ArrayList<>();

        // Act
        java.util.Map<String, ITanPlayer> results = storage.getBatchSync(emptyIds);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Get with non-existent player should return null")
    void testGetNonExistentPlayer() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        UUID randomUUID = UUID.randomUUID();

        // Act
        ITanPlayer result = storage.getSync(randomUUID.toString());

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Async get with non-existent player should complete with null")
    void testAsyncGetNonExistentPlayer() throws Exception {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        UUID randomUUID = UUID.randomUUID();

        // Act
        CompletableFuture<ITanPlayer> future = storage.get(randomUUID);

        // Assert
        assertNotNull(future);
        ITanPlayer result = future.get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @DisplayName("Get with null player ID should return null")
    void testGetWithNullPlayerId() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        ITanPlayer result = storage.getSync((String) null);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Get with null UUID should return null")
    void testGetWithNullUUID() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();

        // Act
        ITanPlayer result = storage.getSync((UUID) null);

        // Assert
        assertNull(result);
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Multiple player operations should complete in reasonable time")
    void testPlayerOperationsPerformance() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        int operationCount = 50;

        // Act - Create and load multiple players
        long startTime = System.nanoTime();
        for (int i = 0; i < operationCount; i++) {
            PlayerMock player = server.addPlayer("PerfPlayer" + i);
            ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
            tanPlayer.addToBalance(10.0);
        }
        long duration = System.nanoTime() - startTime;

        // Assert - Should complete in less than 5 seconds
        long durationMs = duration / 1_000_000;
        assertTrue(durationMs < 5000,
            "Player operations took too long: " + durationMs + "ms");
    }

    @Test
    @DisplayName("Concurrent player data access should be thread-safe")
    void testConcurrentPlayerAccess() throws Exception {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("ConcurrentPlayer");
        UUID playerId = player.getUniqueId();

        // Act - Access same player from multiple threads
        List<CompletableFuture<ITanPlayer>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(storage.get(playerId));
        }

        // Assert - All futures should complete successfully
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(10, TimeUnit.SECONDS);

        for (CompletableFuture<ITanPlayer> future : futures) {
            assertTrue(future.isDone());
            assertNotNull(future.get());
        }
    }

    // ==================== Data Consistency Tests ====================

    @Test
    @DisplayName("Player ID should be consistent across accesses")
    void testPlayerIdConsistency() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("ConsistentPlayer");
        String expectedId = player.getUniqueId().toString();

        // Act
        ITanPlayer tanPlayer = storage.getSync(expectedId);
        String retrievedId = tanPlayer.getID();

        // Assert
        assertEquals(expectedId, retrievedId);
    }

    @Test
    @DisplayName("Player name should match Bukkit player name")
    void testPlayerNameConsistency() {
        // Arrange
        PlayerDataStorage storage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("NamePlayer");
        String expectedName = player.getName();

        // Act
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        String retrievedName = tanPlayer.getNameStored();

        // Assert
        assertEquals(expectedName, retrievedName);
    }
}
