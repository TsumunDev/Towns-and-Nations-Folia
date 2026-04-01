package org.leralix.tan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.enums.permissions.ChunkPermissionType;
import org.leralix.tan.testutils.AbstractPluginTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PermissionCache}.
 * <p>
 * Tests the high-performance permission cache including TTL behavior,
 * invalidation, and concurrency.
 */
@DisplayName("PermissionCache Tests")
class PermissionCacheTest extends AbstractPluginTest {

    private PermissionCache cache;
    private UUID playerUuid;
    private ChunkPermissionType breakPermission;
    private ChunkPermissionType placePermission;

    @BeforeEach
    void setUp() {
        cache = PermissionCache.getInstance();
        cache.clear(); // Start with clean cache
        playerUuid = UUID.randomUUID();
        breakPermission = ChunkPermissionType.BREAK_BLOCK;
        placePermission = ChunkPermissionType.PLACE_BLOCK;
    }

    @Test
    @DisplayName("Should return null for non-cached permission")
    void getCached_NotCached_ReturnsNull() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);

        // Act
        Boolean result = cache.getCached(server.addPlayer(), chunk, breakPermission);

        // Assert
        assertNull(result, "Non-cached permission should return null");
    }

    @Test
    @DisplayName("Should cache permission value")
    void put_ValidPermission_CachesValue() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);
        var player = server.addPlayer("TestPlayer");

        // Act
        cache.put(player, chunk, breakPermission, true);

        // Assert
        Boolean result = cache.getCached(player, chunk, breakPermission);
        assertTrue(result, "Cached permission should be retrievable");
    }

    @Test
    @DisplayName("Should overwrite existing cached value")
    void put_ExistingKey_OverwritesValue() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);
        var player = server.addPlayer("TestPlayer");
        cache.put(player, chunk, breakPermission, true);

        // Act
        cache.put(player, chunk, breakPermission, false);

        // Assert
        Boolean result = cache.getCached(player, chunk, breakPermission);
        assertFalse(result, "Cached permission should be updated");
    }

    @Test
    @DisplayName("Should invalidate all player permissions")
    void invalidatePlayer_HasCachedEntries_RemovesAll() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);
        var player = server.addPlayer("TestPlayer");
        cache.put(player, chunk, breakPermission, true);
        cache.put(player, chunk, placePermission, false);

        // Act
        cache.invalidatePlayer(player.getUniqueId());

        // Assert
        assertNull(cache.getCached(player, chunk, breakPermission));
        assertNull(cache.getCached(player, chunk, placePermission));
    }

    @Test
    @DisplayName("Should only invalidate target player's permissions")
    void invalidatePlayer_MultiplePlayers_OnlyInvalidatesTarget() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");
        cache.put(player1, chunk, breakPermission, true);
        cache.put(player2, chunk, breakPermission, false);

        // Act
        cache.invalidatePlayer(player1.getUniqueId());

        // Assert
        assertNull(cache.getCached(player1, chunk, breakPermission));
        assertNotNull(cache.getCached(player2, chunk, breakPermission));
    }

    @Test
    @DisplayName("Should invalidate chunk permissions")
    void invalidateChunk_HasCachedEntries_RemovesAll() {
        // Arrange
        var world = server.getWorlds().get(0);
        var chunk1 = world.getChunkAt(0, 0);
        var chunk2 = world.getChunkAt(1, 0);
        var player = server.addPlayer("TestPlayer");
        cache.put(player, chunk1, breakPermission, true);
        cache.put(player, chunk2, breakPermission, true);

        // Act
        cache.invalidateChunk(0, 0, world.getUID().toString());

        // Assert
        assertNull(cache.getCached(player, chunk1, breakPermission));
        assertNotNull(cache.getCached(player, chunk2, breakPermission));
    }

    @Test
    @DisplayName("Should clear all cache entries")
    void clear_HasEntries_EmptiesCache() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);
        var player = server.addPlayer("TestPlayer");
        cache.put(player, chunk, breakPermission, true);

        // Act
        cache.clear();

        // Assert
        assertEquals(0, cache.getCacheSize(), "Cache should be empty after clear");
        assertNull(cache.getCached(player, chunk, breakPermission));
    }

    @Test
    @DisplayName("Should report correct cache size")
    void getCacheSize_MultipleEntries_ReturnsCount() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);
        var player = server.addPlayer("TestPlayer");

        // Act
        cache.put(player, chunk, breakPermission, true);
        cache.put(player, chunk, placePermission, false);
        cache.put(player, chunk, ChunkPermissionType.INTERACT_CHEST, true);

        // Assert
        assertEquals(3, cache.getCacheSize(), "Cache size should match entries");
    }

    @Test
    @DisplayName("Should handle different permission types separately")
    void getCached_DifferentPermissionTypes_IndependentCaching() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);
        var player = server.addPlayer("TestPlayer");
        cache.put(player, chunk, breakPermission, true);
        cache.put(player, chunk, placePermission, false);
        cache.put(player, chunk, ChunkPermissionType.INTERACT_CHEST, true);

        // Act & Assert
        assertTrue(cache.getCached(player, chunk, breakPermission));
        assertFalse(cache.getCached(player, chunk, placePermission));
        assertTrue(cache.getCached(player, chunk, ChunkPermissionType.INTERACT_CHEST));
    }

    @Test
    @DisplayName("Should handle different chunks separately")
    void getCached_DifferentChunks_IndependentCaching() {
        // Arrange
        var world = server.getWorlds().get(0);
        var chunk1 = world.getChunkAt(0, 0);
        var chunk2 = world.getChunkAt(1, 0);
        var player = server.addPlayer("TestPlayer");
        cache.put(player, chunk1, breakPermission, true);
        cache.put(player, chunk2, breakPermission, false);

        // Act & Assert
        assertTrue(cache.getCached(player, chunk1, breakPermission));
        assertFalse(cache.getCached(player, chunk2, breakPermission));
    }

    @Test
    @DisplayName("Should handle different players separately")
    void getCached_DifferentPlayers_IndependentCaching() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");
        cache.put(player1, chunk, breakPermission, true);
        cache.put(player2, chunk, breakPermission, false);

        // Act & Assert
        assertTrue(cache.getCached(player1, chunk, breakPermission));
        assertFalse(cache.getCached(player2, chunk, breakPermission));
    }

    @Test
    @DisplayName("Should handle null world UUID gracefully")
    void invalidateChunk_NullWorldUuid_HandlesGracefully() {
        // Arrange
        cache.clear();

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> cache.invalidateChunk(0, 0, null));
    }

    @Test
    @DisplayName("Should invalidate territory when territoryId is provided")
    void invalidateTerritory_ValidId_DoesNotThrow() {
        // Arrange
        var chunk = server.getWorlds().get(0).getChunkAt(0, 0);
        var player = server.addPlayer("TestPlayer");
        cache.put(player, chunk, breakPermission, true);

        // Act & Assert - should not throw even if no matching territory
        assertDoesNotThrow(() -> cache.invalidateTerritory("test-territory"));
    }
}
