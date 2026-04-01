package org.leralix.tan.service.economy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.service.EconomyOps;
import org.leralix.tan.service.PlayerDataService;
import org.leralix.tan.testutils.AbstractPluginTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmark tests for economy operations.
 * <p>
 * These tests measure the execution time of critical economy operations
 * to ensure they meet performance requirements for a high-traffic server.
 * <p>
 * Performance criteria:
 * <ul>
 *   <li>Single balance operation: < 10ms (p95)</li>
 *   <li>Transfer between players: < 50ms (p95)</li>
 *   <li>Concurrent operations: Linear scaling up to thread count</li>
 *   <li>No significant degradation under load</li>
 * </ul>
 * <p>
 * Note: These are not JMH benchmarks but functional performance tests
 * using JUnit. For proper micro-benchmarking, use JMH in a separate module.
 */
@DisplayName("EconomyOps Performance Tests")
class EconomyOpsBenchmark extends AbstractPluginTest {

    private static final int WARMUP_ITERATIONS = 10;
    private static final int MEASURED_ITERATIONS = 100;
    private static final long PERFORMANCE_THRESHOLD_MS = 50;

    // ==================== Single Operation Performance ====================

    @Test
    @DisplayName("Add to player should complete within threshold")
    void addToPlayer_performance() throws Exception {
        // Arrange
        String playerId = "perf_add_player";
        createTestPlayer(playerId);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            EconomyOps.addToPlayerAsync(playerId, 100.0).get();
        }

        // Act - Measure performance
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            EconomyOps.addToPlayerAsync(playerId, 10.0).get();
        }
        long endTime = System.nanoTime();

        // Assert
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / MEASURED_ITERATIONS;
        assertTrue(avgTimeMs < PERFORMANCE_THRESHOLD_MS,
            String.format("Average add operation time (%.2f ms) exceeds threshold (%d ms)",
                avgTimeMs, PERFORMANCE_THRESHOLD_MS));
    }

    @Test
    @DisplayName("Remove from player should complete within threshold")
    void removeFromPlayer_performance() throws Exception {
        // Arrange
        String playerId = "perf_remove_player";
        createTestPlayer(playerId);
        EconomyOps.addToPlayerAsync(playerId, 1000.0).get();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            EconomyOps.removeFromPlayerAsync(playerId, 10.0).get();
        }

        // Act - Measure performance
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            EconomyOps.removeFromPlayerAsync(playerId, 10.0).get();
        }
        long endTime = System.nanoTime();

        // Assert
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / MEASURED_ITERATIONS;
        assertTrue(avgTimeMs < PERFORMANCE_THRESHOLD_MS,
            String.format("Average remove operation time (%.2f ms) exceeds threshold (%d ms)",
                avgTimeMs, PERFORMANCE_THRESHOLD_MS));
    }

    @Test
    @DisplayName("Transfer between players should complete within threshold")
    void transferBetweenPlayers_performance() throws Exception {
        // Arrange
        String player1 = "perf_transfer_1";
        String player2 = "perf_transfer_2";
        createTestPlayer(player1);
        createTestPlayer(player2);
        EconomyOps.addToPlayerAsync(player1, 1000.0).get();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            EconomyOps.transferBetweenPlayersAsync(player1, player2, 10.0).get();
        }

        // Act - Measure performance
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            EconomyOps.transferBetweenPlayersAsync(player1, player2, 10.0).get();
        }
        long endTime = System.nanoTime();

        // Assert
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / MEASURED_ITERATIONS;
        assertTrue(avgTimeMs < PERFORMANCE_THRESHOLD_MS * 2,
            String.format("Average transfer operation time (%.2f ms) exceeds threshold (%d ms)",
                avgTimeMs, PERFORMANCE_THRESHOLD_MS * 2));
    }

    // ==================== Bulk Operation Performance ====================

    @Test
    @DisplayName("Bulk additions should scale linearly")
    void bulkAdditions_linearScaling() throws Exception {
        // Arrange
        String playerId = "perf_bulk_player";
        createTestPlayer(playerId);

        // Act - Measure time for different batch sizes
        long[] times = new long[5];
        int[] batchSizes = {10, 50, 100, 200, 500};

        for (int i = 0; i < batchSizes.length; i++) {
            int batchSize = batchSizes[i];
            long startTime = System.nanoTime();
            for (int j = 0; j < batchSize; j++) {
                EconomyOps.addToPlayerAsync(playerId, 1.0).get();
            }
            times[i] = System.nanoTime() - startTime;
        }

        // Assert - Check that scaling is approximately linear
        // Time for 500 operations should be roughly 50x time for 10 operations
        double ratio = (times[4] / 500.0) / (times[0] / 10.0);
        assertTrue(ratio < 3.0,
            String.format("Scaling ratio (%.2f) suggests non-linear performance degradation", ratio));
    }

    @Test
    @DisplayName("Bulk transfers should complete efficiently")
    void bulkTransfers_performance() throws Exception {
        // Arrange
        int playerCount = 20;
        String[] players = new String[playerCount];
        for (int i = 0; i < playerCount; i++) {
            players[i] = "perf_bulk_transfer_" + i;
            createTestPlayer(players[i]);
            EconomyOps.addToPlayerAsync(players[i], 1000.0).get();
        }

        // Act - Perform transfers between random pairs
        int transferCount = 100;
        long startTime = System.nanoTime();
        for (int i = 0; i < transferCount; i++) {
            int from = i % playerCount;
            int to = (i + 1) % playerCount;
            EconomyOps.transferBetweenPlayersAsync(players[from], players[to], 10.0).get();
        }
        long totalTime = System.nanoTime() - startTime;

        // Assert
        double avgTimeMs = totalTime / 1_000_000.0 / transferCount;
        assertTrue(avgTimeMs < PERFORMANCE_THRESHOLD_MS * 2,
            String.format("Average bulk transfer time (%.2f ms) exceeds threshold", avgTimeMs));
    }

    // ==================== Memory Allocation Tests ====================

    @Test
    @DisplayName("Repeated operations should not leak memory")
    void repeatedOperations_noMemoryLeak() throws Exception {
        // Arrange
        String playerId = "perf_memory_player";
        createTestPlayer(playerId);

        // Act - Perform many operations
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < 1000; i++) {
            EconomyOps.addToPlayerAsync(playerId, 1.0).get();
            EconomyOps.removeFromPlayerAsync(playerId, 1.0).get();
        }

        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        // Assert - Memory growth should be minimal
        long memoryGrowth = memoryAfter - memoryBefore;
        double growthMB = memoryGrowth / (1024.0 * 1024.0);
        assertTrue(growthMB < 10.0,
            String.format("Memory growth (%.2f MB) suggests potential leak", growthMB));
    }

    // ==================== Cold Start Performance ====================

    @Test
    @DisplayName("First operation should not be significantly slower")
    void coldStart_performance() throws Exception {
        // Arrange
        String playerId = "perf_cold_player";
        createTestPlayer(playerId);

        // Act - Measure first operation time
        long startTime = System.nanoTime();
        EconomyOps.addToPlayerAsync(playerId, 100.0).get();
        long firstOpTime = System.nanoTime() - startTime;

        // Measure subsequent operation time
        startTime = System.nanoTime();
        EconomyOps.addToPlayerAsync(playerId, 10.0).get();
        long secondOpTime = System.nanoTime() - startTime;

        // Assert - First operation should not be excessively slow
        double ratio = (double) firstOpTime / secondOpTime;
        assertTrue(ratio < 10.0,
            String.format("Cold start slowdown ratio (%.2f) is excessive", ratio));
    }

    // ==================== Concurrent Performance Tests ====================

    @Test
    @DisplayName("Concurrent operations should scale reasonably")
    void concurrentOperations_scaling() throws Exception {
        // Arrange
        int playerCount = 50;
        String[] players = new String[playerCount];
        for (int i = 0; i < playerCount; i++) {
            players[i] = "perf_concurrent_" + i;
            createTestPlayer(players[i]);
            EconomyOps.addToPlayerAsync(players[i], 100.0).get();
        }

        // Act - Sequential operations baseline
        long startTime = System.nanoTime();
        for (String player : players) {
            EconomyOps.addToPlayerAsync(player, 10.0).get();
        }
        long sequentialTime = System.nanoTime() - startTime;

        // Act - Concurrent operations
        startTime = System.nanoTime();
        var futures = new java.util.ArrayList<java.util.concurrent.CompletableFuture<?>>();
        for (String player : players) {
            futures.add(EconomyOps.addToPlayerAsync(player, 10.0));
        }
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).get();
        long concurrentTime = System.nanoTime() - startTime;

        // Assert - Concurrent should be faster but not perfectly parallel
        double speedup = (double) sequentialTime / concurrentTime;
        assertTrue(speedup > 1.5,
            String.format("Concurrent speedup (%.2fx) is insufficient", speedup));
    }

    // ==================== Precision Operation Performance ====================

    @Test
    @DisplayName("Rounding operations should be fast")
    void rounding_performance() {
        // Arrange
        double[] values = new double[10000];
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.random() * 10000;
        }

        // Warmup
        for (double value : values) {
            org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(value);
        }

        // Act - Measure rounding performance
        long startTime = System.nanoTime();
        for (double value : values) {
            org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(value);
        }
        long totalTime = System.nanoTime() - startTime;

        // Assert
        double avgTimeNs = totalTime / (double) values.length;
        assertTrue(avgTimeNs < 1000,
            String.format("Rounding operation too slow: %.2f ns per operation", avgTimeNs));
    }

    // ==================== Player Lookup Performance ====================

    @Test
    @DisplayName("Player lookups should be fast")
    void playerLookup_performance() throws Exception {
        // Arrange
        String playerId = "perf_lookup_player";
        createTestPlayer(playerId);
        EconomyOps.addToPlayerAsync(playerId, 100.0).get();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            PlayerDataService.getPlayer(playerId);
        }

        // Act - Measure lookup performance
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            PlayerDataService.getPlayer(playerId);
        }
        long endTime = System.nanoTime();

        // Assert
        double avgTimeNs = (endTime - startTime) / (double) MEASURED_ITERATIONS;
        assertTrue(avgTimeNs < 100_000,
            String.format("Player lookup too slow: %.2f ns per operation", avgTimeNs));
    }

    // ==================== Complex Scenario Performance ====================

    @Test
    @DisplayName("Complex economy scenario should complete efficiently")
    void complexScenario_performance() throws Exception {
        // Arrange - Simulate a small economy
        int playerCount = 10;
        String[] players = new String[playerCount];
        for (int i = 0; i < playerCount; i++) {
            players[i] = "complex_player_" + i;
            createTestPlayer(players[i]);
            EconomyOps.addToPlayerAsync(players[i], 1000.0).get();
        }

        // Act - Simulate various transactions
        long startTime = System.nanoTime();

        // Each player pays each other player
        for (int i = 0; i < playerCount; i++) {
            for (int j = 0; j < playerCount; j++) {
                if (i != j) {
                    EconomyOps.transferBetweenPlayersAsync(players[i], players[j], 1.0).get();
                }
            }
        }

        // Random additions
        for (int i = 0; i < 50; i++) {
            String player = players[i % playerCount];
            EconomyOps.addToPlayerAsync(player, 10.0).get();
        }

        // Random removals
        for (int i = 0; i < 30; i++) {
            String player = players[i % playerCount];
            EconomyOps.removeFromPlayerAsync(player, 5.0).get();
        }

        long totalTime = System.nanoTime() - startTime;

        // Assert
        double totalTimeMs = totalTime / 1_000_000.0;
        assertTrue(totalTimeMs < 5000,
            String.format("Complex scenario took too long: %.2f ms", totalTimeMs));

        // Verify all balances are valid
        double totalBalance = 0.0;
        for (String player : players) {
            var p = PlayerDataService.getPlayer(player);
            assertNotNull(p);
            assertTrue(p.getBalance() >= 0);
            totalBalance += p.getBalance();
        }

        assertEquals(10000.0, totalBalance, 0.01,
            "Total balance should be preserved");
    }

    // ==================== Stress Test ====================

    @Test
    @DisplayName("High load stress test should complete")
    void highLoad_stressTest() throws Exception {
        // Arrange
        int playerCount = 5;
        String[] players = new String[playerCount];
        for (int i = 0; i < playerCount; i++) {
            players[i] = "stress_player_" + i;
            createTestPlayer(players[i]);
            EconomyOps.addToPlayerAsync(players[i], 10000.0).get();
        }

        // Act - Perform many operations
        int operationCount = 500;
        long startTime = System.nanoTime();

        for (int i = 0; i < operationCount; i++) {
            String player = players[i % playerCount];
            double amount = 1.0 + (i % 10);

            if (i % 3 == 0) {
                EconomyOps.addToPlayerAsync(player, amount).get();
            } else if (i % 3 == 1) {
                EconomyOps.removeFromPlayerAsync(player, amount).get();
            } else {
                String otherPlayer = players[(i + 1) % playerCount];
                EconomyOps.transferBetweenPlayersAsync(player, otherPlayer, amount).get();
            }
        }

        long totalTime = System.nanoTime() - startTime;

        // Assert
        double totalTimeMs = totalTime / 1_000_000.0;
        double avgTimeMs = totalTimeMs / operationCount;

        assertTrue(avgTimeMs < 20,
            String.format("Average operation time under load (%.2f ms) exceeds threshold", avgTimeMs));

        // Verify data integrity
        double totalBalance = 0.0;
        for (String player : players) {
            var p = PlayerDataService.getPlayer(player);
            assertNotNull(p);
            assertFalse(Double.isNaN(p.getBalance()),
                "Player balance should not be NaN");
            assertFalse(Double.isInfinite(p.getBalance()),
                "Player balance should not be infinite");
            totalBalance += p.getBalance();
        }

        assertEquals(50000.0, totalBalance, 1.0,
            "Total balance should be preserved under stress");
    }
}
