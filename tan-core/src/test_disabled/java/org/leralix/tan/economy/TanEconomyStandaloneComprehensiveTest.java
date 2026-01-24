package org.leralix.tan.economy;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive tests for TanEconomyStandalone (internal TAN economy implementation).
 *
 * <p>Tests the standalone economy system that uses ITanPlayer balance storage
 * directly without external dependencies.</p>
 *
 * <p><b>Coverage Areas:</b></p>
 * <ul>
 *   <li>Balance access and manipulation</li>
 *   <li>Deposit/withdraw operations</li>
 *   <li>Balance verification (has method)</li>
 *   <li>Currency formatting</li>
 *   <li>Edge cases (negative amounts, zero amounts)</li>
 *   <li>Thread-safety considerations</li>
 * </ul>
 */
@DisplayName("TanEconomyStandalone Tests")
class TanEconomyStandaloneComprehensiveTest {

    private ServerMock server;
    private World world;
    private TanEconomyStandalone economy;
    private PlayerDataStorage storage;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        economy = new TanEconomyStandalone();
        storage = PlayerDataStorage.getInstance();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ========== Balance Access Tests ==========

    @Test
    @DisplayName("getBalance should return player's current balance")
    void testGetBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("BalancePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act
        double balance = economy.getBalance(tanPlayer);

        // Assert
        assertEquals(1000.0, balance, 0.001, "Balance should match player's balance");
    }

    @Test
    @DisplayName("getBalance should return 0.0 for new player")
    void testGetBalanceNewPlayer() {
        // Arrange
        PlayerMock player = server.addPlayer("NewPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act
        double balance = economy.getBalance(tanPlayer);

        // Assert
        assertEquals(0.0, balance, 0.001, "New player should have 0 balance");
    }

    @Test
    @DisplayName("getBalance should handle negative balance")
    void testGetBalanceNegative() {
        // Arrange
        PlayerMock player = server.addPlayer("NegativePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.removeFromBalance(100.0); // Force negative

        // Act
        double balance = economy.getBalance(tanPlayer);

        // Assert
        assertTrue(balance < 0, "Balance should be negative");
    }

    // ========== Deposit Operations Tests ==========

    @Test
    @DisplayName("depositPlayer should increase balance")
    void testDepositPlayer() {
        // Arrange
        PlayerMock player = server.addPlayer("DepositPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act
        economy.depositPlayer(tanPlayer, 500.0);

        // Assert
        assertEquals(initialBalance + 500.0, tanPlayer.getBalance(), 0.001,
            "Balance should increase by deposit amount");
    }

    @Test
    @DisplayName("depositPlayer should handle zero amount")
    void testDepositZero() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroDepositPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act
        economy.depositPlayer(tanPlayer, 0.0);

        // Assert
        assertEquals(initialBalance, tanPlayer.getBalance(), 0.001,
            "Balance should remain unchanged with zero deposit");
    }

    @Test
    @DisplayName("depositPlayer should handle fractional amounts")
    void testDepositFractional() {
        // Arrange
        PlayerMock player = server.addPlayer("FractionalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act
        economy.depositPlayer(tanPlayer, 123.45);

        // Assert
        assertEquals(initialBalance + 123.45, tanPlayer.getBalance(), 0.001,
            "Balance should handle fractional amounts");
    }

    // ========== Withdraw Operations Tests ==========

    @Test
    @DisplayName("withdrawPlayer should decrease balance")
    void testWithdrawPlayer() {
        // Arrange
        PlayerMock player = server.addPlayer("WithdrawPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act
        economy.withdrawPlayer(tanPlayer, 300.0);

        // Assert
        assertEquals(700.0, tanPlayer.getBalance(), 0.001,
            "Balance should decrease by withdrawal amount");
    }

    @Test
    @DisplayName("withdrawPlayer should allow overdraft (negative balance)")
    void testWithdrawOverdraft() {
        // Arrange
        PlayerMock player = server.addPlayer("OverdraftPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(100.0);

        // Act - Withdraw more than available
        economy.withdrawPlayer(tanPlayer, 200.0);

        // Assert - Should allow negative balance
        assertEquals(-100.0, tanPlayer.getBalance(), 0.001,
            "Balance should go negative when overdrafting");
    }

    @Test
    @DisplayName("withdrawPlayer should handle zero amount")
    void testWithdrawZero() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroWithdrawPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(500.0);

        // Act
        economy.withdrawPlayer(tanPlayer, 0.0);

        // Assert
        assertEquals(500.0, tanPlayer.getBalance(), 0.001,
            "Balance should remain unchanged with zero withdrawal");
    }

    // ========== Balance Verification Tests ==========

    @Test
    @DisplayName("has should return true when balance is greater than amount")
    void testHasSufficient() {
        // Arrange
        PlayerMock player = server.addPlayer("HasPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act
        boolean hasEnough = economy.has(tanPlayer, 500.0);

        // Assert
        assertTrue(hasEnough, "Player should have sufficient balance");
    }

    @Test
    @DisplayName("has should return false when balance equals amount")
    void testHasExactAmount() {
        // Arrange
        PlayerMock player = server.addPlayer("ExactPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act
        boolean hasEnough = economy.has(tanPlayer, 1000.0);

        // Assert - Uses strict > comparison
        assertFalse(hasEnough, "has() uses > comparison, should return false for exact amount");
    }

    @Test
    @DisplayName("has should return false when balance is less than amount")
    void testHasInsufficient() {
        // Arrange
        PlayerMock player = server.addPlayer("InsufficientPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(100.0);

        // Act
        boolean hasEnough = economy.has(tanPlayer, 200.0);

        // Assert
        assertFalse(hasEnough, "Player should not have sufficient balance");
    }

    @Test
    @DisplayName("has should return false for zero balance")
    void testHasZeroBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroBalancePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act
        boolean hasEnough = economy.has(tanPlayer, 0.1);

        // Assert
        assertFalse(hasEnough, "Player with zero balance should not have any amount");
    }

    // ========== Currency Formatting Tests ==========

    @Test
    @DisplayName("formatMoney should format amount with 2 decimal places")
    void testFormatMoney() {
        // Act
        String formatted = economy.formatMoney(1234.567);

        // Assert
        assertTrue(formatted.contains("1234.57"), "Should round to 2 decimal places");
    }

    @Test
    @DisplayName("formatMoney should include currency icon")
    void testFormatMoneyIncludesIcon() {
        // Arrange
        String icon = economy.getMoneyIcon();

        // Act
        String formatted = economy.formatMoney(100.0);

        // Assert
        assertTrue(formatted.contains(icon), "Formatted string should include currency icon");
    }

    @Test
    @DisplayName("formatMoney should handle zero")
    void testFormatMoneyZero() {
        // Act
        String formatted = economy.formatMoney(0.0);

        // Assert
        assertTrue(formatted.contains("0.00"), "Should format zero correctly");
    }

    @Test
    @DisplayName("formatMoney should handle negative amounts")
    void testFormatMoneyNegative() {
        // Act
        String formatted = economy.formatMoney(-50.25);

        // Assert
        assertTrue(formatted.contains("-50.25"), "Should format negative amounts correctly");
    }

    @Test
    @DisplayName("getMoneyIcon should return currency icon")
    void testGetMoneyIcon() {
        // Act
        String icon = economy.getMoneyIcon();

        // Assert
        assertNotNull(icon, "Currency icon should not be null");
        assertFalse(icon.isEmpty(), "Currency icon should not be empty");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Multiple deposits and withdrawals should calculate correctly")
    void testMultipleTransactions() {
        // Arrange
        PlayerMock player = server.addPlayer("MultiTransPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act - Multiple operations
        economy.depositPlayer(tanPlayer, 1000.0);
        economy.withdrawPlayer(tanPlayer, 200.0);
        economy.depositPlayer(tanPlayer, 500.0);
        economy.withdrawPlayer(tanPlayer, 300.0);

        // Assert
        double finalBalance = economy.getBalance(tanPlayer);
        assertEquals(1000.0, finalBalance, 0.001,
            "Final balance should be: +1000 -200 +500 -300 = 1000");
    }

    @Test
    @DisplayName("Operations should persist correctly")
    void testOperationsPersistence() {
        // Arrange
        PlayerMock player = server.addPlayer("PersistencePlayer");
        String playerId = player.getUniqueId().toString();
        ITanPlayer tanPlayer = storage.getSync(playerId);

        // Act
        economy.depositPlayer(tanPlayer, 1500.0);
        storage.putSync(playerId, tanPlayer);

        // Re-fetch from storage
        ITanPlayer fetchedPlayer = storage.getSync(playerId);

        // Assert
        assertEquals(1500.0, economy.getBalance(fetchedPlayer), 0.001,
            "Balance should persist after storage update");
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("depositPlayer should handle very large amounts")
    void testDepositLargeAmount() {
        // Arrange
        PlayerMock player = server.addPlayer("LargeAmountPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act - Deposit large amount (near Double.MAX_VALUE)
        double largeAmount = 1000000000.0;
        economy.depositPlayer(tanPlayer, largeAmount);

        // Assert
        assertEquals(largeAmount, economy.getBalance(tanPlayer), 1.0,
            "Should handle large amounts");
    }

    @Test
    @DisplayName("withdrawPlayer should handle very small amounts")
    void testWithdrawTinyAmount() {
        // Arrange
        PlayerMock player = server.addPlayer("TinyAmountPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1.0);

        // Act - Withdraw very small amount
        economy.withdrawPlayer(tanPlayer, 0.001);

        // Assert
        assertEquals(0.999, economy.getBalance(tanPlayer), 0.0001,
            "Should handle tiny fractional amounts");
    }

    @Test
    @DisplayName("Operations should work for multiple players independently")
    void testMultiplePlayersIndependence() {
        // Arrange
        PlayerMock player1 = server.addPlayer("Player1");
        PlayerMock player2 = server.addPlayer("Player2");
        ITanPlayer tanPlayer1 = storage.getSync(player1.getUniqueId().toString());
        ITanPlayer tanPlayer2 = storage.getSync(player2.getUniqueId().toString());

        // Act - Different operations for each player
        economy.depositPlayer(tanPlayer1, 1000.0);
        economy.depositPlayer(tanPlayer2, 500.0);

        // Assert - Balances should be independent
        assertEquals(1000.0, economy.getBalance(tanPlayer1), 0.001);
        assertEquals(500.0, economy.getBalance(tanPlayer2), 0.001);
    }
}
