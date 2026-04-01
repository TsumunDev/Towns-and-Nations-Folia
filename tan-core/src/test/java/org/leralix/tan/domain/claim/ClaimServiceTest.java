package org.leralix.tan.domain.claim;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.position.Vector2D;
import org.leralix.tan.dataclass.chunk.TownClaimedChunk;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ClaimService.
 *
 * <p>Tests the new claim service that extracts claim logic from TownData.</p>
 *
 * @since 2.0.0
 */
@DisplayName("ClaimService Tests")
class ClaimServiceTest {

    private ClaimService service;
    private TownDataStorage mockTownStorage;

    @BeforeEach
    void setUp() {
        // Create mock storage instance
        mockTownStorage = mock(TownDataStorage.class);

        // Create the service with mocked dependency
        service = new ClaimServiceImpl(mockTownStorage);
    }

    @AfterEach
    void tearDown() {
        // Reset holder to avoid polluting other tests
        ClaimHolder.clear();
    }

    @Test
    @Disabled("Requires NewClaimedChunkStorage singleton mocking")
    @DisplayName("getNumberOfClaimedChunks should return correct count")
    void testGetNumberOfClaimedChunks() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        int result = service.getNumberOfClaimedChunks(townId).get();

        // Assert
        assertNotNull(mockTown);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("getNumberOfClaimedChunks should return 0 for null town")
    void testGetNumberOfClaimedChunksNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        int result = service.getNumberOfClaimedChunks(townId).get();

        // Assert
        assertEquals(0, result);
    }

    @Test
    @DisplayName("getClaimCost should return correct cost")
    void testGetClaimCost() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getClaimCost()).thenReturn(100);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        int result = service.getClaimCost(townId).get();

        // Assert
        assertEquals(100, result);
    }

    @Test
    @DisplayName("getClaimCost should return 0 for null town")
    void testGetClaimCostNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        int result = service.getClaimCost(townId).get();

        // Assert
        assertEquals(0, result);
    }

    @Test
    @DisplayName("getCapitalLocation should return capital when set")
    void testGetCapitalLocation() throws Exception {
        // Arrange
        String townId = "town_test";
        Vector2D capital = new Vector2D(10, 20, "world-uuid");
        TownData mockTown = mock(TownData.class);

        when(mockTown.getCapitalLocation()).thenReturn(Optional.of(capital));
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Optional<Vector2D> result = service.getCapitalLocation(townId).get();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(capital, result.get());
    }

    @Test
    @DisplayName("getCapitalLocation should return empty when not set")
    void testGetCapitalLocationNotSet() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getCapitalLocation()).thenReturn(Optional.empty());
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Optional<Vector2D> result = service.getCapitalLocation(townId).get();

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getCapitalLocation should return empty for null town")
    void testGetCapitalLocationNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Optional<Vector2D> result = service.getCapitalLocation(townId).get();

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("setCapitalLocation should update town and save")
    void testSetCapitalLocation() throws Exception {
        // Arrange
        String townId = "town_test";
        Vector2D location = new Vector2D(15, 25, "world-uuid");
        TownData mockTown = mock(TownData.class);

        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        service.setCapitalLocation(townId, location).get();

        // Assert
        verify(mockTown).setCapitalLocation(location);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("setCapitalLocation should handle null town gracefully")
    void testSetCapitalLocationNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        Vector2D location = new Vector2D(15, 25, "world-uuid");

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            service.setCapitalLocation(townId, location).get();
        });
    }

    @Test
    @Disabled("Requires NewClaimedChunkStorage singleton mocking")
    @DisplayName("claimChunk should claim chunk and set capital on first claim")
    void testClaimChunkFirstClaim() throws Exception {
        // Arrange
        String townId = "town_test";
        World mockWorld = mock(World.class);
        when(mockWorld.getUID()).thenReturn(java.util.UUID.fromString("12345678-1234-1234-1234-123456789012"));
        Chunk mockChunk = mock(Chunk.class);
        when(mockChunk.getX()).thenReturn(10);
        when(mockChunk.getZ()).thenReturn(20);
        when(mockChunk.getWorld()).thenReturn(mockWorld);

        TownClaimedChunk mockClaimedChunk = mock(TownClaimedChunk.class);
        Vector2D chunkVector = new Vector2D(10, 20, "12345678-1234-1234-1234-123456789012");
        when(mockClaimedChunk.getVector2D()).thenReturn(chunkVector);

        TownData mockTown = mock(TownData.class);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTown.getClaimCost()).thenReturn(100);
        when(mockTown.getNumberOfClaimedChunk()).thenReturn(0); // First claim
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        TownClaimedChunk result = service.claimChunk(townId, mockChunk).get();

        // Assert - Basic validation only due to singleton dependency
        assertNotNull(result);
        verify(mockTown).removeFromBalance(100);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("claimChunk should handle null town gracefully")
    void testClaimChunkNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        Chunk mockChunk = mock(Chunk.class);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        TownClaimedChunk result = service.claimChunk(townId, mockChunk).get();

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("ClaimHolder should return service instance")
    void testClaimHolder() {
        // Arrange
        ClaimHolder.setService(service);

        // Act
        ClaimService retrieved = ClaimHolder.getService();

        // Assert
        assertNotNull(retrieved);
        assertEquals(service, retrieved);
    }

    @Test
    @DisplayName("isEnabled should return false when plugin is null")
    void testIsEnabledDefault() {
        // Act - No plugin initialized during unit tests
        boolean result = service.isEnabled();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("ClaimHolder clear should reset instance")
    @Disabled("Requires NewClaimedChunkStorage singleton mocking")
    void testClaimHolderClear() {
        // Arrange
        ClaimHolder.setService(service);
        ClaimHolder.clear();

        // Act - Getting a new service after clear should create a new instance
        ClaimService newService = ClaimHolder.getService();

        // Assert - The new service is not the same as the old one (unless singleton returns same instance)
        // In production, this would be a real service instance, not the mock
        assertNotNull(newService);
    }
}
