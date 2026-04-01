package org.leralix.tan.utils.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RateLimiter}.
 * <p>
 * Tests thread-safety, cooldown behavior, and edge cases for command rate limiting.
 */
@DisplayName("RateLimiter Tests")
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
    @DisplayName("First execution should be allowed")
    void testFirstExecutionIsAllowed() {
        var result = rateLimiter.canExecute(player1);
        assertTrue(result.isAllowed(), "First execution should be allowed");
        assertEquals(0, result.getRemainingMillis(), "Remaining time should be 0");
    }

    @Test
    @DisplayName("Immediate second execution should be denied")
    void testImmediateSecondExecutionIsDenied() {
        rateLimiter.canExecute(player1); // First execution
        var result = rateLimiter.canExecute(player1); // Immediate second

        assertFalse(result.isAllowed(), "Immediate second execution should be denied");
        assertTrue(result.getRemainingMillis() > 0, "Remaining time should be positive");
        assertTrue(result.getRemainingMillis() <= 2000, "Remaining time should not exceed cooldown");
    }

    @Test
    @DisplayName("Different players should have independent cooldowns")
    void testDifferentPlayersIndependentCooldowns() {
        rateLimiter.canExecute(player1);
        rateLimiter.canExecute(player1); // player1 is now on cooldown

        var result = rateLimiter.canExecute(player2);
        assertTrue(result.isAllowed(), "Different player should not be affected by first player's cooldown");
    }

    @Test
    @DisplayName("Cooldown should expire after time")
    void testCooldownExpiresAfterTime() throws InterruptedException {
        rateLimiter.canExecute(player1);
        Thread.sleep(2100); // Wait longer than 2 second cooldown

        var result = rateLimiter.canExecute(player1);
        assertTrue(result.isAllowed(), "Execution should be allowed after cooldown expires");
    }

    @Test
    @DisplayName("Should get remaining cooldown correctly")
    void testGetRemainingCooldown() {
        assertEquals(0, rateLimiter.getRemainingCooldown(player1), "No cooldown initially");

        rateLimiter.canExecute(player1);
        long remaining = rateLimiter.getRemainingCooldown(player1);
        assertTrue(remaining > 0, "Should have remaining cooldown after execution");
        assertTrue(remaining <= 2000, "Remaining should not exceed total cooldown");
    }

    @Test
    @DisplayName("Should reset cooldown manually")
    void testResetCooldown() {
        rateLimiter.canExecute(player1);
        rateLimiter.canExecute(player1); // On cooldown

        rateLimiter.resetCooldown(player1);
        var result = rateLimiter.canExecute(player1);
        assertTrue(result.isAllowed(), "After reset, execution should be allowed immediately");
    }

    @Test
    @DisplayName("checkOnly should not update cooldown")
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
    @DisplayName("checkOnly should return allowed when not on cooldown")
    void testCheckOnlyAllowedWhenNotOnCooldown() {
        var result = rateLimiter.checkOnly(player1);
        assertTrue(result.isAllowed(), "checkOnly should return allowed when not on cooldown");
    }

    @Test
    @DisplayName("Should clear all cooldowns")
    void testClearAll() {
        rateLimiter.canExecute(player1);
        rateLimiter.canExecute(player2);

        rateLimiter.clearAll();

        assertEquals(0, rateLimiter.getRemainingCooldown(player1), "Cooldown should be cleared for player1");
        assertEquals(0, rateLimiter.getRemainingCooldown(player2), "Cooldown should be cleared for player2");
    }

    @Test
    @DisplayName("Should get command name")
    void testGetCommandName() {
        assertEquals("test", rateLimiter.getCommandName(), "Command name should match");
    }

    @Test
    @DisplayName("Should get cooldown millis")
    void testGetCooldownMillis() {
        assertEquals(2000, rateLimiter.getCooldownMillis(), "Cooldown millis should match");
    }

    @Test
    @DisplayName("Remaining seconds should round up")
    void testGetRemainingSecondsRoundsUp() {
        rateLimiter.canExecute(player1);
        var result = rateLimiter.checkOnly(player1);

        long seconds = result.getRemainingSeconds();
        assertTrue(seconds >= 1 && seconds <= 2, "Seconds should be rounded up");
    }

    @Test
    @DisplayName("Should accept seconds in constructor")
    void testConstructorWithSeconds() {
        RateLimiter limiter = new RateLimiter("test", 5);
        assertEquals(5000, limiter.getCooldownMillis(), "Should convert seconds to millis");
    }

    @Test
    @DisplayName("Zero cooldown should allow immediate execution")
    void testZeroCooldownAllowsImmediateExecution() {
        RateLimiter noCooldown = new RateLimiter("instant", 0, TimeUnit.SECONDS);

        noCooldown.canExecute(player1);
        var result = noCooldown.canExecute(player1);

        assertTrue(result.isAllowed(), "Zero cooldown should allow immediate re-execution");
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
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

        // Due to race conditions, a few threads might succeed before cooldown is applied
        // But not all threads should succeed (that would indicate no cooldown)
        assertTrue(successCount[0] >= 1, "At least one execution should succeed");
        assertTrue(successCount[0] < threadCount, "Not all threads should succeed (cooldown should limit executions)");
        // In most cases, should be 1-2 due to minimal race window
        assertTrue(successCount[0] <= 3, "Rate limiter should prevent most concurrent executions");
    }

    @Test
    @DisplayName("Should handle multiple concurrent players")
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