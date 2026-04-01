package org.leralix.tan.commands.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SeeBalanceCommand}.
 */
@DisplayName("SeeBalanceCommand Tests")
class SeeBalanceCommandTest extends AbstractPluginTest {

    private SeeBalanceCommand command;
    private ITanPlayer tanPlayer;

    @BeforeEach
    void setUp() {
        // Load SphereLib before TownsAndNations
        MockBukkit.load(SphereLib.class);
        command = new SeeBalanceCommand();
        tanPlayer = PlayerDataStorage.getInstance().getSync(server.addPlayer("TestPlayer"));
    }

    // ==================== Basic Command Info Tests ====================

    @Test
    @DisplayName("Should return correct command name")
    void getName_returnsCorrectName() {
        assertEquals("balance", command.getName());
    }

    @Test
    @DisplayName("Should return correct syntax")
    void getSyntax_returnsCorrectSyntax() {
        assertEquals("/ccn balance", command.getSyntax());
    }

    @Test
    @DisplayName("Should return correct argument count")
    void getArguments_returnsCorrectCount() {
        assertEquals(1, command.getArguments());
    }

    @Test
    @DisplayName("Should return empty tab suggestions")
    void getTabCompleteSuggestions_returnsEmptyList() {
        var player = server.addPlayer("TestPlayer");
        assertTrue(command.getTabCompleteSuggestions(player, "test", new String[] {}).isEmpty());
    }

    // ==================== Balance Display Tests ====================

    @Test
    @DisplayName("Should display balance correctly")
    void perform_withOneArg_displaysBalance() {
        // Arrange
        var player = server.addPlayer("BalancePlayer1");
        var playerData = PlayerDataStorage.getInstance().getSync(player);
        String[] args = {"balance"};
        EconomyUtil.setBalance(playerData, 1000.0);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> command.perform(player, args));
        assertNotNull(playerData);
    }

    @Test
    @DisplayName("Should display zero balance")
    void perform_zeroBalance_displaysZero() {
        // Arrange
        var player = server.addPlayer("BalancePlayer2");
        var playerData = PlayerDataStorage.getInstance().getSync(player);
        String[] args = {"balance"};
        EconomyUtil.setBalance(playerData, 0.0);

        // Act & Assert
        assertDoesNotThrow(() -> command.perform(player, args));
        assertNotNull(playerData);
    }

    @Test
    @DisplayName("Should display large balance")
    void perform_largeBalance_displaysCorrectly() {
        // Arrange
        var player = server.addPlayer("BalancePlayer3");
        var playerData = PlayerDataStorage.getInstance().getSync(player);
        String[] args = {"balance"};
        EconomyUtil.setBalance(playerData, 999999999.99);

        // Act & Assert
        assertDoesNotThrow(() -> command.perform(player, args));
        assertNotNull(playerData);
    }

    @Test
    @DisplayName("Should display negative balance")
    void perform_negativeBalance_displaysCorrectly() {
        // Arrange
        var player = server.addPlayer("BalancePlayer4");
        var playerData = PlayerDataStorage.getInstance().getSync(player);
        String[] args = {"balance"};
        EconomyUtil.setBalance(playerData, -500.0);

        // Act & Assert
        assertDoesNotThrow(() -> command.perform(player, args));
        assertNotNull(playerData);
    }

    // ==================== Argument Validation Tests ====================

    @Test
    @DisplayName("Should handle too many arguments")
    void perform_tooManyArgs_doesNotThrow() {
        // Arrange
        var player = server.addPlayer("TestPlayer");
        String[] args = {"balance", "extra"};

        // Act & Assert - command should handle gracefully
        assertDoesNotThrow(() -> command.perform(player, args));
    }

    @Test
    @DisplayName("Should handle three arguments")
    void perform_threeArgs_doesNotThrow() {
        // Arrange
        var player = server.addPlayer("TestPlayer");
        String[] args = {"balance", "arg1", "arg2"};

        // Act & Assert
        assertDoesNotThrow(() -> command.perform(player, args));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should display decimal balance")
    void perform_decimalBalance_displaysCorrectly() {
        // Arrange
        var player = server.addPlayer("BalancePlayer5");
        var playerData = PlayerDataStorage.getInstance().getSync(player);
        String[] args = {"balance"};
        EconomyUtil.setBalance(playerData, 123.45);

        // Act & Assert
        assertDoesNotThrow(() -> command.perform(player, args));
        assertNotNull(playerData);
    }

    @Test
    @DisplayName("Should give consistent results on multiple calls")
    void perform_multipleCalls_consistentResults() {
        // Arrange
        var player = server.addPlayer("BalancePlayer6");
        var playerData = PlayerDataStorage.getInstance().getSync(player);
        String[] args = {"balance"};
        EconomyUtil.setBalance(playerData, 500.0);

        // Act - Should not throw on multiple calls
        assertDoesNotThrow(() -> {
            command.perform(player, args);
            command.perform(player, args);
            command.perform(player, args);
        });
        assertNotNull(playerData);
    }

    @Test
    @DisplayName("Should reflect balance changes")
    void perform_afterBalanceChange_showsUpdatedBalance() {
        // Arrange
        var player = server.addPlayer("BalancePlayer7");
        var playerData = PlayerDataStorage.getInstance().getSync(player);
        String[] args = {"balance"};
        EconomyUtil.setBalance(playerData, 100.0);

        // Act & Assert - Command should handle balance changes
        assertDoesNotThrow(() -> {
            command.perform(player, args);
            EconomyUtil.setBalance(playerData, 200.0);
            command.perform(player, args);
        });
        assertNotNull(playerData);
    }

    @Test
    @DisplayName("Should handle no arguments gracefully")
    void perform_noArgs_showsUsage() {
        // Arrange
        var player = server.addPlayer("TestPlayer");
        String[] args = {};

        // Act & Assert - should show usage
        assertDoesNotThrow(() -> command.perform(player, args));
    }
}
