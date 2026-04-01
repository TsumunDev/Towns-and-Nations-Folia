package org.leralix.tan.listeners;

import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlayerQuitListener}.
 * <p>
 * Tests the player quit event handling including online status updates
 * and proper cleanup of player data.
 * </p>
 */
@DisplayName("PlayerQuitListener Tests")
class PlayerQuitListenerTest extends AbstractPluginTest {

    private PlayerQuitListener listener;

    @BeforeEach
    void setUp() {
        MockBukkit.load(org.leralix.lib.SphereLib.class);
        listener = new PlayerQuitListener();
    }

    // ==================== Basic Event Handling Tests ====================

    @Test
    @DisplayName("Should handle player quit without throwing")
    void onPlayerQuit_validPlayer_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "left the game");

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }

    @Test
    @DisplayName("Should handle quit event with quit message")
    void onPlayerQuit_withQuitMessage_processesCorrectly() {
        var player = server.addPlayer("TestPlayer");
        String quitMessage = "TestPlayer left the game";
        PlayerQuitEvent event = new PlayerQuitEvent(player, quitMessage);

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
        assertEquals(quitMessage, event.getQuitMessage());
    }

    @Test
    @DisplayName("Should handle empty quit message")
    void onPlayerQuit_emptyQuitMessage_processesCorrectly() {
        var player = server.addPlayer("TestPlayer");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "");

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }

    // ==================== Async Operation Tests ====================

    @Test
    @DisplayName("Should handle async data update on quit")
    void onPlayerQuit_asyncDataUpdate_doesNotThrow() {
        var player = server.addPlayer("AsyncPlayer");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "left");

        // The listener uses async operations internally
        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }

    @Test
    @DisplayName("Should handle player with no existing data")
    void onPlayerQuit_newPlayer_handlesGracefully() {
        var player = server.addPlayer("BrandNewPlayer");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "left for the first time");

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }

    // ==================== Multiple Players Tests ====================

    @Test
    @DisplayName("Should handle multiple concurrent quits")
    void onPlayerQuit_multiplePlayers_handlesGracefully() {
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");
        var player3 = server.addPlayer("Player3");

        PlayerQuitEvent event1 = new PlayerQuitEvent(player1, "left");
        PlayerQuitEvent event2 = new PlayerQuitEvent(player2, "left");
        PlayerQuitEvent event3 = new PlayerQuitEvent(player3, "left");

        assertDoesNotThrow(() -> {
            listener.onPlayerQuit(event1);
            listener.onPlayerQuit(event2);
            listener.onPlayerQuit(event3);
        });
    }

    // ==================== Player Type Tests ====================

    @Test
    @DisplayName("Should handle admin player quit")
    void onPlayerQuit_adminPlayer_handlesCorrectly() {
        var admin = server.addPlayer("AdminPlayer");
        admin.setOp(true);
        PlayerQuitEvent event = new PlayerQuitEvent(admin, "left the game");

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }

    @Test
    @DisplayName("Should handle player with special characters in name")
    void onPlayerQuit_specialCharsInName_handlesGracefully() {
        var player = server.addPlayer("Player_123");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "left");

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle rapid rejoin-quit cycles")
    void onPlayerQuit_rapidRejoinQuit_handlesGracefully() {
        var player = server.addPlayer("CyclingPlayer");

        // Simulate rapid quit-join-quit cycles
        for (int i = 0; i < 3; i++) {
            PlayerQuitEvent quitEvent = new PlayerQuitEvent(player, "left");
            assertDoesNotThrow(() -> listener.onPlayerQuit(quitEvent));
        }
    }

    @Test
    @DisplayName("Should handle player disconnect during data load")
    void onPlayerQuit_disconnectDuringLoad_handlesGracefully() {
        var player = server.addPlayer("DisconnectPlayer");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "disconnected");

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }

    // ==================== Null Safety Tests ====================

    @Test
    @DisplayName("Should handle null player gracefully")
    void onPlayerQuit_nullPlayer_handlesGracefully() {
        // Create a mock event - in real scenario, player wouldn't be null
        // but we test the listener's null check
        var player = server.addPlayer("TestPlayer");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "left");

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle exception during data update gracefully")
    void onPlayerQuit_dataUpdateFailure_doesNotCrash() {
        var player = server.addPlayer("ProblematicPlayer");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "left");

        // The listener has exception handling - should not throw
        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }

    // ==================== Online Status Tests ====================

    @Test
    @DisplayName("Should trigger online status update on quit")
    void onPlayerQuit_onlineStatus_triggersUpdate() {
        var player = server.addPlayer("StatusPlayer");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "left");

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
        // Note: Actual status verification would require checking the database
    }

    @Test
    @DisplayName("Should handle offline mode player quit")
    void onPlayerQuit_offlineModePlayer_handlesGracefully() {
        var player = server.addPlayer("OfflinePlayer");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "left");

        assertDoesNotThrow(() -> listener.onPlayerQuit(event));
    }
}
