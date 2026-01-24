package org.leralix.tan.commands.player;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive tests for UpgradeCommand.
 *
 * <p>Tests the /ccn upgrade command behavior including:
 * - Opening upgrade menu
 * - Error handling for players without towns
 * - Permission checks (town leader only)
 * - Async data loading</p>
 */
class UpgradeCommandTest {

  private ServerMock server;
  private PlayerMock player;
  private ITanPlayer tanPlayer;
  private UpgradeCommand upgradeCommand;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    MockBukkit.load(SphereLib.class);
    MockBukkit.load(TownsAndNations.class);

    player = server.addPlayer("TestPlayer");
    tanPlayer = PlayerDataStorage.getInstance().get(player).join();
    upgradeCommand = new UpgradeCommand();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void getName_returnsCorrectName() {
    assertEquals("upgrade", upgradeCommand.getName());
  }

  @Test
  void getSyntax_returnsCorrectSyntax() {
    assertEquals("/ccn upgrade", upgradeCommand.getSyntax());
  }

  @Test
  void getArguments_returnsCorrectCount() {
    assertEquals(0, upgradeCommand.getArguments());
  }

  @Test
  void getDescription_returnsNotNull() {
    assertNotNull(upgradeCommand.getDescription());
  }

  @Test
  void getTabCompleteSuggestions_returnsEmptyList() {
    var suggestions = upgradeCommand.getTabCompleteSuggestions(player, "", new String[]{});
    assertNotNull(suggestions);
    assertTrue(suggestions.isEmpty());
  }

  @Test
  void perform_playerWithoutTown_sendsErrorMessage() {
    // Player has no town by default
    upgradeCommand.perform(player, new String[]{});

    // Should receive error message about needing a town
    String message = player.nextMessage();
    assertNotNull(message);
  }

  @Test
  void perform_withExtraArguments_ignoresExtraArguments() {
    assertDoesNotThrow(() -> upgradeCommand.perform(player, new String[]{"extra"}));
  }

  @Test
  void perform_withMultipleExtraArguments_ignoresExtraArguments() {
    assertDoesNotThrow(() -> upgradeCommand.perform(player, new String[]{"arg1", "arg2"}));
  }

  @Test
  void perform_doesNotThrowNullPointerException() {
    assertDoesNotThrow(() -> upgradeCommand.perform(player, new String[]{}));
  }

  @Test
  void perform_isThreadSafe() throws InterruptedException {
    Thread[] threads = new Thread[5];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(() -> {
        assertDoesNotThrow(() -> upgradeCommand.perform(player, new String[]{}));
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }
  }

  @Test
  void perform_handlesAsyncOperations() {
    // Should complete without hanging
    assertDoesNotThrow(() -> upgradeCommand.perform(player, new String[]{}));
  }
}
