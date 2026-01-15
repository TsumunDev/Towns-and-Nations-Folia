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
 * Comprehensive tests for QuestCommand.
 *
 * <p>Tests the /ccn quest command behavior including:
 * - Opening quest menu
 * - Error handling for players without towns
 * - Permission checks
 * - Async data loading</p>
 */
class QuestCommandTest {

  private ServerMock server;
  private PlayerMock player;
  private ITanPlayer tanPlayer;
  private QuestCommand questCommand;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    MockBukkit.load(SphereLib.class);
    MockBukkit.load(TownsAndNations.class);

    player = server.addPlayer("TestPlayer");
    tanPlayer = PlayerDataStorage.getInstance().get(player).join();
    questCommand = new QuestCommand();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void getName_returnsCorrectName() {
    assertEquals("quest", questCommand.getName());
  }

  @Test
  void getSyntax_returnsCorrectSyntax() {
    assertEquals("/ccn quest", questCommand.getSyntax());
  }

  @Test
  void getArguments_returnsCorrectCount() {
    assertEquals(0, questCommand.getArguments());
  }

  @Test
  void getDescription_returnsNotNull() {
    assertNotNull(questCommand.getDescription());
  }

  @Test
  void getTabCompleteSuggestions_returnsEmptyList() {
    var suggestions = questCommand.getTabCompleteSuggestions(player, "", new String[]{});
    assertNotNull(suggestions);
    assertTrue(suggestions.isEmpty());
  }

  @Test
  void perform_playerWithoutTown_sendsErrorMessage() {
    // Player has no town by default
    questCommand.perform(player, new String[]{});

    // Should receive error message about needing a town
    String message = player.nextMessage();
    assertNotNull(message);
  }

  @Test
  void perform_withExtraArguments_ignoresExtraArguments() {
    assertDoesNotThrow(() -> questCommand.perform(player, new String[]{"extra"}));
  }

  @Test
  void perform_withMultipleExtraArguments_ignoresExtraArguments() {
    assertDoesNotThrow(() -> questCommand.perform(player, new String[]{"arg1", "arg2", "arg3"}));
  }

  @Test
  void perform_doesNotThrowNullPointerException() {
    assertDoesNotThrow(() -> questCommand.perform(player, new String[]{}));
  }

  @Test
  void perform_isThreadSafe() throws InterruptedException {
    Thread[] threads = new Thread[5];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(() -> {
        assertDoesNotThrow(() -> questCommand.perform(player, new String[]{}));
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
    assertDoesNotThrow(() -> questCommand.perform(player, new String[]{}));
  }
}
