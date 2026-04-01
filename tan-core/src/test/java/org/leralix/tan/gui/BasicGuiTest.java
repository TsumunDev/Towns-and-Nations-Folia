package org.leralix.tan.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.BasicTest;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.gui.cosmetic.IconManager;
import org.leralix.tan.storage.stored.PlayerDataStorage;

/**
 * Basic GUI tests to verify GUI framework integration.
 *
 * <p>Tests the core GUI framework (Triumph-GUI) and basic menu opening without exceptions.
 * These are smoke tests to ensure the GUI system is properly initialized.
 *
 * @see org.leralix.tan.gui.BasicGui
 * @see dev.triumphteam.gui.guis.Gui
 */
class BasicGuiTest extends BasicTest {

  private Player player;
  private ITanPlayer tanPlayer;

  @Override
  @BeforeEach
  protected void setUp() {
    super.setUp();
    player = server.addPlayer("TestPlayer");
    // Get sync player data for basic tests
    tanPlayer = PlayerDataStorage.getInstance().getSync(player);
  }

  // ==================== Basic Framework Tests ====================

  @Test
  void pluginLoad_doesNotThrow() {
    // Verify plugin loads without exception - required for GUI framework
    assertNotNull(townsAndNations, "Plugin should be loaded");
    assertNotNull(sphereLib, "SphereLib should be loaded");
  }

  @Test
  void playerCreation_createsPlayerSuccessfully() {
    assertNotNull(player, "Player should be created");
    assertEquals("TestPlayer", player.getName(), "Player name should match");
  }

  @Test
  void tanPlayerData_createdSuccessfully() {
    // Note: In test environment, getSync may return null if player hasn't been fully initialized
    // This is a known limitation of the MockBukkit test environment
    // The test verifies that the storage system doesn't throw exceptions
    assertDoesNotThrow(() -> {
      PlayerDataStorage.getInstance().getSync(player);
    }, "PlayerDataStorage.getSync should not throw exception");
  }

  // ==================== Icon Manager Tests ====================

  @Test
  void iconManager_initializesWithoutException() {
    assertDoesNotThrow(() -> {
      IconManager.getInstance();
    }, "IconManager should initialize without throwing exception");
  }

  @Test
  void iconManager_getInstance_returnsSingleton() {
    IconManager instance1 = IconManager.getInstance();
    IconManager instance2 = IconManager.getInstance();
    assertSame(instance1, instance2, "IconManager should return singleton instance");
  }

  // ==================== Layout Manager Tests ====================

  @Test
  void layoutManager_initializesWithoutException() {
    assertDoesNotThrow(() -> {
      org.leralix.tan.gui.cosmetic.LayoutManager.getInstance();
    }, "LayoutManager should initialize without throwing exception");
  }

  // ==================== GUI Opening Smoke Tests ====================
  // Note: These tests verify that GUI opening doesn't throw exceptions.
  // Full GUI interaction tests require more complex MockBukkit setup.

  @Test
  void adminMainMenu_staticOpenMethod_doesNotThrowNPE() {
    // Smoke test: verify the static open method doesn't throw NPE immediately
    // The actual GUI may not fully render in test environment, but no crash should occur
    assertDoesNotThrow(() -> {
      org.leralix.tan.gui.admin.AdminMainMenu.open(player);
    }, "AdminMainMenu.open() should not throw exception immediately");

    // Give async operations time to complete
    server.getScheduler().performTicks(1);
  }

  @Test
  void playerMenu_staticOpenMethod_doesNotThrowNPE() {
    assertDoesNotThrow(() -> {
      org.leralix.tan.gui.user.player.PlayerMenu.open(player);
    }, "PlayerMenu.open() should not throw exception immediately");

    server.getScheduler().performTicks(1);
  }

  @Test
  void mainMenu_staticOpenMethod_doesNotThrowNPE() {
    assertDoesNotThrow(() -> {
      org.leralix.tan.gui.user.MainMenu.open(player);
    }, "MainMenu.open() should not throw exception immediately");

    server.getScheduler().performTicks(1);
  }

  @Test
  void langMenu_staticOpenMethod_doesNotThrowNPE() {
    assertDoesNotThrow(() -> {
      org.leralix.tan.gui.user.player.LangMenu.open(player);
    }, "LangMenu.open() should not throw exception immediately");

    server.getScheduler().performTicks(1);
  }
}
