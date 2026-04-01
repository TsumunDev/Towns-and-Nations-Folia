package org.leralix.tan.domain.economy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.progression.TownTier;
import org.leralix.tan.domain.prestige.model.PrestigePoints;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for TownEconomyService.
 *
 * <p>Tests the new economy service that extracts economy logic from TownData.</p>
 *
 * @since 2.0.0
 */
@DisplayName("TownEconomyService Tests")
class TownEconomyServiceTest {

    private TownEconomyService service;
    private TownDataStorage mockTownStorage;

    @BeforeEach
    void setUp() {
        // Create mock storage instance
        mockTownStorage = mock(TownDataStorage.class);

        // Create the service with mocked dependency
        service = new TownEconomyServiceImpl(mockTownStorage);
    }

    @AfterEach
    void tearDown() {
        // Reset holder to avoid polluting other tests
        TownEconomyHolder.clear();
    }

    @Test
    @DisplayName("getTownTier should return correct tier")
    void testGetTownTier() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);
        when(mockTown.getTownTier()).thenReturn(TownTier.VILLAGE);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        TownTier result = service.getTownTier(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(TownTier.VILLAGE, result);
    }

    @Test
    @DisplayName("getTownTier should return TIER_1 for null town")
    void testGetTownTierNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        TownTier result = service.getTownTier(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(TownTier.CAMPING, result); // Default fallback
    }

    @Test
    @DisplayName("getTownLevel should return correct level")
    void testGetTownLevel() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);
        when(mockTown.getTownLevel()).thenReturn(5);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Integer result = service.getTownLevel(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(5, result);
    }

    @Test
    @DisplayName("getTownLevel should return 1 for null town")
    void testGetTownLevelNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Integer result = service.getTownLevel(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(1, result); // Default fallback
    }

    @Test
    @DisplayName("getPrestigeBalance should return correct balance")
    void testGetPrestigeBalance() throws Exception {
        // Arrange
        String townId = "town_test";
        long expectedBalance = 5000L;

        TownData mockTown = mock(TownData.class);
        when(mockTown.getPrestigeBalance()).thenReturn(expectedBalance);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Long result = service.getPrestigeBalance(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(expectedBalance, result);
    }

    @Test
    @DisplayName("getPrestigeBalance should return 0 for null town")
    void testGetPrestigeBalanceNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Long result = service.getPrestigeBalance(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(0L, result); // Default fallback
    }

    @Test
    @DisplayName("getPrestigePoints should return correct points")
    void testGetPrestigePoints() throws Exception {
        // Arrange
        String townId = "town_test";
        PrestigePoints expectedPoints = PrestigePoints.create();

        TownData mockTown = mock(TownData.class);
        when(mockTown.getPrestigePoints()).thenReturn(expectedPoints);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        PrestigePoints result = service.getPrestigePoints(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(expectedPoints, result);
    }

    @Test
    @DisplayName("hasPurchasedUpgrade should return true for purchased upgrade")
    void testHasPurchasedUpgradeTrue() throws Exception {
        // Arrange
        String townId = "town_test";
        String upgradeId = "mob_ban";

        TownData mockTown = mock(TownData.class);
        when(mockTown.hasPurchasedUpgrade(upgradeId)).thenReturn(true);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Boolean result = service.hasPurchasedUpgrade(townId, upgradeId).get();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("hasPurchasedUpgrade should return false for non-purchased upgrade")
    void testHasPurchasedUpgradeFalse() throws Exception {
        // Arrange
        String townId = "town_test";
        String upgradeId = "spawn";

        TownData mockTown = mock(TownData.class);
        when(mockTown.hasPurchasedUpgrade(upgradeId)).thenReturn(false);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Boolean result = service.hasPurchasedUpgrade(townId, upgradeId).get();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("hasPurchasedUpgrade should return false for null town")
    void testHasPurchasedUpgradeNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        String upgradeId = "mob_ban";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Boolean result = service.hasPurchasedUpgrade(townId, upgradeId).get();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("getPurchasedUpgrades should return all upgrades")
    void testGetPurchasedUpgrades() throws Exception {
        // Arrange
        String townId = "town_test";
        Set<String> expectedUpgrades = Set.of("mob_ban", "spawn", "teleport");

        TownData mockTown = mock(TownData.class);
        when(mockTown.getPurchasedUpgrades()).thenReturn(expectedUpgrades);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Set<String> result = service.getPurchasedUpgrades(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("mob_ban"));
        assertTrue(result.contains("spawn"));
        assertTrue(result.contains("teleport"));
    }

    @Test
    @DisplayName("getPurchasedUpgrades should return empty set for null town")
    void testGetPurchasedUpgradesNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Set<String> result = service.getPurchasedUpgrades(townId).get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("addPurchasedUpgrade should add upgrade and save")
    void testAddPurchasedUpgrade() throws Exception {
        // Arrange
        String townId = "town_test";
        String upgradeId = "mob_ban";

        TownData mockTown = mock(TownData.class);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        service.addPurchasedUpgrade(townId, upgradeId).get();

        // Assert
        verify(mockTown).addPurchasedUpgrade(upgradeId);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("addPurchasedUpgrade should handle null town gracefully")
    void testAddPurchasedUpgradeNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        String upgradeId = "mob_ban";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            service.addPurchasedUpgrade(townId, upgradeId).get();
        });
    }

    @Test
    @DisplayName("TownEconomyHolder should return service instance")
    void testTownEconomyHolder() {
        // Arrange
        TownEconomyHolder.setService(service);

        // Act
        TownEconomyService retrieved = TownEconomyHolder.getService();

        // Assert
        assertNotNull(retrieved);
        assertEquals(service, retrieved);
    }

    @Test
    @DisplayName("TownEconomyHolder should create default instance if not set")
    void testTownEconomyHolderDefaultInstance() {
        // Arrange
        TownEconomyHolder.clear(); // Ensure clear state

        // Act
        TownEconomyService retrieved = TownEconomyHolder.getService();

        // Assert
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof TownEconomyServiceImpl);
    }

    @Test
    @DisplayName("isEnabled should return false by default")
    void testIsEnabledDefault() {
        // Act
        boolean result = service.isEnabled();

        // Assert
        assertFalse(result);
    }
}
