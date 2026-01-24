package org.leralix.tan.economy;

import static org.junit.jupiter.api.Assertions.*;

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
import net.milkbowl.vault.economy.EconomyResponse;

/**
 * Comprehensive tests for TanEconomyVault (Vault Economy API implementation).
 *
 * <p>Tests the complete Vault Economy interface implementation that exposes
 * TAN's economy system to other Vault-compatible plugins.</p>
 *
 * <p><b>Coverage Areas:</b></p>
 * <ul>
 *   <li>Vault Economy API compliance</li>
 *   <li>EconomyResponse handling (success/failure)</li>
 *   <li>OfflinePlayer support</li>
 *   <li>Bank operations (return null - not supported)</li>
 *   <li>Currency formatting and metadata</li>
 *   <li>Account management</li>
 *   <li>Error handling for invalid operations</li>
 * </ul>
 *
 * <p><b>Important Note:</b> These tests use blocking getSync() calls which are
 * necessary for Vault API compliance but should be avoided in Folia region threads.</p>
 */
@DisplayName("TanEconomyVault Tests")
class TanEconomyVaultComprehensiveTest {

    private ServerMock server;
    private TanEconomyVault economy;
    private PlayerDataStorage storage;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        economy = new TanEconomyVault();
        storage = PlayerDataStorage.getInstance();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ========== Vault API Metadata Tests ==========

    @Test
    @DisplayName("isEnabled should always return true")
    void testIsEnabled() {
        // Act
        boolean enabled = economy.isEnabled();

        // Assert
        assertTrue(enabled, "TAN economy should always be enabled");
    }

    @Test
    @DisplayName("getName should return plugin name")
    void testGetName() {
        // Act
        String name = economy.getName();

        // Assert
        assertEquals("Towns and Nations Economy", name,
            "Should return correct plugin name");
    }

    @Test
    @DisplayName("hasBankSupport should return false")
    void testHasBankSupport() {
        // Act
        boolean hasBankSupport = economy.hasBankSupport();

        // Assert
        assertFalse(hasBankSupport, "Banks are not supported");
    }

    @Test
    @DisplayName("fractionalDigits should return configured digits")
    void testFractionalDigits() {
        // Act
        int digits = economy.fractionalDigits();

        // Assert
        // Should match Constants.getNbDigits()
        assertTrue(digits >= 0, "Should return non-negative digit count");
    }

    @Test
    @DisplayName("format should return string representation")
    void testFormat() {
        // Arrange
        double amount = 1234.56;

        // Act
        String formatted = economy.format(amount);

        // Assert
        assertNotNull(formatted, "Formatted string should not be null");
        assertTrue(formatted.contains("1234.56") || formatted.contains("1234"),
            "Should contain amount");
    }

    @Test
    @DisplayName("currencyNamePlural should return currency icon")
    void testCurrencyNamePlural() {
        // Act
        String plural = economy.currencyNamePlural();

        // Assert
        assertNotNull(plural, "Currency name should not be null");
        assertFalse(plural.isEmpty(), "Currency name should not be empty");
    }

    @Test
    @DisplayName("currencyNameSingular should return currency icon")
    void testCurrencyNameSingular() {
        // Act
        String singular = economy.currencyNameSingular();

        // Assert
        assertNotNull(singular, "Currency name should not be null");
        assertFalse(singular.isEmpty(), "Currency name should not be empty");
    }

    // ========== Account Management Tests ==========

    @Test
    @DisplayName("hasAccount(String) should always return true")
    void testHasAccountString() {
        // Act
        boolean hasAccount = economy.hasAccount("playerName");

        // Assert
        assertTrue(hasAccount, "All players have accounts");
    }

    @Test
    @DisplayName("hasAccount(OfflinePlayer) should always return true")
    void testHasAccountOfflinePlayer() {
        // Arrange
        PlayerMock player = server.addPlayer("AccountPlayer");

        // Act
        boolean hasAccount = economy.hasAccount(player);

        // Assert
        assertTrue(hasAccount, "All players have accounts");
    }

    @Test
    @DisplayName("hasAccount with world name should always return true")
    void testHasAccountWithWorld() {
        // Arrange
        PlayerMock player = server.addPlayer("WorldAccountPlayer");

        // Act
        boolean hasAccount = economy.hasAccount(player, "world");

        // Assert
        assertTrue(hasAccount, "All players have accounts in any world");
    }

    @Test
    @DisplayName("createPlayerAccount should return false")
    void testCreatePlayerAccount() {
        // Arrange
        PlayerMock player = server.addPlayer("CreatePlayer");

        // Act
        boolean created = economy.createPlayerAccount(player);

        // Assert
        assertFalse(created, "Account creation returns false (auto-created)");
    }

    // ========== Balance Access Tests ==========

    @Test
    @DisplayName("getBalance(String) should return player balance")
    void testGetBalanceString() {
        // Arrange
        PlayerMock player = server.addPlayer("StringBalancePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1500.0);

        // Act
        double balance = economy.getBalance(player.getName());

        // Assert
        assertEquals(1500.0, balance, 0.001, "Should return player balance");
    }

    @Test
    @DisplayName("getBalance(OfflinePlayer) should return player balance")
    void testGetBalanceOfflinePlayer() {
        // Arrange
        PlayerMock player = server.addPlayer("OfflineBalancePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(2000.0);

        // Act
        double balance = economy.getBalance(player);

        // Assert
        assertEquals(2000.0, balance, 0.001, "Should return player balance");
    }

    @Test
    @DisplayName("getBalance should return 0 for new player")
    void testGetBalanceNewPlayer() {
        // Arrange
        PlayerMock player = server.addPlayer("NewBalancePlayer");

        // Act
        double balance = economy.getBalance(player);

        // Assert
        assertEquals(0.0, balance, 0.001, "New player should have 0 balance");
    }

    // ========== Withdraw Tests ==========

    @Test
    @DisplayName("withdrawPlayer with sufficient funds should succeed")
    void testWithdrawPlayerSuccess() {
        // Arrange
        PlayerMock player = server.addPlayer("WithdrawSuccessPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act
        EconomyResponse response = economy.withdrawPlayer(player, 500.0);

        // Assert
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type,
            "Withdrawal should succeed");
        assertEquals(500.0, tanPlayer.getBalance(), 0.001,
            "Balance should be updated correctly");
    }

    @Test
    @DisplayName("withdrawPlayer with insufficient funds should fail")
    void testWithdrawPlayerInsufficient() {
        // Arrange
        PlayerMock player = server.addPlayer("WithdrawFailPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(100.0);

        // Act
        EconomyResponse response = economy.withdrawPlayer(player, 200.0);

        // Assert
        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type,
            "Withdrawal should fail");
        assertEquals(100.0, tanPlayer.getBalance(), 0.001,
            "Balance should remain unchanged");
    }

    @Test
    @DisplayName("withdrawPlayer with negative amount should fail")
    void testWithdrawPlayerNegative() {
        // Arrange
        PlayerMock player = server.addPlayer("WithdrawNegativePlayer");

        // Act
        EconomyResponse response = economy.withdrawPlayer(player, -100.0);

        // Assert
        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type,
            "Negative withdrawal should fail");
        assertTrue(response.errorMessage.contains("negative"),
            "Error message should mention negative amount");
    }

    @Test
    @DisplayName("withdrawPlayer with exact balance should fail")
    void testWithdrawPlayerExact() {
        // Arrange
        PlayerMock player = server.addPlayer("WithdrawExactPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(500.0);

        // Act
        EconomyResponse response = economy.withdrawPlayer(player, 500.0);

        // Assert - Uses > comparison, not >=
        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type,
            "Withdrawal of exact amount should fail (uses > comparison)");
    }

    // ========== Deposit Tests ==========

    @Test
    @DisplayName("depositPlayer should increase balance")
    void testDepositPlayer() {
        // Arrange
        PlayerMock player = server.addPlayer("DepositVaultPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act
        EconomyResponse response = economy.depositPlayer(player, 500.0);

        // Assert
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type,
            "Deposit should succeed");
        assertEquals(1500.0, tanPlayer.getBalance(), 0.001,
            "Balance should increase correctly");
    }

    @Test
    @DisplayName("depositPlayer with zero should succeed")
    void testDepositPlayerZero() {
        // Arrange
        PlayerMock player = server.addPlayer("DepositZeroPlayer");

        // Act
        EconomyResponse response = economy.depositPlayer(player, 0.0);

        // Assert
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type,
            "Zero deposit should succeed");
    }

    @Test
    @DisplayName("depositPlayer should handle negative amounts")
    void testDepositPlayerNegative() {
        // Arrange
        PlayerMock player = server.addPlayer("DepositNegativePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act - Some economies allow negative deposits
        EconomyResponse response = economy.depositPlayer(player, -500.0);

        // Assert - Should succeed (implementation allows it)
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type,
            "Negative deposit should succeed");
        assertEquals(500.0, tanPlayer.getBalance(), 0.001,
            "Balance should decrease");
    }

    // ========== Balance Verification Tests ==========

    @Test
    @DisplayName("has with sufficient balance should return true")
    void testHasSufficient() {
        // Arrange
        PlayerMock player = server.addPlayer("HasVaultPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act
        boolean hasEnough = economy.has(player, 500.0);

        // Assert
        assertTrue(hasEnough, "Player should have sufficient balance");
    }

    @Test
    @DisplayName("has with insufficient balance should return false")
    void testHasInsufficient() {
        // Arrange
        PlayerMock player = server.addPlayer("NotHasVaultPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(100.0);

        // Act
        boolean hasEnough = economy.has(player, 200.0);

        // Assert
        assertFalse(hasEnough, "Player should not have sufficient balance");
    }

    @Test
    @DisplayName("has with exact balance should return false")
    void testHasExact() {
        // Arrange
        PlayerMock player = server.addPlayer("ExactVaultPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(500.0);

        // Act
        boolean hasEnough = economy.has(player, 500.0);

        // Assert - Uses > comparison
        assertFalse(hasEnough, "has() uses > comparison, not >=");
    }

    // ========== Bank Operations Tests (Not Supported) ==========

    @Test
    @DisplayName("createBank should return null")
    void testCreateBank() {
        // Act
        EconomyResponse response = economy.createBank("bankName", "playerName");

        // Assert
        assertNull(response, "Bank creation is not supported");
    }

    @Test
    @DisplayName("deleteBank should return null")
    void testDeleteBank() {
        // Act
        EconomyResponse response = economy.deleteBank("bankName");

        // Assert
        assertNull(response, "Bank deletion is not supported");
    }

    @Test
    @DisplayName("bankBalance should return null")
    void testBankBalance() {
        // Act
        EconomyResponse response = economy.bankBalance("bankName");

        // Assert
        assertNull(response, "Bank balance is not supported");
    }

    @Test
    @DisplayName("bankHas should return null")
    void testBankHas() {
        // Act
        EconomyResponse response = economy.bankHas("bankName", 100.0);

        // Assert
        assertNull(response, "Bank operations are not supported");
    }

    @Test
    @DisplayName("bankWithdraw should return null")
    void testBankWithdraw() {
        // Act
        EconomyResponse response = economy.bankWithdraw("bankName", 100.0);

        // Assert
        assertNull(response, "Bank operations are not supported");
    }

    @Test
    @DisplayName("bankDeposit should return null")
    void testBankDeposit() {
        // Act
        EconomyResponse response = economy.bankDeposit("bankName", 100.0);

        // Assert
        assertNull(response, "Bank operations are not supported");
    }

    @Test
    @DisplayName("isBankOwner should return null")
    void testIsBankOwner() {
        // Act
        EconomyResponse response = economy.isBankOwner("bankName", "playerName");

        // Assert
        assertNull(response, "Bank operations are not supported");
    }

    @Test
    @DisplayName("isBankMember should return null")
    void testIsBankMember() {
        // Act
        EconomyResponse response = economy.isBankMember("bankName", "playerName");

        // Assert
        assertNull(response, "Bank operations are not supported");
    }

    @Test
    @DisplayName("getBanks should return empty list")
    void testGetBanks() {
        // Act
        var banks = economy.getBanks();

        // Assert
        assertNotNull(banks, "Banks list should not be null");
        assertTrue(banks.isEmpty(), "Banks list should be empty");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Multiple operations should work correctly")
    void testMultipleOperations() {
        // Arrange
        PlayerMock player = server.addPlayer("MultiVaultPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act - Multiple operations
        economy.depositPlayer(player, 500.0);
        economy.withdrawPlayer(player, 300.0);
        economy.depositPlayer(player, 200.0);

        // Assert
        assertEquals(1400.0, tanPlayer.getBalance(), 0.001,
            "Balance should be: +1000 +500 -300 +200 = 1400");
    }

    @Test
    @DisplayName("Operations should work independently for multiple players")
    void testMultiplePlayers() {
        // Arrange
        PlayerMock player1 = server.addPlayer("VaultPlayer1");
        PlayerMock player2 = server.addPlayer("VaultPlayer2");
        ITanPlayer tanPlayer1 = storage.getSync(player1.getUniqueId().toString());
        ITanPlayer tanPlayer2 = storage.getSync(player2.getUniqueId().toString());
        tanPlayer1.addToBalance(1000.0);
        tanPlayer2.addToBalance(500.0);

        // Act - Different operations
        economy.withdrawPlayer(player1, 200.0);
        economy.depositPlayer(player2, 300.0);

        // Assert
        assertEquals(800.0, tanPlayer1.getBalance(), 0.001);
        assertEquals(800.0, tanPlayer2.getBalance(), 0.001);
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Should handle very large amounts")
    void testLargeAmounts() {
        // Arrange
        PlayerMock player = server.addPlayer("LargeVaultPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000000000.0);

        // Act
        EconomyResponse response = economy.withdrawPlayer(player, 1000000.0);

        // Assert
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type);
    }

    @Test
    @DisplayName("Should handle very small fractional amounts")
    void testTinyAmounts() {
        // Arrange
        PlayerMock player = server.addPlayer("TinyVaultPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1.0);

        // Act
        EconomyResponse response = economy.withdrawPlayer(player, 0.001);

        // Assert
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type);
        assertEquals(0.999, tanPlayer.getBalance(), 0.0001);
    }

    @Test
    @DisplayName("Should handle player name lookup")
    void testPlayerNameLookup() {
        // Arrange
        PlayerMock player = server.addPlayer("NameLookupPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act - Use player name instead of OfflinePlayer
        double balance = economy.getBalance(player.getName());

        // Assert
        assertEquals(1000.0, balance, 0.001,
            "Should find player by name");
    }

    // ========== EconomyResponse Tests ==========

    @Test
    @DisplayName("Successful withdrawal should have correct response")
    void testSuccessfulWithdrawalResponse() {
        // Arrange
        PlayerMock player = server.addPlayer("SuccessResponsePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act
        EconomyResponse response = economy.withdrawPlayer(player, 500.0);

        // Assert
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type);
        assertEquals(500.0, response.amount, 0.001);
        assertEquals(500.0, response.balance, 0.001);
        assertTrue(response.errorMessage == null || response.errorMessage.isEmpty(),
            "Success response should have no error message");
    }

    @Test
    @DisplayName("Failed withdrawal should have error message")
    void testFailedWithdrawalResponse() {
        // Arrange
        PlayerMock player = server.addPlayer("FailResponsePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(100.0);

        // Act
        EconomyResponse response = economy.withdrawPlayer(player, 200.0);

        // Assert
        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type);
        assertNotNull(response.errorMessage, "Failure response should have error message");
        assertTrue(response.errorMessage.contains("enough money") ||
                   response.errorMessage.toLowerCase().contains("insufficient"),
            "Error message should mention insufficient funds");
    }

    @Test
    @DisplayName("Successful deposit should have correct response")
    void testSuccessfulDepositResponse() {
        // Arrange
        PlayerMock player = server.addPlayer("DepositResponsePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);

        // Act
        EconomyResponse response = economy.depositPlayer(player, 500.0);

        // Assert
        assertEquals(EconomyResponse.ResponseType.SUCCESS, response.type);
        assertEquals(500.0, response.amount, 0.001);
        assertEquals(1500.0, response.balance, 0.001);
    }
}
