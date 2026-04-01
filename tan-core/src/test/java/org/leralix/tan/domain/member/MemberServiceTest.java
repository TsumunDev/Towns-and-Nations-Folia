package org.leralix.tan.domain.member;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.RankData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for MemberService.
 *
 * <p>Tests the new member service that extracts member logic from TownData.</p>
 *
 * @since 2.0.0
 */
@DisplayName("MemberService Tests")
class MemberServiceTest {

    private MemberService service;
    private TownDataStorage mockTownStorage;

    @BeforeEach
    void setUp() {
        // Create mock storage instance
        mockTownStorage = mock(TownDataStorage.class);

        // Create the service with mocked dependency
        service = new MemberServiceImpl(mockTownStorage);
    }

    @AfterEach
    void tearDown() {
        // Reset holder to avoid polluting other tests
        MemberHolder.clear();
    }

    // ===== Player Management Tests =====

    @Test
    @DisplayName("addPlayer should add player to town")
    void testAddPlayer() throws Exception {
        // Arrange
        String townId = "town_test";
        ITanPlayer mockPlayer = mock(ITanPlayer.class);
        TownData mockTown = mock(TownData.class);

        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        service.addPlayer(townId, mockPlayer).get();

        // Assert
        verify(mockTown).addPlayer(mockPlayer);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("addPlayer should handle null town gracefully")
    void testAddPlayerNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        ITanPlayer mockPlayer = mock(ITanPlayer.class);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            service.addPlayer(townId, mockPlayer).get();
        });
    }

    @Test
    @DisplayName("removePlayer should remove player from town by ID")
    void testRemovePlayerById() throws Exception {
        // Arrange
        String townId = "town_test";
        String playerId = "player_123";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        service.removePlayer(townId, playerId).get();

        // Assert
        verify(mockTown).removePlayer(playerId);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("removePlayer should remove player from town")
    void testRemovePlayer() throws Exception {
        // Arrange
        String townId = "town_test";
        ITanPlayer mockPlayer = mock(ITanPlayer.class);
        TownData mockTown = mock(TownData.class);

        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        service.removePlayer(townId, mockPlayer).get();

        // Assert
        verify(mockTown).removePlayer(mockPlayer);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("getPlayerIDs should return player ID list")
    void testGetPlayerIDs() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);
        Collection<String> playerIds = List.of("player1", "player2", "player3");

        when(mockTown.getPlayerIDList()).thenReturn(playerIds);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Collection<String> result = service.getPlayerIDs(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("player1"));
        assertTrue(result.contains("player2"));
        assertTrue(result.contains("player3"));
    }

    @Test
    @DisplayName("getPlayerIDs should return empty for null town")
    void testGetPlayerIDsNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Collection<String> result = service.getPlayerIDs(townId).get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getPlayerCount should return correct count")
    void testGetPlayerCount() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getITanPlayerList()).thenReturn(List.of(mock(ITanPlayer.class), mock(ITanPlayer.class), mock(ITanPlayer.class), mock(ITanPlayer.class), mock(ITanPlayer.class)));
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        int result = service.getPlayerCount(townId).get();

        // Assert
        assertEquals(5, result);
    }

    @Test
    @DisplayName("isMember should return true when player is member")
    void testIsMemberTrue() throws Exception {
        // Arrange
        String townId = "town_test";
        String playerId = "member_player";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getPlayerIDList()).thenReturn(List.of("member_player", "other_player"));
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        boolean result = service.isMember(townId, playerId).get();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("isMember should return false when player is not member")
    void testIsMemberFalse() throws Exception {
        // Arrange
        String townId = "town_test";
        String playerId = "non_member";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getPlayerIDList()).thenReturn(List.of("member_player", "other_player"));
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        boolean result = service.isMember(townId, playerId).get();

        // Assert
        assertFalse(result);
    }

    // ===== Join Request Tests =====

    @Test
    @DisplayName("addJoinRequest should add request for player")
    void testAddJoinRequest() throws Exception {
        // Arrange
        String townId = "town_test";
        Player mockPlayer = mock(Player.class);
        TownData mockTown = mock(TownData.class);

        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        service.addJoinRequest(townId, mockPlayer).get();

        // Assert
        verify(mockTown).addPlayerJoinRequest(mockPlayer);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("removeJoinRequest should remove request")
    void testRemoveJoinRequest() throws Exception {
        // Arrange
        String townId = "town_test";
        Player mockPlayer = mock(Player.class);
        TownData mockTown = mock(TownData.class);

        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        service.removeJoinRequest(townId, mockPlayer).get();

        // Assert
        verify(mockTown).removePlayerJoinRequest(mockPlayer);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("hasJoinRequest should return true when request exists")
    void testHasJoinRequest() throws Exception {
        // Arrange
        String townId = "town_test";
        String playerUuid = "requesting_player";
        TownData mockTown = mock(TownData.class);

        when(mockTown.isPlayerAlreadyRequested(playerUuid)).thenReturn(true);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        boolean result = service.hasJoinRequest(townId, playerUuid).get();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("getJoinRequests should return request set")
    void testGetJoinRequests() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);
        Set<String> requests = Set.of("player1", "player2");

        when(mockTown.getPlayerJoinRequestSet()).thenReturn(requests);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Set<String> result = service.getJoinRequests(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ===== Leader Tests =====

    @Test
    @DisplayName("getLeaderId should return leader ID")
    void testGetLeaderId() throws Exception {
        // Arrange
        String townId = "town_test";
        String leaderId = "leader_player";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getLeaderID()).thenReturn(leaderId);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        String result = service.getLeaderId(townId).get();

        // Assert
        assertEquals(leaderId, result);
    }

    @Test
    @DisplayName("getLeaderId should return null for null town")
    void testGetLeaderIdNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        String result = service.getLeaderId(townId).get();

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("setLeader should update town leader")
    void testSetLeader() throws Exception {
        // Arrange
        String townId = "town_test";
        String newLeaderId = "new_leader";
        TownData mockTown = mock(TownData.class);

        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        service.setLeader(townId, newLeaderId).get();

        // Assert
        verify(mockTown).setLeaderID(newLeaderId);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("isLeader should return true when player is leader")
    void testIsLeader() throws Exception {
        // Arrange
        String townId = "town_test";
        String playerId = "leader_player";
        TownData mockTown = mock(TownData.class);

        when(mockTown.isLeader(playerId)).thenReturn(true);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        boolean result = service.isLeader(townId, playerId).get();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("hasNoLeader should return false when town has leader")
    void testHasNoLeaderFalse() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        when(mockTown.haveNoLeader()).thenReturn(false);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        boolean result = service.hasNoLeader(townId).get();

        // Assert
        assertFalse(result);
    }

    // ===== Rank Tests =====

    @Test
    @DisplayName("getRank should return player rank")
    void testGetRank() throws Exception {
        // Arrange
        String townId = "town_test";
        ITanPlayer mockPlayer = mock(ITanPlayer.class);
        RankData mockRank = mock(RankData.class);
        TownData mockTown = mock(TownData.class);

        when(mockTown.getRank(mockPlayer)).thenReturn(mockRank);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        RankData result = service.getRank(townId, mockPlayer).get();

        // Assert
        assertNotNull(result);
        assertEquals(mockRank, result);
    }

    @Test
    @DisplayName("getDefaultRank should return default rank")
    void testGetDefaultRank() throws Exception {
        // Arrange
        String townId = "town_test";
        RankData mockRank = mock(RankData.class);
        TownData mockTown = mock(TownData.class);

        when(mockTown.getTownDefaultRank()).thenReturn(mockRank);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        RankData result = service.getDefaultRank(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(mockRank, result);
    }

    // ===== Kick Tests =====

    @Test
    @DisplayName("kickPlayer should kick player from town")
    void testKickPlayer() throws Exception {
        // Arrange
        String townId = "town_test";
        OfflinePlayer mockPlayer = mock(OfflinePlayer.class);
        TownData mockTown = mock(TownData.class);

        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));
        when(mockTown.kickPlayerAsync(mockPlayer))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        service.kickPlayer(townId, mockPlayer).get();

        // Assert
        verify(mockTown).kickPlayerAsync(mockPlayer);
    }

    // ===== Holder Tests =====

    @Test
    @DisplayName("MemberHolder should return service instance")
    void testMemberHolder() {
        // Arrange
        MemberHolder.setService(service);

        // Act
        MemberService retrieved = MemberHolder.getService();

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
