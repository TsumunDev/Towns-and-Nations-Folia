package org.leralix.tan.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.commands.player.OpenGuiCommand;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Integration tests for town creation workflow.
 *
 * <p>Tests the end-to-end flow of creating a town including:
 * - Player data loading
 * - Town creation
 * - GUI updates
 * - Database persistence</p>
 */
@DisplayName("Town Creation Workflow Integration Tests")
class TownCreationWorkflowTest {

    private ServerMock server;
    private TownsAndNations plugin;
    private PlayerMock player;
    private ITanPlayer tanPlayer;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        plugin = MockBukkit.load(TownsAndNations.class);

        player = server.addPlayer("TestPlayer");
        player.setLocation(new Location(server.addSimpleWorld("world"), 0, 64, 0));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Complete town creation workflow")
    void testCompleteTownCreation() throws Exception {
        // Step 1: Load player data asynchronously
        CompletableFuture<ITanPlayer> playerLoadFuture = PlayerDataStorage.getInstance()
            .get(player);

        assertNotNull(playerLoadFuture, "Player load future should not be null");
        tanPlayer = playerLoadFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(tanPlayer, "Player data should load successfully");

        // Step 2: Verify player has no town initially
        assertFalse(tanPlayer.hasTown(), "Player should not have a town initially");

        // Step 3: Create a town
        String townName = "TestTown";
        String townId = UUID.randomUUID().toString();

        TownData townData = new TownData(townId, townName, tanPlayer);
        TownDataStorage.getInstance().createSync(townData);

        // Step 4: Verify town was created in storage
        TownData loadedTown = TownDataStorage.getInstance().getSync(townId);
        assertNotNull(loadedTown, "Town should be loaded from storage");
        assertEquals(townName, loadedTown.getName(), "Town name should match");
        assertEquals(tanPlayer.getID(), loadedTown.getLeaderID(), "Player should be town leader");

        // Step 5: Update player to have town
        tanPlayer.setTownID(townId);
        PlayerDataStorage.getInstance().updateSync(tanPlayer);

        // Step 6: Reload player data and verify town membership
        ITanPlayer reloadedPlayer = PlayerDataStorage.getInstance()
            .get(player)
            .get(5, TimeUnit.SECONDS);

        assertNotNull(reloadedPlayer, "Reloaded player should not be null");
        assertTrue(reloadedPlayer.hasTown(), "Player should now have a town");
        assertEquals(townId, reloadedPlayer.getTownID(), "Town ID should match");

        // Step 7: Verify town can be accessed via player
        TownData playerTown = reloadedPlayer.getTown().get(5, TimeUnit.SECONDS);
        assertNotNull(playerTown, "Player town should load successfully");
        assertEquals(townName, playerTown.getName(), "Town name should match");
    }

    @Test
    @DisplayName("Town creation with GUI integration")
    void testTownCreationWithGUI() throws Exception {
        // Load player data
        tanPlayer = PlayerDataStorage.getInstance().get(player).get(5, TimeUnit.SECONDS);

        // Create town
        String townName = "GUITown";
        String townId = UUID.randomUUID().toString();
        TownData townData = new TownData(townId, townName, tanPlayer);

        TownDataStorage.getInstance().createSync(townData);
        tanPlayer.setTownID(townId);
        PlayerDataStorage.getInstance().updateSync(tanPlayer);

        // Open GUI (should not throw)
        OpenGuiCommand guiCommand = new OpenGuiCommand();
        assertDoesNotThrow(() -> guiCommand.perform(player, new String[]{}));

        // Verify player still has town after GUI open
        ITanPlayer playerAfterGUI = PlayerDataStorage.getInstance()
            .get(player)
            .get(5, TimeUnit.SECONDS);

        assertTrue(playerAfterGUI.hasTown(), "Player should still have town after GUI open");
    }

    @Test
    @DisplayName("Town creation handles concurrent access")
    void testConcurrentTownCreation() throws Exception {
        // Create multiple towns concurrently
        int townCount = 5;
        CompletableFuture<TownData>[] futures = new CompletableFuture[townCount];

        for (int i = 0; i < townCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                String townName = "ConcurrentTown" + index;
                String townId = UUID.randomUUID().toString();

                tanPlayer = PlayerDataStorage.getInstance().getSync(player);
                TownData town = new TownData(townId, townName, tanPlayer);
                TownDataStorage.getInstance().createSync(town);

                return town;
            });
        }

        // Wait for all towns to be created
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

        // Verify all towns were created
        for (int i = 0; i < townCount; i++) {
            TownData town = futures[i].get(5, TimeUnit.SECONDS);
            assertNotNull(town, "Town " + i + " should be created");
            assertEquals("ConcurrentTown" + i, town.getName());
        }
    }

    @Test
    @DisplayName("Town creation workflow with error recovery")
    void testTownCreationWithErrorRecovery() throws Exception {
        tanPlayer = PlayerDataStorage.getInstance().get(player).get(5, TimeUnit.SECONDS);

        // Try to create town with invalid data
        String invalidTownName = ""; // Empty name
        String townId = UUID.randomUUID().toString();

        // This should handle gracefully
        assertDoesNotThrow(() -> {
            try {
                TownData townData = new TownData(townId, invalidTownName, tanPlayer);
                TownDataStorage.getInstance().createSync(townData);
            } catch (Exception e) {
                // Expected - invalid town name
                assertNotNull(e.getMessage(), "Error should have message");
            }
        });

        // Verify system still works after error
        String validTownName = "ValidTown";
        String validTownId = UUID.randomUUID().toString();
        TownData validTown = new TownData(validTownId, validTownName, tanPlayer);

        assertDoesNotThrow(() -> TownDataStorage.getInstance().createSync(validTown));

        TownData loaded = TownDataStorage.getInstance().getSync(validTownId);
        assertNotNull(loaded, "Valid town should be created");
        assertEquals(validTownName, loaded.getName());
    }
}
