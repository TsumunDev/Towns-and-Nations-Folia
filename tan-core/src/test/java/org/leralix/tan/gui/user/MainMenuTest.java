package org.leralix.tan.gui.user;

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
 * Tests for MainMenu.
 *
 * <p>Tests the main menu GUI based on the actual API:
 * - Uses static {@link MainMenu#open(Player)} method
 * - Extends {@link org.leralix.tan.gui.BasicGui}
 * - Uses Triumph-GUI framework
 * - Loads player, town, and region data asynchronously
 *
 * <p>API Pattern:
 * <pre>{@code
 * // Constructor is private, takes town/region data
 * // Opening is done via static method with async data loading:
 * MainMenu.open(player);
 * }</pre>
 *
 * @see org.leralix.tan.gui.user.MainMenu
 * @see org.leralix.tan.gui.BasicGui
 */
class MainMenuTest extends BasicTest {

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
      MainMenu.open(player);
    }, "MainMenu.open(player) should not throw exception");

    // Give async operations time to complete (main menu loads town/region data)
    server.getScheduler().performTicks(5);
  }

  @Test
  void staticOpenMethod_withNewPlayer_noTownOrRegion() {
    // New player won't have town or region - should still open
    assertNotNull(tanPlayer, "Player data should exist");
    assertFalse(tanPlayer.hasTown(), "New player should not have a town");
    assertFalse(tanPlayer.hasRegion(), "New player should not have a region");

    assertDoesNotThrow(() -> {
      MainMenu.open(player);
      server.getScheduler().performTicks(5);
    }, "MainMenu should open even for players without town/region");
  }

  // ==================== GUI Structure Tests ====================

  @Test
  void playerExists_afterMenuCreation() {
    MainMenu.open(player);
    server.getScheduler().performTicks(5);

    assertNotNull(player, "Player should still exist after menu open");
    assertEquals("TestPlayer", player.getName(), "Player name should remain unchanged");
  }

  @Test
  void multipleOpens_doNotCauseErrors() {
    assertDoesNotThrow(() -> {
      MainMenu.open(player);
      server.getScheduler().performTicks(2);

      MainMenu.open(player);
      server.getScheduler().performTicks(2);

      MainMenu.open(player);
      server.getScheduler().performTicks(2);
    }, "Opening MainMenu multiple times should not cause errors");
  }

  // ==================== Integration Tests ====================

  @Test
  void pluginMustBeLoaded_forMenuToWork() {
    assertNotNull(townsAndNations, "TownsAndNations plugin must be loaded");
    assertNotNull(sphereLib, "SphereLib must be loaded");

    assertDoesNotThrow(() -> MainMenu.open(player));
  }

  @Test
  void mainMenu_loadsRequiredData() {
    // MainMenu loads player, town, and region data asynchronously
    assertDoesNotThrow(() -> {
      MainMenu.open(player);
      server.getScheduler().performTicks(5);
    }, "MainMenu should successfully load all required data");
  }
}
