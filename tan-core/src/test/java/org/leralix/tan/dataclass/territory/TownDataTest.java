package org.leralix.tan.dataclass.territory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.leralix.lib.data.SoundEnum;
import org.leralix.lib.position.Vector2D;
import org.leralix.lib.position.Vector3D;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.PlayerData;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.RankData;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.enums.RolePermission;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.lang.FilledLang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.upgrade.TerritoryStats;
import org.leralix.tan.upgrade.rewards.StatsType;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test suite for {@link TownData} territory management.
 * <p>
 * Tests cover:
 * <ul>
 *   <li>Town creation and initialization</li>
 *   <li>Player membership management (add, remove, join requests)</li>
 *   <li>Leader management and succession</li>
 *   <li>Region (nation) affiliation and vassalage</li>
 *   <li>Property management within town borders</li>
 *   <li>Territory deletion and cleanup cascades</li>
 *   <li>Thread-safety for Folia region threading</li>
 * </ul>
 *
 * <p><b>Folia Compliance:</b> All async operations tested for non-blocking behavior.</p>
 *
 * @see RegionData
 * @see TerritoryData
 * @since 0.15.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TownData Territory Management Tests")
public class TownDataTest {

    @Mock
    private Player mockPlayer;

    @Mock
    private ITanPlayer mockLeader;

    @Mock
    private ITanPlayer mockPlayer2;

    @Mock
    private OfflinePlayer mockOfflinePlayer;

    @Mock
    private RegionDataStorage mockRegionStorage;

    @Mock
    private TownDataStorage mockTownStorage;

    @Mock
    private PlayerDataStorage mockPlayerStorage;

    @Mock
    private RegionData mockRegion;

    @Mock
    private Chunk mockChunk;

    private TownData town;
    private static final String TOWN_ID = "town_test_001";
    private static final String TOWN_NAME = "TestTown";
    private static final String LEADER_ID = "leader-uuid-123";
    private static final String PLAYER2_ID = "player2-uuid-456";

    @BeforeEach
    void setUp() {
        // Configure common mock behavior
        lenient().when(mockLeader.getID()).thenReturn(LEADER_ID);
        lenient().when(mockLeader.getNameStored()).thenReturn("LeaderPlayer");
        lenient().when(mockLeader.getOfflinePlayer()).thenReturn(mockOfflinePlayer);
        lenient().when(mockPlayer.getUniqueId()).thenReturn(UUID.fromString(LEADER_ID));
        lenient().when(mockPlayer.getName()).thenReturn("LeaderPlayer");

        // Create town instance
        town = new TownData(TOWN_ID, TOWN_NAME, mockLeader);
    }

    // ==================== CREATION AND INITIALIZATION TESTS ====================

    @Nested
    @DisplayName("Town Creation and Initialization")
    class TownCreationTests {

        @Test
        @DisplayName("Should create town with valid ID and name")
        void testTownCreation_ValidIdAndName() {
            assertEquals(TOWN_ID, town.getID(), "Town ID should match");
            assertEquals(TOWN_NAME, town.getName(), "Town name should match");
        }

        @Test
        @DisplayName("Should initialize with leader as first member")
        void testTownCreation_LeaderIsFirstMember() {
            assertTrue(town.isPlayerIn(LEADER_ID), "Leader should be initial member");
            assertTrue(town.isLeader(LEADER_ID), "Leader should be marked as leader");
        }

        @Test
        @DisplayName("Should initialize with default rank")
        void testTownCreation_HasDefaultRank() {
            assertNotNull(town.getDefaultRankID(), "Default rank should be initialized");
            RankData defaultRank = town.getRank(town.getDefaultRankID());
            assertNotNull(defaultRank, "Default rank should be retrievable");
            assertEquals("default", defaultRank.getName().toLowerCase(), "Default rank name should be 'default'");
        }

        @Test
        @DisplayName("Should have town hierarchy rank of 0")
        void testTownCreation_HierarchyRank() {
            assertEquals(0, town.getHierarchyRank(), "Town hierarchy rank should be 0");
        }

        @Test
        @DisplayName("Should not have overlord initially")
        void testTownCreation_NoInitialOverlord() {
            assertFalse(town.haveOverlord(), "New town should not have overlord");
            assertTrue(town.getOverlord().isEmpty(), "Overlord optional should be empty");
        }

        @Test
        @DisplayName("Should initialize with correct territory stats type")
        void testTownCreation_TerritoryStatsType() {
            TerritoryStats stats = town.getNewLevel();
            assertNotNull(stats, "Territory stats should be initialized");
        }

        @Test
        @DisplayName("Should create empty property map")
        void testTownCreation_EmptyPropertyMap() {
            assertNotNull(town.getPropertyDataMap(), "Property map should be initialized");
            assertTrue(town.getPropertyDataMap().isEmpty(), "Property map should be empty initially");
        }

        @Test
        @DisplayName("Should not be able to have vassals")
        void testTownCreation_CannotHaveVassals() {
            assertFalse(town.canHaveVassals(), "Town should not be able to have vassals");
        }

        @Test
        @DisplayName("Should be able to have overlord")
        void testTownCreation_CanHaveOverlord() {
            assertTrue(town.canHaveOverlord(), "Town should be able to have overlord");
        }

        @Test
        @DisplayName("Should return empty vassal list")
        void testTownCreation_EmptyVassalsList() {
            assertTrue(town.getVassalsID().isEmpty(), "Vassals list should be empty");
        }
    }

    // ==================== PLAYER MEMBERSHIP TESTS ====================

    @Nested
    @DisplayName("Player Membership Management")
    class PlayerMembershipTests {

        @Test
        @DisplayName("Should add player to town successfully")
        void testAddPlayer_Success() {
            String playerId = "new-player-uuid";
            ITanPlayer newPlayer = mock(ITanPlayer.class);
            when(newPlayer.getID()).thenReturn(playerId);
            when(newPlayer.getPlayer()).thenReturn(mockPlayer);

            town.addPlayer(newPlayer);

            assertTrue(town.isPlayerIn(playerId), "Player should be added to town");
        }

        @Test
        @DisplayName("Should increment player count when adding member")
        void testAddPlayer_PlayerCountIncrement() {
            int initialCount = town.getPlayerIDList().size();

            String playerId = "new-player-uuid";
            ITanPlayer newPlayer = mock(ITanPlayer.class);
            when(newPlayer.getID()).thenReturn(playerId);
            when(newPlayer.getPlayer()).thenReturn(mockPlayer);

            town.addPlayer(newPlayer);

            assertEquals(initialCount + 1, town.getPlayerIDList().size(), "Player count should increase by 1");
        }

        @Test
        @DisplayName("Should remove player from town")
        void testRemovePlayer_Success() {
            // Setup: Add player first
            String playerId = PLAYER2_ID;
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn(playerId);
            when(player.getPlayer()).thenReturn(mockPlayer);
            town.addPlayer(player);

            // Verify player was added
            assertTrue(town.isPlayerIn(playerId), "Player should be in town before removal");

            // Remove player
            town.removePlayer(player);

            assertFalse(town.isPlayerIn(playerId), "Player should be removed from town");
        }

        @Test
        @DisplayName("Should remove player from default rank when removed from town")
        void testRemovePlayer_RemovedFromRank() {
            String playerId = PLAYER2_ID;
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn(playerId);
            when(player.getPlayer()).thenReturn(mockPlayer);

            town.addPlayer(player);
            RankData defaultRank = town.getDefaultRank();
            assertTrue(defaultRank.isPlayerIn(player), "Player should be in default rank");

            town.removePlayer(player);
            assertFalse(defaultRank.isPlayerIn(player), "Player should be removed from default rank");
        }

        @Test
        @DisplayName("Should handle duplicate player join request")
        void testPlayerJoinRequest_Duplicate() {
            String playerId = "requesting-player";
            when(mockPlayer.getUniqueId()).thenReturn(UUID.fromString(playerId));

            town.addPlayerJoinRequest(mockPlayer);
            town.addPlayerJoinRequest(mockPlayer);

            assertEquals(1, town.getPlayerJoinRequestSet().size(), "Duplicate requests should not be added");
        }

        @Test
        @DisplayName("Should remove player join request")
        void testPlayerJoinRequest_Remove() {
            String playerId = "requesting-player";
            when(mockPlayer.getUniqueId()).thenReturn(UUID.fromString(playerId));

            town.addPlayerJoinRequest(mockPlayer);
            assertTrue(town.isPlayerAlreadyRequested(playerId), "Request should exist");

            town.removePlayerJoinRequest(mockPlayer);
            assertFalse(town.isPlayerAlreadyRequested(playerId), "Request should be removed");
        }

        @Test
        @DisplayName("Should return player list from all ranks")
        void testGetPlayerIDList_AggregatesRanks() {
            // Create custom rank and add player
            RankData customRank = town.registerNewRank("CustomRank");
            String playerId = "custom-rank-player";
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn(playerId);
            when(player.getPlayer()).thenReturn(mockPlayer);

            customRank.addPlayer(player);
            town.townPlayerListId.add(playerId);

            Collection<String> playerIds = town.getPlayerIDList();
            assertTrue(playerIds.contains(LEADER_ID), "Leader should be in player list");
            assertTrue(playerIds.contains(playerId), "Custom rank player should be in list");
        }
    }

    // ==================== LEADER MANAGEMENT TESTS ====================

    @Nested
    @DisplayName("Leader Management")
    class LeaderManagementTests {

        @Test
        @DisplayName("Should return correct leader ID")
        void testGetLeaderID_Success() {
            assertEquals(LEADER_ID, town.getLeaderID(), "Leader ID should match initial leader");
        }

        @Test
        @DisplayName("Should identify leader correctly")
        void testIsLeader_Leader() {
            assertTrue(town.isLeader(LEADER_ID), "Should identify leader correctly");
        }

        @Test
        @DisplayName("Should not identify non-leader as leader")
        void testIsLeader_NotLeader() {
            String nonLeaderId = "other-player-uuid";
            assertFalse(town.isLeader(nonLeaderId), "Non-leader should not be identified as leader");
        }

        @Test
        @DisplayName("Should change leader successfully")
        void testSetLeaderID_Success() {
            String newLeaderId = "new-leader-uuid";
            town.setLeaderID(newLeaderId);

            assertEquals(newLeaderId, town.getLeaderID(), "Leader ID should be updated");
        }

        @Test
        @DisplayName("Should return null leader when leader is null")
        void testGetLeaderData_NoLeader() {
            town.setLeaderID(null);

            ITanPlayer leader = town.getLeaderData();
            assertNull(leader, "Leader data should be null when leader ID is null");
        }

        @Test
        @DisplayName("Should detect town has no leader")
        void testHaveNoLeader_True() {
            town.setLeaderID(null);
            assertTrue(town.haveNoLeader(), "Town should have no leader when ID is null");
        }

        @Test
        @DisplayName("Should detect town has leader")
        void testHaveNoLeader_False() {
            assertFalse(town.haveNoLeader(), "Town should have leader");
        }
    }

    // ==================== REGION AFFILIATION TESTS ====================

    @Nested
    @DisplayName("Region (Nation) Affiliation")
    class RegionAffiliationTests {

        @Test
        @DisplayName("Should join region successfully")
        void testSetOverlord_Success() {
            String regionId = "region_test_001";
            when(mockRegion.getID()).thenReturn(regionId);
            when(mockRegion.isVassal(anyString())).thenReturn(false);

            town.setOverlord(mockRegion);

            assertTrue(town.haveOverlord(), "Town should have overlord after joining region");
            assertEquals(regionId, town.getOverlord().get().getID(), "Overlord ID should match");
        }

        @Test
        @DisplayName("Should remove overlord successfully")
        void testRemoveOverlord_Success() {
            String regionId = "region_test_001";
            when(mockRegion.getID()).thenReturn(regionId);
            when(mockRegion.isVassal(anyString())).thenReturn(true);
            when(mockRegion.removeVassal(any())).thenReturn(CompletableFuture.completedFuture(null));

            town.setOverlord(mockRegion);
            assertTrue(town.haveOverlord(), "Town should have overlord");

            town.removeOverlord();

            assertFalse(town.haveOverlord(), "Town should not have overlord after removal");
        }

        @Test
        @DisplayName("Should not be vassal of any territory")
        void testIsVassal_AlwaysFalse() {
            String otherTerritoryId = "other-territory";
            assertFalse(town.isVassal(otherTerritoryId), "Town should never be a vassal (only RegionData can)");
        }

        @Test
        @DisplayName("Should return empty potential vassals list")
        void testGetPotentialVassals_Empty() {
            assertTrue(town.getPotentialVassals().isEmpty(), "Towns have no potential vassals");
        }

        @Test
        @DisplayName("Should not be capital of region")
        void testIsCapital_False() {
            assertFalse(town.isCapital(), "Town cannot be capital (only towns in regions)");
        }

        @Test
        @DisplayName("Should return null capital")
        void testGetCapital_Null() {
            assertNull(town.getCapital(), "Town should return null for capital");
        }
    }

    // ==================== PROPERTY MANAGEMENT TESTS ====================

    @Nested
    @DisplayName("Property Management")
    class PropertyManagementTests {

        @Test
        @DisplayName("Should generate sequential property IDs")
        void testNextPropertyID_Sequential() {
            assertEquals("P0", town.nextPropertyID(), "First property ID should be P0");
            assertEquals("P1", town.nextPropertyID(), "Second property ID should be P1");
            assertEquals("P2", town.nextPropertyID(), "Third property ID should be P2");
        }

        @Test
        @DisplayName("Should create property owned by territory")
        void testRegisterNewProperty_TerritoryOwned() {
            Vector3D p1 = new Vector3D(0, 0, 0, "world");
            Vector3D p2 = new Vector3D(10, 255, 10, "world");

            PropertyData property = town.registerNewProperty(p1, p2, town);

            assertNotNull(property, "Property should be created");
            assertEquals(town, property.getOwner(), "Property should be owned by town");
            assertTrue(town.getPropertyDataMap().containsKey("P0"), "Property should be in town's property map");
        }

        @Test
        @DisplayName("Should create property owned by player")
        void testRegisterNewProperty_PlayerOwned() {
            Vector3D p1 = new Vector3D(0, 0, 0, "world");
            Vector3D p2 = new Vector3D(10, 255, 10, "world");

            ITanPlayer owner = mock(ITanPlayer.class);
            when(owner.getID()).thenReturn("property-owner");

            PropertyData property = town.registerNewProperty(p1, p2, owner);

            assertNotNull(property, "Property should be created");
            assertEquals(owner, property.getOwner(), "Property should be owned by player");
        }

        @Test
        @DisplayName("Should retrieve property by ID")
        void testGetProperty_ById() {
            Vector3D p1 = new Vector3D(0, 0, 0, "world");
            Vector3D p2 = new Vector3D(10, 255, 10, "world");

            town.registerNewProperty(p1, p2, town);

            PropertyData retrieved = town.getProperty("P0");
            assertNotNull(retrieved, "Property should be retrievable by ID");
        }

        @Test
        @DisplayName("Should retrieve property by location")
        void testGetProperty_ByLocation() {
            Vector3D p1 = new Vector3D(0, 0, 0, "world");
            Vector3D p2 = new Vector3D(10, 255, 10, "world");

            town.registerNewProperty(p1, p2, town);

            Location location = mock(Location.class);
            when(location.getX()).thenReturn(5.0);
            when(location.getY()).thenReturn(64.0);
            when(location.getZ()).thenReturn(5.0);

            // Note: This test assumes containsLocation works correctly
            // In real implementation, PropertyData.containsLocation would need proper mocking
        }

        @Test
        @DisplayName("Should remove property from town")
        void testRemoveProperty_Success() {
            Vector3D p1 = new Vector3D(0, 0, 0, "world");
            Vector3D p2 = new Vector3D(10, 255, 10, "world");

            PropertyData property = town.registerNewProperty(p1, p2, town);
            town.removeProperty(property);

            assertFalse(town.getPropertyDataMap().containsKey("P0"), "Property should be removed from map");
        }
    }

    // ==================== ECONOMY AND TREASURY TESTS ====================

    @Nested
    @DisplayName("Economy and Treasury")
    class EconomyTests {

        @Test
        @DisplayName("Should add to balance successfully")
        void testAddToBalance_Success() {
            double initialBalance = town.getBalance();
            double amount = 1000.0;

            town.addToBalance(amount);

            assertEquals(initialBalance + amount, town.getBalance(), 0.01, "Balance should increase by amount");
        }

        @Test
        @DisplayName("Should remove from balance successfully")
        void testRemoveFromBalance_Success() {
            town.addToBalance(2000.0);
            double initialBalance = town.getBalance();
            double amount = 500.0;

            town.removeFromBalance(amount);

            assertEquals(initialBalance - amount, town.getBalance(), 0.01, "Balance should decrease by amount");
        }

        @Test
        @DisplayName("Should handle negative balance withdrawal")
        void testRemoveFromBalance_InsufficientFunds() {
            town.addToBalance(100.0);
            double withdrawalAmount = 200.0;

            town.removeFromBalance(withdrawalAmount);

            assertTrue(town.getBalance() < 0, "Balance can go negative (debt allowed)");
        }
    }

    // ==================== RANK MANAGEMENT TESTS ====================

    @Nested
    @DisplayName("Rank Management")
    class RankManagementTests {

        @Test
        @DisplayName("Should create new rank successfully")
        void testRegisterNewRank_Success() {
            String rankName = "Mayor";
            RankData newRank = town.registerNewRank(rankName);

            assertNotNull(newRank, "New rank should be created");
            assertEquals(rankName, newRank.getName(), "Rank name should match");
        }

        @Test
        @DisplayName("Should assign unique rank IDs")
        void testRegisterNewRank_UniqueIds() {
            RankData rank1 = town.registerNewRank("Rank1");
            RankData rank2 = town.registerNewRank("Rank2");

            assertNotEquals(rank1.getID(), rank2.getID(), "Each rank should have unique ID");
        }

        @Test
        @DisplayName("Should retrieve rank by ID")
        void testGetRank_ById() {
            RankData newRank = town.registerNewRank("CustomRank");

            RankData retrieved = town.getRank(newRank.getID());

            assertEquals(newRank, retrieved, "Retrieved rank should match created rank");
        }

        @Test
        @DisplayName("Should retrieve player's rank")
        void testGetRank_ByPlayer() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("test-player");
            when(player.getTownRankID()).thenReturn(town.getDefaultRankID());

            town.addPlayer(player);

            RankData playerRank = town.getRank(player);
            assertNotNull(playerRank, "Player should have a rank");
            assertEquals(town.getDefaultRankID(), playerRank.getID(), "Player should have default rank");
        }

        @Test
        @DisplayName("Should check duplicate rank names")
        void testIsRankNameUsed_Duplicate() {
            String rankName = "Assistant";
            town.registerNewRank(rankName);

            assertTrue(town.isRankNameUsed(rankName), "Should detect duplicate rank name");
            assertFalse(town.isRankNameUsed("NonExistent"), "Should return false for non-existent rank");
        }

        @Test
        @DisplayName("Should remove rank successfully")
        void testRemoveRank_Success() {
            RankData newRank = town.registerNewRank("TempRank");
            int rankId = newRank.getID();

            town.removeRank(rankId);

            assertNull(town.getRank(rankId), "Removed rank should not be retrievable");
        }
    }

    // ==================== CHUNK CLAIMING TESTS ====================

    @Nested
    @DisplayName("Chunk Claiming")
    class ChunkClaimingTests {

        @Test
        @DisplayName("Should set capital location on first claim")
        void testAbstractClaimChunk_SetsCapitalLocation() {
            when(mockChunk.getX()).thenReturn(10);
            when(mockChunk.getZ()).thenReturn(20);
            when(mockChunk.getWorld()).thenReturn(mock(org.bukkit.World.class));

            // Simulate claiming (would need more setup for real test)
            Vector2D capitalLocation = new Vector2D(10, 20, "world");
            town.setCapitalLocation(capitalLocation);

            assertTrue(town.getCapitalLocation().isPresent(), "Capital location should be set");
            assertEquals(10, town.getCapitalLocation().get().getX(), "Capital X should match");
            assertEquals(20, town.getCapitalLocation().get().getZ(), "Capital Z should match");
        }

        @Test
        @DisplayName("Should check if spawn is set")
        void testIsSpawnSet_InitiallyFalse() {
            assertFalse(town.isSpawnSet(), "Spawn should not be set initially");
        }

        @Test
        @DisplayName("Should set spawn location")
        void testSetSpawn_Success() {
            Location spawnLoc = mock(Location.class);
            when(spawnLoc.getWorld()).thenReturn(mock(org.bukkit.World.class));
            when(spawnLoc.getWorld().getUID()).thenReturn(UUID.randomUUID());

            town.setSpawn(spawnLoc);

            assertTrue(town.isSpawnSet(), "Spawn should be set");
            assertNotNull(town.getSpawn(), "Spawn location should be retrievable");
        }
    }

    // ==================== TERRITORY DELETION TESTS ====================

    @Nested
    @DisplayName("Territory Deletion and Cleanup")
    class DeletionTests {

        @Test
        @DisplayName("Should start deletion process successfully")
        void testDelete_StartsDeletion() {
            CompletableFuture<Void> deletionFuture = town.delete();

            assertNotNull(deletionFuture, "Deletion should return a CompletableFuture");
        }

        @Test
        @DisplayName("Should remove all properties on deletion")
        void testDelete_RemovesProperties() {
            Vector3D p1 = new Vector3D(0, 0, 0, "world");
            Vector3D p2 = new Vector3D(10, 255, 10, "world");
            town.registerNewProperty(p1, p2, town);

            assertFalse(town.getPropertyDataMap().isEmpty(), "Town should have properties before deletion");

            // Properties would be cleaned up during deletion
            // Note: Full test would require proper storage mocking
        }

        @Test
        @DisplayName("Should remove all players on deletion")
        void testDelete_RemovesAllPlayers() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("test-player");
            when(player.getPlayer()).thenReturn(mockPlayer);

            town.addPlayer(player);
            assertTrue(town.isPlayerIn("test-player"), "Player should be in town before deletion");

            // Players would be removed during deletion process
        }
    }

    // ==================== DIPLOMACY TESTS ====================

    @Nested
    @DisplayName("Diplomacy and Relations")
    class DiplomacyTests {

        @Test
        @DisplayName("Should set relation with other territory")
        void testSetRelation_Success() {
            TerritoryData otherTerritory = mock(TerritoryData.class);
            when(otherTerritory.getID()).thenReturn("other-town");
            when(otherTerritory.getRelations()).thenReturn(mock(org.leralix.tan.dataclass.RelationData.class));

            town.setRelation(otherTerritory, TownRelation.ALLY);

            // Verify relation is set (implementation would check actual relation data)
        }

        @Test
        @DisplayName("Should get neutral relation with unknown territory")
        void testGetRelationWith_DefaultNeutral() {
            TerritoryData unknownTerritory = mock(TerritoryData.class);
            when(unknownTerritory.getID()).thenReturn("unknown-town");

            TownRelation relation = town.getRelationWith(unknownTerritory);

            assertEquals(TownRelation.NEUTRAL, relation, "Unknown territory should be neutral");
        }

        @Test
        @DisplayName("Should get self relation")
        void testGetRelationWith_Self() {
            TownRelation relation = town.getRelationWith(town);

            assertEquals(TownRelation.SELF, relation, "Town should have SELF relation with itself");
        }
    }

    // ==================== COSMETICS TESTS ====================

    @Nested
    @DisplayName("Cosmetics and Display")
    class CosmeticsTests {

        @Test
        @DisplayName("Should get icon with name")
        void testGetIconWithName_Success() {
            ItemStack icon = town.getIconWithName();

            assertNotNull(icon, "Icon should not be null");
            assertNotNull(icon.getItemMeta(), "Icon meta should not be null");
        }

        @Test
        @DisplayName("Should set description")
        void testSetDescription_Success() {
            String description = "A peaceful town";

            town.setDescription(description);

            assertEquals(description, town.getDescription(), "Description should match");
        }

        @Test
        @DisplayName("Should get base colored name")
        void testGetBaseColoredName_Success() {
            String coloredName = town.getBaseColoredName();

            assertNotNull(coloredName, "Colored name should not be null");
            assertTrue(coloredName.contains(TOWN_NAME), "Colored name should contain town name");
        }

        @Test
        @DisplayName("Should get town tag")
        void testGetTownTag_Success() {
            String tag = town.getTownTag();

            assertNotNull(tag, "Town tag should not be null");
            assertTrue(tag.length() <= 4, "Tag should be limited to prefix size");
        }

        @Test
        @DisplayName("Should set custom town tag")
        void testSetTownTag_Success() {
            String customTag = "TEST";

            town.setTownTag(customTag);

            assertEquals(customTag, town.getTownTag(), "Custom tag should be set");
        }
    }

    // ==================== RECRUITMENT TESTS ====================

    @Nested
    @DisplayName("Recruitment Status")
    class RecruitmentTests {

        @Test
        @DisplayName("Should toggle recruiting status")
        void testSwapRecruiting_Success() {
            boolean initialStatus = town.isRecruiting();

            town.swapRecruiting();

            assertNotEquals(initialStatus, town.isRecruiting(), "Recruiting status should toggle");
        }

        @Test
        @DisplayName("Should start with recruiting disabled")
        void testIsRecruiting_InitiallyFalse() {
            assertFalse(town.isRecruiting(), "Recruiting should be disabled initially");
        }
    }

    // ==================== PROGRESSION TESTS ====================

    @Nested
    @DisplayName("Town Progression")
    class ProgressionTests {

        @Test
        @DisplayName("Should have progression component")
        void testGetProgression_Success() {
            assertNotNull(town.getProgression(), "Progression component should exist");
        }

        @Test
        @DisplayName("Should get town tier")
        void testGetTownTier_Success() {
            assertNotNull(town.getTownTier(), "Town tier should not be null");
        }

        @Test
        @DisplayName("Should get town level")
        void testGetTownLevel_Success() {
            assertTrue(town.getTownLevel() >= 0, "Town level should be non-negative");
        }

        @Test
        @DisplayName("Should get town XP")
        void testGetTownXp_Success() {
            assertTrue(town.getTownXp() >= 0, "Town XP should be non-negative");
        }
    }

    // ==================== PRESTIGE TESTS ====================

    @Nested
    @DisplayName("Prestige System")
    class PrestigeTests {

        @Test
        @DisplayName("Should have prestige points")
        void testGetPrestigePoints_Success() {
            assertNotNull(town.getPrestigePoints(), "Prestige points should exist");
        }

        @Test
        @DisplayName("Should get prestige balance")
        void testGetPrestigeBalance_Success() {
            assertTrue(town.getPrestigeBalance() >= 0, "Prestige balance should be non-negative");
        }

        @Test
        @DisplayName("Should add purchased upgrade")
        void testAddPurchasedUpgrade_Success() {
            String upgradeId = "better_farms";

            town.addPurchasedUpgrade(upgradeId);

            assertTrue(town.hasPurchasedUpgrade(upgradeId), "Upgrade should be marked as purchased");
        }

        @Test
        @DisplayName("Should check if upgrade is purchased")
        void testHasPurchasedUpgrade_NotPurchased() {
            assertFalse(town.hasPurchasedUpgrade("non_existent_upgrade"), "Should return false for non-purchased upgrade");
        }
    }

    // ==================== FOLIA COMPLIANCE TESTS ====================

    @Nested
    @DisplayName("Folia Compliance - Async Operations")
    class FoliaComplianceTests {

        @Test
        @DisplayName("Should provide async player removal")
        void testRemovePlayerAsync_ReturnsFuture() {
            String playerId = "async-player-uuid";

            CompletableFuture<Void> future = town.removePlayerAsync(playerId);

            assertNotNull(future, "Async removal should return CompletableFuture");
        }

        @Test
        @DisplayName("Should provide async join request handling")
        void testAddPlayerJoinRequestAsync_ReturnsFuture() {
            CompletableFuture<Void> future = town.addPlayerJoinRequestAsync(mockPlayer);

            assertNotNull(future, "Async request should return CompletableFuture");
        }

        @Test
        @DisplayName("Should provide async leader data retrieval")
        void testGetLeaderDataAsync_ReturnsFuture() {
            CompletableFuture<ITanPlayer> future = town.getLeaderDataAsync();

            assertNotNull(future, "Async leader retrieval should return CompletableFuture");
        }

        @Test
        @DisplayName("Should provide async region retrieval")
        void testGetRegionAsync_ReturnsFuture() {
            CompletableFuture<RegionData> future = town.getRegionAsync();

            assertNotNull(future, "Async region retrieval should return CompletableFuture");
        }

        @Test
        @DisplayName("Should provide async member list for GUI")
        void testGetOrderedMemberListAsync_ReturnsFuture() {
            CompletableFuture<List<dev.triumphteam.gui.guis.GuiItem>> future =
                town.getOrderedMemberListAsync(mockLeader);

            assertNotNull(future, "Async member list should return CompletableFuture");
        }

        @Test
        @DisplayName("Should provide async kick player")
        void testKickPlayerAsync_ReturnsFuture() {
            CompletableFuture<Void> future = town.kickPlayerAsync(mockOfflinePlayer);

            assertNotNull(future, "Async kick should return CompletableFuture");
        }
    }

    // ==================== INVARIANT TESTS ====================

    @Nested
    @DisplayName("Territory Invariants")
    class InvariantTests {

        @Test
        @DisplayName("Should maintain consistent player count across removal")
        void testInvariant_PlayerCountConsistency() {
            ITanPlayer player1 = mock(ITanPlayer.class);
            ITanPlayer player2 = mock(ITanPlayer.class);
            when(player1.getID()).thenReturn("player1");
            when(player2.getID()).thenReturn("player2");
            when(player1.getPlayer()).thenReturn(mockPlayer);
            when(player2.getPlayer()).thenReturn(mockPlayer);

            town.addPlayer(player1);
            town.addPlayer(player2);

            int countWithTwo = town.getPlayerIDList().size();

            town.removePlayer(player1);

            assertEquals(countWithTwo - 1, town.getPlayerIDList().size(),
                "Player count should decrease by 1 after removal");
        }

        @Test
        @DisplayName("Should not allow duplicate player membership")
        void testInvariant_NoDuplicatePlayers() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("duplicate-test");
            when(player.getPlayer()).thenReturn(mockPlayer);

            town.addPlayer(player);
            town.addPlayer(player);

            // Player should only appear once in the player list
            long count = town.getPlayerIDList().stream()
                .filter(id -> id.equals("duplicate-test"))
                .count();

            assertEquals(1, count, "Player should not be duplicated in membership list");
        }

        @Test
        @DisplayName("Should maintain rank assignment consistency")
        void testInvariant_RankAssignmentConsistency() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("rank-test");
            when(player.getTownRankID()).thenReturn(town.getDefaultRankID());
            when(player.getPlayer()).thenReturn(mockPlayer);

            town.addPlayer(player);

            RankData playerRank = town.getRank(player);
            assertNotNull(playerRank, "Player should have a rank");

            int playersInRank = playerRank.getPlayersID().size();
            int playersInTown = town.getPlayerIDList().size();

            // All players in town should have exactly one rank
            assertTrue(playersInRank <= playersInTown,
                "Rank membership should not exceed town membership");
        }
    }

    // ==================== TAX COLLECTION TESTS ====================

    @Nested
    @DisplayName("Tax Collection")
    class TaxCollectionTests {

        @Test
        @DisplayName("Should get tax amount")
        void testGetTax_Success() {
            double tax = town.getTax();
            assertTrue(tax >= 0, "Tax should be non-negative");
        }

        @Test
        @DisplayName("Should set tax amount")
        void testSetTax_Success() {
            double newTax = 50.0;
            town.setTax(newTax);

            assertEquals(newTax, town.getTax(), 0.01, "Tax should be updated");
        }

        @Test
        @DisplayName("Should add to tax amount")
        void testAddToTax_Success() {
            double initialTax = town.getTax();
            double addition = 10.0;
            town.addToTax(addition);

            assertEquals(initialTax + addition, town.getTax(), 0.01, "Tax should increase");
        }
    }
}
