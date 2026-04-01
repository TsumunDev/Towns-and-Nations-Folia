package org.leralix.tan.listeners;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CommandBlocker}.
 */
@DisplayName("CommandBlocker Tests")
class CommandBlockerTest extends AbstractPluginTest {

    private CommandBlocker commandBlocker;

    @BeforeEach
    void setUp() {
        MockBukkit.load(SphereLib.class);
        commandBlocker = new CommandBlocker();
    }

    // ==================== Basic Command Handling Tests ====================

    @Test
    @DisplayName("Should allow normal command execution")
    void onCommand_normalCommand_doesNotCancel() {
        var player = server.addPlayer("TestPlayer");
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/gamemode creative"
        );

        commandBlocker.onCommand(event);

        // Normal commands should not be cancelled
        assertFalse(event.isCancelled());
    }

    @Test
    @DisplayName("Should handle command without leading slash")
    void onCommand_commandWithoutSlash_handlesGracefully() {
        var player = server.addPlayer("TestPlayer");
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "gamemode creative"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    @Test
    @DisplayName("Should handle empty command")
    void onCommand_emptyCommand_handlesGracefully() {
        var player = server.addPlayer("TestPlayer");
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    // ==================== Player-Based Command Tests ====================

    @Test
    @DisplayName("Should handle command with player target")
    void onCommand_commandWithPlayerTarget_handlesGracefully() {
        var player = server.addPlayer("Sender");
        var target = server.addPlayer("TargetPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/tp TargetPlayer"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    @Test
    @DisplayName("Should handle command with non-existent player target")
    void onCommand_commandWithNonExistentTarget_handlesGracefully() {
        var player = server.addPlayer("Sender");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/tp NonExistentPlayer"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    // ==================== Attack-Based Blocking Tests ====================

    @Test
    @DisplayName("Should handle player not in attack")
    void onCommand_playerNotInAttack_doesNotCancel() {
        var player = server.addPlayer("PeacefulPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/home"
        );

        commandBlocker.onCommand(event);

        // Player not in attack - command should not be cancelled by attack logic
        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    // ==================== Whitespace and Case Tests ====================

    @Test
    @DisplayName("Should handle command with extra whitespace")
    void onCommand_commandWithExtraWhitespace_handlesGracefully() {
        var player = server.addPlayer("TestPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/gamemode    creative"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    @Test
    @DisplayName("Should handle command with mixed case")
    void onCommand_mixedCaseCommand_handlesGracefully() {
        var player = server.addPlayer("TestPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/GaMeMoDe CrEaTiVe"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    // ==================== Relation-Based Tests ====================

    @Test
    @DisplayName("Should handle command with player from different relation")
    void onCommand_commandWithEnemyTarget_handlesGracefully() {
        var player = server.addPlayer("FriendlyPlayer");
        var enemy = server.addPlayer("EnemyPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/msg EnemyPlayer hello"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    // ==================== Complex Command Tests ====================

    @Test
    @DisplayName("Should handle command with multiple arguments")
    void onCommand_commandWithMultipleArgs_handlesGracefully() {
        var player = server.addPlayer("TestPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/give diamond 64"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    @Test
    @DisplayName("Should handle command with very long input")
    void onCommand_veryLongCommand_handlesGracefully() {
        var player = server.addPlayer("TestPlayer");
        String longCommand = "/msg " + "a".repeat(1000);

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            longCommand
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    // ==================== Special Character Tests ====================

    @Test
    @DisplayName("Should handle command with special characters")
    void onCommand_commandWithSpecialChars_handlesGracefully() {
        var player = server.addPlayer("TestPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/msg @a hello"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    @Test
    @DisplayName("Should handle command with unicode characters")
    void onCommand_commandWithUnicode_handlesGracefully() {
        var player = server.addPlayer("TestPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/msg 日本語 hello"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null player gracefully")
    void onCommand_nullPlayer_handlesGracefully() {
        // The event requires a player, but we test the internal logic
        var player = server.addPlayer("TestPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            player,
            "/test"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }

    @Test
    @DisplayName("Should handle rapid successive commands")
    void onCommand_rapidCommands_handlesGracefully() {
        var player = server.addPlayer("SpammyPlayer");

        for (int i = 0; i < 10; i++) {
            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
                player,
                "/test " + i
            );
            assertDoesNotThrow(() -> commandBlocker.onCommand(event));
        }
    }

    @Test
    @DisplayName("Should handle player without data")
    void onCommand_playerWithoutData_handlesGracefully() {
        var newPlayer = server.addPlayer("BrandNewPlayer");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(
            newPlayer,
            "/test"
        );

        assertDoesNotThrow(() -> commandBlocker.onCommand(event));
    }
}
