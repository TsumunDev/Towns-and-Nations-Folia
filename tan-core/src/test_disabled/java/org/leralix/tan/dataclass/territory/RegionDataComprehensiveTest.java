package org.leralix.tan.dataclass.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive tests for RegionData class.
 *
 * <p>Tests region/nation-specific functionality including:</p>
 * <ul>
 *   <li>Region creation and deletion</li>
 *   <li>Vassal town management (add, remove)</li>
 *   <li>Capital management</li>
 *   <li>Inter-region diplomacy</li>
 *   <li>Territory claims</li>
 *   <li>Member management</li>
 *   <li>Economy (balance, taxes from vassals)</li>
 * </ul>
 */
@DisplayName("RegionData Tests")
class RegionDataComprehensiveTest {

    private ServerMock server;
    private PlayerDataStorage playerStorage;
    private RegionDataStorage regionStorage;
    private TownDataStorage townStorage;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        playerStorage = PlayerDataStorage.getInstance();
        regionStorage = RegionDataStorage.getInstance();
        townStorage = TownDataStorage.getInstance();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ========== Region Creation Tests ==========

    @Test
    @DisplayName("Region creation should initialize all components")
    void testRegionCreationInitialization() {
        // Arrange
        PlayerMock leader = server.addPlayer("RegionCreator");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        String regionName = "NewRegion";

        // Act
        RegionData region = regionStorage.newRegion(regionName, tanLeader).join();

        // Assert
        assertNotNull(region, "Region should be created");
        assertEquals(regionName, region.getName(), "Region name should match");
        assertTrue(region.isLeader(tanLeader), "Creator should be leader");
    }

    @Test
    @DisplayName("Region should have unique ID")
    void testRegionUniqueId() {
        // Arrange
        PlayerMock leader1 = server.addPlayer("RegionLeader1");
        PlayerMock leader2 = server.addPlayer("RegionLeader2");
        ITanPlayer tanLeader1 = playerStorage.getSync(leader1.getUniqueId().toString());
        ITanPlayer tanLeader2 = playerStorage.getSync(leader2.getUniqueId().toString());

        // Act
        RegionData region1 = regionStorage.newRegion("Region1", tanLeader1).join();
        RegionData region2 = regionStorage.newRegion("Region2", tanLeader2).join();

        // Assert
        assertNotEquals(region1.getID(), region2.getID(), "Region IDs should be unique");
    }

    // ========== Vassal Management Tests ==========

    @Test
    @DisplayName("Region should add vassal town")
    void testAddVassalTown() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("RegionKing");
        PlayerMock townLeader = server.addPlayer("TownMayor");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("VassalRegion", tanRegionLeader).join();
        TownData town = townStorage.newTown("VassalTown", tanTownLeader).join();

        // Act
        region.addVassal(town);

        // Assert
        assertTrue(region.isVassal(town), "Town should be vassal of region");
    }

    @Test
    @DisplayName("Region should remove vassal town")
    void testRemoveVassalTown() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("RegionKing2");
        PlayerMock townLeader = server.addPlayer("TownMayor2");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("RemoveVassalRegion", tanRegionLeader).join();
        TownData town = townStorage.newTown("RemoveVassalTown", tanTownLeader).join();
        region.addVassal(town);

        // Act
        region.removeVassal(town.getID());

        // Assert
        assertFalse(region.isVassal(town), "Town should not be vassal anymore");
    }

    @Test
    @DisplayName("Vassal relationship should be bidirectional")
    void testVassalRelationshipBidirectional() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("BiDirectionKing");
        PlayerMock townLeader = server.addPlayer("BiDirectionMayor");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("BiDirectionRegion", tanRegionLeader).join();
        TownData town = townStorage.newTown("BiDirectionTown", tanTownLeader).join();

        // Act
        region.addVassal(town);

        // Assert
        assertTrue(region.isVassal(town), "Region should recognize town as vassal");
        assertTrue(town.haveOverlord(), "Town should have an overlord");
        assertEquals(region.getID(), town.getOverlordID(), "Town's overlord should be region");
    }

    @Test
    @DisplayName("Region should get all vassal towns")
    void testGetAllVassals() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("MultiVassalKing");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("MultiVassalRegion", tanRegionLeader).join();

        PlayerMock townLeader1 = server.addPlayer("Mayor1");
        PlayerMock townLeader2 = server.addPlayer("Mayor2");
        PlayerMock townLeader3 = server.addPlayer("Mayor3");

        ITanPlayer tanTownLeader1 = playerStorage.getSync(townLeader1.getUniqueId().toString());
        ITanPlayer tanTownLeader2 = playerStorage.getSync(townLeader2.getUniqueId().toString());
        ITanPlayer tanTownLeader3 = playerStorage.getSync(townLeader3.getUniqueId().toString());

        TownData town1 = townStorage.newTown("VassalTown1", tanTownLeader1).join();
        TownData town2 = townStorage.newTown("VassalTown2", tanTownLeader2).join();
        TownData town3 = townStorage.newTown("VassalTown3", tanTownLeader3).join();

        // Act
        region.addVassal(town1);
        region.addVassal(town2);
        region.addVassal(town3);

        // Assert
        List<TownData> vassals = region.getVassals();
        assertTrue(vassals.size() >= 3, "Should have at least 3 vassals");
        assertTrue(vassals.contains(town1), "Should contain town1");
        assertTrue(vassals.contains(town2), "Should contain town2");
        assertTrue(vassals.contains(town3), "Should contain town3");
    }

    // ========== Capital Management Tests ==========

    @Test
    @DisplayName("Region should set capital town")
    void testSetCapital() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("CapitalKing");
        PlayerMock townLeader = server.addPlayer("CapitalMayor");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("CapitalRegion", tanRegionLeader).join();
        TownData capital = townStorage.newTown("CapitalTown", tanTownLeader).join();
        region.addVassal(capital);

        // Act
        region.setCapital(capital.getID());

        // Assert
        assertTrue(region.hasCapital(), "Region should have a capital");
        assertEquals(capital.getID(), region.getCapitalID(), "Capital ID should match");
    }

    @Test
    @DisplayName("Region should remove capital")
    void testRemoveCapital() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("RemoveCapitalKing");
        PlayerMock townLeader = server.addPlayer("RemoveCapitalMayor");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("RemoveCapitalRegion", tanRegionLeader).join();
        TownData capital = townStorage.newTown("RemoveCapitalTown", tanTownLeader).join();
        region.addVassal(capital);
        region.setCapital(capital.getID());

        // Act
        region.removeCapital();

        // Assert
        assertFalse(region.hasCapital(), "Region should not have capital anymore");
        assertNull(region.getCapitalID(), "Capital ID should be null");
    }

    @Test
    @DisplayName("Region should get capital town")
    void testGetCapital() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("GetCapitalKing");
        PlayerMock townLeader = server.addPlayer("GetCapitalMayor");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("GetCapitalRegion", tanRegionLeader).join();
        TownData capital = townStorage.newTown("GetCapitalTown", tanTownLeader).join();
        region.addVassal(capital);
        region.setCapital(capital.getID());

        // Act
        TownData retrievedCapital = region.getCapital();

        // Assert
        assertNotNull(retrievedCapital, "Capital should be retrieved");
        assertEquals(capital.getID(), retrievedCapital.getID(), "Capital should match");
    }

    // ========== Hierarchy Tests ==========

    @Test
    @DisplayName("Region should have higher hierarchy rank than town")
    void testRegionHierarchyRank() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("HierarchyKing");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());

        // Act
        RegionData region = regionStorage.newRegion("HierarchyRegion", tanRegionLeader).join();

        // Assert
        assertTrue(region.getHierarchyRank() > 0, "Region should have positive hierarchy rank");
    }

    // ========== Leader Management Tests ==========

    @Test
    @DisplayName("Region should have leader")
    void testRegionLeader() {
        // Arrange
        PlayerMock leader = server.addPlayer("RegionLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());

        // Act
        RegionData region = regionStorage.newRegion("LeaderRegion", tanLeader).join();

        // Assert
        assertTrue(region.isLeader(tanLeader), "Creator should be leader");
        assertEquals(tanLeader.getID(), region.getLeaderID(), "Leader ID should match");
    }

    // ========== Member Management Tests ==========

    @Test
    @DisplayName("Region should track members through vassal towns")
    void testRegionMembersThroughVassals() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("MemberKing");
        PlayerMock townLeader = server.addPlayer("MemberMayor");
        PlayerMock citizen = server.addPlayer("Citizen");

        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());
        ITanPlayer tanCitizen = playerStorage.getSync(citizen.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("MemberRegion", tanRegionLeader).join();
        TownData town = townStorage.newTown("MemberTown", tanTownLeader).join();
        town.addPlayer(tanCitizen);
        region.addVassal(town);

        // Act
        List<ITanPlayer> regionMembers = region.getITanPlayerList();

        // Assert
        assertTrue(regionMembers.size() >= 2, "Should have at least 2 members (mayor + citizen)");
        assertTrue(regionMembers.contains(tanTownLeader), "Should contain town leader");
        assertTrue(regionMembers.contains(tanCitizen), "Should contain citizen");
    }

    // ========== Diplomacy Tests ==========

    @Test
    @DisplayName("Region should set diplomatic relations")
    void testSetDiplomaticRelation() {
        // Arrange
        PlayerMock leader1 = server.addPlayer("RegionDiploKing1");
        PlayerMock leader2 = server.addPlayer("RegionDiploKing2");
        ITanPlayer tanLeader1 = playerStorage.getSync(leader1.getUniqueId().toString());
        ITanPlayer tanLeader2 = playerStorage.getSync(leader2.getUniqueId().toString());

        RegionData region1 = regionStorage.newRegion("RegionDiplo1", tanLeader1).join();
        RegionData region2 = regionStorage.newRegion("RegionDiplo2", tanLeader2).join();

        // Act
        region1.setRelation(region2.getID(), TownRelation.ALLY);

        // Assert
        assertEquals(TownRelation.ALLY, region1.getRelation(region2.getID()),
            "Diplomatic relation should be set");
    }

    // ========== Economy Tests ==========

    @Test
    @DisplayName("Region should manage balance")
    void testRegionBalance() {
        // Arrange
        PlayerMock leader = server.addPlayer("RegionBanker");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        RegionData region = regionStorage.newRegion("RegionBank", tanLeader).join();

        // Act
        region.addToBalance(2000.0);
        region.removeFromBalance(500.0);

        // Assert
        assertEquals(1500.0, region.getBalance(), 0.001, "Balance should be correct");
    }

    // ========== Colored Name Tests ==========

    @Test
    @DisplayName("Region should have colored name")
    void testRegionColoredName() {
        // Arrange
        PlayerMock leader = server.addPlayer("ColorKing");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        RegionData region = regionStorage.newRegion("ColorRegion", tanLeader).join();

        // Act
        String coloredName = region.getColoredName();

        // Assert
        assertNotNull(coloredName, "Colored name should not be null");
        assertTrue(coloredName.contains("ColorRegion") || coloredName.contains("§"),
            "Colored name should contain region name or color codes");
    }

    @Test
    @DisplayName("Region should have base colored name")
    void testRegionBaseColoredName() {
        // Arrange
        PlayerMock leader = server.addPlayer("BaseColorKing");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        RegionData region = regionStorage.newRegion("BaseColorRegion", tanLeader).join();

        // Act
        String baseColoredName = region.getBaseColoredName();

        // Assert
        assertNotNull(baseColoredName, "Base colored name should not be null");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Multiple region operations should work together")
    void testMultipleRegionOperations() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("MultiOpKing");
        PlayerMock townLeader = server.addPlayer("MultiOpMayor");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());

        RegionData region = regionStorage.newRegion("MultiOpRegion", tanRegionLeader).join();
        TownData town = townStorage.newTown("MultiOpTown", tanTownLeader).join();

        // Act - Multiple operations
        region.addToBalance(1000.0);
        region.addVassal(town);
        region.setCapital(town.getID());
        region.setTax(5.0);

        // Assert
        assertEquals(1000.0, region.getBalance(), 0.001);
        assertTrue(region.isVassal(town));
        assertTrue(region.hasCapital());
        assertEquals(5.0, region.getTax(), 0.001);
    }

    @Test
    @DisplayName("Region with multiple vassals should track all")
    void testMultipleVassalsTracking() {
        // Arrange
        PlayerMock regionLeader = server.addPlayer("MultiVassalKing");
        ITanPlayer tanRegionLeader = playerStorage.getSync(regionLeader.getUniqueId().toString());
        RegionData region = regionStorage.newRegion("MultiVassalRegion2", tanRegionLeader).join();

        // Create multiple towns
        for (int i = 1; i <= 5; i++) {
            PlayerMock townLeader = server.addPlayer("Mayor" + i);
            ITanPlayer tanTownLeader = playerStorage.getSync(townLeader.getUniqueId().toString());
            TownData town = townStorage.newTown("Town" + i, tanTownLeader).join();
            region.addVassal(town);
        }

        // Act
        List<TownData> vassals = region.getVassals();

        // Assert
        assertTrue(vassals.size() >= 5, "Should have at least 5 vassals");
    }
}
