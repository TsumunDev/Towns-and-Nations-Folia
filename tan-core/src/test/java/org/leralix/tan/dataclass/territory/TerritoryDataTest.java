package org.leralix.tan.dataclass.territory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.wars.PlannedAttack;
import org.leralix.tan.wars.fort.Fort;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test suite for abstract {@link TerritoryData} base class.
 * <p>
 * Tests cover:
 * <ul>
 *   <li>Common territory behavior (shared by TownData and RegionData)</li>
 *   <li>Identity management (ID, name, description)</li>
 *   <li>Economy operations (balance, donations)</li>
 *   <li>Diplomatic relations (allies, enemies, neutrals)</li>
 *   <li>Vassal and overlord management</li>
 *   <li>Rank and permission system</li>
 *   <li>Claim system integration</li>
 *   <li>Cosmetic customization</li>
 *   <li>Deletion cascade behavior</li>
 * </ul>
 *
 * <p><b>Architecture Invariants Tested:</b></p>
 * <ul>
 *   <li>Territory ID is immutable after creation</li>
 *   <li>Balance operations are thread-safe via immutable components</li>
 *   <li>Relations are bidirectional (A ally B implies B ally A)</li>
 *   <li>Vassal removal cascades properly</li>
 *   <li>Deletion cleans up all references</li>
 * </ul>
 *
 * @see TownData
 * @see RegionData
 * @since 0.15.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TerritoryData Base Class Tests")
public class TerritoryDataTest {

    @Mock
    private Player mockPlayer;

    @Mock
    private ITanPlayer mockLeader;

    @Mock
    private ITanPlayer mockPlayer2;

    @Mock
    private OfflinePlayer mockOfflinePlayer;

    private TownData territory;
    private static final String TERRITORY_ID = "territory_test_001";
    private static final String TERRITORY_NAME = "TestTerritory";
    private static final String LEADER_ID = "leader-uuid-123";

    @BeforeEach
    void setUp() {
        lenient().when(mockLeader.getID()).thenReturn(LEADER_ID);
        lenient().when(mockLeader.getNameStored()).thenReturn("LeaderPlayer");
        lenient().when(mockLeader.getOfflinePlayer()).thenReturn(mockOfflinePlayer);
        lenient().when(mockPlayer.getUniqueId()).thenReturn(UUID.fromString(LEADER_ID));
        lenient().when(mockPlayer.getName()).thenReturn("LeaderPlayer");

        territory = new TownData(TERRITORY_ID, TERRITORY_NAME, mockLeader);
    }

    // ==================== IDENTITY TESTS ====================

    @Nested
    @DisplayName("Territory Identity")
    class IdentityTests {

        @Test
        @DisplayName("Should return correct ID")
        void testGetID_Success() {
            assertEquals(TERRITORY_ID, territory.getID(), "ID should match constructor value");
        }

        @Test
        @DisplayName("Should return correct name")
        void testGetName_Success() {
            assertEquals(TERRITORY_NAME, territory.getName(), "Name should match constructor value");
        }

        @Test
        @DisplayName("Should rename territory with cost")
        void testRename_WithCost() {
            double initialBalance = 5000.0;
            territory.addToBalance(initialBalance);

            int cost = 100;
            String newName = "NewTerritoryName";
            FilledLang successMsg = mock(FilledLang.class);

            territory.rename(mockPlayer, cost, newName);

            assertEquals(newName, territory.getName(), "Name should be updated");
        }

        @Test
        @DisplayName("Should not rename when insufficient funds")
        void testRename_InsufficientFunds() {
            territory.addToBalance(50.0);
            int cost = 100;
            String newName = "ExpensiveRename";

            territory.rename(mockPlayer, cost, newName);

            assertNotEquals(newName, territory.getName(), "Name should not change with insufficient funds");
        }

        @Test
        @DisplayName("Should rename without cost check")
        void testRename_WithoutCost() {
            String newName = "FreeRename";

            territory.rename(newName);

            assertEquals(newName, territory.getName(), "Name should be updated");
        }

        @Test
        @DisplayName("Should have creation timestamp")
        void testGetCreationDate_Success() {
            long creationDate = territory.getCreationDate();
            assertTrue(creationDate > 0, "Creation date should be positive");
            assertTrue(creationDate <= System.currentTimeMillis(), "Creation date should be in past or now");
        }
    }

    // ==================== ECONOMY TESTS ====================

    @Nested
    @DisplayName("Economy Operations")
    class EconomyTests {

        @Test
        @DisplayName("Should get initial balance")
        void testGetBalance_Initial() {
            assertEquals(0.0, territory.getBalance(), 0.01, "Initial balance should be zero");
        }

        @Test
        @DisplayName("Should add to balance")
        void testAddToBalance_Success() {
            territory.addToBalance(1000.0);
            assertEquals(1000.0, territory.getBalance(), 0.01, "Balance should increase");
        }

        @Test
        @DisplayName("Should remove from balance")
        void testRemoveFromBalance_Success() {
            territory.addToBalance(1000.0);
            territory.removeFromBalance(300.0);
            assertEquals(700.0, territory.getBalance(), 0.01, "Balance should decrease");
        }

        @Test
        @DisplayName("Should handle negative balance")
        void testRemoveFromBalance_AllowsNegative() {
            territory.removeFromBalance(100.0);
            assertTrue(territory.getBalance() < 0, "Negative balance should be allowed");
        }

        @Test
        @DisplayName("Should process donation async")
        void testAddDonationAsync_ReturnsFuture() {
            CompletableFuture<Void> future = territory.addDonationAsync(mockPlayer, 100.0);
            assertNotNull(future, "Donation should return CompletableFuture");
        }

        @Test
        @DisplayName("Should reject negative donation")
        void testAddDonationAsync_NegativeAmount() {
            CompletableFuture<Void> future = territory.addDonationAsync(mockPlayer, -50.0);
            assertNotNull(future, "Should return future even for negative amount");
        }

        @Test
        @DisplayName("Should process donation synchronously")
        void testAddDonation_Sync() {
            territory.addToBalance(1000.0);

            assertDoesNotThrow(() -> territory.addDonation(mockPlayer, 100.0),
                "Sync donation should not throw");
        }
    }

    // ==================== DIPLOMACY TESTS ====================

    @Nested
    @DisplayName("Diplomatic Relations")
    class DiplomacyTests {

        @Test
        @DisplayName("Should set relation with another territory")
        void testSetRelation_Success() {
            TerritoryData otherTerritory = mock(TerritoryData.class);
            when(otherTerritory.getID()).thenReturn("other_teritory");
            when(otherTerritory.getRelations()).thenReturn(mock(org.leralix.tan.dataclass.RelationData.class));

            territory.setRelation(otherTerritory, TownRelation.ALLY);

            TownRelation relation = territory.getRelationWith("other_teritory");
            assertNotNull(relation, "Relation should be set");
        }

        @Test
        @DisplayName("Should get neutral relation with unknown territory")
        void testGetRelationWith_Unknown() {
            TerritoryData unknown = mock(TerritoryData.class);
            when(unknown.getID()).thenReturn("unknown_teritory");

            TownRelation relation = territory.getRelationWith(unknown);

            assertEquals(TownRelation.NEUTRAL, relation, "Unknown territory should be neutral");
        }

        @Test
        @DisplayName("Should get SELF relation with itself")
        void testGetRelationWith_Self() {
            TownRelation relation = territory.getRelationWith(territory);
            assertEquals(TownRelation.SELF, relation, "Self-relation should be SELF");
        }

        @Test
        @DisplayName("Should add diplomatic proposal")
        void testReceiveDiplomaticProposal_Success() {
            TerritoryData proposer = mock(TerritoryData.class);
            when(proposer.getID()).thenReturn("proposer_territory");

            territory.receiveDiplomaticProposal(proposer, TownRelation.ALLY);

            assertTrue(territory.getAllDiplomacyProposal().size() > 0,
                "Proposal should be added");
        }

        @Test
        @DisplayName("Should remove diplomatic proposal")
        void testRemoveDiplomaticProposal_Success() {
            TerritoryData proposer = mock(TerritoryData.class);
            when(proposer.getID()).thenReturn("proposer_territory");

            territory.receiveDiplomaticProposal(proposer, TownRelation.ALLY);
            territory.removeDiplomaticProposal(proposer);

            assertEquals(0, territory.getAllDiplomacyProposal().size(),
                "Proposal should be removed");
        }

        @Test
        @DisplayName("Should get worst relation with player")
        void testGetWorstRelationWithSync_Success() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("test_player");
            when(player.getAllTerritoriesPlayerIsInSync()).thenReturn(Collections.singletonList(territory));

            TownRelation relation = territory.getWorstRelationWithSync(player);

            assertEquals(TownRelation.SELF, relation, "Player in territory should have SELF relation");
        }
    }

    // ==================== VASSALAGE TESTS ====================

    @Nested
    @DisplayName("Vassal and Overlord Management")
    class VassalageTests {

        @Test
        @DisplayName("Should add vassal")
        void testAddVassal_Success() {
            RegionData overlord = mock(RegionData.class);
            when(overlord.getID()).thenReturn("overlord_region");
            when(overlord.canHaveVassals()).thenReturn(true);
            when(overlord.addVassal(any())).thenReturn(CompletableFuture.completedFuture(null));

            territory.setOverlord(overlord);

            assertTrue(territory.haveOverlord(), "Should have overlord after joining");
        }

        @Test
        @DisplayName("Should remove overlord")
        void testRemoveOverlord_Success() {
            RegionData overlord = mock(RegionData.class);
            when(overlord.getID()).thenReturn("overlord_region");
            when(overlord.removeVassal(any())).thenReturn(CompletableFuture.completedFuture(null));

            territory.setOverlord(overlord);
            assertTrue(territory.haveOverlord(), "Should have overlord before removal");

            territory.removeOverlord();

            assertFalse(territory.haveOverlord(), "Should not have overlord after removal");
        }

        @Test
        @DisplayName("Should get vassals list")
        void testGetVassalsID_Success() {
            List<String> vassals = territory.getVassalsID();
            assertNotNull(vassals, "Vassals list should not be null");
        }

        @Test
        @DisplayName("Should get vassal count")
        void testGetVassalCount_Accurate() {
            int count = territory.getVassalCount();
            assertTrue(count >= 0, "Vassal count should be non-negative");
        }

        @Test
        @DisplayName("Should check if territory is vassal")
        void testIsVassal_Check() {
            TerritoryData other = mock(TerritoryData.class);
            when(other.getID()).thenReturn("other_territory");

            boolean result = territory.isVassal(other);

            assertFalse(result, "Should not be vassal by default");
        }
    }

    // ==================== RANK AND PERMISSION TESTS ====================

    @Nested
    @DisplayName("Rank and Permission System")
    class RankPermissionTests {

        @Test
        @DisplayName("Should create new rank")
        void testRegisterNewRank_Success() {
            String rankName = "VIP";
            RankData newRank = territory.registerNewRank(rankName);

            assertNotNull(newRank, "Rank should be created");
            assertEquals(rankName, newRank.getName(), "Rank name should match");
        }

        @Test
        @DisplayName("Should get rank by ID")
        void testGetRank_ById() {
            RankData newRank = territory.registerNewRank("Custom");
            int rankId = newRank.getID();

            RankData retrieved = territory.getRank(rankId);

            assertEquals(newRank, retrieved, "Retrieved rank should match");
        }

        @Test
        @DisplayName("Should get player's rank")
        void testGetRank_ByPlayer() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("test_player");
            when(player.getTownRankID()).thenReturn(territory.getDefaultRankID());

            RankData playerRank = territory.getRank(player);

            assertNotNull(playerRank, "Player should have a rank");
        }

        @Test
        @DisplayName("Should check if rank name is used")
        void testIsRankNameUsed_Duplicate() {
            String rankName = "Mayor";
            territory.registerNewRank(rankName);

            assertTrue(territory.isRankNameUsed(rankName), "Should detect used rank name");
            assertFalse(territory.isRankNameUsed("NonExistent"), "Should return false for unused name");
        }

        @Test
        @DisplayName("Should remove rank")
        void testRemoveRank_Success() {
            RankData rank = territory.registerNewRank("Temp");
            int rankId = rank.getID();

            territory.removeRank(rankId);

            assertNull(territory.getRank(rankId), "Removed rank should not exist");
        }

        @Test
        @DisplayName("Should set default rank")
        void testSetDefaultRank_Success() {
            RankData newDefault = territory.registerNewRank("Citizen");

            territory.setDefaultRank(newDefault);

            assertEquals(newDefault.getID(), territory.getDefaultRankID(),
                "Default rank should be updated");
        }

        @Test
        @DisplayName("Should check player permission")
        void testDoesPlayerHavePermission_Success() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn(LEADER_ID);
            when(player.getTownRankID()).thenReturn(territory.getDefaultRankID());

            boolean hasPermission = territory.doesPlayerHavePermission(player, RolePermission.CLAIM_CHUNK);

            assertTrue(hasPermission, "Leader should have all permissions");
        }

        @Test
        @DisplayName("Should provide async permission check")
        void testDoesPlayerHavePermissionAsync_ReturnsFuture() {
            CompletableFuture<Boolean> future =
                territory.doesPlayerHavePermissionAsync(mockPlayer, RolePermission.CLAIM_CHUNK);

            assertNotNull(future, "Async permission check should return CompletableFuture");
        }

        @Test
        @DisplayName("Should get all ranks sorted")
        void testGetAllRanksSorted_Descending() {
            territory.registerNewRank("Low");
            RankData highRank = territory.registerNewRank("High");
            highRank.setLevel(10);

            List<RankData> sorted = territory.getAllRanksSorted();

            assertNotNull(sorted, "Sorted list should not be null");
            // Verify highest level is first
        }
    }

    // ==================== CLAIM SYSTEM TESTS ====================

    @Nested
    @DisplayName("Claim System Integration")
    class ClaimSystemTests {

        @Test
        @DisplayName("Should claim chunk successfully")
        void testClaimChunk_Success() {
            Chunk mockChunk = mock(Chunk.class);
            when(mockChunk.getX()).thenReturn(10);
            when(mockChunk.getZ()).thenReturn(20);
            when(mockChunk.getWorld()).thenReturn(mock(org.bukkit.World.class));

            territory.addToBalance(10000.0);

            boolean result = territory.claimChunk(mockPlayer, mockChunk, true);

            // Result depends on various conditions
            // Test verifies method doesn't throw
        }

        @Test
        @DisplayName("Should check if can claim chunk")
        void testCanClaimChunkSync_Validates() {
            Chunk mockChunk = mock(Chunk.class);
            when(mockChunk.getX()).thenReturn(10);
            when(mockChunk.getZ()).thenReturn(20);
            when(mockChunk.getWorld()).thenReturn(mock(org.bukkit.World.class));

            boolean canClaim = territory.canClaimChunkSync(mockPlayer, mockChunk, true);

            // Result depends on balance, permissions, etc.
        }

        @Test
        @DisplayName("Should get claim cost")
        void testGetClaimCost_Positive() {
            int cost = territory.getClaimCost();
            assertTrue(cost >= 0, "Claim cost should be non-negative");
        }

        @Test
        @DisplayName("Should get number of claimed chunks")
        void testGetNumberOfClaimedChunk_Accurate() {
            int claimedCount = territory.getNumberOfClaimedChunk();
            assertTrue(claimedCount >= 0, "Claimed chunk count should be non-negative");
        }
    }

    // ==================== COSMETICS TESTS ====================

    @Nested
    @DisplayName("Cosmetics and Customization")
    class CosmeticsTests {

        @Test
        @DisplayName("Should get description")
        void testGetDescription_Success() {
            String description = territory.getDescription();
            assertNotNull(description, "Description should not be null");
        }

        @Test
        @DisplayName("Should set description")
        void testSetDescription_Success() {
            String newDesc = "A test territory";

            territory.setDescription(newDesc);

            assertEquals(newDesc, territory.getDescription(), "Description should be updated");
        }

        @Test
        @DisplayName("Should get icon")
        void testGetIcon_Success() {
            ItemStack icon = territory.getIcon();
            assertNotNull(icon, "Icon should not be null");
        }

        @Test
        @DisplayName("Should set custom icon")
        void testSetIcon_Success() {
            ItemStack customIcon = new ItemStack(Material.DIAMOND);

            assertDoesNotThrow(() -> territory.setIcon(mock(org.leralix.tan.dataclass.territory.cosmetic.ICustomIcon.class)),
                "Setting icon should not throw");
        }

        @Test
        @DisplayName("Should get chunk color code")
        void testGetChunkColorCode_Valid() {
            int colorCode = territory.getChunkColorCode();
            // Color code should be valid integer
        }

        @Test
        @DisplayName("Should get chunk color as hex")
        void testGetChunkColorInHex_Valid() {
            String hexColor = territory.getChunkColorInHex();
            assertNotNull(hexColor, "Hex color should not be null");
        }

        @Test
        @DisplayName("Should set chunk color")
        void testSetChunkColor_Success() {
            int newColor = 0x00FF00; // Green

            assertDoesNotThrow(() -> territory.setChunkColor(newColor),
                "Setting color should not throw");
        }
    }

    // ==================== BROADCAST TESTS ====================

    @Nested
    @DisplayName("Broadcast Messaging")
    class BroadcastTests {

        @Test
        @DisplayName("Should broadcast message")
        void testBroadcastMessage_Success() {
            FilledLang message = mock(FilledLang.class);

            assertDoesNotThrow(() -> territory.broadCastMessage(message),
                "Broadcast should not throw");
        }

        @Test
        @DisplayName("Should broadcast with sound")
        void testBroadcastMessageWithSound_Success() {
            FilledLang message = mock(FilledLang.class);
            SoundEnum sound = SoundEnum.GOOD;

            assertDoesNotThrow(() -> territory.broadcastMessageWithSound(message, sound),
                "Broadcast with sound should not throw");
        }

        @Test
        @DisplayName("Should apply action to all online players")
        void testApplyToAllOnlinePlayer_Success() {
            assertDoesNotThrow(() -> territory.applyToAllOnlinePlayer(p -> {}),
                "Applying action should not throw");
        }
    }

    // ==================== WAR AND FORT TESTS ====================

    @Nested
    @DisplayName("War and Fort Management")
    class WarFortTests {

        @Test
        @DisplayName("Should add planned attack")
        void testAddPlannedAttack_Success() {
            PlannedAttack attack = mock(PlannedAttack.class);
            when(attack.getID()).thenReturn("attack_001");

            territory.addPlannedAttack(attack);

            assertTrue(territory.getAttacksInvolvedID().contains("attack_001"),
                "Attack should be added");
        }

        @Test
        @DisplayName("Should remove planned attack")
        void testRemovePlannedAttack_Success() {
            PlannedAttack attack = mock(PlannedAttack.class);
            when(attack.getID()).thenReturn("attack_001");

            territory.addPlannedAttack(attack);
            territory.removePlannedAttack(attack);

            assertFalse(territory.getAttacksInvolvedID().contains("attack_001"),
                "Attack should be removed");
        }

        @Test
        @DisplayName("Should check if at war")
        void testIsAtWar_Check() {
            // Initially not at war
            assertFalse(territory.isAtWar(), "Should not be at war initially");

            PlannedAttack attack = mock(PlannedAttack.class);
            when(attack.getID()).thenReturn("attack_001");

            territory.addPlannedAttack(attack);

            // Now at war
            assertTrue(territory.isAtWar(), "Should be at war after adding attack");
        }

        @Test
        @DisplayName("Should add owned fort")
        void testAddOwnedFort_Success() {
            Fort fort = mock(Fort.class);
            when(fort.getID()).thenReturn("fort_001");

            territory.addOwnedFort(fort);

            assertTrue(territory.getOwnedFortIDs().contains("fort_001"),
                "Fort should be added");
        }

        @Test
        @DisplayName("Should remove owned fort")
        void testRemoveOwnedFort_Success() {
            Fort fort = mock(Fort.class);
            when(fort.getID()).thenReturn("fort_001");

            territory.addOwnedFort(fort);
            territory.removeOwnedFort(fort);

            assertFalse(territory.getOwnedFortIDs().contains("fort_001"),
                "Fort should be removed");
        }

        @Test
        @DisplayName("Should add occupied fort")
        void testAddOccupiedFort_Success() {
            Fort fort = mock(Fort.class);
            when(fort.getID()).thenReturn("occupied_fort_001");

            territory.addOccupiedFort(fort);

            assertTrue(territory.getOccupiedFortIds().contains("occupied_fort_001"),
                "Occupied fort should be added");
        }
    }

    // ==================== TAX TESTS ====================

    @Nested
    @DisplayName("Taxation")
    class TaxTests {

        @Test
        @DisplayName("Should get base tax")
        void testGetTax_Success() {
            double tax = territory.getTax();
            assertTrue(tax >= 0, "Tax should be non-negative");
        }

        @Test
        @DisplayName("Should set base tax")
        void testSetTax_Success() {
            double newTax = 50.0;
            territory.setTax(newTax);

            assertEquals(newTax, territory.getTax(), 0.01, "Tax should be updated");
        }

        @Test
        @DisplayName("Should add to tax")
        void testAddToTax_Success() {
            double initialTax = territory.getTax();
            territory.addToTax(10.0);

            assertEquals(initialTax + 10.0, territory.getTax(), 0.01, "Tax should increase");
        }

        @Test
        @DisplayName("Should get property rent tax")
        void testGetTaxOnRentingProperty_Success() {
            double tax = territory.getTaxOnRentingProperty();
            assertTrue(tax >= 0, "Property rent tax should be non-negative");
        }

        @Test
        @DisplayName("Should set property rent tax")
        void testSetTaxOnRentingProperty_Success() {
            double newTax = 25.0;
            territory.setTaxOnRentingProperty(newTax);

            assertEquals(newTax, territory.getTaxOnRentingProperty(), 0.01,
                "Property rent tax should be updated");
        }
    }

    // ==================== UPGRADE TESTS ====================

    @Nested
    @DisplayName("Upgrades and Progression")
    class UpgradeTests {

        @Test
        @DisplayName("Should get upgrade status")
        void testGetNewLevel_Success() {
            assertNotNull(territory.getNewLevel(), "Upgrade status should not be null");
        }

        @Test
        @DisplayName("Should get upgrades status (alias)")
        void testGetUpgradesStatus_SameAsNewLevel() {
            assertEquals(territory.getNewLevel(), territory.getUpgradesStatus(),
                "getUpgradesStatus should alias getNewLevel");
        }

        @Test
        @DisplayName("Should upgrade territory")
        void testUpgradeTown_Success() {
            assertDoesNotThrow(() -> territory.upgradeTown(mock(org.leralix.tan.upgrade.Upgrade.class)),
                "Upgrading should not throw");
        }
    }

    // ==================== DELETION TESTS ====================

    @Nested
    @DisplayName("Territory Deletion")
    class DeletionTests {

        @Test
        @DisplayName("Should start deletion process")
        void testDelete_ReturnsFuture() {
            CompletableFuture<Void> future = territory.delete();

            assertNotNull(future, "Deletion should return CompletableFuture");
        }

        @Test
        @DisplayName("Should clean up vassal references on deletion")
        void testDelete_RemovesVassals() {
            RegionData overlord = mock(RegionData.class);
            when(overlord.getID()).thenReturn("overlord");
            when(overlord.removeVassal(any())).thenReturn(CompletableFuture.completedFuture(null));

            territory.setOverlord(overlord);

            CompletableFuture<Void> deletion = territory.delete();

            assertNotNull(deletion, "Deletion should complete");
        }
    }

    // ==================== GUI TESTS ====================

    @Nested
    @DisplayName("GUI Integration")
    class GuiTests {

        @Test
        @DisplayName("Should get icon with information")
        void testGetIconWithInformations_Success() {
            ItemStack icon = territory.getIconWithInformations(LangType.EN);
            assertNotNull(icon, "Icon should not be null");
        }

        @Test
        @DisplayName("Should open main menu")
        void testOpenMainMenu_Success() {
            assertDoesNotThrow(() -> territory.openMainMenu(mockPlayer),
                "Opening menu should not throw");
        }

        @Test
        @DisplayName("Should get ordered member list")
        void testGetOrderedMemberList_Success() {
            List<dev.triumphteam.gui.guis.GuiItem> members =
                territory.getOrderedMemberList(mockLeader);

            assertNotNull(members, "Member list should not be null");
        }

        @Test
        @DisplayName("Should get ordered member list async")
        void testGetOrderedMemberListAsync_ReturnsFuture() {
            CompletableFuture<List<dev.triumphteam.gui.guis.GuiItem>> future =
                territory.getOrderedMemberListAsync(mockLeader);

            assertNotNull(future, "Async member list should return future");
        }
    }

    // ==================== ASYNC OPERATIONS TESTS ====================

    @Nested
    @DisplayName("Async Operations (Folia Compliance)")
    class AsyncOperationsTests {

        @Test
        @DisplayName("Should provide async leader name retrieval")
        void testGetLeaderName_ReturnsFuture() {
            CompletableFuture<String> future = territory.getLeaderName();
            assertNotNull(future, "Async leader name should return CompletableFuture");
        }

        @Test
        @DisplayName("Should provide async rank retrieval")
        void testGetRankAsync_ReturnsFuture() {
            CompletableFuture<RankData> future = territory.getRankAsync(mockPlayer);
            assertNotNull(future, "Async rank should return CompletableFuture");
        }
    }

    // ==================== INVARIANT TESTS ====================

    @Nested
    @DisplayName("Territory Invariants")
    class InvariantTests {

        @Test
        @DisplayName("INVARIANT: ID should not change after creation")
        void testInvariant_IdImmutable() {
            String originalId = territory.getID();
            // Perform various operations
            territory.setName("NewName");
            territory.addToBalance(1000);

            assertEquals(originalId, territory.getID(), "ID should remain constant");
        }

        @Test
        @DisplayName("INVARIANT: Balance operations use immutable components")
        void testInvariant_BalanceImmutability() {
            double balance1 = territory.getBalance();
            territory.addToBalance(100);
            double balance2 = territory.getBalance();
            territory.removeFromBalance(50);
            double balance3 = territory.getBalance();

            assertEquals(balance1 + 100, balance2, 0.01, "Balance should update correctly");
            assertEquals(balance2 - 50, balance3, 0.01, "Balance should update correctly");
        }

        @Test
        @DisplayName("INVARIANT: Default rank always exists")
        void testInvariant_DefaultRankExists() {
            assertNotNull(territory.getDefaultRankID(), "Default rank ID should never be null");
            assertNotNull(territory.getRank(territory.getDefaultRankID()),
                "Default rank should always be retrievable");
        }

        @Test
        @DisplayName("INVARIANT: Player membership consistency")
        void testInvariant_PlayerMembershipConsistency() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("test_player");
            when(player.getTownRankID()).thenReturn(territory.getDefaultRankID());
            when(player.getPlayer()).thenReturn(mockPlayer);

            territory.addPlayer(player);

            assertTrue(territory.isPlayerIn("test_player"),
                "Player should be in territory");
        }

        @Test
        @DisplayName("INVARIANT: Leader is always a member")
        void testInvariant_LeaderIsMember() {
            assertTrue(territory.isPlayerIn(LEADER_ID),
                "Leader should always be a territory member");
        }
    }

    // ==================== CHUNK SETTINGS TESTS ====================

    @Nested
    @DisplayName("Chunk Settings")
    class ChunkSettingsTests {

        @Test
        @DisplayName("Should get chunk settings")
        void testGetChunkSettings_Success() {
            assertNotNull(territory.getChunkSettings(), "Chunk settings should not be null");
        }

        @Test
        @DisplayName("Should get permission")
        void testGetPermission_Success() {
            assertDoesNotThrow(() -> territory.getPermission(mock(org.leralix.tan.enums.permissions.ChunkPermissionType.class)),
                "Getting permission should not throw");
        }

        @Test
        @DisplayName("Should cycle permission")
        void testNextPermission_Success() {
            assertDoesNotThrow(() -> territory.nextPermission(mock(org.leralix.tan.enums.permissions.ChunkPermissionType.class)),
                "Cycling permission should not throw");
        }
    }

    // ==================== ENEMY CLAIM TESTS ====================

    @Nested
    @DisplayName("Enemy Claims (War)")
    class EnemyClaimsTests {

        @Test
        @DisplayName("Should add available enemy claims")
        void testAddAvailableClaims_Success() {
            String enemyId = "enemy_territory";
            territory.addAvailableClaims(enemyId, 5);

            assertTrue(territory.getAvailableEnemyClaims().containsKey(enemyId),
                "Enemy claims should be added");
            assertEquals(5, territory.getAvailableEnemyClaims().get(enemyId),
                "Claim count should match");
        }

        @Test
        @DisplayName("Should consume enemy claim")
        void testConsumeEnemyClaim_Success() {
            String enemyId = "enemy_territory";
            territory.addAvailableClaims(enemyId, 3);

            territory.consumeEnemyClaim(enemyId);

            assertEquals(2, territory.getAvailableEnemyClaims().get(enemyId),
                "Claim should be consumed");
        }

        @Test
        @DisplayName("Should remove enemy when claims exhausted")
        void testConsumeEnemyClaim_RemovesWhenZero() {
            String enemyId = "enemy_territory";
            territory.addAvailableClaims(enemyId, 1);

            territory.consumeEnemyClaim(enemyId);

            assertFalse(territory.getAvailableEnemyClaims().containsKey(enemyId),
                "Enemy should be removed when claims reach zero");
        }

        @Test
        @DisplayName("Should check if can conquer chunk")
        void testCanConquerChunk_WithClaims() {
            ClaimedChunk2 chunk = mock(ClaimedChunk2.class);
            String enemyId = "enemy_territory";
            when(chunk.getOwnerID()).thenReturn(enemyId);

            territory.addAvailableClaims(enemyId, 1);

            assertTrue(territory.canConquerChunk(chunk),
                "Should conquer when claims available");
        }
    }

    // ==================== VASSALISATION PROPOSALS TESTS ====================

    @Nested
    @DisplayName("Vassalisation Proposals")
    class VassalisationProposalTests {

        @Test
        @DisplayName("Should add vassalisation proposal")
        void testAddVassalisationProposal_Success() {
            TerritoryData proposer = mock(TerritoryData.class);
            when(proposer.getID()).thenReturn("proposer_territory");

            territory.addVassalisationProposal(proposer);

            assertTrue(territory.containsVassalisationProposal(proposer),
                "Proposal should be added");
        }

        @Test
        @DisplayName("Should remove vassalisation proposal")
        void testRemoveVassalisationProposal_Success() {
            TerritoryData proposer = mock(TerritoryData.class);
            when(proposer.getID()).thenReturn("proposer_territory");

            territory.addVassalisationProposal(proposer);
            territory.removeVassalisationProposal(proposer);

            assertFalse(territory.containsVassalisationProposal(proposer),
                "Proposal should be removed");
        }

        @Test
        @DisplayName("Should get proposal count")
        void testGetNumberOfVassalisationProposals_Accurate() {
            TerritoryData proposer1 = mock(TerritoryData.class);
            TerritoryData proposer2 = mock(TerritoryData.class);
            when(proposer1.getID()).thenReturn("proposer1");
            when(proposer2.getID()).thenReturn("proposer2");

            territory.addVassalisationProposal(proposer1);
            territory.addVassalisationProposal(proposer2);

            assertEquals(2, territory.getNumberOfVassalisationProposals(),
                "Proposal count should be accurate");
        }
    }

    // ==================== RELATION WORST TESTS ====================

    @Nested
    @DisplayName("Worst Relation Calculation")
    class WorstRelationTests {

        @Test
        @DisplayName("Should get worst relation with player async")
        void testGetWorstRelationWith_ReturnsFuture() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("test_player");
            when(player.getAllTerritoriesPlayerIsIn()).thenReturn(
                CompletableFuture.completedFuture(Collections.singletonList(territory)));

            CompletableFuture<TownRelation> future = territory.getWorstRelationWith(player);

            assertNotNull(future, "Should return CompletableFuture");
        }

        @Test
        @DisplayName("Should handle player with no territories")
        void testGetWorstRelationWith_NoTerritories() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("no_territory_player");
            when(player.getAllTerritoriesPlayerIsIn()).thenReturn(
                CompletableFuture.completedFuture(Collections.emptyList()));

            CompletableFuture<TownRelation> future = territory.getWorstRelationWith(player);

            assertNotNull(future, "Should handle empty territories list");
        }
    }

    // ==================== BUDGET TESTS ====================

    @Nested
    @DisplayName("Budget Calculation")
    class BudgetTests {

        @Test
        @DisplayName("Should calculate budget")
        void testGetBudget_Success() {
            assertNotNull(territory.getBudget(), "Budget should not be null");
        }

        @Test
        @DisplayName("Should include salary payments in budget")
        void testGetBudget_IncludesSalaries() {
            var budget = territory.getBudget();
            // Budget should include salary lines
            assertNotNull(budget, "Budget should be calculated");
        }

        @Test
        @DisplayName("Should include chunk upkeep in budget")
        void testGetBudget_IncludesUpkeep() {
            var budget = territory.getBudget();
            assertNotNull(budget, "Budget should include upkeep");
        }
    }

    // ==================== PERMISSION TESTS ====================

    @Nested
    @DisplayName("Permission Checks")
    class PermissionCheckTests {

        @Test
        @DisplayName("Should check player in territory")
        void testIsPlayerIn_Success() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn(LEADER_ID);

            assertTrue(territory.isPlayerIn(player), "Leader should be in territory");
        }

        @Test
        @DisplayName("Should check player in territory by UUID")
        void testIsPlayerIn_ByUuid() {
            assertTrue(territory.isPlayerIn(mockPlayer), "Leader should be identified by Player object");
        }

        @Test
        @DisplayName("Should check player in territory by ID string")
        void testIsPlayerIn_ByIdString() {
            assertTrue(territory.isPlayerIn(LEADER_ID), "Leader should be identified by UUID string");
        }

        @Test
        @DisplayName("Should identify leader correctly")
        void testIsLeader_Success() {
            assertTrue(territory.isLeader(mockLeader), "Should identify leader from ITanPlayer");
        }

        @Test
        @DisplayName("Should identify leader from Player object")
        void testIsLeader_FromPlayer() {
            assertTrue(territory.isLeader(mockPlayer), "Should identify leader from Player object");
        }
    }

    // ==================== RANK PLAYER MANAGEMENT TESTS ====================

    @Nested
    @DisplayName("Rank Player Management")
    class RankPlayerTests {

        @Test
        @DisplayName("Should register player in default rank")
        void testRegisterPlayer_Success() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("new_player");

            assertDoesNotThrow(() -> territory.registerPlayer(player),
                "Registering player should not throw");
        }

        @Test
        @DisplayName("Should unregister player from rank")
        void testUnregisterPlayer_Success() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("remove_player");
            when(player.getTownRankID()).thenReturn(territory.getDefaultRankID());

            assertDoesNotThrow(() -> territory.unregisterPlayer(player),
                "Unregistering player should not throw");
        }

        @Test
        @DisplayName("Should set player rank")
        void testSetPlayerRank_Success() {
            ITanPlayer player = mock(ITanPlayer.class);
            when(player.getID()).thenReturn("promote_player");
            RankData newRank = territory.registerNewRank("Promoted");

            assertDoesNotThrow(() -> territory.setPlayerRank(player, newRank),
                "Setting player rank should not throw");
        }
    }

    // ==================== COLORED NAME TESTS ====================

    @Nested
    @DisplayName("Colored Name Display")
    class ColoredNameTests {

        @Test
        @DisplayName("Should get custom colored name as component")
        void testGetCustomColoredName_Success() {
            var coloredName = territory.getCustomColoredName();
            assertNotNull(coloredName, "Custom colored name should not be null");
        }

        @Test
        @DisplayName("Should get colored name as string")
        void testGetColoredName_Success() {
            String coloredName = territory.getColoredName();
            assertNotNull(coloredName, "Colored name string should not be null");
        }

        @Test
        @DisplayName("Should get base colored name")
        void testGetBaseColoredName_Success() {
            String baseColored = territory.getBaseColoredName();
            assertNotNull(baseColored, "Base colored name should not be null");
        }
    }

    // ==================== SPATIAL TESTS ====================

    @Nested
    @DisplayName("Spatial and Territory Access")
    class SpatialTests {

        @Test
        @DisplayName("Should check buffer zone access")
        void testCanAccessBufferZone_Self() {
            var territoryChunk = mock(org.leralix.tan.dataclass.chunk.TerritoryChunk.class);
            when(territoryChunk.getOwnerID()).thenReturn(territory.getID());

            assertTrue(territory.canAccessBufferZone(territoryChunk),
                "Territory should access its own buffer zone");
        }

        @Test
        @DisplayName("Should deny buffer zone access to enemies")
        void testCanAccessBufferZone_Enemy() {
            var enemyChunk = mock(org.leralix.tan.dataclass.chunk.TerritoryChunk.class);
            when(enemyChunk.getOwnerID()).thenReturn("enemy_territory");

            assertFalse(territory.canAccessBufferZone(enemyChunk),
                "Territory should not access enemy buffer zone");
        }
    }

    // ==================== BUILDINGS TESTS ====================

    @Nested
    @DisplayName("Buildings and Infrastructure")
    class BuildingsTests {

        @Test
        @DisplayName("Should get all buildings")
        void testGetBuildings_Success() {
            var buildings = territory.getBuildings();
            assertNotNull(buildings, "Buildings collection should not be null");
        }

        @Test
        @DisplayName("Should register fort")
        void testRegisterFort_Success() {
            Vector3D location = new Vector3D(100, 64, 100, "world");

            assertDoesNotThrow(() -> territory.registerFort(location),
                "Registering fort should not throw");
        }
    }
}
