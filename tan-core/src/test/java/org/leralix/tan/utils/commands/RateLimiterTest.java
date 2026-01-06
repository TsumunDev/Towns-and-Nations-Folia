package org.leralix.tan.utils.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for RateLimiter.
 * Tests thread-safety, cooldown behavior, and edge cases.
 */
class RateLimiterTest {

  private RateLimiter rateLimiter;
  private UUID player1;
  private UUID player2;

  @BeforeEach
  void setUp() {
    rateLimiter = new RateLimiter("test", 2, TimeUnit.SECONDS); // 2 second cooldown
    player1 = UUID.randomUUID();
    player2 = UUID.randomUUID();
  }

  @Test
  void testFirstExecutionIsAllowed() {
    var result = rateLimiter.canExecute(player1);
    assertTrue(result.isAllowed(), "First execution should be allowed");
    assertEquals(0, result.getRemainingMillis(), "Remaining time should be 0");
  }

  @Test
  void testImmediateSecondExecutionIsDenied() {
    rateLimiter.canExecute(player1); // First execution
    var result = rateLimiter.canExecute(player1); // Immediate second

    assertFalse(result.isAllowed(), "Immediate second execution should be denied");
    assertTrue(result.getRemainingMillis() > 0, "Remaining time should be positive");
    assertTrue(result.getRemainingMillis() <= 2000, "Remaining time should not exceed cooldown");
  }

  @Test
  void testDifferentPlayersIndependentCooldowns() {
    rateLimiter.canExecute(player1);
    rateLimiter.canExecute(player1); // player1 is now on cooldown

    var result = rateLimiter.canExecute(player2);
    assertTrue(result.isAllowed(), "Different player should not be affected by first player's cooldown");
  }

  @Test
  void testCooldownExpiresAfterTime() throws InterruptedException {
    rateLimiter.canExecute(player1);
    Thread.sleep(2100); // Wait longer than 2 second cooldown

    var result = rateLimiter.canExecute(player1);
    assertTrue(result.isAllowed(), "Execution should be allowed after cooldown expires");
  }

  @Test
  void testGetRemainingCooldown() {
    assertEquals(0, rateLimiter.getRemainingCooldown(player1), "No cooldown initially");

    rateLimiter.canExecute(player1);
    long remaining = rateLimiter.getRemainingCooldown(player1);
    assertTrue(remaining > 0, "Should have remaining cooldown after execution");
    assertTrue(remaining <= 2000, "Remaining should not exceed total cooldown");
  }

  @Test
  void testResetCooldown() {
    rateLimiter.canExecute(player1);
    rateLimiter.canExecute(player1); // On cooldown

    rateLimiter.resetCooldown(player1);
    var result = rateLimiter.canExecute(player1);
    assertTrue(result.isAllowed(), "After reset, execution should be allowed immediately");
  }

  @Test
  void testCheckOnlyDoesNotUpdateCooldown() {
    rateLimiter.canExecute(player1);
    var checkResult = rateLimiter.checkOnly(player1);

    assertFalse(checkResult.isAllowed(), "checkOnly should return denied state");

    // If checkOnly updated the cooldown, this would extend it
    long remainingAfterCheck = rateLimiter.getRemainingCooldown(player1);
    long remainingBeforeCheck = checkResult.getRemainingMillis();

    // Times should be approximately equal (within small tolerance for execution time)
    assertTrue(Math.abs(remainingAfterCheck - remainingBeforeCheck) < 100,
        "checkOnly should not update the cooldown timestamp");
  }

  @Test
  void testCheckOnlyAllowedWhenNotOnCooldown() {
    var result = rateLimiter.checkOnly(player1);
    assertTrue(result.isAllowed(), "checkOnly should return allowed when not on cooldown");
  }

  @Test
  void testClearAll() {
    rateLimiter.canExecute(player1);
    rateLimiter.canExecute(player2);

    rateLimiter.clearAll();

    assertEquals(0, rateLimiter.getRemainingCooldown(player1), "Cooldown should be cleared for player1");
    assertEquals(0, rateLimiter.getRemainingCooldown(player2), "Cooldown should be cleared for player2");
  }

  @Test
  void testGetCommandName() {
    assertEquals("test", rateLimiter.getCommandName(), "Command name should match");
  }

  @Test
  void testGetCooldownMillis() {
    assertEquals(2000, rateLimiter.getCooldownMillis(), "Cooldown millis should match");
  }

  @Test
  void testGetRemainingSecondsRoundsUp() {
    rateLimiter.canExecute(player1);
    var result = rateLimiter.checkOnly(player1);

    long seconds = result.getRemainingSeconds();
    assertTrue(seconds >= 1 && seconds <= 2, "Seconds should be rounded up");
  }

  @Test
  void testConstructorWithSeconds() {
    RateLimiter limiter = new RateLimiter("test", 5);
    assertEquals(5000, limiter.getCooldownMillis(), "Should convert seconds to millis");
  }

  @Test
  void testZeroCooldownAllowsImmediateExecution() {
    RateLimiter noCooldown = new RateLimiter("instant", 0, TimeUnit.SECONDS);

    noCooldown.canExecute(player1);
    var result = noCooldown.canExecute(player1);

    assertTrue(result.isAllowed(), "Zero cooldown should allow immediate re-execution");
  }

  @Test
  void testConcurrentAccess() throws InterruptedException {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    int[] successCount = {0};

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          if (rateLimiter.canExecute(player1).isAllowed()) {
            synchronized (successCount) {
              successCount[0]++;
            }
          }
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    // With proper synchronization, only the first thread should succeed
    assertEquals(1, successCount[0], "Only first execution should succeed in concurrent scenario");
  }

  @Test
  void testMultiplePlayersConcurrent() throws InterruptedException {
    int playerCount = 100;
    ExecutorService executor = Executors.newFixedThreadPool(20);
    CountDownLatch latch = new CountDownLatch(playerCount);
    int[] successCount = {0};

    for (int i = 0; i < playerCount; i++) {
      final UUID player = UUID.randomUUID();
      executor.submit(() -> {
        try {
          if (rateLimiter.canExecute(player).isAllowed()) {
            synchronized (successCount) {
              successCount[0]++;
            }
          }
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    // All different players should be allowed
    assertEquals(playerCount, successCount[0], "All different players should be allowed");
  }
}
