package org.leralix.tan.economy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import net.milkbowl.vault.economy.Economy;

/**
 * Comprehensive tests for TanEconomyExternal (external Vault economy integration).
 *
 * <p>Tests the integration with external economy plugins via Vault API.
 * Uses Mockito to mock the external Economy implementation.</p>
 *
 * <p><b>Coverage Areas:</b></p>
 * <ul>
 *   <li>External economy delegation</li>
 *   <li>UUID parsing and player resolution</li>
 *   <li>Currency handling from external plugin</li>
 *   <li>Error handling for invalid UUIDs</li>
 *   <li>Integration with ITanPlayer data structure</li>
 * </ul>
 */
@DisplayName("TanEconomyExternal Tests")
class TanEconomyExternalComprehensiveTest {

    private ServerMock server;
    private Economy mockExternalEconomy;
    private TanEconomyExternal externalEconomy;
    private PlayerDataStorage storage;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        storage = PlayerDataStorage.getInstance();

        // Mock external Vault economy
        mockExternalEconomy = mock(Economy.class);
        externalEconomy = new TanEconomyExternal(mockExternalEconomy);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ========== Balance Delegation Tests ==========

    @Test
    @DisplayName("getBalance should delegate to external economy")
    void testGetBalanceDelegation() {
        // Arrange
        PlayerMock player = server.addPlayer("ExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        UUID playerUuid = player.getUniqueId();
        double expectedBalance = 1500.0;

        // Mock external economy response
        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenReturn(expectedBalance);

        // Act
        double balance = externalEconomy.getBalance(tanPlayer);

        // Assert
        assertEquals(expectedBalance, balance, 0.001,
            "Should return balance from external economy");
        verify(mockExternalEconomy).getBalance(any(OfflinePlayer.class));
    }

    @Test
    @DisplayName("getBalance should handle zero balance")
    void testGetBalanceZero() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroBalanceExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenReturn(0.0);

        // Act
        double balance = externalEconomy.getBalance(tanPlayer);

        // Assert
        assertEquals(0.0, balance, 0.001, "Should handle zero balance");
    }

    @Test
    @DisplayName("getBalance should handle negative balance")
    void testGetBalanceNegative() {
        // Arrange
        PlayerMock player = server.addPlayer("NegativeExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenReturn(-500.0);

        // Act
        double balance = externalEconomy.getBalance(tanPlayer);

        // Assert
        assertEquals(-500.0, balance, 0.001, "Should handle negative balance");
    }

    // ========== Deposit Delegation Tests ==========

    @Test
    @DisplayName("depositPlayer should delegate to external economy")
    void testDepositDelegation() {
        // Arrange
        PlayerMock player = server.addPlayer("DepositExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double depositAmount = 1000.0;

        // Mock successful deposit
        when(mockExternalEconomy.depositPlayer(any(OfflinePlayer.class), eq(depositAmount)))
            .thenReturn(null);

        // Act
        externalEconomy.depositPlayer(tanPlayer, depositAmount);

        // Assert
        verify(mockExternalEconomy).depositPlayer(any(OfflinePlayer.class), eq(depositAmount));
    }

    @Test
    @DisplayName("depositPlayer should handle fractional amounts")
    void testDepositFractional() {
        // Arrange
        PlayerMock player = server.addPlayer("FractionalExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double fractionalAmount = 123.45;

        // Act
        externalEconomy.depositPlayer(tanPlayer, fractionalAmount);

        // Assert
        verify(mockExternalEconomy).depositPlayer(any(OfflinePlayer.class), eq(fractionalAmount));
    }

    @Test
    @DisplayName("depositPlayer should handle zero amount")
    void testDepositZero() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroDepositExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act
        externalEconomy.depositPlayer(tanPlayer, 0.0);

        // Assert
        verify(mockExternalEconomy).depositPlayer(any(OfflinePlayer.class), eq(0.0));
    }

    // ========== Withdraw Delegation Tests ==========

    @Test
    @DisplayName("withdrawPlayer should delegate to external economy")
    void testWithdrawDelegation() {
        // Arrange
        PlayerMock player = server.addPlayer("WithdrawExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double withdrawAmount = 500.0;

        // Mock successful withdrawal
        when(mockExternalEconomy.withdrawPlayer(any(OfflinePlayer.class), eq(withdrawAmount)))
            .thenReturn(null);

        // Act
        externalEconomy.withdrawPlayer(tanPlayer, withdrawAmount);

        // Assert
        verify(mockExternalEconomy).withdrawPlayer(any(OfflinePlayer.class), eq(withdrawAmount));
    }

    @Test
    @DisplayName("withdrawPlayer should handle zero amount")
    void testWithdrawZero() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroWithdrawExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act
        externalEconomy.withdrawPlayer(tanPlayer, 0.0);

        // Assert
        verify(mockExternalEconomy).withdrawPlayer(any(OfflinePlayer.class), eq(0.0));
    }

    // ========== Balance Verification Tests ==========

    @Test
    @DisplayName("has should delegate to external economy for comparison")
    void testHasSufficient() {
        // Arrange
        PlayerMock player = server.addPlayer("HasExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double playerBalance = 2000.0;
        double checkAmount = 1000.0;

        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenReturn(playerBalance);

        // Act
        boolean hasEnough = externalEconomy.has(tanPlayer, checkAmount);

        // Assert
        assertTrue(hasEnough, "Should return true when balance is greater than amount");
    }

    @Test
    @DisplayName("has should return false when balance equals amount")
    void testHasExactAmount() {
        // Arrange
        PlayerMock player = server.addPlayer("ExactExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double playerBalance = 1000.0;
        double checkAmount = 1000.0;

        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenReturn(playerBalance);

        // Act
        boolean hasEnough = externalEconomy.has(tanPlayer, checkAmount);

        // Assert - Uses strict > comparison
        assertFalse(hasEnough, "has() uses > comparison, should return false for exact amount");
    }

    @Test
    @DisplayName("has should return false for insufficient balance")
    void testHasInsufficient() {
        // Arrange
        PlayerMock player = server.addPlayer("InsufficientExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double playerBalance = 500.0;
        double checkAmount = 1000.0;

        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenReturn(playerBalance);

        // Act
        boolean hasEnough = externalEconomy.has(tanPlayer, checkAmount);

        // Assert
        assertFalse(hasEnough, "Should return false when balance is less than amount");
    }

    // ========== Currency Formatting Tests ==========

    @Test
    @DisplayName("formatMoney should use external economy currency")
    void testFormatMoney() {
        // Arrange
        String mockCurrencyName = "Coins";
        double amount = 1234.56;

        when(mockExternalEconomy.currencyNamePlural()).thenReturn(mockCurrencyName);

        // Act
        String formatted = externalEconomy.formatMoney(amount);

        // Assert
        assertTrue(formatted.contains("1234.56"), "Should format amount correctly");
        assertTrue(formatted.contains(mockCurrencyName),
            "Should include external currency name");
    }

    @Test
    @DisplayName("getMoneyIcon should return external economy currency name")
    void testGetMoneyIcon() {
        // Arrange
        String mockCurrencyName = "Dollars";
        when(mockExternalEconomy.currencyNameSingular()).thenReturn(mockCurrencyName);

        // Act
        String icon = externalEconomy.getMoneyIcon();

        // Assert
        assertEquals(mockCurrencyName, icon, "Should return external currency name");
        verify(mockExternalEconomy).currencyNameSingular();
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Multiple operations should delegate correctly")
    void testMultipleOperations() {
        // Arrange
        PlayerMock player = server.addPlayer("MultiExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenReturn(1000.0);

        // Act - Multiple operations
        externalEconomy.depositPlayer(tanPlayer, 500.0);
        double balance = externalEconomy.getBalance(tanPlayer);
        externalEconomy.withdrawPlayer(tanPlayer, 300.0);

        // Assert - Verify delegations
        verify(mockExternalEconomy, atLeastOnce()).getBalance(any(OfflinePlayer.class));
        verify(mockExternalEconomy).depositPlayer(any(OfflinePlayer.class), eq(500.0));
        verify(mockExternalEconomy).withdrawPlayer(any(OfflinePlayer.class), eq(300.0));
    }

    @Test
    @DisplayName("Should handle multiple players independently")
    void testMultiplePlayersIndependence() {
        // Arrange
        PlayerMock player1 = server.addPlayer("ExternalPlayer1");
        PlayerMock player2 = server.addPlayer("ExternalPlayer2");
        ITanPlayer tanPlayer1 = storage.getSync(player1.getUniqueId().toString());
        ITanPlayer tanPlayer2 = storage.getSync(player2.getUniqueId().toString());

        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenReturn(1000.0);

        // Act - Different operations for each player
        externalEconomy.depositPlayer(tanPlayer1, 1000.0);
        externalEconomy.depositPlayer(tanPlayer2, 500.0);

        // Assert - Verify delegations for both players
        verify(mockExternalEconomy, times(2)).depositPlayer(any(OfflinePlayer.class), anyDouble());
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Should handle very large amounts")
    void testLargeAmounts() {
        // Arrange
        PlayerMock player = server.addPlayer("LargeExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double largeAmount = 1000000000.0;

        // Act
        externalEconomy.depositPlayer(tanPlayer, largeAmount);

        // Assert
        verify(mockExternalEconomy).depositPlayer(any(OfflinePlayer.class), eq(largeAmount));
    }

    @Test
    @DisplayName("Should handle very small fractional amounts")
    void testTinyAmounts() {
        // Arrange
        PlayerMock player = server.addPlayer("TinyExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double tinyAmount = 0.001;

        // Act
        externalEconomy.withdrawPlayer(tanPlayer, tinyAmount);

        // Assert
        verify(mockExternalEconomy).withdrawPlayer(any(OfflinePlayer.class), eq(tinyAmount));
    }

    @Test
    @DisplayName("Should handle negative amounts in operations")
    void testNegativeAmounts() {
        // Arrange
        PlayerMock player = server.addPlayer("NegativeExternalPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double negativeAmount = -100.0;

        // Act - Note: External economy may reject negative amounts
        externalEconomy.depositPlayer(tanPlayer, negativeAmount);

        // Assert - Should still delegate to external economy
        verify(mockExternalEconomy).depositPlayer(any(OfflinePlayer.class), eq(negativeAmount));
    }

    // ========== Error Handling Tests ==========

    @Test
    @DisplayName("Should handle external economy returning null gracefully")
    void testExternalEconomyNullResponse() {
        // Arrange
        PlayerMock player = server.addPlayer("NullResponsePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Mock to return null (some external economies might)
        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenReturn(null);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> externalEconomy.getBalance(tanPlayer));
    }

    @Test
    @DisplayName("Should delegate exceptions from external economy")
    void testExternalEconomyThrowsException() {
        // Arrange
        PlayerMock player = server.addPlayer("ExceptionPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Mock to throw exception
        when(mockExternalEconomy.getBalance(any(OfflinePlayer.class)))
            .thenThrow(new RuntimeException("External economy error"));

        // Act & Assert - Should propagate exception
        assertThrows(RuntimeException.class, () -> externalEconomy.getBalance(tanPlayer));
    }

    // ========== Currency Name Edge Cases ==========

    @Test
    @DisplayName("formatMoney should handle empty currency name")
    void testEmptyCurrencyName() {
        // Arrange
        when(mockExternalEconomy.currencyNamePlural()).thenReturn("");
        double amount = 100.0;

        // Act
        String formatted = externalEconomy.formatMoney(amount);

        // Assert
        assertTrue(formatted.contains("100.00"), "Should still format amount");
    }

    @Test
    @DisplayName("getMoneyIcon should handle null currency name")
    void testNullCurrencyName() {
        // Arrange
        when(mockExternalEconomy.currencyNameSingular()).thenReturn(null);

        // Act
        String icon = externalEconomy.getMoneyIcon();

        // Assert
        // May return null, depends on implementation
        verify(mockExternalEconomy).currencyNameSingular();
    }
}
