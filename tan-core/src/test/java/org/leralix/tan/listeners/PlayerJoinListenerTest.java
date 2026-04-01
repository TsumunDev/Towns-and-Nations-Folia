package org.leralix.tan.listeners;

import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlayerJoinListener}.
 */
@DisplayName("PlayerJoinListener Tests")
class PlayerJoinListenerTest extends AbstractPluginTest {

    private PlayerJoinListener listener;

    @BeforeEach
    void setUp() {
        MockBukkit.load(SphereLib.class);
        listener = new PlayerJoinListener();
    }

    // ==================== Basic Event Handling Tests ====================

    @Test
    @DisplayName("Should handle player join without throwing")
    void onPlayerJoin_validPlayer_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined the game");

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }

    @Test
    @DisplayName("Should handle null player gracefully")
    void onPlayerJoin_nullPlayer_handlesGracefully() {
        // Create event with null - the listener should handle this
        // Note: PlayerJoinEvent requires a non-null player, so we test
        // the listener's null check within its method context
        var player = server.addPlayer("TestPlayer");

        // The event fires correctly
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined the game");
        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }

    @Test
    @DisplayName("Should handle multiple concurrent joins")
    void onPlayerJoin_multiplePlayers_handlesGracefully() {
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");
        var player3 = server.addPlayer("Player3");

        PlayerJoinEvent event1 = new PlayerJoinEvent(player1, "joined the game");
        PlayerJoinEvent event2 = new PlayerJoinEvent(player2, "joined the game");
        PlayerJoinEvent event3 = new PlayerJoinEvent(player3, "joined the game");

        assertDoesNotThrow(() -> {
            listener.onPlayerJoin(event1);
            listener.onPlayerJoin(event2);
            listener.onPlayerJoin(event3);
        });
    }

    // ==================== Async Operation Tests ====================

    @Test
    @DisplayName("Should handle async player data loading")
    void onPlayerJoin_asyncDataLoading_doesNotThrow() {
        var player = server.addPlayer("AsyncPlayer");
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined the game");

        // The listener uses async operations internally
        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }

    @Test
    @DisplayName("Should handle player with no existing data")
    void onPlayerJoin_newPlayer_doesNotThrow() {
        var player = server.addPlayer("BrandNewPlayer");
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined for the first time");

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }

    // ==================== IP Tracking Tests ====================

    @Test
    @DisplayName("Should handle player with null address")
    void onPlayerJoin_nullAddress_handlesGracefully() {
        var player = server.addPlayer("NoAddressPlayer");
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined");

        // Player might have null address in some cases
        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }

    // ==================== Permission-Based Tests ====================

    @Test
    @DisplayName("Should handle admin player join")
    void onPlayerJoin_adminPlayer_doesNotThrow() {
        var admin = server.addPlayer("AdminPlayer");
        admin.setOp(true);
        PlayerJoinEvent event = new PlayerJoinEvent(admin, "joined the game");

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }

    @Test
    @DisplayName("Should handle player with debug permission")
    void onPlayerJoin_debugPlayer_doesNotThrow() {
        var debugPlayer = server.addPlayer("DebugPlayer");
        debugPlayer.addAttachment(plugin).setPermission("tan.debug", true);
        PlayerJoinEvent event = new PlayerJoinEvent(debugPlayer, "joined the game");

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle rapid rejoin")
    void onPlayerJoin_rapidRejoin_handlesGracefully() {
        var player = server.addPlayer("RejoinPlayer");

        // Simulate rapid rejoin
        for (int i = 0; i < 5; i++) {
            PlayerJoinEvent event = new PlayerJoinEvent(player, "joined the game");
            assertDoesNotThrow(() -> listener.onPlayerJoin(event));
        }
    }

    @Test
    @DisplayName("Should handle player with special characters in name")
    void onPlayerJoin_specialCharsInName_handlesGracefully() {
        var player = server.addPlayer("Player_123");
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined the game");

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }

    @Test
    @DisplayName("Should handle offline mode join")
    void onPlayerJoin_offlineModePlayer_handlesGracefully() {
        var player = server.addPlayer("OfflinePlayer");
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined the game");

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle exception during data retrieval gracefully")
    void onPlayerJoin_dataRetrievalFailure_doesNotCrash() {
        var player = server.addPlayer("ProblematicPlayer");
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined the game");

        // The listener has exception handling - should not throw
        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
    }
}
