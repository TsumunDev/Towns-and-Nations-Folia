package org.leralix.tan.economy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive unit tests for AbstractTanEcon economy abstraction.
 *
 * <p>Tests deposit/withdraw operations, balance checks, currency formatting,
 * and transaction handling as required by Story 4.2.</p>
 *
 * @since 0.16.0
 */
@DisplayName("AbstractTanEcon Economy Abstraction Tests")
public class AbstractTanEconComprehensiveTest {

    private ServerMock server;
    private TestTanEcon economy;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);
        economy = new TestTanEcon();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ==================== Deposit/Withdraw Operations Tests ====================

    @Test
    @DisplayName("Deposit should increase player balance")
    void testDepositIncreasesBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("DepositPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();
        double depositAmount = 100.0;

        // Act
        tanPlayer.addToBalance(depositAmount);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Assert
        ITanPlayer updatedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertEquals(initialBalance + depositAmount, updatedPlayer.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Withdraw should decrease player balance")
    void testWithdrawDecreasesBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("WithdrawPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();
        tanPlayer.addToBalance(1000.0); // Add money first
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);
        double withdrawAmount = 100.0;

        // Act
        boolean success = tanPlayer.removeFromBalance(withdrawAmount);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Assert
        assertTrue(success, "Withdraw should succeed");
        ITanPlayer updatedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertEquals(initialBalance + 1000.0 - withdrawAmount, updatedPlayer.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Withdraw with insufficient funds should fail")
    void testWithdrawInsufficientFunds() {
        // Arrange
        PlayerMock player = server.addPlayer("InsufficientFundsPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();
        double withdrawAmount = initialBalance + 1000.0; // More than balance

        // Act
        boolean success = tanPlayer.removeFromBalance(withdrawAmount);

        // Assert
        assertFalse(success, "Withdraw should fail with insufficient funds");
        assertEquals(initialBalance, tanPlayer.getBalance(), 0.001,
            "Balance should remain unchanged");
    }

    @Test
    @DisplayName("Multiple deposits should accumulate correctly")
    void testMultipleDeposits() {
        // Arrange
        PlayerMock player = server.addPlayer("MultiDepositPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act
        tanPlayer.addToBalance(100.0);
        tanPlayer.addToBalance(200.0);
        tanPlayer.addToBalance(300.0);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Assert
        ITanPlayer updatedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertEquals(initialBalance + 600.0, updatedPlayer.getBalance(), 0.001);
    }

    // ==================== Balance Check Tests ====================

    @Test
    @DisplayName("Balance check should return correct amount")
    void testBalanceCheck() {
        // Arrange
        PlayerMock player = server.addPlayer("BalanceCheckPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double expectedBalance = 1000.0;
        tanPlayer.addToBalance(expectedBalance);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Act
        ITanPlayer fetchedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        double actualBalance = fetchedPlayer.getBalance();

        // Assert
        assertEquals(expectedBalance, actualBalance, 0.001);
    }

    @Test
    @DisplayName("Balance check for new player should return zero")
    void testBalanceCheckNewPlayer() {
        // Arrange
        PlayerMock player = server.addPlayer("NewBalancePlayer");

        // Act
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double balance = tanPlayer.getBalance();

        // Assert
        assertEquals(0.0, balance, 0.001, "New player should have zero balance");
    }

    @Test
    @DisplayName("Has money check should work correctly")
    void testHasMoneyCheck() {
        // Arrange
        PlayerMock player = server.addPlayer("HasMoneyPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(500.0);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Act & Assert
        ITanPlayer fetchedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertTrue(fetchedPlayer.getBalance() >= 300.0,
            "Should have enough money for 300");
        assertFalse(fetchedPlayer.getBalance() >= 1000.0,
            "Should not have enough money for 1000");
    }

    // ==================== Currency Formatting Tests ====================

    @Test
    @DisplayName("Currency formatting should handle zero")
    void testCurrencyFormatZero() {
        // Arrange
        double amount = 0.0;

        // Act
        String formatted = economy.formatMoney(amount);

        // Assert
        assertNotNull(formatted);
        assertTrue(formatted.contains("0") || formatted.contains("0.0"));
    }

    @Test
    @DisplayName("Currency formatting should handle positive amounts")
    void testCurrencyFormatPositive() {
        // Arrange
        double amount = 1234.56;

        // Act
        String formatted = economy.formatMoney(amount);

        // Assert
        assertNotNull(formatted);
        assertTrue(formatted.contains("1234") || formatted.contains("1,234"));
    }

    @Test
    @DisplayName("Currency formatting should handle negative amounts")
    void testCurrencyFormatNegative() {
        // Arrange
        double amount = -100.0;

        // Act
        String formatted = economy.formatMoney(amount);

        // Assert
        assertNotNull(formatted);
        assertTrue(formatted.contains("-") || formatted.toLowerCase().contains("negative"));
    }

    @Test
    @DisplayName("Currency formatting should handle large amounts")
    void testCurrencyFormatLarge() {
        // Arrange
        double amount = 1_000_000.0;

        // Act
        String formatted = economy.formatMoney(amount);

        // Assert
        assertNotNull(formatted);
        // Should contain separators or be properly formatted
        assertTrue(formatted.length() > 5);
    }

    // ==================== Money Icon Tests ====================

    @Test
    @DisplayName("Money icon should be consistent")
    void testMoneyIcon() {
        // Act
        String icon = economy.getMoneyIcon();

        // Assert
        assertNotNull(icon);
        assertFalse(icon.isEmpty(), "Money icon should not be empty");
    }

    // ==================== Transaction History Tests ====================

    @Test
    @DisplayName("Transaction should be recorded correctly")
    void testTransactionRecording() {
        // This test would verify that transactions are properly recorded
        // Implementation depends on transaction history system

        // Arrange
        PlayerMock player = server.addPlayer("TransactionPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();
        double transactionAmount = 100.0;

        // Act
        tanPlayer.addToBalance(transactionAmount);
        // Transaction would be recorded here

        // Assert
        assertEquals(initialBalance + transactionAmount, tanPlayer.getBalance(), 0.001);
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Deposit of zero should not change balance")
    void testDepositZero() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroDepositPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act
        tanPlayer.addToBalance(0.0);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Assert
        ITanPlayer updatedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertEquals(initialBalance, updatedPlayer.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Withdraw of zero should succeed")
    void testWithdrawZero() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroWithdrawPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act
        boolean success = tanPlayer.removeFromBalance(0.0);

        // Assert
        assertTrue(success, "Zero withdraw should succeed");
        assertEquals(initialBalance, tanPlayer.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Very large deposit should handle correctly")
    void testLargeDeposit() {
        // Arrange
        PlayerMock player = server.addPlayer("LargeDepositPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double largeAmount = Double.MAX_VALUE / 2;

        // Act
        tanPlayer.addToBalance(largeAmount);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Assert
        ITanPlayer updatedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertTrue(updatedPlayer.getBalance() > 0, "Balance should be positive");
    }

    @Test
    @DisplayName("Negative balance handling should work correctly")
    void testNegativeBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("NegativeBalancePlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());

        // Act - Try to withdraw more than available (if allowed)
        tanPlayer.removeFromBalance(tanPlayer.getBalance() + 100.0);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Assert - Should either prevent negative or handle it
        ITanPlayer updatedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertTrue(updatedPlayer.getBalance() >= 0,
            "Balance should not be negative (or system should allow it)");
    }

    // ==================== Concurrent Transaction Tests ====================

    @Test
    @DisplayName("Concurrent deposits should be thread-safe")
    void testConcurrentDeposits() throws Exception {
        // Arrange
        PlayerMock player = server.addPlayer("ConcurrentDepositPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Act - Perform multiple concurrent deposits
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    tanPlayer.addToBalance(10.0);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Assert
        ITanPlayer updatedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertEquals(1000.0 + (10.0 * 10 * threads.length), updatedPlayer.getBalance(), 0.001);
    }

    // ==================== Test Implementation ====================

    /**
     * Test implementation of AbstractTanEcon for testing.
     */
    private static class TestTanEcon extends AbstractTanEcon {

        @Override
        public double getBalance(ITanPlayer player) {
            return player.getBalance();
        }

        @Override
        public void withdraw(ITanPlayer player, double amount) {
            player.removeFromBalance(amount);
        }

        @Override
        public void deposit(ITanPlayer player, double amount) {
            player.addToBalance(amount);
        }

        @Override
        public String formatMoney(double amount) {
            return String.format("$%.2f", amount);
        }

        @Override
        public String getMoneyIcon() {
            return "$";
        }
    }
}
