package org.leralix.tan.domain.diplomacy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.DiplomacyProposal;
import org.leralix.tan.dataclass.RelationData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DiplomacyService.
 *
 * <p>Tests the new diplomacy service that extracts diplomacy logic from TerritoryData.</p>
 *
 * @since 2.0.0
 */
@DisplayName("DiplomacyService Tests")
class DiplomacyServiceTest {

    private DiplomacyService service;
    private TownDataStorage mockTownStorage;

    @BeforeEach
    void setUp() {
        // Create mock storage instance
        mockTownStorage = mock(TownDataStorage.class);

        // Create the service with mocked dependency
        service = new DiplomacyServiceImpl(mockTownStorage);
    }

    @AfterEach
    void tearDown() {
        // Reset holder to avoid polluting other tests
        DiplomacyHolder.clear();
    }

    @Test
    @DisplayName("getRelations should return relation data")
    void testGetRelations() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);
        RelationData relations = new RelationData();

        when(mockTown.getRelations()).thenReturn(relations);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        RelationData result = service.getRelations(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(relations, result);
    }

    @Test
    @DisplayName("getRelations should return empty data for null town")
    void testGetRelationsNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        RelationData result = service.getRelations(townId).get();

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("getRelationWith should return correct relation")
    void testGetRelationWith() throws Exception {
        // Arrange
        String townId = "town_test";
        String otherId = "town_other";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getRelationWith(otherId)).thenReturn(TownRelation.ALLIANCE);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        TownRelation result = service.getRelationWith(townId, otherId).get();

        // Assert
        assertEquals(TownRelation.ALLIANCE, result);
    }

    @Test
    @DisplayName("getRelationWith should return NEUTRAL for null town")
    void testGetRelationWithNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        String otherId = "town_other";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        TownRelation result = service.getRelationWith(townId, otherId).get();

        // Assert
        assertEquals(TownRelation.NEUTRAL, result);
    }

    @Test
    @DisplayName("getDiplomaticProposals should return proposals")
    void testGetDiplomaticProposals() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        DiplomacyProposal proposal1 = mock(DiplomacyProposal.class);
        DiplomacyProposal proposal2 = mock(DiplomacyProposal.class);
        Collection<DiplomacyProposal> proposals = List.of(proposal1, proposal2);

        when(mockTown.getAllDiplomacyProposal()).thenReturn(proposals);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Collection<DiplomacyProposal> result = service.getDiplomaticProposals(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(proposal1));
        assertTrue(result.contains(proposal2));
    }

    @Test
    @DisplayName("getDiplomaticProposals should return empty for null town")
    void testGetDiplomaticProposalsNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Collection<DiplomacyProposal> result = service.getDiplomaticProposals(townId).get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("hasOverlord should return true when town has overlord")
    void testHasOverlord() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        when(mockTown.haveOverlord()).thenReturn(true);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Boolean result = service.hasOverlord(townId).get();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("hasOverlord should return false for null town")
    void testHasOverlordNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Boolean result = service.hasOverlord(townId).get();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("getVassals should return vassal list")
    void testGetVassals() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        TerritoryData vassal1 = mock(TerritoryData.class);
        TerritoryData vassal2 = mock(TerritoryData.class);
        List<TerritoryData> vassals = List.of(vassal1, vassal2);

        when(mockTown.getVassals()).thenReturn(vassals);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        List<TerritoryData> result = service.getVassals(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(vassal1));
        assertTrue(result.contains(vassal2));
    }

    @Test
    @DisplayName("getVassals should return empty for null town")
    void testGetVassalsNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        List<TerritoryData> result = service.getVassals(townId).get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("isVassal should return true when other is vassal")
    void testIsVassal() throws Exception {
        // Arrange
        String townId = "town_test";
        String otherId = "town_vassal";
        TownData mockTown = mock(TownData.class);

        when(mockTown.isVassal(otherId)).thenReturn(true);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Boolean result = service.isVassal(townId, otherId).get();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("isVassal should return false for null town")
    void testIsVassalNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        String otherId = "town_other";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Boolean result = service.isVassal(townId, otherId).get();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("isAtWar should return true when at war")
    void testIsAtWar() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        when(mockTown.isAtWar()).thenReturn(true);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Boolean result = service.isAtWar(townId).get();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("isAtWar should return false for null town")
    void testIsAtWarNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Boolean result = service.isAtWar(townId).get();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("getAttacksInvolvedIds should return attack IDs")
    void testGetAttacksInvolvedIds() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        Collection<String> attackIds = List.of("attack1", "attack2");

        when(mockTown.getAttacksInvolvedID()).thenReturn(attackIds);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Collection<String> result = service.getAttacksInvolvedIds(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("attack1"));
        assertTrue(result.contains("attack2"));
    }

    @Test
    @DisplayName("getAttacksInvolvedIds should return empty for null town")
    void testGetAttacksInvolvedIdsNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Collection<String> result = service.getAttacksInvolvedIds(townId).get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("removeDiplomaticProposal should handle null town gracefully")
    void testRemoveDiplomaticProposalNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        String proposingId = "town_proposing";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            service.removeDiplomaticProposal(townId, proposingId).get();
        });
    }

    @Test
    @DisplayName("removeOverlord should handle null town gracefully")
    void testRemoveOverlordNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            service.removeOverlord(townId).get();
        });
    }

    @Test
    @DisplayName("DiplomacyHolder should return service instance")
    void testDiplomacyHolder() {
        // Arrange
        DiplomacyHolder.setService(service);

        // Act
        DiplomacyService retrieved = DiplomacyHolder.getService();

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
}
