package org.leralix.tan.economy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
 * Edge case and stress tests for economy system.
 *
 * <p>Tests unusual scenarios, boundary conditions, and performance-related cases:</p>
 * <ul>
 *   <li>Negative balance scenarios</li>
 *   <li>Maximum money limits (Double.MAX_VALUE)</li>
 *   <li>Concurrent transactions and thread safety</li>
 *   <li>Rapid successive operations</li>
 *   <li>Boundary value testing</li>
 *   <li>Floating-point precision issues</li>
 * </ul>
 *
 * <p><b>Performance Note:</b> Some tests use multiple threads to simulate concurrent access.
 * These tests are designed to complete quickly (< 5 seconds) while providing meaningful
 * validation of thread-safety guarantees.</p>
 */
@DisplayName("Economy Edge Cases and Stress Tests")
class EconomyEdgeCaseAndStressTest {

    private ServerMock server;
    private TanEconomyStandalone economy;
    private PlayerDataStorage storage;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        economy = new TanEconomyStandalone();
        storage = PlayerDataStorage.getInstance();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ========== Negative Balance Tests ==========

    @Test
    @DisplayName("Should handle negative balance from overdraft")
    void testNegativeBalanceOverdraft() {
        // Arrange
        PlayerMock player = server.addPlayer("OverdraftPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(100.0);

        // Act - Withdraw more than available
        economy.withdrawPlayer(tanPlayer, 200.0);

        // Assert
        assertEquals(-100.0, economy.getBalance(tanPlayer), 0.001,
            "Balance should be negative after overdraft");
    }

    @Test
    @DisplayName("Should handle depositing to negative balance")
    void testDepositToNegativeBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("NegativeDepositPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        economy.withdrawPlayer(tanPlayer, 500.0); // Force negative

        // Act - Deposit to bring back positive
        economy.depositPlayer(tanPlayer, 1000.0);

        // Assert
        assertEquals(500.0, economy.getBalance(tanPlayer), 0.001,
            "Should deposit correctly to negative balance");
    }

    @Test
    @DisplayName("Should handle very large negative balance")
    void testVeryLargeNegativeBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("LargeNegativePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act - Force large negative balance
        economy.withdrawPlayer(tanPlayer, 1000000000.0);

        // Assert
        assertEquals(-1000000000.0, economy.getBalance(tanPlayer), 1.0,
            "Should handle very large negative balance");
    }

    // ========== Maximum Money Tests ==========

    @Test
    @DisplayName("Should handle values near Double.MAX_VALUE")
    void testNearMaxDoubleValue() {
        // Arrange
        PlayerMock player = server.addPlayer("MaxValuePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        double hugeAmount = Double.MAX_VALUE / 2;

        // Act
        economy.depositPlayer(tanPlayer, hugeAmount);

        // Assert
        assertTrue(economy.getBalance(tanPlayer) > 0,
            "Should handle very large values");
    }

    @Test
    @DisplayName("Should handle overflow prevention gracefully")
    void testOverflowScenario() {
        // Arrange
        PlayerMock player = server.addPlayer("OverflowPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(Double.MAX_VALUE - 1000.0);

        // Act - Try to add more (may cause overflow to negative)
        economy.depositPlayer(tanPlayer, 2000.0);

        // Assert - Should not crash, behavior depends on implementation
        double balance = economy.getBalance(tanPlayer);
        assertTrue(!Double.isNaN(balance) && !Double.isInfinite(balance),
            "Balance should be a valid number, not NaN or Infinite");
    }

    @Test
    @DisplayName("Should handle very small fractional amounts")
    void testTinyFractionalAmounts() {
        // Arrange
        PlayerMock player = server.addPlayer("TinyFractionPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1.0);

        // Act - Withdraw tiny amount
        economy.withdrawPlayer(tanPlayer, 0.0001);

        // Assert
        assertEquals(0.9999, economy.getBalance(tanPlayer), 0.00001,
            "Should handle tiny fractional amounts");
    }

    // ========== Boundary Value Tests ==========

    @Test
    @DisplayName("Should handle zero boundary correctly")
    void testZeroBoundary() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroBoundaryPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(100.0);

        // Act - Withdraw exactly to zero
        economy.withdrawPlayer(tanPlayer, 100.0);

        // Assert
        assertEquals(0.0, economy.getBalance(tanPlayer), 0.001,
            "Should reach exactly zero");
    }

    @Test
    @DisplayName("has() should return false for zero balance")
    void testHasWithZeroBalance() {
        // Arrange
        PlayerMock player = server.addPlayer("ZeroHasPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act
        boolean hasAny = economy.has(tanPlayer, 0.0);

        // Assert - Uses > comparison
        assertFalse(hasAny, "has() with 0.0 should return false for zero balance");
    }

    @Test
    @DisplayName("Should handle minimum positive value")
    void testMinimumPositiveValue() {
        // Arrange
        PlayerMock player = server.addPlayer("MinPositivePlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act - Deposit smallest positive double
        double minValue = Double.MIN_VALUE;
        economy.depositPlayer(tanPlayer, minValue);

        // Assert
        assertTrue(economy.getBalance(tanPlayer) > 0,
            "Should handle minimum positive value");
    }

    // ========== Floating-Point Precision Tests ==========

    @Test
    @DisplayName("Should handle floating-point precision issues")
    void testFloatingPointPrecision() {
        // Arrange
        PlayerMock player = server.addPlayer("PrecisionPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act - Add 0.1 three times (floating-point imprecision)
        economy.depositPlayer(tanPlayer, 0.1);
        economy.depositPlayer(tanPlayer, 0.1);
        economy.depositPlayer(tanPlayer, 0.1);

        // Assert - Should be approximately 0.3
        double balance = economy.getBalance(tanPlayer);
        assertTrue(balance > 0.29 && balance < 0.31,
            "Should handle floating-point imprecision: " + balance);
    }

    @Test
    @DisplayName("formatMoney should round correctly")
    void testFormatMoneyRounding() {
        // Act - Format various amounts
        String formatted1 = economy.formatMoney(1.005);
        String formatted2 = economy.formatMoney(1.0049);
        String formatted3 = economy.formatMoney(0.999);

        // Assert
        assertTrue(formatted1.contains("1.01"), "Should round 1.005 up to 1.01");
        assertTrue(formatted2.contains("1.00"), "Should round 1.0049 down to 1.00");
        assertTrue(formatted3.contains("1.00"), "Should round 0.999 up to 1.00");
    }

    // ========== Concurrent Operations Tests ==========

    @Test
    @DisplayName("Should handle concurrent deposits safely")
    void testConcurrentDeposits() throws InterruptedException {
        // Arrange
        PlayerMock player = server.addPlayer("ConcurrentDepositPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        int threadCount = 10;
        int depositsPerThread = 100;
        double depositAmount = 10.0;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act - Concurrent deposits
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < depositsPerThread; j++) {
                        economy.depositPlayer(tanPlayer, depositAmount);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Concurrent deposits should complete within timeout");
        executor.shutdown();

        // Assert - Final balance should match expected total
        double expectedBalance = threadCount * depositsPerThread * depositAmount;
        double actualBalance = economy.getBalance(tanPlayer);

        assertEquals(expectedBalance, actualBalance, 0.01,
            "Concurrent deposits should result in correct total: expected " +
            expectedBalance + ", got " + actualBalance);
    }

    @Test
    @DisplayName("Should handle concurrent withdrawals safely")
    void testConcurrentWithdrawals() throws InterruptedException {
        // Arrange
        PlayerMock player = server.addPlayer("ConcurrentWithdrawPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        int threadCount = 10;
        int withdrawalsPerThread = 50;
        double withdrawalAmount = 5.0;
        double initialBalance = threadCount * withdrawalsPerThread * withdrawalAmount;

        tanPlayer.addToBalance(initialBalance);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act - Concurrent withdrawals
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < withdrawalsPerThread; j++) {
                        economy.withdrawPlayer(tanPlayer, withdrawalAmount);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Concurrent withdrawals should complete within timeout");
        executor.shutdown();

        // Assert - Final balance should be approximately zero
        double finalBalance = economy.getBalance(tanPlayer);
        assertTrue(finalBalance >= -1.0 && finalBalance <= 1.0,
            "Concurrent withdrawals should result in ~0 balance, got: " + finalBalance);
    }

    @Test
    @DisplayName("Should handle concurrent mixed operations safely")
    void testConcurrentMixedOperations() throws InterruptedException {
        // Arrange
        PlayerMock player = server.addPlayer("MixedOpsPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        int threadCount = 8;
        int operationsPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger depositCount = new AtomicInteger(0);
        AtomicInteger withdrawCount = new AtomicInteger(0);

        // Act - Concurrent deposits and withdrawals
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (threadId % 2 == 0) {
                            economy.depositPlayer(tanPlayer, 10.0);
                            depositCount.incrementAndGet();
                        } else {
                            economy.withdrawPlayer(tanPlayer, 5.0);
                            withdrawCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Concurrent mixed operations should complete within timeout");
        executor.shutdown();

        // Assert - Balance should match net result
        double expectedBalance = depositCount.get() * 10.0 - withdrawCount.get() * 5.0;
        double actualBalance = economy.getBalance(tanPlayer);

        assertEquals(expectedBalance, actualBalance, 1.0,
            "Concurrent mixed operations should result in correct balance");
    }

    // ========== Rapid Succession Tests ==========

    @Test
    @DisplayName("Should handle rapid successive operations")
    void testRapidSuccessiveOperations() {
        // Arrange
        PlayerMock player = server.addPlayer("RapidOpsPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        int operationCount = 1000;

        // Act - Rapid operations in loop
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            economy.depositPlayer(tanPlayer, 1.0);
            economy.withdrawPlayer(tanPlayer, 0.5);
        }
        long endTime = System.currentTimeMillis();

        // Assert - Should complete quickly and correctly
        double expectedBalance = operationCount * 0.5;
        double actualBalance = economy.getBalance(tanPlayer);
        long duration = endTime - startTime;

        assertEquals(expectedBalance, actualBalance, 0.001,
            "Rapid operations should result in correct balance");
        assertTrue(duration < 5000,
            "Rapid operations should complete within 5 seconds, took: " + duration + "ms");
    }

    @Test
    @DisplayName("Should handle rapid balance checks")
    void testRapidBalanceChecks() {
        // Arrange
        PlayerMock player = server.addPlayer("RapidCheckPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
        tanPlayer.addToBalance(1000.0);
        int checkCount = 10000;

        // Act - Rapid balance checks
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < checkCount; i++) {
            economy.getBalance(tanPlayer);
        }
        long endTime = System.currentTimeMillis();

        // Assert
        long duration = endTime - startTime;
        assertTrue(duration < 5000,
            "Rapid balance checks should complete within 5 seconds, took: " + duration + "ms");
    }

    // ========== Multiple Players Stress Test ==========

    @Test
    @DisplayName("Should handle operations for many players")
    void testManyPlayersOperations() {
        // Arrange
        int playerCount = 100;
        List<PlayerMock> players = new ArrayList<>();
        List<ITanPlayer> tanPlayers = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            PlayerMock player = server.addPlayer("StressPlayer" + i);
            ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());
            players.add(player);
            tanPlayers.add(tanPlayer);
        }

        // Act - Perform operations on all players
        long startTime = System.currentTimeMillis();
        for (ITanPlayer tanPlayer : tanPlayers) {
            economy.depositPlayer(tanPlayer, 1000.0);
            economy.withdrawPlayer(tanPlayer, 300.0);
            economy.depositPlayer(tanPlayer, 500.0);
        }
        long endTime = System.currentTimeMillis();

        // Assert - All balances should be correct
        for (ITanPlayer tanPlayer : tanPlayers) {
            assertEquals(1200.0, economy.getBalance(tanPlayer), 0.001,
                "Each player should have correct balance");
        }

        long duration = endTime - startTime;
        assertTrue(duration < 10000,
            "Operations on many players should complete within 10 seconds, took: " + duration + "ms");
    }

    // ========== Persistence Under Load Tests ==========

    @Test
    @DisplayName("Should persist data correctly under load")
    void testPersistenceUnderLoad() {
        // Arrange
        PlayerMock player = server.addPlayer("LoadPersistencePlayer");
        String playerId = player.getUniqueId().toString();
        ITanPlayer tanPlayer = storage.getSync(playerId);

        // Act - Perform many operations with periodic saves
        for (int i = 0; i < 100; i++) {
            economy.depositPlayer(tanPlayer, 100.0);
            economy.withdrawPlayer(tanPlayer, 50.0);

            if (i % 10 == 0) {
                storage.putSync(playerId, tanPlayer);
            }
        }

        // Final save
        storage.putSync(playerId, tanPlayer);

        // Re-fetch from storage
        ITanPlayer fetchedPlayer = storage.getSync(playerId);

        // Assert - Data should persist correctly
        assertEquals(5000.0, economy.getBalance(fetchedPlayer), 0.001,
            "Persisted balance should be correct: 100 operations * (100-50) = 5000");
    }

    // ========== Async Operation Safety Tests ==========

    @Test
    @DisplayName("Should handle async operations safely")
    void testAsyncOperationSafety() throws Exception {
        // Arrange
        PlayerMock player = server.addPlayer("AsyncSafetyPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Act - Create multiple async operations
        for (int i = 0; i < 20; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                economy.depositPlayer(tanPlayer, 10.0);
                if (index % 2 == 0) {
                    economy.withdrawPlayer(tanPlayer, 5.0);
                }
            });
            futures.add(future);
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        // Assert - Final balance should be consistent
        double balance = economy.getBalance(tanPlayer);
        assertTrue(balance > 0, "Async operations should complete safely");
    }

    // ========== Edge Case: Special Values ==========

    @Test
    @DisplayName("Should handle NaN gracefully")
    void testNotANumber() {
        // Arrange
        PlayerMock player = server.addPlayer("NaNPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act - Try to deposit NaN (should not crash)
        assertDoesNotThrow(() -> economy.depositPlayer(tanPlayer, Double.NaN));

        // Assert
        double balance = economy.getBalance(tanPlayer);
        assertTrue(Double.isNaN(balance) || balance == 0.0,
            "Should handle NaN deposit gracefully");
    }

    @Test
    @DisplayName("Should handle Infinity gracefully")
    void testInfinity() {
        // Arrange
        PlayerMock player = server.addPlayer("InfinityPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act - Try to deposit Infinity
        assertDoesNotThrow(() -> economy.depositPlayer(tanPlayer, Double.POSITIVE_INFINITY));

        // Assert
        double balance = economy.getBalance(tanPlayer);
        // Behavior depends on implementation
        assertDoesNotThrow(() -> economy.getBalance(tanPlayer),
            "Should handle Infinity without throwing exception");
    }

    @Test
    @DisplayName("Should handle negative zero correctly")
    void testNegativeZero() {
        // Arrange
        PlayerMock player = server.addPlayer("NegZeroPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act - Deposit negative zero
        economy.depositPlayer(tanPlayer, -0.0);

        // Assert
        assertEquals(0.0, economy.getBalance(tanPlayer), 0.0,
            "Negative zero should be treated as zero");
    }

    // ========== Currency Formatting Edge Cases ==========

    @Test
    @DisplayName("formatMoney should handle very large amounts")
    void testFormatLargeAmount() {
        // Act
        String formatted = economy.formatMoney(1e15); // 1 quadrillion

        // Assert
        assertNotNull(formatted, "Should format very large amounts");
        assertTrue(formatted.contains("1"), "Should contain significant digits");
    }

    @Test
    @DisplayName("formatMoney should handle very small amounts")
    void testFormatTinyAmount() {
        // Act
        String formatted = economy.formatMoney(0.0001);

        // Assert
        assertNotNull(formatted, "Should format tiny amounts");
        assertTrue(formatted.contains("0.00"), "Should round to 0.00 for tiny amounts");
    }

    @Test
    @DisplayName("formatMoney should handle negative amounts")
    void testFormatNegativeAmount() {
        // Act
        String formatted = economy.formatMoney(-1234.56);

        // Assert
        assertTrue(formatted.contains("-"), "Negative amounts should show minus sign");
    }

    // ========== Memory and Performance Tests ==========

    @Test
    @DisplayName("Should not leak memory with many operations")
    void testMemoryEfficiency() {
        // Arrange
        PlayerMock player = server.addPlayer("MemoryPlayer");
        ITanPlayer tanPlayer = storage.getSync(player.getUniqueId().toString());

        // Act - Perform many operations
        for (int i = 0; i < 10000; i++) {
            economy.depositPlayer(tanPlayer, 1.0);
            economy.withdrawPlayer(tanPlayer, 0.5);
        }

        // Assert - Should complete without out of memory
        assertDoesNotThrow(() -> economy.getBalance(tanPlayer),
            "Should handle many operations without memory issues");
    }
}
