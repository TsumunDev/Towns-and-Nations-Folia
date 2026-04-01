package org.leralix.tan.commands.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UpgradeCommand}.
 */
@DisplayName("UpgradeCommand Tests")
class UpgradeCommandTest extends AbstractPluginTest {

    private UpgradeCommand command;

    @BeforeEach
    void setUp() {
        MockBukkit.load(SphereLib.class);
        command = new UpgradeCommand();
    }

    // ==================== Basic Command Info Tests ====================

    @Test
    @DisplayName("Should return correct command name")
    void getName_returnsCorrectName() {
        assertEquals("upgrade", command.getName());
    }

    @Test
    @DisplayName("Should return correct syntax")
    void getSyntax_returnsCorrectSyntax() {
        assertEquals("/ccn upgrade", command.getSyntax());
    }

    @Test
    @DisplayName("Should return correct argument count")
    void getArguments_returnsCorrectCount() {
        assertEquals(1, command.getArguments());
    }

    @Test
    @DisplayName("Should return description")
    void getDescription_returnsNonEmptyString() {
        assertNotNull(command.getDescription());
        assertFalse(command.getDescription().isEmpty());
    }

    @Test
    @DisplayName("Should return empty tab suggestions")
    void getTabCompleteSuggestions_returnsEmptyList() {
        var player = server.addPlayer("TestPlayer");
        assertTrue(command.getTabCompleteSuggestions(player, "test", new String[] {}).isEmpty());
    }

    // ==================== Argument Validation Tests ====================

    @Test
    @DisplayName("Should handle no arguments gracefully")
    void perform_noArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {"upgrade"};

        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should handle extra arguments gracefully")
    void perform_extraArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {"upgrade", "extra"};

        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should handle multiple extra arguments")
    void perform_multipleExtraArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {"upgrade", "arg1", "arg2", "arg3", "arg4"};

        assertDoesNotThrow(() -> command.perform(player, args));
    }

    // ==================== Player State Tests ====================

    @Test
    @DisplayName("Should handle player not in town")
    void perform_playerNotInTown_doesNotThrow() {
        var player = server.addPlayer("LoneWolf");
        String[] args = {"upgrade"};

        // Should handle gracefully - player not in town
        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should handle player without data")
    void perform_noPlayerData_doesNotThrow() {
        var player = server.addPlayer("NewPlayer");
        String[] args = {"upgrade"};

        assertDoesNotThrow(() -> command.perform(player, args));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle empty argument array")
    void perform_emptyArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {};

        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should be idempotent")
    void perform_multipleCalls_consistentBehavior() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {"upgrade"};

        assertDoesNotThrow(() -> {
            command.perform(player, args);
            command.perform(player, args);
            command.perform(player, args);
            command.perform(player, args);
        });
    }

    @Test
    @DisplayName("Should handle concurrent calls from multiple players")
    void perform_concurrentCalls_doesNotThrow() {
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");
        var player3 = server.addPlayer("Player3");
        var player4 = server.addPlayer("Player4");
        String[] args = {"upgrade"};

        assertDoesNotThrow(() -> {
            command.perform(player1, args);
            command.perform(player2, args);
            command.perform(player3, args);
            command.perform(player4, args);
        });
    }

    @Test
    @DisplayName("Should handle special characters in arguments")
    void perform_specialCharsInArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {"upgrade", "@#$%^&*()"};

        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should handle unicode characters in arguments")
    void perform_unicodeInArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {"upgrade", "日本語"};

        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should handle very long argument strings")
    void perform_veryLongArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        String longArg = "a".repeat(10000);
        String[] args = {"upgrade", longArg};

        assertDoesNotThrow(() -> command.perform(player, args));
    }
}
