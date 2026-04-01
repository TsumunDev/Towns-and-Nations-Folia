package org.leralix.tan.testutils;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.leralix.tan.TownsAndNations;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Base class for all tests requiring plugin initialization.
 * <p>
 * This class handles automatic setup and teardown of MockBukkit server
 * and plugin loading. Tests extending this class will have:
 * <ul>
 *   <li>A mocked Bukkit server instance</li>
 *   <li>A fully loaded TownsAndNations plugin instance</li>
 *   <li>Automatic cleanup after each test</li>
 * </ul>
 * <p>
 * Uses MockBukkit v1.21 (4.108.0) for Paper 1.21 compatibility.
 * <p>
 * Usage example:
 * <pre>{@code
 * class MyTest extends AbstractPluginTest {
 *     @Test
 *     void testSomething() {
 *         Player player = createTestPlayer("TestPlayer");
 *         // Test code here
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractPluginTest {

    protected ServerMock server;
    protected TownsAndNations plugin;

    /**
     * Sets up the mocked server and loads the plugin before each test.
     * <p>
     * This method is called automatically by JUnit before each test method.
     * It initializes MockBukkit and loads the TownsAndNations plugin.
     */
    @BeforeEach
    void setUpPlugin() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TownsAndNations.class);
    }

    /**
     * Cleans up the mocked server after each test.
     * <p>
     * This method is called automatically by JUnit after each test method.
     * It unloads MockBukkit and releases all resources.
     */
    @AfterEach
    void tearDownPlugin() {
        if (server != null) {
            MockBukkit.unmock();
            server = null;
            plugin = null;
        }
    }

    /**
     * Creates a test player with the given name and adds them to the server.
     * <p>
     * The player is fully initialized and ready to use in tests.
     *
     * @param name The name for the test player
     * @return A mock Player instance
     */
    protected Player createTestPlayer(String name) {
        return server.addPlayer(name);
    }

    /**
     * Creates multiple test players at once.
     *
     * @param names Variable arguments of player names
     * @return Array of mock Player instances
     */
    protected Player[] createTestPlayers(String... names) {
        Player[] players = new Player[names.length];
        for (int i = 0; i < names.length; i++) {
            players[i] = createTestPlayer(names[i]);
        }
        return players;
    }
}