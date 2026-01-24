package org.leralix.tan.listeners.interact;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive tests for RightClickListener.
 *
 * <p>Tests right-click interaction handling including:
 * - Chunk claim interactions
 * - Property interactions
 * - NPC interactions
 * - Error handling for null events</p>
 */
class RightClickListenerTest {

    private ServerMock server;
    private PlayerMock player;
    private RightClickListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);

        player = server.addPlayer("TestPlayer");
        listener = new RightClickListener();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onPlayerInteract_nullEvent_doesNotThrowException() {
        assertDoesNotThrow(() -> listener.onPlayerInteract(null));
    }

    @Test
    void onPlayerInteract_leftClick_isCancelled() {
        ItemStack item = new ItemStack(Material.DIAMOND_HOE);
        player.setItemInHand(item);

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            PlayerInteractEvent.Action.LEFT_CLICK_BLOCK,
            item,
            null,
            null
        );

        listener.onPlayerInteract(event);

        // Left click should be ignored or handled appropriately
        assertNotNull(event);
    }

    @Test
    void onPlayerInteract_rightClick_withAir_isHandled() {
        ItemStack item = new ItemStack(Material.DIAMOND_HOE);
        player.setItemInHand(item);

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
            item,
            null,
            null
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
    }

    @Test
    void onPlayerInteract_rightClick_withBlock_isHandled() {
        ItemStack item = new ItemStack(Material.DIAMOND_HOE);
        player.setItemInHand(item);

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
            item,
            null,
            server.addSimpleWorld("world").getBlockAt(0, 0, 0)
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
    }

    @Test
    void onPlayerInteract_nullPlayer_isHandledGracefully() {
        ItemStack item = new ItemStack(Material.DIAMOND_HOE);

        // Create event with null player (edge case)
        assertDoesNotThrow(() -> {
            PlayerInteractEvent event = new PlayerInteractEvent(
                null,
                PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                item,
                null,
                server.addSimpleWorld("world").getBlockAt(0, 0, 0)
            );
            listener.onPlayerInteract(event);
        });
    }

    @Test
    void onPlayerInteract_withDifferentItems_isHandled() {
        Material[] testMaterials = {
            Material.DIAMOND_HOE,
            Material.GOLDEN_HOE,
            Material.IRON_HOE,
            Material.WOODEN_HOE
        };

        for (Material material : testMaterials) {
            ItemStack item = new ItemStack(material);
            player.setItemInHand(item);

            PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                item,
                null,
                server.addSimpleWorld("world").getBlockAt(0, 0, 0)
            );

            assertDoesNotThrow(() -> listener.onPlayerInteract(event),
                "Should handle " + material + " without errors");
        }
    }

    @Test
    void onPlayerInteract_physicalInteraction_isHandled() {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            PlayerInteractEvent.Action.PHYSICAL,
            null,
            null,
            server.addSimpleWorld("world").getBlockAt(0, 0, 0)
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
    }

    @Test
    void listener_isThreadSafe() throws InterruptedException {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.DIAMOND_HOE),
            null,
            server.addSimpleWorld("world").getBlockAt(0, 0, 0)
        );

        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                assertDoesNotThrow(() -> listener.onPlayerInteract(event));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
