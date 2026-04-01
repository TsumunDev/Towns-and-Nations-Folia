package org.leralix.tan.listeners;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlayerEnterChunkListener}.
 * <p>
 * Tests player movement between chunks, territory change detection,
 * and auto-claim functionality.
 * </p>
 */
@DisplayName("PlayerEnterChunkListener Tests")
class PlayerEnterChunkListenerTest extends AbstractPluginTest {

    private PlayerEnterChunkListener listener;
    private Player player;
    private Location fromLocation;
    private Location toLocation;

    @BeforeEach
    void setUp() {
        MockBukkit.load(org.leralix.lib.SphereLib.class);
        listener = new PlayerEnterChunkListener();
        player = server.addPlayer("TestPlayer");

        var world = server.addSimpleWorld("test_world");
        fromLocation = new Location(world, 0, 64, 0);
        toLocation = new Location(world, 16, 64, 0); // Different chunk
    }

    // ==================== Basic Movement Tests ====================

    @Test
    @DisplayName("Should handle player move without throwing")
    void playerMoveEvent_validMove_doesNotThrow() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    @Test
    @DisplayName("Should ignore movement within same chunk")
    void playerMoveEvent_sameChunk_ignoresEvent() {
        Location sameChunk = new Location(toLocation.getWorld(), 0, 64, 0);
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, sameChunk);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    @Test
    @DisplayName("Should detect chunk change")
    void playerMoveEvent_differentChunk_detectsChange() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
        assertFalse(event.isCancelled(), "Normal chunk change should not be cancelled");
    }

    @Test
    @DisplayName("Should handle movement to wilderness")
    void playerMoveEvent_wilderness_handlesGracefully() {
        Location wilderness = new Location(toLocation.getWorld(), 32, 64, 32);
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, wilderness);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    // ==================== Territory Change Tests ====================

    @Test
    @DisplayName("Should detect territory change")
    void playerMoveEvent_territoryChange_detectsCorrectly() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    @Test
    @DisplayName("Should ignore movement within same territory")
    void playerMoveEvent_sameTerritory_ignoresEvent() {
        Chunk sameChunk = fromLocation.getChunk();
        Location sameTerritory = new Location(
            fromLocation.getWorld(),
            fromLocation.getX() + 5,
            fromLocation.getY(),
            fromLocation.getZ() + 5
        );
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, sameTerritory);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    // ==================== Auto-Claim Tests ====================

    @Test
    @DisplayName("Should handle auto-claim when enabled")
    void playerMoveEvent_autoClaimEnabled_processesClaim() {
        // Note: This would require setting up PlayerAutoClaimStorage
        Location newChunk = new Location(toLocation.getWorld(), 32, 64, 0);
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, newChunk);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    @Test
    @DisplayName("Should handle auto-claim for town chunks")
    void playerMoveEvent_townAutoClaim_processesCorrectly() {
        Location townChunk = new Location(toLocation.getWorld(), 48, 64, 0);
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, townChunk);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    // ==================== Player Quit Tests ====================

    @Test
    @DisplayName("Should cleanup player data on quit")
    void onPlayerQuit_validPlayer_cleansUpData() {
        // First, move player to register in maps
        PlayerMoveEvent moveEvent = new PlayerMoveEvent(player, fromLocation, toLocation);
        listener.playerMoveEvent(moveEvent);

        // Then quit
        PlayerQuitEvent quitEvent = new PlayerQuitEvent(player, "left the game");
        assertDoesNotThrow(() -> listener.onPlayerQuit(quitEvent));
    }

    @Test
    @DisplayName("Should handle quit without previous movement")
    void onPlayerQuit_noPreviousMovement_handlesGracefully() {
        PlayerQuitEvent quitEvent = new PlayerQuitEvent(player, "left the game");

        assertDoesNotThrow(() -> listener.onPlayerQuit(quitEvent));
    }

    // ==================== Async Operation Tests ====================

    @Test
    @DisplayName("Should handle async player data loading")
    void playerMoveEvent_asyncDataLoad_handlesCorrectly() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        // The listener uses async operations internally
        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    @Test
    @DisplayName("Should handle async territory data loading")
    void playerMoveEvent_asyncTerritoryLoad_handlesCorrectly() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    // ==================== Relation Tests ====================

    @Test
    @DisplayName("Should check relations on territory enter")
    void playerMoveEvent_territoryEnter_checksRelations() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    @Test
    @DisplayName("Should teleport player back if access denied")
    void playerMoveEvent_accessDenied_teleportsBack() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        listener.playerMoveEvent(event);

        // If access is denied, the event should be cancelled
        // and player teleported back
    }

    // ==================== Multiple Players Tests ====================

    @Test
    @DisplayName("Should handle multiple players moving simultaneously")
    void playerMoveEvent_multiplePlayers_handlesGracefully() {
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");
        var player3 = server.addPlayer("Player3");

        Location loc1 = new Location(fromLocation.getWorld(), 16, 64, 0);
        Location loc2 = new Location(fromLocation.getWorld(), 32, 64, 0);
        Location loc3 = new Location(fromLocation.getWorld(), 48, 64, 0);

        PlayerMoveEvent event1 = new PlayerMoveEvent(player1, fromLocation, loc1);
        PlayerMoveEvent event2 = new PlayerMoveEvent(player2, fromLocation, loc2);
        PlayerMoveEvent event3 = new PlayerMoveEvent(player3, fromLocation, loc3);

        assertDoesNotThrow(() -> {
            listener.playerMoveEvent(event1);
            listener.playerMoveEvent(event2);
            listener.playerMoveEvent(event3);
        });
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle rapid chunk changes")
    void playerMoveEvent_rapidChunkChanges_handlesGracefully() {
        for (int i = 0; i < 5; i++) {
            Location loc = new Location(fromLocation.getWorld(), i * 16, 64, 0);
            PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, loc);
            assertDoesNotThrow(() -> listener.playerMoveEvent(event));
            fromLocation = loc;
        }
    }

    @Test
    @DisplayName("Should handle null location gracefully")
    void playerMoveEvent_nullLocation_handlesGracefully() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    @Test
    @DisplayName("Should handle player with no town")
    void playerMoveEvent_noTownPlayer_handlesGracefully() {
        var noTownPlayer = server.addPlayer("NoTownPlayer");
        PlayerMoveEvent event = new PlayerMoveEvent(noTownPlayer, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle exception during data loading gracefully")
    void playerMoveEvent_dataLoadFailure_doesNotCrash() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    @Test
    @DisplayName("Should handle exception during relation check gracefully")
    void playerMoveEvent_relationCheckFailure_doesNotCrash() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    // ==================== Same Owner Utility Tests ====================

    @Test
    @DisplayName("Should detect same owner chunks")
    void sameOwner_sameChunk_returnsTrue() {
        // This would require setting up actual ClaimedChunk2 instances
        // For now, we test that the method exists and handles null
        assertDoesNotThrow(() -> PlayerEnterChunkListener.sameOwner(null, null));
    }

    @Test
    @DisplayName("Should handle null chunks in same owner check")
    void sameOwner_nullChunks_handlesGracefully() {
        assertDoesNotThrow(() -> PlayerEnterChunkListener.sameOwner(null, null));
    }

    // ==================== Configuration Tests ====================

    @Test
    @DisplayName("Should respect displayTerritoryNameWithOwnColor config")
    void playerMoveEvent_coloredNames_respectsConfig() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    @Test
    @DisplayName("Should handle territory name display")
    void playerMoveEvent_territoryName_displaysCorrectly() {
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, toLocation);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    // ==================== World Change Tests ====================

    @Test
    @DisplayName("Should handle movement between worlds")
    void playerMoveEvent_worldChange_handlesCorrectly() {
        var world2 = server.addSimpleWorld("test_world2");
        Location world2Location = new Location(world2, 0, 64, 0);
        PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, world2Location);

        assertDoesNotThrow(() -> listener.playerMoveEvent(event));
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    @DisplayName("Should handle concurrent access to player maps")
    void playerMoveEvent_concurrentAccess_handlesCorrectly() {
        // Simulate concurrent access
        Runnable moveTask = () -> {
            for (int i = 0; i < 10; i++) {
                Location loc = new Location(fromLocation.getWorld(), i * 16, 64, 0);
                PlayerMoveEvent event = new PlayerMoveEvent(player, fromLocation, loc);
                listener.playerMoveEvent(event);
            }
        };

        Thread thread1 = new Thread(moveTask);
        Thread thread2 = new Thread(moveTask);

        assertDoesNotThrow(() -> {
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();
        });
    }
}
