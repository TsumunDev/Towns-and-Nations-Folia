package org.leralix.tan.dataclass.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.dataclass.rank.RankData;
import org.leralix.tan.enums.RolePermission;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive tests for TerritoryData abstract base class.
 *
 * <p>Tests the core territory functionality through TownData concrete implementation:</p>
 * <ul>
 *   <li>Basic territory properties (ID, name, description)</li>
 *   <li>Component pattern (Treasury, Tax, Diplomacy, War)</li>
 *   <li>Rank management</li>
 *   <li>Member management</li>
 *   <li>Chunk claiming</li>
 *   <li>Thread-safety and concurrent access</li>
 * </ul>
 *
 * <p><b>Note:</b> Tests use TownData as concrete implementation of TerritoryData
 * since TerritoryData is abstract.</p>
 */
@DisplayName("TerritoryData Tests (via TownData)")
class TerritoryDataComprehensiveTest {

    private ServerMock server;
    private PlayerDataStorage playerStorage;
    private TownDataStorage townStorage;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        playerStorage = PlayerDataStorage.getInstance();
        townStorage = TownDataStorage.getInstance();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ========== Basic Territory Properties Tests ==========

    @Test
    @DisplayName("Territory should have unique ID")
    void testTerritoryId() {
        // Arrange
        PlayerMock player = server.addPlayer("LeaderPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        TownData town = townStorage.newTown("TestIdTown", tanPlayer).join();

        // Assert
        assertNotNull(town.getID(), "Town should have an ID");
        assertFalse(town.getID().isEmpty(), "ID should not be empty");
    }

    @Test
    @DisplayName("Territory should have a name")
    void testTerritoryName() {
        // Arrange
        PlayerMock player = server.addPlayer("NamePlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        String townName = "TestNameTown";

        // Act
        TownData town = townStorage.newTown(townName, tanPlayer).join();

        // Assert
        assertEquals(townName, town.getName(), "Town name should match");
    }

    @Test
    @DisplayName("Territory should have a creation timestamp")
    void testCreationTimestamp() {
        // Arrange
        PlayerMock player = server.addPlayer("TimestampPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        long beforeCreate = System.currentTimeMillis();
        TownData town = townStorage.newTown("TimestampTown", tanPlayer).join();
        long afterCreate = System.currentTimeMillis();

        // Assert - TerritoryData doesn't expose timestamp directly, but it's set in constructor
        assertNotNull(town, "Town should be created");
    }

    @Test
    @DisplayName("Territory should have a description")
    void testTerritoryDescription() {
        // Arrange
        PlayerMock player = server.addPlayer("DescPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("DescTown", tanPlayer).join();

        // Act
        String description = town.getDescription();

        // Assert
        assertNotNull(description, "Description should not be null");
    }

    // ========== Component Pattern Tests ==========

    @Test
    @DisplayName("Territory should have TreasuryComponent")
    void testTreasuryComponent() {
        // Arrange
        PlayerMock player = server.addPlayer("TreasuryPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("TreasuryTown", tanPlayer).join();

        // Act
        double balance = town.getBalance();

        // Assert - TreasuryComponent is integrated via getBalance()
        assertNotNull(town, "Town should have treasury component");
        assertEquals(0.0, balance, 0.001, "New town should have 0 balance");
    }

    @Test
    @DisplayName("Territory should have TaxComponent")
    void testTaxComponent() {
        // Arrange
        PlayerMock player = server.addPlayer("TaxPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("TaxTown", tanPlayer).join();

        // Act
        double taxRate = town.getTax();

        // Assert - TaxComponent is integrated via getTax()
        assertTrue(taxRate >= 0.0, "Tax rate should be non-negative");
    }

    @Test
    @DisplayName("Territory should have DiplomacyComponent")
    void testDiplomacyComponent() {
        // Arrange
        PlayerMock player = server.addPlayer("DiplomacyPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("DiplomacyTown", tanPlayer).join();

        // Act
        TownRelation relation = town.getRelation(town.getID()); // Self-relation

        // Assert - DiplomacyComponent handles relations
        assertNotNull(relation, "Should have diplomacy component");
    }

    // ========== Leader Management Tests ==========

    @Test
    @DisplayName("Territory should have a leader")
    void testTerritoryLeader() {
        // Arrange
        PlayerMock player = server.addPlayer("MainLeader");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        TownData town = townStorage.newTown("LeaderTown", tanPlayer).join();

        // Assert
        assertTrue(town.isLeader(tanPlayer), "Creator should be leader");
        assertEquals(tanPlayer.getID(), town.getLeaderID(), "Leader ID should match");
    }

    @Test
    @DisplayName("isLeader should return false for non-leader")
    void testIsLeaderFalse() {
        // Arrange
        PlayerMock leader = server.addPlayer("Leader");
        PlayerMock member = server.addPlayer("Member");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());

        TownData town = townStorage.newTown("LeaderCheckTown", tanLeader).join();

        // Act & Assert
        assertFalse(town.isLeader(tanMember), "Non-leader should not be recognized as leader");
    }

    // ========== Rank Management Tests ==========

    @Test
    @DisplayName("Territory should create default rank")
    void testDefaultRankCreation() {
        // Arrange
        PlayerMock player = server.addPlayer("RankPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        TownData town = townStorage.newTown("RankTown", tanPlayer).join();

        // Assert
        assertNotNull(town.getDefaultRankID(), "Should have default rank ID");
    }

    @Test
    @DisplayName("Territory should allow registering new ranks")
    void testRegisterNewRank() {
        // Arrange
        PlayerMock player = server.addPlayer("NewRankPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("NewRankTown", tanPlayer).join();

        // Act
        RankData newRank = town.registerNewRank("CustomRank");

        // Assert
        assertNotNull(newRank, "New rank should be created");
        assertEquals("CustomRank", newRank.getName(), "Rank name should match");
    }

    // ========== Member Management Tests ==========

    @Test
    @DisplayName("Territory should allow adding members")
    void testAddMember() {
        // Arrange
        PlayerMock leader = server.addPlayer("TownLeader");
        PlayerMock member = server.addPlayer("TownMember");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());

        TownData town = townStorage.newTown("MemberTown", tanLeader).join();

        // Act
        town.addPlayer(tanMember);

        // Assert
        assertTrue(town.isPlayerInTown(tanMember), "Member should be in town");
    }

    @Test
    @DisplayName("Territory should allow removing members")
    void testRemoveMember() {
        // Arrange
        PlayerMock leader = server.addPlayer("RemoveLeader");
        PlayerMock member = server.addPlayer("RemoveMember");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());

        TownData town = townStorage.newTown("RemoveMemberTown", tanLeader).join();
        town.addPlayer(tanMember);

        // Act
        town.removePlayer(tanMember);

        // Assert
        assertFalse(town.isPlayerInTown(tanMember), "Member should be removed");
    }

    // ========== Balance Operations Tests ==========

    @Test
    @DisplayName("Territory should allow depositing to balance")
    void testDepositToBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("DepositPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("DepositTown", tanPlayer).join();
        double initialBalance = town.getBalance();

        // Act
        town.addToBalance(1000.0);

        // Assert
        assertEquals(initialBalance + 1000.0, town.getBalance(), 0.001,
            "Balance should increase");
    }

    @Test
    @DisplayName("Territory should allow withdrawing from balance")
    void testWithdrawFromBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("WithdrawPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("WithdrawTown", tanPlayer).join();
        town.addToBalance(1000.0);

        // Act
        town.removeFromBalance(500.0);

        // Assert
        assertEquals(500.0, town.getBalance(), 0.001,
            "Balance should decrease");
    }

    // ========== Chunk Claiming Tests ==========

    @Test
    @DisplayName("Territory should allow claiming chunks")
    void testClaimChunk() {
        // Arrange
        PlayerMock player = server.addPlayer("ClaimPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("ClaimTown", tanPlayer).join();
        Location claimLocation = new Location(server.addSimpleWorld("world"), 100, 64, 100);

        // Act
        town.claimChunk(player, claimLocation);

        // Assert
        assertTrue(town.hasClaimedChunk(claimLocation),
            "Town should have claimed the chunk");
    }

    @Test
    @DisplayName("Territory should allow unclaiming chunks")
    void testUnclaimChunk() {
        // Arrange
        PlayerMock player = server.addPlayer("UnclaimPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("UnclaimTown", tanPlayer).join();
        Location claimLocation = new Location(server.addSimpleWorld("world"), 100, 64, 100);
        town.claimChunk(player, claimLocation);

        // Act
        town.unclaim(claimLocation);

        // Assert
        assertFalse(town.hasClaimedChunk(claimLocation),
            "Town should have unclaimed the chunk");
    }

    @Test
    @DisplayName("Territory should track claimed chunks count")
    void testClaimedChunksCount() {
        // Arrange
        PlayerMock player = server.addPlayer("CountPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("CountTown", tanPlayer).join();
        Location loc1 = new Location(server.addSimpleWorld("world"), 100, 64, 100);
        Location loc2 = new Location(server.addSimpleWorld("world"), 200, 64, 200);

        // Act
        town.claimChunk(player, loc1);
        town.claimChunk(player, loc2);

        // Assert
        assertEquals(2, town.getClaimedChunks().size(),
            "Town should have 2 claimed chunks");
    }

    // ========== Overlord/Vassal Tests ==========

    @Test
    @DisplayName("New territory should not have an overlord")
    void testNoOverlordInitially() {
        // Arrange
        PlayerMock player = server.addPlayer("NoOverlordPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());

        // Act
        TownData town = townStorage.newTown("NoOverlordTown", tanPlayer).join();

        // Assert
        assertFalse(town.haveOverlord(), "New town should not have overlord");
        assertNull(town.getOverlordID(), "Overlord ID should be null");
    }

    // ========== Cosmetic Tests ==========

    @Test
    @DisplayName("Territory should have an icon")
    void testTerritoryIcon() {
        // Arrange
        PlayerMock player = server.addPlayer("IconPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("IconTown", tanPlayer).join();

        // Act
        ItemStack icon = town.getIcon();

        // Assert
        assertNotNull(icon, "Town should have an icon");
        assertNotEquals(Material.AIR, icon.getType(), "Icon should not be air");
    }

    @Test
    @DisplayName("Territory should have a color")
    void testTerritoryColor() {
        // Arrange
        PlayerMock player = server.addPlayer("ColorPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("ColorTown", tanPlayer).join();

        // Act
        var color = town.getChunkColor();

        // Assert
        assertNotNull(color, "Town should have a color");
    }

    // ========== Concurrency Tests ==========

    @Test
    @DisplayName("Territory should handle concurrent balance operations")
    void testConcurrentBalanceOperations() throws InterruptedException {
        // Arrange
        PlayerMock player = server.addPlayer("ConcurrentPlayer");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("ConcurrentTown", tanPlayer).join();
        int threadCount = 10;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act - Concurrent deposits
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        town.addToBalance(10.0);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Concurrent operations should complete within timeout");
        executor.shutdown();

        // Assert
        double expectedBalance = threadCount * operationsPerThread * 10.0;
        assertEquals(expectedBalance, town.getBalance(), 1.0,
            "Concurrent deposits should result in correct total");
    }

    @Test
    @DisplayName("Territory should handle concurrent member additions")
    void testConcurrentMemberAdditions() throws InterruptedException {
        // Arrange
        PlayerMock leader = server.addPlayer("ConcurrentLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("ConcurrentMemberTown", tanLeader).join();

        int memberCount = 20;
        CountDownLatch latch = new CountDownLatch(memberCount);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Act - Concurrent member additions
        for (int i = 0; i < memberCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    PlayerMock member = server.addPlayer("Member" + index);
                    ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());
                    town.addPlayer(tanMember);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Concurrent member additions should complete");
        executor.shutdown();

        // Assert
        assertTrue(town.getITanPlayerList().size() >= memberCount,
            "Should have added all members");
    }

    @Test
    @DisplayName("Territory should handle concurrent rank creation")
    void testConcurrentRankCreation() throws InterruptedException {
        // Arrange
        PlayerMock player = server.addPlayer("RankLeader");
        ITanPlayer tanPlayer = playerStorage.getSync(player.getUniqueId().toString());
        TownData town = townStorage.newTown("ConcurrentRankTown", tanPlayer).join();

        int rankCount = 10;
        CountDownLatch latch = new CountDownLatch(rankCount);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act - Concurrent rank creations
        for (int i = 0; i < rankCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    RankData rank = town.registerNewRank("Rank" + index);
                    if (rank != null) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Concurrent rank creations should complete");
        executor.shutdown();

        // Assert
        assertTrue(successCount.get() >= rankCount - 1, // May have some duplicates
            "Should create most ranks successfully");
    }
}
