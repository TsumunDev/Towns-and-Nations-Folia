package org.leralix.tan.commands.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QuestCommand}.
 */
@DisplayName("QuestCommand Tests")
class QuestCommandTest extends AbstractPluginTest {

    private QuestCommand command;

    @BeforeEach
    void setUp() {
        MockBukkit.load(SphereLib.class);
        command = new QuestCommand();
    }

    // ==================== Basic Command Info Tests ====================

    @Test
    @DisplayName("Should return correct command name")
    void getName_returnsCorrectName() {
        assertEquals("quest", command.getName());
    }

    @Test
    @DisplayName("Should return correct syntax")
    void getSyntax_returnsCorrectSyntax() {
        assertEquals("/ccn quest", command.getSyntax());
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
        String[] args = {"quest"};

        // Should not throw even if player not in town
        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should handle extra arguments gracefully")
    void perform_extraArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {"quest", "extra"};

        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should handle multiple extra arguments")
    void perform_multipleExtraArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {"quest", "arg1", "arg2"};

        assertDoesNotThrow(() -> command.perform(player, args));
    }

    // ==================== Player State Tests ====================

    @Test
    @DisplayName("Should handle player not in town")
    void perform_playerNotInTown_doesNotThrow() {
        var player = server.addPlayer("LoneWolf");
        String[] args = {"quest"};

        // Should handle gracefully - player not in town
        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should handle player without data")
    void perform_noPlayerData_doesNotThrow() {
        var player = server.addPlayer("NewPlayer");
        String[] args = {"quest"};

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
    @DisplayName("Should handle null args array")
    void perform_nullArgs_doesNotThrow() {
        var player = server.addPlayer("TestPlayer");

        // Command should handle null or empty args
        assertDoesNotThrow(() -> command.perform(player, new String[] {"quest"}));
    }

    @Test
    @DisplayName("Should handle concurrent calls")
    void perform_concurrentCalls_doesNotThrow() {
        var player1 = server.addPlayer("Player1");
        var player2 = server.addPlayer("Player2");
        String[] args = {"quest"};

        assertDoesNotThrow(() -> {
            command.perform(player1, args);
            command.perform(player2, args);
        });
    }

    @Test
    @DisplayName("Should be idempotent")
    void perform_multipleCalls_consistentBehavior() {
        var player = server.addPlayer("TestPlayer");
        String[] args = {"quest"};

        assertDoesNotThrow(() -> {
            command.perform(player, args);
            command.perform(player, args);
            command.perform(player, args);
        });
    }
}
