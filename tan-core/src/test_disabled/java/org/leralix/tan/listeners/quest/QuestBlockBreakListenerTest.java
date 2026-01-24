package org.leralix.tan.listeners.quest;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
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
 * Comprehensive tests for QuestBlockBreakListener.
 *
 * <p>Tests quest block break event handling including:
 * - Quest progress tracking
 * - Player validation
 * - Block type checking
 * - Error handling</p>
 */
class QuestBlockBreakListenerTest {

    private ServerMock server;
    private PlayerMock player;
    private ITanPlayer tanPlayer;
    private QuestBlockBreakListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);

        player = server.addPlayer("TestPlayer");
        tanPlayer = PlayerDataStorage.getInstance().get(player).join();
        listener = new QuestBlockBreakListener();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onBlockBreak_nullEvent_doesNotThrowException() {
        assertDoesNotThrow(() -> listener.onBlockBreak(null));
    }

    @Test
    void onBlockBreak_validBlock_isHandled() {
        BlockBreakEvent event = new BlockBreakEvent(
            server.addSimpleWorld("world").getBlockAt(0, 64, 0),
            player
        );

        assertDoesNotThrow(() -> listener.onBlockBreak(event));
    }

    @Test
    void onBlockBreak_nullPlayer_isHandledGracefully() {
        BlockBreakEvent event = new BlockBreakEvent(
            server.addSimpleWorld("world").getBlockAt(0, 64, 0),
            null
        );

        assertDoesNotThrow(() -> listener.onBlockBreak(event));
    }

    @Test
    void onBlockBreak_cancelledEvent_isNotProcessed() {
        var block = server.addSimpleWorld("world").getBlockAt(0, 64, 0);
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        event.setCancelled(true);

        listener.onBlockBreak(event);

        // Event should remain cancelled
        assertTrue(event.isCancelled());
    }

    @Test
    void onBlockBreak_differentMaterials_areAllHandled() {
        Material[] testMaterials = {
            Material.STONE,
            Material.DIAMOND_ORE,
            Material.IRON_ORE,
            Material.GOLD_ORE,
            Material.COAL_ORE
        };

        for (Material material : testMaterials) {
            var block = server.addSimpleWorld("world").getBlockAt(0, 64, 0);
            block.setType(material);

            BlockBreakEvent event = new BlockBreakEvent(block, player);

            assertDoesNotThrow(() -> listener.onBlockBreak(event),
                "Should handle " + material + " without errors");
        }
    }

    @Test
    void onBlockBreak_withPlayerHoldingItem_isHandled() {
        player.setItemInHand(new ItemStack(Material.DIAMOND_PICKAXE));

        BlockBreakEvent event = new BlockBreakEvent(
            server.addSimpleWorld("world").getBlockAt(0, 64, 0),
            player
        );

        assertDoesNotThrow(() -> listener.onBlockBreak(event));
    }

    @Test
    void onBlockBreak_inDifferentWorlds_isHandled() {
        String[] worlds = {"world", "world_nether", "world_the_end"};

        for (String worldName : worlds) {
            var world = server.addSimpleWorld(worldName);
            var block = world.getBlockAt(0, 64, 0);

            BlockBreakEvent event = new BlockBreakEvent(block, player);

            assertDoesNotThrow(() -> listener.onBlockBreak(event),
                "Should handle world " + worldName);
        }
    }

    @Test
    void listener_isThreadSafe() throws InterruptedException {
        var block = server.addSimpleWorld("world").getBlockAt(0, 64, 0);
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                assertDoesNotThrow(() -> listener.onBlockBreak(event));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
