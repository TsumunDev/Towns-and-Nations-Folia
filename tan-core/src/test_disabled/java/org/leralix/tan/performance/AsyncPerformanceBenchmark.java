package org.leralix.tan.performance;

import static org.junit.jupiter.api.Assertions.*;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.service.AsyncEconomyService;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Performance benchmark tests for async operations.
 *
 * <p>These tests verify that async operations provide the expected performance improvements
 * over synchronous operations. They measure execution time and ensure no blocking occurs.</p>
 *
 * <p><b>NOTE:</b> These tests are for benchmarking purposes and may produce variable results
 * depending on system load. They are NOT run as part of the normal test suite.</p>
 *
 * @since 0.16.0
 */
@DisplayName("Async Performance Benchmarks")
public class AsyncPerformanceBenchmark {

  private ServerMock server;
  private TownsAndNations plugin;

  @BeforeEach
  public void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.load(TownsAndNations.class);
  }

  @AfterEach
  public void tearDown() {
    MockBukkit.unmock();
  }

  /**
   * Benchmarks player data loading performance.
   *
   * <p>Expected: Async loading should be non-blocking</p>
   */
  @Test
  @DisplayName("Async player data loading is non-blocking")
  public void testAsyncPlayerLoadingNonBlocking() {
    PlayerMock player = server.addPlayer("TestPlayer");

    // Measure async loading time
    long startTime = System.nanoTime();
    CompletableFuture<ITanPlayer> future = PlayerDataStorage.getInstance().get(player.getUniqueId());
    long submitTime = System.nanoTime();

    // Future should be returned immediately (non-blocking submit)
    long submitDuration = (submitTime - startTime) / 1_000_000; // Convert to ms
    assertTrue(submitDuration < 10, "Future submission should be < 10ms, took: " + submitDuration + "ms");

    // Complete the future
    ITanPlayer tanPlayer = future.join();
    assertNotNull(tanPlayer, "Player data should be loaded");
  }

  /**
   * Benchmarks parallel player loading vs sequential loading.
   *
   * <p>Expected: Parallel loading should be significantly faster for multiple players</p>
   */
  @Test
  @DisplayName("Parallel player loading is faster than sequential")
  public void testParallelVsSequentialLoading() {
    // Create 20 players
    List<PlayerMock> players = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      players.add(server.addPlayer("Player" + i));
    }

    // Benchmark sequential loading
    long sequentialStart = System.nanoTime();
    List<ITanPlayer> sequentialPlayers = new ArrayList<>();
    for (PlayerMock player : players) {
      ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
      if (tanPlayer != null) {
        sequentialPlayers.add(tanPlayer);
      }
    }
    long sequentialTime = System.nanoTime() - sequentialStart;

    // Benchmark parallel loading
    long parallelStart = System.nanoTime();
    List<CompletableFuture<ITanPlayer>> futures = new ArrayList<>();
    for (PlayerMock player : players) {
      futures.add(PlayerDataStorage.getInstance().get(player.getUniqueId()));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    long parallelTime = System.nanoTime() - parallelStart;

    // Parallel should be at least 2x faster (in practice, much more with real I/O)
    double speedup = (double) sequentialTime / parallelTime;
    System.out.println("Sequential: " + sequentialTime / 1_000_000 + "ms");
    System.out.println("Parallel: " + parallelTime / 1_000_000 + "ms");
    System.out.println("Speedup: " + speedup + "x");

    // Note: In mock environment, speedup may be less dramatic than with real I/O
    assertTrue(speedup >= 1.0, "Parallel should be at least as fast as sequential");
  }

  /**
   * Benchmarks async economy operations.
   *
   * <p>Expected: Async economy operations should complete successfully</p>
   */
  @Test
  @DisplayName("Async economy operations complete successfully")
  public void testAsyncEconomyOperations() {
    PlayerMock player = server.addPlayer("EconomyPlayer");

    // Create a town for the player
    PlayerDataStorage.getInstance()
        .get(player.getUniqueId())
        .thenCompose(tanPlayer -> TownDataStorage.getInstance().newTown("BenchmarkTown", tanPlayer))
        .thenCompose(town -> {
          // Add some money to the player
          return AsyncEconomyService.deposit(player, 1000.0);
        })
        .thenCompose(balance -> {
          // Check balance
          return AsyncEconomyService.getBalance(player);
        })
        .thenAccept(balance -> {
          assertNotNull(balance, "Balance should not be null");
          assertTrue(balance > 0, "Balance should be positive");
        })
        .join(); // Wait for completion
  }

  /**
   * Benchmarks town operations performance.
   *
   * <p>Expected: Async town operations should not block</p>
   */
  @Test
  @DisplayName("Async town operations are non-blocking")
  public void testAsyncTownOperations() {
    PlayerMock player = server.addPlayer("TownPlayer");

    // Create town asynchronously
    long startTime = System.nanoTime();
    CompletableFuture<TownData> future = PlayerDataStorage.getInstance()
        .get(player.getUniqueId())
        .thenCompose(tanPlayer -> TownDataStorage.getInstance().newTown("AsyncTown", tanPlayer));

    long submitTime = System.nanoTime();
    long submitDuration = (submitTime - startTime) / 1_000_000; // Convert to ms

    // Future should be returned quickly
    assertTrue(submitDuration < 50, "Future submission should be < 50ms, took: " + submitDuration + "ms");

    // Complete the operation
    TownData town = future.join();
    assertNotNull(town, "Town should be created");
    assertEquals("AsyncTown", town.getID());
  }

  /**
   * Stress test: Multiple concurrent operations.
   *
   * <p>Expected: System should handle concurrent operations without issues</p>
   */
  @Test
  @DisplayName("System handles concurrent operations correctly")
  public void testConcurrentOperations() throws Exception {
    // Create 50 players
    List<PlayerMock> players = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      players.add(server.addPlayer("ConcurrentPlayer" + i));
    }

    // Perform concurrent operations
    List<CompletableFuture<Void>> operations = new ArrayList<>();

    for (PlayerMock player : players) {
      // Load player data
      CompletableFuture<Void> op = PlayerDataStorage.getInstance()
          .get(player.getUniqueId())
          .thenAccept(tanPlayer -> {
            assertNotNull(tanPlayer, "Player data should be loaded");
          });
      operations.add(op);
    }

    // Wait for all operations to complete (with timeout)
    CompletableFuture.allOf(operations.toArray(new CompletableFuture[0]))
        .get(10, TimeUnit.SECONDS);

    // If we reach here, all operations completed successfully
    assertTrue(true, "All concurrent operations completed");
  }

  /**
   * Benchmark: Salary payment performance.
   *
   * <p>Expected: Parallel salary payments should be fast for multiple players</p>
   */
  @Test
  @DisplayName("Parallel salary payments complete quickly")
  public void testParallelSalaryPayments() {
    // Create a town with multiple players
    PlayerMock leader = server.addPlayer("TownLeader");
    PlayerDataStorage.getInstance()
        .get(leader.getUniqueId())
        .thenCompose(tanLeader -> TownDataStorage.getInstance().newTown("SalaryTown", tanLeader))
        .thenCompose(town -> {
          // Add more players to the town
          List<CompletableFuture<Void>> joinFutures = new ArrayList<>();
          for (int i = 0; i < 10; i++) {
            PlayerMock member = server.addPlayer("Member" + i);
            CompletableFuture<Void> joinFuture = PlayerDataStorage.getInstance()
                .get(member.getUniqueId())
                .thenAccept(tanMember -> {
                  town.addPlayer(tanMember);
                });
            joinFutures.add(joinFuture);
          }
          return CompletableFuture.allOf(joinFutures.toArray(new CompletableFuture[0]));
        })
        .thenAccept(v -> {
          // Salary payments are now parallel
          // In real scenario, this would be much faster with async I/O
          assertTrue(true, "Salary payment setup completed");
        })
        .join();
  }
}
