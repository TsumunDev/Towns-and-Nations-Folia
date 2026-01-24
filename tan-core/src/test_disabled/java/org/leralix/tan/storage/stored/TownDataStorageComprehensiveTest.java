package org.leralix.tan.storage.stored;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.TownRelation;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * Comprehensive unit tests for TownDataStorage class.
 *
 * <p>Tests town storage operations including town creation/deletion,
 * component updates (economy, diplomacy), and chunk claim updates
 * as required by Story 4.1.</p>
 *
 * @since 0.16.0
 */
@DisplayName("TownDataStorage Comprehensive Tests")
public class TownDataStorageComprehensiveTest {

    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);
        world = new WorldMock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ==================== Town Creation/Deletion Tests ====================

    @Test
    @DisplayName("Town creation should succeed with valid player")
    void testTownCreation() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("TownCreator");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        CompletableFuture<TownData> future = storage.newTown("TestTown", tanPlayer);

        // Assert
        assertNotNull(future);
        assertDoesNotThrow(() -> {
            TownData town = future.get(5, TimeUnit.SECONDS);
            assertNotNull(town);
            assertEquals("TestTown", town.getID());
            assertEquals(tanPlayer.getID(), town.getLeaderID());
        });
    }

    @Test
    @DisplayName("Town creation should set leader correctly")
    void testTownCreationSetsLeader() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("LeaderPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        TownData town = storage.newTown("LeaderTown", tanPlayer).join();

        // Assert
        assertNotNull(town);
        assertEquals(tanPlayer.getID(), town.getLeaderID());
        assertTrue(town.isLeader(tanPlayer));
    }

    @Test
    @DisplayName("Town deletion should remove town from storage")
    void testTownDeletion() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("TownDeleter");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = storage.newTown("DeletableTown", tanPlayer).join();

        // Act
        storage.deleteTown(town);

        // Assert
        TownData deletedTown = storage.getSync(town.getID());
        assertNull(deletedTown, "Deleted town should not be retrievable");
    }

    @Test
    @DisplayName("Town deletion should handle non-existent town gracefully")
    void testTownDeletionNonExistent() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> storage.deleteTown(null));
    }

    // ==================== Component Updates Tests ====================

    @Test
    @DisplayName("Economy component updates should persist")
    void testEconomyComponentUpdate() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("EconomyPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = storage.newTown("EconomyTown", tanPlayer).join();

        // Act
        double initialBalance = town.getBalance();
        town.addToBalance(1000.0);
        storage.putSync(town.getID(), town);

        // Assert
        TownData fetchedTown = storage.getSync(town.getID());
        assertEquals(initialBalance + 1000.0, fetchedTown.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Tax collection should update economy component")
    void testTaxCollectionUpdate() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock leader = server.addPlayer("TaxLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = storage.newTown("TaxTown", tanLeader).join();

        // Set tax
        double taxAmount = 10.0;
        town.setTax(taxAmount);
        storage.putSync(town.getID(), town);

        // Assert
        TownData fetchedTown = storage.getSync(town.getID());
        assertEquals(taxAmount, fetchedTown.getTax(), 0.001);
    }

    @Test
    @DisplayName("Diplomacy component updates should persist")
    void testDiplomacyComponentUpdate() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player1 = server.addPlayer("DiplomacyPlayer1");
        PlayerMock player2 = server.addPlayer("DiplomacyPlayer2");

        ITanPlayer tanPlayer1 = playerStorage.getSync(player1.getUniqueId().toString());
        ITanPlayer tanPlayer2 = playerStorage.getSync(player2.getUniqueId().toString());

        TownData town1 = storage.newTown("DiplomacyTown1", tanPlayer1).join();
        TownData town2 = storage.newTown("DiplomacyTown2", tanPlayer2).join();

        // Act - Set diplomatic relation
        town1.setRelation(town2.getID(), TownRelation.ALLY);
        storage.putSync(town1.getID(), town1);

        // Assert
        TownData fetchedTown = storage.getSync(town1.getID());
        assertEquals(TownRelation.ALLY, fetchedTown.getRelation(town2.getID()));
    }

    // ==================== Chunk Claim Tests ====================

    @Test
    @DisplayName("Chunk claim should persist correctly")
    void testChunkClaim() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("ClaimPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = storage.newTown("ClaimTown", tanPlayer).join();

        Location claimLocation = new Location(world, 100, 64, 100);

        // Act
        town.claimChunk(player, claimLocation);
        storage.putSync(town.getID(), town);

        // Assert
        TownData fetchedTown = storage.getSync(town.getID());
        assertTrue(fetchedTown.hasClaimedChunk(claimLocation),
            "Town should have claimed the chunk");
    }

    @Test
    @DisplayName("Multiple chunk claims should persist")
    void testMultipleChunkClaims() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("MultiClaimPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = storage.newTown("MultiClaimTown", tanPlayer).join();

        List<Location> locations = new ArrayList<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                locations.add(new Location(world, x * 16, 64, z * 16));
            }
        }

        // Act - Claim multiple chunks
        for (Location loc : locations) {
            town.claimChunk(player, loc);
        }
        storage.putSync(town.getID(), town);

        // Assert
        TownData fetchedTown = storage.getSync(town.getID());
        assertEquals(9, fetchedTown.getNumberOfClaimedChunk(),
            "Town should have 9 claimed chunks");
    }

    @Test
    @DisplayName("Chunk unclaim should remove chunk correctly")
    void testChunkUnclaim() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("UnclaimPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = storage.newTown("UnclaimTown", tanPlayer).join();

        Location claimLocation = new Location(world, 200, 64, 200);
        town.claimChunk(player, claimLocation);

        // Act
        town.unclaimChunk(claimLocation);
        storage.putSync(town.getID(), town);

        // Assert
        TownData fetchedTown = storage.getSync(town.getID());
        assertFalse(fetchedTown.hasClaimedChunk(claimLocation),
            "Town should not have the unclaimed chunk");
    }

    // ==================== Member Management Tests ====================

    @Test
    @DisplayName("Adding town member should persist")
    void testAddTownMember() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock leader = server.addPlayer("MemberLeader");
        PlayerMock member = server.addPlayer("TownMember");

        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());

        TownData town = storage.newTown("MemberTown", tanLeader).join();

        // Act
        town.addPlayer(tanMember);
        storage.putSync(town.getID(), town);

        // Assert
        TownData fetchedTown = storage.getSync(town.getID());
        assertTrue(fetchedTown.isPlayerIn(tanMember),
            "Member should be in town");
        assertTrue(fetchedTown.getITanPlayerList().contains(tanMember),
            "Member should be in player list");
    }

    @Test
    @DisplayName("Removing town member should persist")
    void testRemoveTownMember() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock leader = server.addPlayer("RemoveLeader");
        PlayerMock member = server.addPlayer("RemoveMember");

        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());

        TownData town = storage.newTown("RemoveTown", tanLeader).join();
        town.addPlayer(tanMember);

        // Act
        town.removePlayer(tanMember.getID());
        storage.putSync(town.getID(), town);

        // Assert
        TownData fetchedTown = storage.getSync(town.getID());
        assertFalse(fetchedTown.isPlayerIn(tanMember),
            "Member should not be in town after removal");
    }

    // ==================== Batch Operations Tests ====================

    @Test
    @DisplayName("Batch town load should retrieve multiple towns efficiently")
    void testBatchTownLoad() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        Collection<String> townIds = new ArrayList<>();

        // Create multiple towns
        for (int i = 0; i < 3; i++) {
            PlayerMock player = server.addPlayer("BatchTownLeader" + i);
            ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
            TownData town = storage.newTown("BatchTown" + i, tanPlayer).join();
            townIds.add(town.getID());
        }

        // Act
        java.util.Map<String, TownData> results = storage.getBatchSync(townIds);

        // Assert
        assertNotNull(results);
        assertEquals(3, results.size());
        for (String townId : townIds) {
            assertTrue(results.containsKey(townId));
            assertNotNull(results.get(townId));
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Get with non-existent town should return null")
    void testGetNonExistentTown() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act
        TownData result = storage.getSync("NonExistentTown");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Get with null town ID should return null")
    void testGetWithNullTownId() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();

        // Act
        TownData result = storage.getSync(null);

        // Assert
        assertNull(result);
    }

    // ==================== Async Operations Tests ====================

    @Test
    @DisplayName("Async town creation should complete successfully")
    void testAsyncTownCreation() throws Exception {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("AsyncTownCreator");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        CompletableFuture<TownData> future = storage.newTown("AsyncTown", tanPlayer);

        // Assert
        assertNotNull(future);
        TownData town = future.get(5, TimeUnit.SECONDS);
        assertNotNull(town);
        assertEquals("AsyncTown", town.getID());
    }

    @Test
    @DisplayName("Async town retrieval should work correctly")
    void testAsyncTownRetrieval() throws Exception {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("AsyncRetrievePlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = storage.newTown("AsyncRetrieveTown", tanPlayer).join();

        // Act
        CompletableFuture<TownData> future = storage.get(town.getID());

        // Assert
        assertNotNull(future);
        TownData result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(town.getID(), result.getID());
    }

    // ==================== Cache Tests ====================

    @Test
    @DisplayName("Town cache should improve performance on repeated access")
    void testTownCacheEfficiency() {
        // Arrange
        TownDataStorage storage = TownDataStorage.getInstance();
        PlayerDataStorage playerStorage = PlayerDataStorage.getInstance();
        PlayerMock player = server.addPlayer("CacheTownPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = storage.newTown("CacheTown", tanPlayer).join();

        // Act - Load same town multiple times
        long startTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            storage.getSync(town.getID());
        }
        long duration = System.nanoTime() - startTime;

        // Assert - Should complete quickly due to caching
        long durationMs = duration / 1_000_000;
        assertTrue(durationMs < 100, "Cached access should be fast, took: " + durationMs + "ms");
    }
}
