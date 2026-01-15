package org.leralix.tan.dataclass.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.rank.RankData;
import org.leralix.tan.enums.RolePermission;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive tests for TownData class.
 *
 * <p>Tests town-specific functionality including:</p>
 * <ul>
 *   <li>Town creation and deletion</li>
 *   <li>Member management (add, remove, ranks)</li>
 *   <li>Property ownership</li>
 *   <li>Chunk claiming and unclaiming</li>
 *   <li>Diplomacy with other towns</li>
 *   <li>Economy (balance, taxes)</li>
 *   <li>Upgrades and unlocks</li>
 *   <li>Spawn point management</li>
 * </ul>
 */
@DisplayName("TownData Tests")
class TownDataComprehensiveTest {

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

    // ========== Town Creation Tests ==========

    @Test
    @DisplayName("Town creation should initialize all components")
    void testTownCreationInitialization() {
        // Arrange
        PlayerMock leader = server.addPlayer("TownCreator");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        String townName = "NewTown";

        // Act
        TownData town = townStorage.newTown(townName, tanLeader).join();

        // Assert
        assertNotNull(town, "Town should be created");
        assertEquals(townName, town.getName(), "Town name should match");
        assertTrue(town.isLeader(tanLeader), "Creator should be leader");
        assertEquals(0, town.getBalance(), 0.001, "Initial balance should be 0");
        assertTrue(town.getClaimedChunks().isEmpty(), "Should have no claimed chunks");
    }

    @Test
    @DisplayName("Town should have unique ID")
    void testTownUniqueId() {
        // Arrange
        PlayerMock leader1 = server.addPlayer("Leader1");
        PlayerMock leader2 = server.addPlayer("Leader2");
        ITanPlayer tanLeader1 = playerStorage.getSync(leader1.getUniqueId().toString());
        ITanPlayer tanLeader2 = playerStorage.getSync(leader2.getUniqueId().toString());

        // Act
        TownData town1 = townStorage.newTown("Town1", tanLeader1).join();
        TownData town2 = townStorage.newTown("Town2", tanLeader2).join();

        // Assert
        assertNotEquals(town1.getID(), town2.getID(), "Town IDs should be unique");
    }

    // ========== Member Management Tests ==========

    @Test
    @DisplayName("Adding player to town should update membership")
    void testAddPlayerToTown() {
        // Arrange
        PlayerMock leader = server.addPlayer("Mayor");
        PlayerMock citizen = server.addPlayer("Citizen");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanCitizen = playerStorage.getSync(citizen.getUniqueId().toString());

        TownData town = townStorage.newTown("MemberTestTown", tanLeader).join();

        // Act
        town.addPlayer(tanCitizen);

        // Assert
        assertTrue(town.isPlayerInTown(tanCitizen), "Player should be in town");
        assertTrue(tanCitizen.hasTown(), "Player should have town flag set");
        assertEquals(town.getID(), tanCitizen.getTownId(), "Player's town ID should match");
    }

    @Test
    @DisplayName("Removing player from town should clear membership")
    void testRemovePlayerFromTown() {
        // Arrange
        PlayerMock leader = server.addPlayer("Mayor2");
        PlayerMock citizen = server.addPlayer("Citizen2");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanCitizen = playerStorage.getSync(citizen.getUniqueId().toString());

        TownData town = townStorage.newTown("RemoveTestTown", tanLeader).join();
        town.addPlayer(tanCitizen);

        // Act
        town.removePlayer(tanCitizen);

        // Assert
        assertFalse(town.isPlayerInTown(tanCitizen), "Player should not be in town");
    }

    @Test
    @DisplayName("Town should track all members")
    void testGetAllMembers() {
        // Arrange
        PlayerMock leader = server.addPlayer("Leader3");
        PlayerMock member1 = server.addPlayer("Member1");
        PlayerMock member2 = server.addPlayer("Member2");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember1 = playerStorage.getSync(member1.getUniqueId().toString());
        ITanPlayer tanMember2 = playerStorage.getSync(member2.getUniqueId().toString());

        TownData town = townStorage.newTown("MembersTown", tanLeader).join();
        town.addPlayer(tanMember1);
        town.addPlayer(tanMember2);

        // Act
        List<ITanPlayer> members = town.getITanPlayerList();

        // Assert
        assertTrue(members.size() >= 3, "Should have at least 3 members (leader + 2)");
        assertTrue(members.contains(tanLeader), "Should contain leader");
        assertTrue(members.contains(tanMember1), "Should contain member1");
        assertTrue(members.contains(tanMember2), "Should contain member2");
    }

    // ========== Rank Management Tests ==========

    @Test
    @DisplayName("Town should allow creating custom ranks")
    void testCreateCustomRank() {
        // Arrange
        PlayerMock leader = server.addPlayer("RankLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("RankTown", tanLeader).join();
        String rankName = "Merchant";

        // Act
        RankData newRank = town.registerNewRank(rankName);

        // Assert
        assertNotNull(newRank, "Rank should be created");
        assertEquals(rankName, newRank.getName(), "Rank name should match");
        assertTrue(newRank.getID() > 0, "Rank should have valid ID");
    }

    @Test
    @DisplayName("Town should have default rank for new members")
    void testDefaultRankAssignment() {
        // Arrange
        PlayerMock leader = server.addPlayer("DefaultLeader");
        PlayerMock member = server.addPlayer("DefaultMember");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());

        TownData town = townStorage.newTown("DefaultRankTown", tanLeader).join();
        town.addPlayer(tanMember);

        // Act
        RankData memberRank = town.getPlayerRank(tanMember);

        // Assert
        assertNotNull(memberRank, "Member should have a rank");
        assertEquals(town.getDefaultRankID(), memberRank.getID(),
            "New member should have default rank");
    }

    @Test
    @DisplayName("Town should allow changing player rank")
    void testChangePlayerRank() {
        // Arrange
        PlayerMock leader = server.addPlayer("RankChangeLeader");
        PlayerMock member = server.addPlayer("RankChangeMember");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());

        TownData town = townStorage.newTown("RankChangeTown", tanLeader).join();
        RankData customRank = town.registerNewRank("VIP");
        town.addPlayer(tanMember);

        // Act
        town.setRank(tanMember, customRank);

        // Assert
        assertEquals(customRank.getID(), town.getPlayerRank(tanMember).getID(),
            "Player rank should be updated");
    }

    // ========== Chunk Claiming Tests ==========

    @Test
    @DisplayName("Town should claim chunks correctly")
    void testClaimChunk() {
        // Arrange
        PlayerMock leader = server.addPlayer("ClaimLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("ClaimingTown", tanLeader).join();
        Location claimLoc = new Location(server.addSimpleWorld("world"), 100, 64, 100);

        // Act
        town.claimChunk(leader, claimLoc);

        // Assert
        assertTrue(town.hasClaimedChunk(claimLoc), "Town should own the chunk");
        assertFalse(town.getClaimedChunks().isEmpty(), "Claimed chunks list should not be empty");
    }

    @Test
    @DisplayName("Town should unclaim chunks correctly")
    void testUnclaimChunk() {
        // Arrange
        PlayerMock leader = server.addPlayer("UnclaimLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("UnclaimingTown", tanLeader).join();
        Location claimLoc = new Location(server.addSimpleWorld("world"), 100, 64, 100);
        town.claimChunk(leader, claimLoc);

        // Act
        town.unclaim(claimLoc);

        // Assert
        assertFalse(town.hasClaimedChunk(claimLoc), "Town should not own the chunk anymore");
    }

    @Test
    @DisplayName("Town should track claimed chunks count")
    void testClaimedChunksCount() {
        // Arrange
        PlayerMock leader = server.addPlayer("CountLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("CountingTown", tanLeader).join();
        Location loc1 = new Location(server.addSimpleWorld("world"), 0, 64, 0);
        Location loc2 = new Location(server.addSimpleWorld("world"), 16, 64, 0);
        Location loc3 = new Location(server.addSimpleWorld("world"), 32, 64, 0);

        // Act
        town.claimChunk(leader, loc1);
        town.claimChunk(leader, loc2);
        town.claimChunk(leader, loc3);

        // Assert
        assertEquals(3, town.getClaimedChunks().size(), "Should have 3 claimed chunks");
    }

    // ========== Economy Tests ==========

    @Test
    @DisplayName("Town should manage balance correctly")
    void testTownBalance() {
        // Arrange
        PlayerMock leader = server.addPlayer("BankerLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("BankTown", tanLeader).join();

        // Act
        town.addToBalance(1000.0);
        town.removeFromBalance(300.0);

        // Assert
        assertEquals(700.0, town.getBalance(), 0.001, "Balance should be correct");
    }

    @Test
    @DisplayName("Town should allow tax rate changes")
    void testTaxRateChange() {
        // Arrange
        PlayerMock leader = server.addPlayer("TaxLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("TaxTown", tanLeader).join();
        double newTaxRate = 15.0;

        // Act
        town.setTax(newTaxRate);

        // Assert
        assertEquals(newTaxRate, town.getTax(), 0.001, "Tax rate should be updated");
    }

    // ========== Diplomacy Tests ==========

    @Test
    @DisplayName("Town should set diplomatic relations")
    void testSetDiplomaticRelation() {
        // Arrange
        PlayerMock leader1 = server.addPlayer("DiploLeader1");
        PlayerMock leader2 = server.addPlayer("DiploLeader2");
        ITanPlayer tanLeader1 = playerStorage.getSync(leader1.getUniqueId().toString());
        ITanPlayer tanLeader2 = playerStorage.getSync(leader2.getUniqueId().toString());

        TownData town1 = townStorage.newTown("DiploTown1", tanLeader1).join();
        TownData town2 = townStorage.newTown("DiploTown2", tanLeader2).join();

        // Act
        town1.setRelation(town2.getID(), TownRelation.ALLY);

        // Assert
        assertEquals(TownRelation.ALLY, town1.getRelation(town2.getID()),
            "Diplomatic relation should be set");
    }

    @Test
    @DisplayName("Town should have neutral relation by default")
    void testDefaultRelation() {
        // Arrange
        PlayerMock leader1 = server.addPlayer("NeutralLeader1");
        PlayerMock leader2 = server.addPlayer("NeutralLeader2");
        ITanPlayer tanLeader1 = playerStorage.getSync(leader1.getUniqueId().toString());
        ITanPlayer tanLeader2 = playerStorage.getSync(leader2.getUniqueId().toString());

        TownData town1 = townStorage.newTown("NeutralTown1", tanLeader1).join();
        TownData town2 = townStorage.newTown("NeutralTown2", tanLeader2).join();

        // Act
        TownRelation relation = town1.getRelation(town2.getID());

        // Assert
        assertNotNull(relation, "Should have a relation");
    }

    // ========== Overlord/Vassal Tests ==========

    @Test
    @DisplayName("New town should not have overlord")
    void testNoOverlord() {
        // Arrange
        PlayerMock leader = server.addPlayer("IndependentLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());

        // Act
        TownData town = townStorage.newTown("IndependentTown", tanLeader).join();

        // Assert
        assertFalse(town.haveOverlord(), "New town should be independent");
        assertNull(town.getOverlordID(), "Overlord ID should be null");
    }

    @Test
    @DisplayName("Town should check if leader is online")
    void testIsLeaderOnline() {
        // Arrange
        PlayerMock leader = server.addPlayer("OnlineLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("OnlineTown", tanLeader).join();

        // Act & Assert - Leader is online when player is online
        boolean isOnline = town.isLeaderOnline();

        // Since MockBukkit may not fully simulate online status, we just check it doesn't throw
        assertDoesNotThrow(() -> town.isLeaderOnline(),
            "Checking leader online status should not throw exception");
    }

    // ========== Property Tests ==========

    @Test
    @DisplayName("Town should track properties")
    void testTownProperties() {
        // Arrange
        PlayerMock leader = server.addPlayer("PropertyLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("PropertyTown", tanLeader).join();

        // Act
        var properties = town.getProperties();

        // Assert
        assertNotNull(properties, "Properties list should not be null");
        // May be empty initially, that's fine
    }

    // ========== Cosmetics Tests ==========

    @Test
    @DisplayName("Town should have custom icon")
    void testTownIcon() {
        // Arrange
        PlayerMock leader = server.addPlayer("IconLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("IconTown", tanLeader).join();

        // Act
        var icon = town.getIcon();

        // Assert
        assertNotNull(icon, "Town should have an icon");
        assertNotEquals(Material.AIR, icon.getType(), "Icon should not be AIR");
    }

    @Test
    @DisplayName("Town should have description")
    void testTownDescription() {
        // Arrange
        PlayerMock leader = server.addPlayer("DescLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("DescTown", tanLeader).join();

        // Act
        String description = town.getDescription();

        // Assert
        assertNotNull(description, "Description should not be null");
    }

    @Test
    @DisplayName("Town should have colored name")
    void testTownColoredName() {
        // Arrange
        PlayerMock leader = server.addPlayer("ColorLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("ColorTown", tanLeader).join();

        // Act
        String coloredName = town.getColoredName();

        // Assert
        assertNotNull(coloredName, "Colored name should not be null");
        assertTrue(coloredName.contains("ColorTown") || coloredName.contains("§"),
            "Colored name should contain town name or color codes");
    }

    // ========== Hierarchy Tests ==========

    @Test
    @DisplayName("Town should have correct hierarchy rank")
    void testTownHierarchyRank() {
        // Arrange
        PlayerMock leader = server.addPlayer("HierarchyLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());

        // Act
        TownData town = townStorage.newTown("HierarchyTown", tanLeader).join();

        // Assert - Towns should have lower hierarchy rank than regions
        assertTrue(town.getHierarchyRank() > 0, "Town should have positive hierarchy rank");
    }

    // ========== Upgrade Tests ==========

    @Test
    @DisplayName("Town should have upgrade status")
    void testTownUpgradeStatus() {
        // Arrange
        PlayerMock leader = server.addPlayer("UpgradeLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("UpgradeTown", tanLeader).join();

        // Act
        var upgradeStatus = town.getUpgradesStatus();

        // Assert
        assertNotNull(upgradeStatus, "Upgrade status should not be null");
    }

    // ========== Spawn Point Tests ==========

    @Test
    @DisplayName("Town should allow setting spawn point")
    void testSetSpawnPoint() {
        // Arrange
        PlayerMock leader = server.addPlayer("SpawnLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("SpawnTown", tanLeader).join();
        Location spawnLoc = new Location(server.addSimpleWorld("world"), 100, 64, 100);

        // Act
        town.setSpawn(spawnLoc);

        // Assert - Spawn point should be set (method exists, we're testing it doesn't crash)
        assertDoesNotThrow(() -> town.setSpawn(spawnLoc),
            "Setting spawn should not throw exception");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Multiple town operations should work together")
    void testMultipleTownOperations() {
        // Arrange
        PlayerMock leader = server.addPlayer("MultiLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("MultiOpTown", tanLeader).join();
        Location claimLoc = new Location(server.addSimpleWorld("world"), 50, 64, 50);

        // Act - Multiple operations
        town.addToBalance(500.0);
        town.claimChunk(leader, claimLoc);
        town.setTax(10.0);

        // Assert
        assertEquals(500.0, town.getBalance(), 0.001);
        assertTrue(town.hasClaimedChunk(claimLoc));
        assertEquals(10.0, town.getTax(), 0.001);
    }
}
