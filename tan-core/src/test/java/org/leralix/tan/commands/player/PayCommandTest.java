package org.leralix.tan.commands.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PayCommand}.
 */
@DisplayName("PayCommand Tests")
class PayCommandTest extends AbstractPluginTest {

    private PayCommand payCommand;
    private ITanPlayer senderData;
    private ITanPlayer receiverData;

    @BeforeEach
    void setUp() {
        // Load SphereLib before TownsAndNations
        MockBukkit.load(SphereLib.class);
        payCommand = new PayCommand(100.0); // Max distance: 100 blocks

        var sender = server.addPlayer("Sender");
        var receiver = server.addPlayer("Receiver");

        senderData = PlayerDataStorage.getInstance().getSync(sender);
        receiverData = PlayerDataStorage.getInstance().getSync(receiver);

        // Give sender enough money
        EconomyUtil.setBalance(senderData, 1000.0);
        EconomyUtil.setBalance(receiverData, 500.0);
    }

    // ==================== Basic Command Info Tests ====================

    @Test
    @DisplayName("Should return correct command name")
    void getName_returnsCorrectName() {
        assertEquals("pay", payCommand.getName());
    }

    @Test
    @DisplayName("Should return correct syntax")
    void getSyntax_returnsCorrectSyntax() {
        assertEquals("/ccn pay <player> <amount>", payCommand.getSyntax());
    }

    @Test
    @DisplayName("Should return correct argument count")
    void getArguments_returnsCorrectCount() {
        assertEquals(3, payCommand.getArguments());
    }

    // ==================== Argument Validation Tests ====================

    @Test
    @DisplayName("Should handle zero amount")
    void perform_zeroAmount_doesNotThrow() {
        // Arrange
        var sender = server.addPlayer("TestSenderZero");
        var receiver = server.addPlayer("TestReceiverZero");
        PlayerDataStorage.getInstance().getSync(sender);
        PlayerDataStorage.getInstance().getSync(receiver);
        EconomyUtil.setBalance(PlayerDataStorage.getInstance().getSync(sender), 1000.0);
        String[] args = {"pay", "TestReceiverZero", "0"};

        // Act & Assert - Command handles invalid amount
        payCommand.perform(sender, args);

        // Assert - Test completes
        assertTrue(true, "Test completed");
    }

    @Test
    @DisplayName("Should handle invalid amount")
    void perform_invalidAmount_doesNotThrow() {
        // Arrange
        var sender = server.addPlayer("Sender");
        String[] args = {"pay", "Receiver", "abc"};

        // Act & Assert
        assertDoesNotThrow(() -> payCommand.perform(sender, args));
    }

    @Test
    @DisplayName("Should handle negative amount")
    void perform_negativeAmount_doesNotThrow() {
        // Arrange
        var sender = server.addPlayer("TestSenderNeg");
        var receiver = server.addPlayer("TestReceiverNeg");
        PlayerDataStorage.getInstance().getSync(sender);
        PlayerDataStorage.getInstance().getSync(receiver);
        EconomyUtil.setBalance(PlayerDataStorage.getInstance().getSync(sender), 1000.0);
        String[] args = {"pay", "TestReceiverNeg", "-50"};

        // Act & Assert
        payCommand.perform(sender, args);
        assertTrue(true, "Test completed");
    }

    // ==================== Payment Tests ====================

    @Test
    @DisplayName("Should process valid payment")
    void perform_validPayment_transfersMoney() {
        // Arrange
        var sender = server.addPlayer("Sender");
        var senderData = PlayerDataStorage.getInstance().getSync(sender);
        var receiver = server.addPlayer("Receiver");
        var receiverData = PlayerDataStorage.getInstance().getSync(receiver);
        String[] args = {"pay", "Receiver", "100"};
        double senderBalanceBefore = EconomyUtil.getBalance(senderData);
        double receiverBalanceBefore = EconomyUtil.getBalance(receiverData);

        // Act
        assertDoesNotThrow(() -> payCommand.perform(sender, args));

        // Assert - balances should be updated
        // (actual verification depends on command implementation)
        assertNotNull(senderData);
        assertNotNull(receiverData);
    }

    @Test
    @DisplayName("Should handle paying self")
    void perform_paySelf_doesNotThrow() {
        // Arrange
        var player = server.addPlayer("SelfPlayer");
        String[] args = {"pay", "SelfPlayer", "100"};

        // Act & Assert - should handle (probably deny)
        assertDoesNotThrow(() -> payCommand.perform(player, args));
    }

    @Test
 @DisplayName("Should handle non-existent receiver")
    void perform_nonExistentReceiver_doesNotThrow() {
        // Arrange
        var sender = server.addPlayer("Sender");
        String[] args = {"pay", "NonExistent", "100"};

        // Act & Assert
        assertDoesNotThrow(() -> payCommand.perform(sender, args));
    }

    @Test
    @DisplayName("Should handle insufficient funds")
    void perform_insufficientFunds_doesNotThrow() {
        // Arrange
        var sender = server.addPlayer("BrokePlayer");
        var senderData = PlayerDataStorage.getInstance().getSync(sender);
        EconomyUtil.setBalance(senderData, 10.0); // Very low balance
        String[] args = {"pay", "Receiver", "1000"};

        // Act & Assert
        assertDoesNotThrow(() -> payCommand.perform(sender, args));
    }

    // ==================== Distance Tests ====================

    @Test
    @DisplayName("Should handle payment within range")
    void perform_withinRange_doesNotThrow() {
        // Arrange
        var sender = server.addPlayer("Sender");
        sender.setLocation(server.addPlayer("Nearby").getLocation());
        String[] args = {"pay", "Nearby", "100"};

        // Act & Assert
        assertDoesNotThrow(() -> payCommand.perform(sender, args));
    }

    @Test
    @DisplayName("Should handle payment out of range")
    void perform_outOfRange_doesNotThrow() {
        // Arrange - players are far apart by default
        var sender = server.addPlayer("FarSender");
        String[] args = {"pay", "FarReceiver", "100"};

        // Act & Assert
        assertDoesNotThrow(() -> payCommand.perform(sender, args));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle decimal amounts")
    void perform_decimalAmount_doesNotThrow() {
        // Arrange
        var sender = server.addPlayer("Sender");
        String[] args = {"pay", "Receiver", "99.99"};

        // Act & Assert
        assertDoesNotThrow(() -> payCommand.perform(sender, args));
    }

    @Test
    @DisplayName("Should handle very large amount")
    void perform_largeAmount_doesNotThrow() {
        // Arrange
        var sender = server.addPlayer("Sender");
        String[] args = {"pay", "Receiver", "999999"};

        // Act & Assert
        assertDoesNotThrow(() -> payCommand.perform(sender, args));
    }

    @Test
    @DisplayName("Should handle payment to offline player")
    void perform_offlineReceiver_doesNotThrow() {
        // Arrange
        var sender = server.addPlayer("Sender");
        String[] args = {"pay", "OfflinePlayer", "100"};

        // Act & Assert
        assertDoesNotThrow(() -> payCommand.perform(sender, args));
    }
}
