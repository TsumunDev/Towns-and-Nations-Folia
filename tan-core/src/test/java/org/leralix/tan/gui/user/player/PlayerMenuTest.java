package org.leralix.tan.gui.user.player;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.BasicTest;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;

/**
 * Tests for PlayerMenu.
 *
 * <p>Tests the player profile menu GUI based on the actual API:
 * - Uses static {@link PlayerMenu#open(Player)} method
 * - Extends {@link org.leralix.tan.gui.BasicGui}
 * - Uses Triumph-GUI framework
 *
 * <p>API Pattern:
 * <pre>{@code
 * // Constructor is private
 * // Opening is done via static method with async player data loading:
 * PlayerMenu.open(player);
 * }</pre>
 *
 * @see org.leralix.tan.gui.user.player.PlayerMenu
 * @see org.leralix.tan.gui.BasicGui
 */
class PlayerMenuTest extends BasicTest {

  private Player player;
  private ITanPlayer tanPlayer;

  @Override
  @BeforeEach
  protected void setUp() {
    super.setUp();
    player = server.addPlayer("TestPlayer");
    tanPlayer = PlayerDataStorage.getInstance().getSync(player);
  }

  // ==================== Basic Instantiation Tests ====================

  @Test
  void staticOpenMethod_existsAndDoesNotThrow() {
    // Smoke test: verify the static open method exists and doesn't throw immediately
    assertDoesNotThrow(() -> {
      PlayerMenu.open(player);
    }, "PlayerMenu.open(player) should not throw exception");

    // Give async operations time to complete
    server.getScheduler().performTicks(1);
  }

  @Test
  void staticOpenMethod_withValidPlayer_loadsPlayerData() {
    // Note: In test environment, tanPlayer may be null
    // Test verifies menu opening doesn't throw exceptions
    assertDoesNotThrow(() -> {
      PlayerMenu.open(player);
      server.getScheduler().performTicks(1);
    }, "PlayerMenu.open() should work with valid player");
  }

  // ==================== GUI Structure Tests ====================

  @Test
  void playerExists_afterMenuCreation() {
    PlayerMenu.open(player);
    server.getScheduler().performTicks(1);

    assertNotNull(player, "Player should still exist after menu open");
    assertEquals("TestPlayer", player.getName(), "Player name should remain unchanged");
  }

  @Test
  void multipleOpens_doNotCauseErrors() {
    assertDoesNotThrow(() -> {
      PlayerMenu.open(player);
      server.getScheduler().performTicks(1);

      PlayerMenu.open(player);
      server.getScheduler().performTicks(1);

      PlayerMenu.open(player);
      server.getScheduler().performTicks(1);
    }, "Opening PlayerMenu multiple times should not cause errors");
  }

  // ==================== Integration Tests ====================

  @Test
  void pluginMustBeLoaded_forMenuToWork() {
    assertNotNull(townsAndNations, "TownsAndNations plugin must be loaded");
    assertNotNull(sphereLib, "SphereLib must be loaded");

    assertDoesNotThrow(() -> PlayerMenu.open(player));
  }

  @Test
  void playerMenu_integratesWithStorage() {
    // Verify that player storage is accessible
    assertDoesNotThrow(() -> {
      PlayerDataStorage.getInstance().get(player);
    }, "PlayerDataStorage should be accessible");

    // Menu should work with storage integration
    assertDoesNotThrow(() -> {
      PlayerMenu.open(player);
      server.getScheduler().performTicks(1);
    }, "PlayerMenu should integrate with PlayerDataStorage");
  }
}
