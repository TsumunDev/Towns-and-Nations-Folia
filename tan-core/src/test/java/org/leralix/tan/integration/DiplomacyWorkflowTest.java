package org.leralix.tan.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.RelationType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Integration tests for diplomacy workflow.
 *
 * <p>Tests the end-to-end diplomacy operations including:
 * - Alliance creation
 * - War declarations
 * - Vassal relationships
 * - Relation changes</p>
 */
@DisplayName("Diplomacy Workflow Integration Tests")
class DiplomacyWorkflowTest {

    private ServerMock server;
    private TownsAndNations plugin;
    private PlayerMock player1;
    private PlayerMock player2;
    private PlayerMock player3;
    private ITanPlayer leader1;
    private ITanPlayer leader2;
    private ITanPlayer leader3;
    private TownData town1;
    private TownData town2;
    private TownData town3;
    private RegionData region;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        plugin = MockBukkit.load(TownsAndNations.class);

        player1 = server.addPlayer("Leader1");
        player2 = server.addPlayer("Leader2");
        player3 = server.addPlayer("Leader3");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Complete alliance creation workflow")
    void testAllianceCreation() throws Exception {
        // Setup: Create two towns
        leader1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        leader2 = PlayerDataStorage.getInstance().get(player2).get(5, TimeUnit.SECONDS);

        String townId1 = java.util.UUID.randomUUID().toString();
        String townId2 = java.util.UUID.randomUUID().toString();

        town1 = new TownData(townId1, "Town1", leader1);
        town2 = new TownData(townId2, "Town2", leader2);

        TownDataStorage.getInstance().createSync(town1);
        TownDataStorage.getInstance().createSync(town2);

        leader1.setTownID(townId1);
        leader2.setTownID(townId2);
        PlayerDataStorage.getInstance().updateSync(leader1);
        PlayerDataStorage.getInstance().updateSync(leader2);

        // Create alliance
        town1.addRelation(townId2, RelationType.ALLIANCE);

        // Verify alliance from town1 perspective
        assertTrue(town1.hasRelationWith(townId2), "Town1 should have relation with Town2");
        assertEquals(RelationType.ALLIANCE, town1.getRelation(townId2),
            "Relation should be ALLIANCE");

        // Save and reload
        TownDataStorage.getInstance().updateSync(town1);

        TownData reloadedTown1 = TownDataStorage.getInstance().get(townId1).get(5, TimeUnit.SECONDS);
        assertNotNull(reloadedTown1, "Town1 should reload successfully");
        assertTrue(reloadedTown1.hasRelationWith(townId2),
            "Reloaded town should maintain alliance");
    }

    @Test
    @DisplayName("Complete vassal relationship workflow")
    void testVassalRelationship() throws Exception {
        // Setup: Create region and towns
        leader1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        leader2 = PlayerDataStorage.getInstance().get(player2).get(5, TimeUnit.SECONDS);

        String regionId = java.util.UUID.randomUUID().toString();
        String townId1 = java.util.UUID.randomUUID().toString();
        String townId2 = java.util.UUID.randomUUID().toString();

        region = new RegionData(regionId, "Region", leader1);
        town1 = new TownData(townId1, "CapitalTown", leader1);
        town2 = new TownData(townId2, "VassalTown", leader2);

        RegionDataStorage.getInstance().createSync(region);
        TownDataStorage.getInstance().createSync(town1);
        TownDataStorage.getInstance().createSync(town2);

        // Set capital and vassal
        region.setCapitalID(townId1);
        town2.setRegionID(regionId);

        leader1.setRegionID(regionId);
        leader1.setTownID(townId1);
        leader2.setTownID(townId2);

        // Add vassal relationship
        region.addVassal(townId2);
        town2.setOverlordID(regionId);

        // Save all
        RegionDataStorage.getInstance().updateSync(region);
        TownDataStorage.getInstance().updateSync(town1);
        TownDataStorage.getInstance().updateSync(town2);
        PlayerDataStorage.getInstance().updateSync(leader1);
        PlayerDataStorage.getInstance().updateSync(leader2);

        // Verify vassal relationship
        assertTrue(region.getVassalIDs().contains(townId2),
            "Region should have town2 as vassal");
        assertEquals(regionId, town2.getOverlordID(),
            "Town2 should have region as overlord");

        // Reload and verify
        RegionData reloadedRegion = RegionDataStorage.getInstance()
            .get(regionId)
            .get(5, TimeUnit.SECONDS);

        assertNotNull(reloadedRegion, "Region should reload successfully");
        assertTrue(reloadedRegion.getVassalIDs().contains(townId2),
            "Reloaded region should maintain vassal relationship");
    }

    @Test
    @DisplayName("War declaration workflow")
    void testWarDeclaration() throws Exception {
        // Setup: Create two towns
        leader1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        leader2 = PlayerDataStorage.getInstance().get(player2).get(5, TimeUnit.SECONDS);

        String townId1 = java.util.UUID.randomUUID().toString();
        String townId2 = java.util.UUID.randomUUID().toString();

        town1 = new TownData(townId1, "Town1", leader1);
        town2 = new TownData(townId2, "Town2", leader2);

        TownDataStorage.getInstance().createSync(town1);
        TownDataStorage.getInstance().createSync(town2);

        // Declare war
        town1.addRelation(townId2, RelationType.WAR);
        town2.addRelation(townId1, RelationType.WAR);

        // Verify war declaration
        assertEquals(RelationType.WAR, town1.getRelation(townId2),
            "Town1 should be at war with Town2");
        assertEquals(RelationType.WAR, town2.getRelation(townId1),
            "Town2 should be at war with Town1");

        // Save and reload
        TownDataStorage.getInstance().updateSync(town1);
        TownDataStorage.getInstance().updateSync(town2);

        TownData reloadedTown1 = TownDataStorage.getInstance().get(townId1).get(5, TimeUnit.SECONDS);
        TownData reloadedTown2 = TownDataStorage.getInstance().get(townId2).get(5, TimeUnit.SECONDS);

        assertEquals(RelationType.WAR, reloadedTown1.getRelation(townId2),
            "Reloaded Town1 should be at war");
        assertEquals(RelationType.WAR, reloadedTown2.getRelation(townId1),
            "Reloaded Town2 should be at war");
    }

    @Test
    @DisplayName("Relation change workflow")
    void testRelationChange() throws Exception {
        // Setup
        leader1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        leader2 = PlayerDataStorage.getInstance().get(player2).get(5, TimeUnit.SECONDS);

        String townId1 = java.util.UUID.randomUUID().toString();
        String townId2 = java.util.UUID.randomUUID().toString();

        town1 = new TownData(townId1, "Town1", leader1);
        town2 = new TownData(townId2, "Town2", leader2);

        TownDataStorage.getInstance().createSync(town1);
        TownDataStorage.getInstance().createSync(town2);

        // Start with alliance
        town1.addRelation(townId2, RelationType.ALLIANCE);
        assertEquals(RelationType.ALLIANCE, town1.getRelation(townId2));

        // Change to war
        town1.addRelation(townId2, RelationType.WAR);
        assertEquals(RelationType.WAR, town1.getRelation(townId2),
            "Relation should change to WAR");

        // Change to neutral
        town1.removeRelation(townId2);
        assertFalse(town1.hasRelationWith(townId2),
            "Relation should be removed");
    }

    @Test
    @DisplayName("Multi-town diplomacy workflow")
    void testMultiTownDiplomacy() throws Exception {
        // Setup: Create three towns in a region
        leader1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        leader2 = PlayerDataStorage.getInstance().get(player2).get(5, TimeUnit.SECONDS);
        leader3 = PlayerDataStorage.getInstance().get(player3).get(5, TimeUnit.SECONDS);

        String regionId = java.util.UUID.randomUUID().toString();
        String townId1 = java.util.UUID.randomUUID().toString();
        String townId2 = java.util.UUID.randomUUID().toString();
        String townId3 = java.util.UUID.randomUUID().toString();

        region = new RegionData(regionId, "Region", leader1);
        town1 = new TownData(townId1, "Town1", leader1);
        town2 = new TownData(townId2, "Town2", leader2);
        town3 = new TownData(townId3, "Town3", leader3);

        RegionDataStorage.getInstance().createSync(region);
        TownDataStorage.getInstance().createSync(town1);
        TownDataStorage.getInstance().createSync(town2);
        TownDataStorage.getInstance().createSync(town3);

        // Create complex diplomacy web
        region.setCapitalID(townId1);
        region.addVassal(townId2);
        region.addVassal(townId3);

        town1.addRelation(townId2, RelationType.ALLIANCE);
        town2.addRelation(townId3, RelationType.WAR);

        // Save all
        RegionDataStorage.getInstance().updateSync(region);
        TownDataStorage.getInstance().updateSync(town1);
        TownDataStorage.getInstance().updateSync(town2);
        TownDataStorage.getInstance().updateSync(town3);

        // Verify all relationships
        assertEquals(2, region.getVassalIDs().size(),
            "Region should have 2 vassals");
        assertTrue(region.getVassalIDs().contains(townId2));
        assertTrue(region.getVassalIDs().contains(townId3));
        assertEquals(RelationType.ALLIANCE, town1.getRelation(townId2));
        assertEquals(RelationType.WAR, town2.getRelation(townId3));
    }
}
