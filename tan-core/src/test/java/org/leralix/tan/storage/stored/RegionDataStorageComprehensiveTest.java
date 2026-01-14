package org.leralix.tan.storage.stored;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.TownRelation;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive unit tests for RegionDataStorage class.
 *
 * <p>Tests region storage operations including region creation/deletion,
 * vassal relationship updates, and capital changes as required by Story 4.1.</p>
 *
 * @since 0.16.0
 */
@DisplayName("RegionDataStorage Comprehensive Tests")
public class RegionDataStorageComprehensiveTest {

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

    // ==================== Region Creation/Deletion Tests ====================

    @Test
    @DisplayName("Region creation should succeed with valid player")
    void testRegionCreation() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("RegionCreator");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        CompletableFuture<RegionData> future = storage.newRegion("TestRegion", tanPlayer);

        // Assert
        assertNotNull(future);
        assertDoesNotThrow(() -> {
            RegionData region = future.get(5, TimeUnit.SECONDS);
            assertNotNull(region);
            assertEquals("TestRegion", region.getID());
            assertEquals(tanPlayer.getID(), region.getLeaderID());
        });
    }

    @Test
    @DisplayName("Region creation should set leader correctly")
    void testRegionCreationSetsLeader() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("RegionLeader");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        RegionData region = storage.newRegion("LeaderRegion", tanPlayer).join();

        // Assert
        assertNotNull(region);
        assertEquals(tanPlayer.getID(), region.getLeaderID());
        assertTrue(region.isLeader(tanPlayer));
    }

    @Test
    @DisplayName("Region deletion should remove region from storage")
    void testRegionDeletion() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("RegionDeleter");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        RegionData region = storage.newRegion("DeletableRegion", tanPlayer).join();

        // Act
        storage.deleteRegion(region);

        // Assert
        RegionData deletedRegion = storage.getSync(region.getID());
        assertNull(deletedRegion, "Deleted region should not be retrievable");
    }

    @Test
    @DisplayName("Region deletion should handle non-existent region gracefully")
    void testRegionDeletionNonExistent() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> storage.deleteRegion(null));
    }

    // ==================== Vassal Relationship Tests ====================

    @Test
    @DisplayName("Adding vassal town should persist correctly")
    void testAddVassalTown() {
        // Arrange
        RegionDataStorage regionStorage = RegionDataStorage.getInstance();
        TownDataStorage townStorage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();

        PlayerMock regionLeader = server.addPlayer("RegionLeaderPlayer");
        PlayerMock townLeader = server.addPlayer("TownLeaderPlayer");

        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("VassalRegion", tanRegionLeader).join();
        TownData town = townStorage.newTown("VassalTown", tanTownLeader).join();

        // Act
        region.addVassal(town);
        regionStorage.putSync(region.getID(), region);

        // Assert
        RegionData fetchedRegion = regionStorage.getSync(region.getID());
        assertTrue(fetchedRegion.isVassal(town),
            "Town should be a vassal of the region");
    }

    @Test
    @DisplayName("Removing vassal town should persist correctly")
    void testRemoveVassalTown() {
        // Arrange
        RegionDataStorage regionStorage = RegionDataStorage.getInstance();
        TownDataStorage townStorage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();

        PlayerMock regionLeader = server.addPlayer("RemoveVassalRegionLeader");
        PlayerMock townLeader = server.addPlayer("RemoveVassalTownLeader");

        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("RemoveVassalRegion", tanRegionLeader).join();
        TownData town = townStorage.newTown("RemoveVassalTown", tanTownLeader).join();

        region.addVassal(town);
        regionStorage.putSync(region.getID(), region);

        // Act
        region.removeVassal(town);
        regionStorage.putSync(region.getID(), region);

        // Assert
        RegionData fetchedRegion = regionStorage.getSync(region.getID());
        assertFalse(fetchedRegion.isVassal(town),
            "Town should not be a vassal after removal");
    }

    @Test
    @DisplayName("Vassal relationship should be bidirectional")
    void testVassalRelationshipBidirectional() {
        // Arrange
        RegionDataStorage regionStorage = RegionDataStorage.getInstance();
        TownDataStorage townStorage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();

        PlayerMock regionLeader = server.addPlayer("BiDirectionalRegionLeader");
        PlayerMock townLeader = server.addPlayer("BiDirectionalTownLeader");

        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("BiDirectionalRegion", tanRegionLeader).join();
        TownData town = townStorage.newTown("BiDirectionalTown", tanTownLeader).join();

        // Act
        region.addVassal(town);
        regionStorage.putSync(region.getID(), region);
        townStorage.putSync(town.getID(), town);

        // Assert - Town should have overlord
        TownData fetchedTown = townStorage.getSync(town.getID());
        assertTrue(fetchedTown.haveOverlord(),
            "Town should have an overlord");
        assertEquals(region.getID(), fetchedTown.getOverlordID(),
            "Overlord ID should match region ID");
    }

    // ==================== Capital Management Tests ====================

    @Test
    @DisplayName("Setting capital should persist correctly")
    void testSetCapital() {
        // Arrange
        RegionDataStorage regionStorage = RegionDataStorage.getInstance();
        TownDataStorage townStorage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();

        PlayerMock regionLeader = server.addPlayer("CapitalRegionLeader");
        PlayerMock townLeader1 = server.addPlayer("CapitalTownLeader1");
        PlayerMock townLeader2 = server.addPlayer("CapitalTownLeader2");

        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader1 = playerStorage.getSync(townLeader1.getUniqueId().toString());
        ITanPlayer tanTownLeader2 = playerStorage.getSync(townLeader2.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("CapitalRegion", tanRegionLeader).join();
        TownData town1 = townStorage.newTown("CapitalTown1", tanTownLeader1).join();
        TownData town2 = townStorage.newTown("CapitalTown2", tanTownLeader2).join();

        region.addVassal(town1);
        region.addVassal(town2);

        // Act - Set initial capital
        region.setCapital(town1);
        regionStorage.putSync(region.getID(), region);

        // Assert
        RegionData fetchedRegion = regionStorage.getSync(region.getID());
        assertEquals(town1.getID(), fetchedRegion.getCapitalID(),
            "Capital should be set to town1");

        // Act - Change capital
        region.setCapital(town2);
        regionStorage.putSync(region.getID(), region);

        // Assert
        RegionData refetchedRegion = regionStorage.getSync(region.getID());
        assertEquals(town2.getID(), refetchedRegion.getCapitalID(),
            "Capital should be changed to town2");
    }

    @Test
    @DisplayName("Removing capital should persist correctly")
    void testRemoveCapital() {
        // Arrange
        RegionDataStorage regionStorage = RegionDataStorage.getInstance();
        TownDataStorage townStorage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();

        PlayerMock regionLeader = server.addPlayer("RemoveCapitalRegionLeader");
        PlayerMock townLeader = server.addPlayer("RemoveCapitalTownLeader");

        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("RemoveCapitalRegion", tanRegionLeader).join();
        TownData town = townStorage.newTown("RemoveCapitalTown", tanTownLeader).join();

        region.addVassal(town);
        region.setCapital(town);
        regionStorage.putSync(region.getID(), region);

        // Act
        region.removeCapital();
        regionStorage.putSync(region.getID(), region);

        // Assert
        RegionData fetchedRegion = regionStorage.getSync(region.getID());
        assertFalse(fetchedRegion.hasCapital(),
            "Region should not have a capital after removal");
    }

    // ==================== Member Management Tests ====================

    @Test
    @DisplayName("Adding region member should persist")
    void testAddRegionMember() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock leader = server.addPlayer("RegionMemberLeader");
        PlayerMock member = server.addPlayer("RegionMember");

        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());

        RegionData region = storage.newRegion("MemberRegion", tanLeader).join();

        // Act
        region.addPlayer(tanMember);
        storage.putSync(region.getID(), region);

        // Assert
        RegionData fetchedRegion = storage.getSync(region.getID());
        assertTrue(fetchedRegion.isPlayerIn(tanMember),
            "Member should be in region");
        assertTrue(fetchedRegion.getITanPlayerList().contains(tanMember),
            "Member should be in player list");
    }

    @Test
    @DisplayName("Removing region member should persist")
    void testRemoveRegionMember() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock leader = server.addPlayer("RemoveRegionMemberLeader");
        PlayerMock member = server.addPlayer("RemoveRegionMember");

        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());

        RegionData region = storage.newRegion("RemoveMemberRegion", tanLeader).join();
        region.addPlayer(tanMember);
        storage.putSync(region.getID(), region);

        // Act
        region.removePlayer(tanMember.getID());
        storage.putSync(region.getID(), region);

        // Assert
        RegionData fetchedRegion = storage.getSync(region.getID());
        assertFalse(fetchedRegion.isPlayerIn(tanMember),
            "Member should not be in region after removal");
    }

    // ==================== Diplomacy Tests ====================

    @Test
    @DisplayName("Diplomatic relations between regions should persist")
    void testRegionDiplomacy() {
        // Arrange
        RegionDataStorage regionStorage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();

        PlayerMock player1 = server.addPlayer("DiplomacyPlayer1");
        PlayerMock player2 = server.addPlayer("DiplomacyPlayer2");

        ITanPlayer tanPlayer1 = playerStorage.getSync(player1.getUniqueId().toString());
        ITanPlayer tanPlayer2 = playerStorage.getSync(player2.getUniqueId().toString());

        RegionData region1 = regionStorage.newRegion("DiplomacyRegion1", tanPlayer1).join();
        RegionData region2 = regionStorage.newRegion("DiplomacyRegion2", tanPlayer2).join();

        // Act
        region1.setRelation(region2.getID(), TownRelation.ALLY);
        regionStorage.putSync(region1.getID(), region1);

        // Assert
        RegionData fetchedRegion = regionStorage.getSync(region1.getID());
        assertEquals(TownRelation.ALLY, fetchedRegion.getRelation(region2.getID()),
            "Diplomatic relation should persist");
    }

    // ==================== Batch Operations Tests ====================

    @Test
    @DisplayName("Batch region load should retrieve multiple regions efficiently")
    void testBatchRegionLoad() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        Collection<String> regionIds = new ArrayList<>();

        // Create multiple regions
        for (int i = 0; i < 3; i++) {
            PlayerMock player = server.addPlayer("BatchRegionLeader" + i);
            ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
            RegionData region = storage.newRegion("BatchRegion" + i, tanPlayer).join();
            regionIds.add(region.getID());
        }

        // Act
        java.util.Map<String, RegionData> results = storage.getBatchSync(regionIds);

        // Assert
        assertNotNull(results);
        assertEquals(3, results.size());
        for (String regionId : regionIds) {
            assertTrue(results.containsKey(regionId));
            assertNotNull(results.get(regionId));
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Get with non-existent region should return null")
    void testGetNonExistentRegion() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();

        // Act
        RegionData result = storage.getSync("NonExistentRegion");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Get with null region ID should return null")
    void testGetWithNullRegionId() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();

        // Act
        RegionData result = storage.getSync(null);

        // Assert
        assertNull(result);
    }

    // ==================== Async Operations Tests ====================

    @Test
    @DisplayName("Async region creation should complete successfully")
    void testAsyncRegionCreation() throws Exception {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("AsyncRegionCreator");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        CompletableFuture<RegionData> future = storage.newRegion("AsyncRegion", tanPlayer);

        // Assert
        assertNotNull(future);
        RegionData region = future.get(5, TimeUnit.SECONDS);
        assertNotNull(region);
        assertEquals("AsyncRegion", region.getID());
    }

    @Test
    @DisplayName("Async region retrieval should work correctly")
    void testAsyncRegionRetrieval() throws Exception {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("AsyncRetrieveRegionPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        RegionData region = storage.newRegion("AsyncRetrieveRegion", tanPlayer).join();

        // Act
        CompletableFuture<RegionData> future = storage.get(region.getID());

        // Assert
        assertNotNull(future);
        RegionData result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(region.getID(), result.getID());
    }

    // ==================== Cache Tests ====================

    @Test
    @DisplayName("Region cache should improve performance on repeated access")
    void testRegionCacheEfficiency() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("CacheRegionPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        RegionData region = storage.newRegion("CacheRegion", tanPlayer).join();

        // Act - Load same region multiple times
        long startTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            storage.getSync(region.getID());
        }
        long duration = System.nanoTime() - startTime;

        // Assert - Should complete quickly due to caching
        long durationMs = duration / 1_000_000;
        assertTrue(durationMs < 100, "Cached access should be fast, took: " + durationMs + "ms");
    }

    // ==================== Economy Tests ====================

    @Test
    @DisplayName("Region economy updates should persist")
    void testRegionEconomyUpdate() {
        // Arrange
        RegionDataStorage storage = RegionDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("RegionEconomyPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        RegionData region = storage.newRegion("EconomyRegion", tanPlayer).join();

        // Act
        double initialBalance = region.getBalance();
        region.addToBalance(5000.0);
        storage.putSync(region.getID(), region);

        // Assert
        RegionData fetchedRegion = storage.getSync(region.getID());
        assertEquals(initialBalance + 5000.0, fetchedRegion.getBalance(), 0.001);
    }
}
