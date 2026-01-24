package org.leralix.tan.utils.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for RateLimitRegistry.
 * Tests singleton behavior, limiter registration, and command execution checks.
 */
class RateLimitRegistryTest {

  private RateLimitRegistry registry;
  private UUID player1;

  @BeforeEach
  void setUp() {
    // Get the singleton instance - already initialized by JVM
    registry = RateLimitRegistry.getInstance();
    player1 = UUID.randomUUID();
  }

  @AfterEach
  void tearDown() {
    if (registry != null) {
      registry.clearAll();
    }
  }

  @Test
  void testSingletonReturnsSameInstance() {
    RateLimitRegistry instance1 = RateLimitRegistry.getInstance();
    RateLimitRegistry instance2 = RateLimitRegistry.getInstance();

    assertSame(instance1, instance2, "getInstance should return the same instance");
  }

  @Test
  void testCanExecuteWithUnregisteredCommand() {
    var result = registry.canExecute("unregistered_command", player1);

    assertTrue(result.isAllowed(), "Unregistered commands should be allowed (no cooldown)");
  }

  @Test
  void testCanExecuteWithRegisteredCommand() {
    registry.registerLimiter("test_command", 2, TimeUnit.SECONDS);

    var result1 = registry.canExecute("test_command", player1);
    assertTrue(result1.isAllowed(), "First execution should be allowed");

    var result2 = registry.canExecute("test_command", player1);
    assertFalse(result2.isAllowed(), "Immediate second execution should be denied");
  }

  @Test
  void testRegisterLimiterReturnsLimiter() {
    RateLimiter limiter = registry.registerLimiter("new_command", 5, TimeUnit.SECONDS);

    assertNotNull(limiter, "registerLimiter should return a RateLimiter");
    assertEquals("new_command", limiter.getCommandName());
    assertEquals(5000, limiter.getCooldownMillis());
  }

  @Test
  void testRegisterLimiterWithSeconds() {
    RateLimiter limiter = registry.registerLimiter("cmd", 10);

    assertEquals(10000, limiter.getCooldownMillis(), "Should convert seconds to millis");
  }

  @Test
  void testGetLimiterReturnsNullForUnregistered() {
    RateLimiter limiter = registry.getLimiter("nonexistent");

    assertNull(limiter, "getLimiter should return null for unregistered commands");
  }

  @Test
  void testGetLimiterReturnsExistingLimiter() {
    registry.registerLimiter("my_command", 3, TimeUnit.SECONDS);
    RateLimiter limiter = registry.getLimiter("my_command");

    assertNotNull(limiter, "getLimiter should return the registered limiter");
    assertEquals("my_command", limiter.getCommandName());
  }

  @Test
  void testGetOrCreateLimiterReturnsExisting() {
    registry.registerLimiter("existing", 5, TimeUnit.SECONDS);
    RateLimiter limiter = registry.getOrCreateLimiter("existing");

    assertNotNull(limiter);
    assertEquals("existing", limiter.getCommandName());
  }

  @Test
  void testGetOrCreateLimiterCreatesNew() {
    RateLimiter limiter = registry.getOrCreateLimiter("new_command");

    assertNotNull(limiter, "getOrCreateLimiter should create a new limiter");
    assertEquals(0, limiter.getCooldownMillis(), "Default cooldown should be 0");
  }

  @Test
  void testCheckOnlyDoesNotUpdateCooldown() {
    registry.registerLimiter("test", 2, TimeUnit.SECONDS);

    registry.canExecute("test", player1); // First execution
    var checkResult = registry.checkOnly("test", player1);
    assertFalse(checkResult.isAllowed());

    // checkOnly should not have updated the timestamp
    long cooldown = registry.getLimiter("test").getRemainingCooldown(player1);
    assertTrue(cooldown > 0 && cooldown <= 2000, "Cooldown should still be active");
  }

  @Test
  void testClearPlayerCooldowns() {
    registry.registerLimiter("cmd1", 5, TimeUnit.SECONDS);
    registry.registerLimiter("cmd2", 5, TimeUnit.SECONDS);

    registry.canExecute("cmd1", player1);
    registry.canExecute("cmd2", player1);

    // Both should be on cooldown
    assertFalse(registry.checkOnly("cmd1", player1).isAllowed());
    assertFalse(registry.checkOnly("cmd2", player1).isAllowed());

    registry.clearPlayerCooldowns(player1);

    // Both should be cleared
    assertTrue(registry.checkOnly("cmd1", player1).isAllowed());
    assertTrue(registry.checkOnly("cmd2", player1).isAllowed());
  }

  @Test
  void testClearAllRemovesAllCooldowns() {
    registry.registerLimiter("cmd", 5, TimeUnit.SECONDS);

    UUID player1 = UUID.randomUUID();
    UUID player2 = UUID.randomUUID();

    registry.canExecute("cmd", player1);
    registry.canExecute("cmd", player2);

    registry.clearAll();

    assertTrue(registry.checkOnly("cmd", player1).isAllowed());
    assertTrue(registry.checkOnly("cmd", player2).isAllowed());
  }

  @Test
  void testDefaultCooldownsInitialized() {
    // These commands should be registered by initializeDefaults()
    assertNotNull(registry.getLimiter("balance"), "balance command should have default limiter");
    assertNotNull(registry.getLimiter("claim"), "claim command should have default limiter");
    assertNotNull(registry.getLimiter("create"), "create command should have default limiter");
    assertNotNull(registry.getLimiter("pay"), "pay command should have default limiter");
  }

  @Test
  void testDefaultCooldownValues() {
    // balance uses FAST_COMMAND_COOLDOWN (1 second)
    RateLimiter balanceLimiter = registry.getLimiter("balance");
    assertEquals(1000, balanceLimiter.getCooldownMillis(), "balance should have 1s cooldown");

    // claim uses NORMAL_COMMAND_COOLDOWN (2 seconds)
    RateLimiter claimLimiter = registry.getLimiter("claim");
    assertEquals(2000, claimLimiter.getCooldownMillis(), "claim should have 2s cooldown");

    // create uses SLOW_COMMAND_COOLDOWN (5 seconds)
    RateLimiter createLimiter = registry.getLimiter("create");
    assertEquals(5000, createLimiter.getCooldownMillis(), "create should have 5s cooldown");
  }

  @Test
  void testMultipleCommandsIndependent() {
    registry.registerLimiter("cmd1", 1, TimeUnit.SECONDS);
    registry.registerLimiter("cmd2", 1, TimeUnit.SECONDS);

    registry.canExecute("cmd1", player1);
    // cmd1 is on cooldown

    var cmd2Result = registry.canExecute("cmd2", player1);
    assertTrue(cmd2Result.isAllowed(), "Different command should not be affected");
  }

  @Test
  void testMultiplePlayersIndependent() {
    registry.registerLimiter("cmd", 2, TimeUnit.SECONDS);

    UUID player2 = UUID.randomUUID();

    registry.canExecute("cmd", player1);
    registry.canExecute("cmd", player1); // player1 on cooldown

    var result = registry.canExecute("cmd", player2);
    assertTrue(result.isAllowed(), "Different player should have independent cooldown");
  }
}
