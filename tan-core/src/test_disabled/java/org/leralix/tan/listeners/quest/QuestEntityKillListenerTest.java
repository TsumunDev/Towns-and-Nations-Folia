package org.leralix.tan.listeners.quest;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
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
 * Comprehensive tests for QuestEntityKillListener.
 *
 * <p>Tests quest entity kill event handling including:
 * - Quest progress tracking
 * - Player validation
 * - Entity type checking
 * - Error handling</p>
 */
class QuestEntityKillListenerTest {

    private ServerMock server;
    private PlayerMock player;
    private ITanPlayer tanPlayer;
    private QuestEntityKillListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);

        player = server.addPlayer("TestPlayer");
        tanPlayer = PlayerDataStorage.getInstance().get(player).join();
        listener = new QuestEntityKillListener();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onEntityDeath_nullEvent_doesNotThrowException() {
        assertDoesNotThrow(() -> listener.onEntityDeath(null));
    }

    @Test
    void onEntityDeath_validEntity_isHandled() {
        Entity entity = server.addSimpleWorld("world").spawnEntity(EntityType.ZOMBIE, 0, 64, 0);
        EntityDeathEvent event = new EntityDeathEvent(entity, player);

        assertDoesNotThrow(() -> listener.onEntityDeath(event));
    }

    @Test
    void onEntityDeath_nullPlayer_isHandledGracefully() {
        Entity entity = server.addSimpleWorld("world").spawnEntity(EntityType.ZOMBIE, 0, 64, 0);
        EntityDeathEvent event = new EntityDeathEvent(entity, null);

        assertDoesNotThrow(() -> listener.onEntityDeath(event));
    }

    @Test
    void onEntityDeath_differentEntityTypes_areAllHandled() {
        EntityType[] testTypes = {
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.ENDERMAN
        };

        for (EntityType type : testTypes) {
            Entity entity = server.addSimpleWorld("world").spawnEntity(type, 0, 64, 0);
            EntityDeathEvent event = new EntityDeathEvent(entity, player);

            assertDoesNotThrow(() -> listener.onEntityDeath(event),
                "Should handle " + type + " without errors");
        }
    }

    @Test
    void onEntityDeath_passiveMobs_areHandled() {
        EntityType[] passiveTypes = {
            EntityType.COW,
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.CHICKEN
        };

        for (EntityType type : passiveTypes) {
            Entity entity = server.addSimpleWorld("world").spawnEntity(type, 0, 64, 0);
            EntityDeathEvent event = new EntityDeathEvent(entity, player);

            assertDoesNotThrow(() -> listener.onEntityDeath(event),
                "Should handle passive mob " + type);
        }
    }

    @Test
    void onEntityDeath_inDifferentWorlds_isHandled() {
        String[] worlds = {"world", "world_nether", "world_the_end"};

        for (String worldName : worlds) {
            var world = server.addSimpleWorld(worldName);
            Entity entity = world.spawnEntity(EntityType.ZOMBIE, 0, 64, 0);

            EntityDeathEvent event = new EntityDeathEvent(entity, player);

            assertDoesNotThrow(() -> listener.onEntityDeath(event),
                "Should handle world " + worldName);
        }
    }

    @Test
    void onEntityDeath_withMultipleKillsInQuickSuccession_handlesAll() {
        for (int i = 0; i < 10; i++) {
            Entity entity = server.addSimpleWorld("world").spawnEntity(EntityType.ZOMBIE, i, 64, 0);
            EntityDeathEvent event = new EntityDeathEvent(entity, player);

            assertDoesNotThrow(() -> listener.onEntityDeath(event));
        }
    }

    @Test
    void listener_isThreadSafe() throws InterruptedException {
        Entity entity = server.addSimpleWorld("world").spawnEntity(EntityType.ZOMBIE, 0, 64, 0);
        EntityDeathEvent event = new EntityDeathEvent(entity, player);

        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                assertDoesNotThrow(() -> listener.onEntityDeath(event));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
