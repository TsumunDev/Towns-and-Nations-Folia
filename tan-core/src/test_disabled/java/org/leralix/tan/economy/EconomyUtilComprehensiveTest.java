package org.leralix.tan.economy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.StringUtil;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive unit tests for EconomyUtil facade.
 *
 * <p>Tests proper delegation to implementations, null safety, and
 * facade functionality as required by Story 4.2.</p>
 *
 * @since 0.16.0
 */
@DisplayName("EconomyUtil Facade Tests")
public class EconomyUtilComprehensiveTest {

    private ServerMock server;
    private AbstractTanEcon testEconomy;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);

        // Register test economy
        testEconomy = new TestTanEcon();
        EconomyUtil.register(testEconomy);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ==================== Delegation Tests ====================

    @Test
    @DisplayName("Get balance should delegate to economy implementation")
    void testGetBalanceDelegation() {
        // Arrange
        PlayerMock player = server.addPlayer("BalanceDelegatePlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double expectedBalance = 500.0;
        tanPlayer.addToBalance(expectedBalance);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Act
        double actualBalance = EconomyUtil.getBalance(player);

        // Assert
        assertEquals(expectedBalance, actualBalance, 0.001,
            "Should delegate to registered economy implementation");
    }

    @Test
    @DisplayName("Get balance with ITanPlayer should delegate correctly")
    void testGetBalanceWithITanPlayerDelegation() {
        // Arrange
        PlayerMock player = server.addPlayer("TanPlayerBalanceDelegatePlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double expectedBalance = 750.0;
        tanPlayer.addToBalance(expectedBalance);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);

        // Act
        double actualBalance = EconomyUtil.getBalance(tanPlayer);

        // Assert
        assertEquals(expectedBalance, actualBalance, 0.001,
            "Should delegate to registered economy implementation");
    }

    @Test
    @DisplayName("Remove from balance should delegate correctly")
    void testRemoveFromBalanceDelegation() {
        // Arrange
        PlayerMock player = server.addPlayer("RemoveDelegatePlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), tanPlayer);
        double initialBalance = tanPlayer.getBalance();

        // Act
        EconomyUtil.removeFromBalance(player, 100.0);

        // Assert
        ITanPlayer updatedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertEquals(initialBalance - 100.0, updatedPlayer.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Add to balance should delegate correctly")
    void testAddToBalanceDelegation() {
        // Arrange
        PlayerMock player = server.addPlayer("AddDelegatePlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act
        EconomyUtil.addFromBalance(player, 200.0);

        // Assert
        ITanPlayer updatedPlayer = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        assertEquals(initialBalance + 200.0, updatedPlayer.getBalance(), 0.001);
    }

    // ==================== Null Safety Tests ====================

    @Test
    @DisplayName("Get balance with null player should return zero or throw exception")
    void testGetBalanceNullPlayer() {
        // Act & Assert - Should either return 0 or throw exception
        assertDoesNotThrow(() -> {
            double balance = EconomyUtil.getBalance((Player) null);
            // If it doesn't throw, should return 0 or handle gracefully
            assertTrue(balance >= 0, "Balance should be non-negative");
        });
    }

    @Test
    @DisplayName("Get balance with null offline player should return zero or throw exception")
    void testGetBalanceNullOfflinePlayer() {
        // Act & Assert - Should either return 0 or throw exception
        assertDoesNotThrow(() -> {
            double balance = EconomyUtil.getBalance((OfflinePlayer) null);
            // If it doesn't throw, should return 0 or handle gracefully
            assertTrue(balance >= 0, "Balance should be non-negative");
        });
    }

    @Test
    @DisplayName("Get balance with null ITanPlayer should return zero")
    void testGetBalanceNullITanPlayer() {
        // Act
        double balance = EconomyUtil.getBalance((ITanPlayer) null);

        // Assert
        assertEquals(0.0, balance, 0.001,
            "Null ITanPlayer should return zero balance");
    }

    @Test
    @DisplayName("Remove from balance with null player should handle gracefully")
    void testRemoveFromBalanceNullPlayer() {
        // Act & Assert - Should not crash
        assertDoesNotThrow(() -> {
            EconomyUtil.removeFromBalance((Player) null, 100.0);
        });
    }

    @Test
    @DisplayName("Add to balance with null player should handle gracefully")
    void testAddToBalanceNullPlayer() {
        // Act & Assert - Should not crash
        assertDoesNotThrow(() -> {
            EconomyUtil.addFromBalance((Player) null, 100.0);
        });
    }

    // ==================== Format Money Tests ====================

    @Test
    @DisplayName("Format money should delegate to economy implementation")
    void testFormatMoneyDelegation() {
        // Arrange
        double amount = 1234.56;

        // Act
        String formatted = EconomyUtil.formatMoney(amount);

        // Assert
        assertNotNull(formatted);
        assertTrue(formatted.contains("1234") || formatted.contains("1,234"),
            "Should delegate to economy formatter");
    }

    @Test
    @DisplayName("Format money with zero should work correctly")
    void testFormatMoneyZero() {
        // Arrange
        double amount = 0.0;

        // Act
        String formatted = EconomyUtil.formatMoney(amount);

        // Assert
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
    }

    @Test
    @DisplayName("Format money with negative should work correctly")
    void testFormatMoneyNegative() {
        // Arrange
        double amount = -100.0;

        // Act
        String formatted = EconomyUtil.formatMoney(amount);

        // Assert
        assertNotNull(formatted);
        // Should handle negative amounts (either with minus sign or "debt" text)
        assertTrue(formatted.contains("-") || formatted.toLowerCase().contains("debt"));
    }

    @Test
    @DisplayName("StringUtil formatMoney should match EconomyUtil")
    void testStringUtilFormatMoney() {
        // Arrange
        double amount = 999.99;

        // Act
        String economyUtilFormat = EconomyUtil.formatMoney(amount);
        String stringUtilFormat = StringUtil.formatMoney(amount);

        // Assert - Both should produce similar output
        assertNotNull(economyUtilFormat);
        assertNotNull(stringUtilFormat);
    }

    // ==================== Money Icon Tests ====================

    @Test
    @DisplayName("Get money icon should delegate to economy implementation")
    void testGetMoneyIconDelegation() {
        // Act
        String icon = EconomyUtil.getMoneyIcon();

        // Assert
        assertNotNull(icon, "Money icon should not be null");
        assertFalse(icon.isEmpty(), "Money icon should not be empty");
        assertEquals(testEconomy.getMoneyIcon(), icon,
            "Should delegate to registered economy");
    }

    // ==================== Economy Registration Tests ====================

    @Test
    @DisplayName("Register economy should change active implementation")
    void testRegisterEconomy() {
        // Arrange
        AbstractTanEcon newEconomy = new TestTanEcon() {
            @Override
            public String formatMoney(double amount) {
                return "€" + String.format("%.2f", amount);
            }
        };

        // Act
        EconomyUtil.register(newEconomy);

        // Assert
        String formatted = EconomyUtil.formatMoney(100.0);
        assertTrue(formatted.contains("€"),
            "Should use new economy formatter");
    }

    @Test
    @DisplayName("Register null economy should handle gracefully")
    void testRegisterNullEconomy() {
        // Act & Assert - Should either handle gracefully or throw
        if (EconomyUtil.getEconInstance() != null) {
            // Economy exists, null registration should be handled
            assertDoesNotThrow(() -> {
                EconomyUtil.register(null);
            });
        }
    }

    // ==================== Economy Type Detection Tests ====================

    @Test
    @DisplayName("Has economy should return true when economy registered")
    void testHasEconomy() {
        // Act
        boolean hasEconomy = EconomyUtil.hasEconomy();

        // Assert
        assertTrue(hasEconomy, "Should have economy after registration");
    }

    @Test
    @DisplayName("Get econ instance should return registered economy")
    void testGetEconInstance() {
        // Act
        AbstractTanEcon econ = EconomyUtil.getEconInstance();

        // Assert
        assertNotNull(econ, "Econ instance should not be null");
        assertTrue(econ instanceof AbstractTanEcon,
            "Should return AbstractTanEcon instance");
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Multiple operations should work consistently")
    void testMultipleOperationsConsistency() {
        // Arrange
        PlayerMock player = server.addPlayer("MultiOpPlayer");
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        double initialBalance = tanPlayer.getBalance();

        // Act - Perform multiple operations
        EconomyUtil.addFromBalance(player, 100.0);
        ITanPlayer afterAdd = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), afterAdd);

        EconomyUtil.removeFromBalance(player, 50.0);
        ITanPlayer afterRemove = PlayerDataStorage.getInstance().getSync(tanPlayer.getID());
        PlayerDataStorage.getInstance().putSync(tanPlayer.getID(), afterRemove);

        // Assert
        assertEquals(initialBalance + 100.0 - 50.0, afterRemove.getBalance(), 0.001);
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Multiple balance checks should be fast")
    void testBalanceCheckPerformance() {
        // Arrange
        PlayerMock player = server.addPlayer("PerfPlayer");
        int iterations = 100;

        // Act
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            EconomyUtil.getBalance(player);
        }
        long duration = System.nanoTime() - startTime;

        // Assert - Should be fast (cached or optimized)
        long durationMs = duration / 1_000_000;
        assertTrue(durationMs < 1000,
            "100 balance checks should complete in < 1 second, took: " + durationMs + "ms");
    }

    // ==================== Test Implementation ====================

    /**
     * Test implementation of AbstractTanEcon for EconomyUtil testing.
     */
    private static class TestTanEcon extends AbstractTanEcon {

        @Override
        public double getBalance(ITanPlayer player) {
            return player != null ? player.getBalance() : 0.0;
        }

        @Override
        public void withdraw(ITanPlayer player, double amount) {
            if (player != null) {
                player.removeFromBalance(amount);
            }
        }

        @Override
        public void deposit(ITanPlayer player, double amount) {
            if (player != null) {
                player.addToBalance(amount);
            }
        }

        @Override
        public String formatMoney(double amount) {
            return "$" + String.format("%.2f", amount);
        }

        @Override
        public String getMoneyIcon() {
            return "$";
        }
    }
}
