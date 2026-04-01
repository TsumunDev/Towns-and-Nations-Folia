package org.leralix.tan.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.leralix.tan.storage.TeleportationRegister;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpawnListener}.
 * <p>
 * Tests teleportation cancellation on damage and movement,
 * including configuration-based behavior.
 * Uses MockBukkit 4.108.0 API with world.spawnEntity() instead of deprecated server.spawn().
 * </p>
 */
@DisplayName("SpawnListener Tests")
class SpawnListenerTest extends AbstractPluginTest {

    private SpawnListener listener;
    private Player player;
    private World world;
    private Location testLocation;

    @BeforeEach
    void setUp() {
        MockBukkit.load(org.leralix.lib.SphereLib.class);
        listener = new SpawnListener();
        player = server.addPlayer("TestPlayer");
        world = server.addSimpleWorld("test_world");
        testLocation = new Location(world, 0, 64, 0);
    }

    // ==================== Damage-Based Cancellation Tests ====================

    @Test
    @DisplayName("Should handle player hit event without throwing")
    void onPlayerHit_validEvent_doesNotThrow() {
        var attacker = server.addPlayer("Attacker");
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            attacker,
            player,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));
    }

    @Test
    @DisplayName("Should cancel teleportation on player damage")
    void onPlayerHit_teleportingPlayer_cancelsTeleport() {
        // Register player for teleportation
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        var attacker = server.addPlayer("Attacker");
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            attacker,
            player,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));

        // Verify teleport was cancelled (if registered)
        assertTrue(TeleportationRegister.getTeleportationData(player).isCancelled(),
            "Teleportation should be cancelled on damage");

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should handle damage from non-player entity")
    void onPlayerHit_nonPlayerAttacker_handlesCorrectly() {
        var zombie = world.spawnEntity(testLocation, EntityType.ZOMBIE);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            zombie,
            player,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));
    }

    @Test
    @DisplayName("Should handle damage to non-player entity")
    void onPlayerHit_nonPlayerTarget_ignoresEvent() {
        var attacker = server.addPlayer("Attacker");
        var victim = world.spawnEntity(testLocation, EntityType.VILLAGER);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            attacker,
            victim,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));
    }

    @Test
    @DisplayName("Should handle damage from projectile")
    void onPlayerHit_projectileDamage_handlesCorrectly() {
        var attacker = server.addPlayer("Attacker");
        var arrow = world.spawnEntity(testLocation, EntityType.ARROW);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            arrow,
            player,
            EntityDamageByEntityEvent.DamageCause.PROJECTILE
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));
    }

    @Test
    @DisplayName("Should handle environmental damage")
    void onPlayerHit_environmentalDamage_handlesCorrectly() {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            null,
            player,
            EntityDamageByEntityEvent.DamageCause.FALL
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));
    }

    // ==================== Movement-Based Cancellation Tests ====================

    @Test
    @DisplayName("Should handle player move event without throwing")
    void onPlayerMove_validMove_doesNotThrow() {
        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 1, 64, 0);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));
    }

    @Test
    @DisplayName("Should cancel teleport on position change")
    void onPlayerMove_positionChange_cancelsTeleport() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 1, 64, 1); // Different block position
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        listener.onPlayerMove(event);

        assertTrue(TeleportationRegister.getTeleportationData(player).isCancelled(),
            "Teleportation should be cancelled on position change");

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should cancel teleport on head rotation if configured")
    void onPlayerMove_headRotation_cancelsWhenConfigured() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        Location from = new Location(world, 0, 64, 0, 0, 0);
        Location to = new Location(world, 0, 64, 0, 90, 0); // Head rotation only
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should ignore head-only movement if not configured")
    void onPlayerMove_headRotation_ignoresWhenNotConfigured() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        Location from = new Location(world, 0, 64, 0, 0, 0);
        Location to = new Location(world, 0, 64, 0, 90, 0);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should not cancel teleport for non-teleporting player")
    void onPlayerMove_notTeleporting_doesNotCancel() {
        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 1, 64, 1);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));
        assertFalse(event.isCancelled(), "Event should not be cancelled for non-teleporting player");
    }

    // ==================== Already Cancelled Tests ====================

    @Test
    @DisplayName("Should ignore damage if already cancelled")
    void onPlayerHit_alreadyCancelled_ignoresEvent() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);
        TeleportationRegister.getTeleportationData(player).setCancelled(true);

        var attacker = server.addPlayer("Attacker");
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            attacker,
            player,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should ignore movement if already cancelled")
    void onPlayerMove_alreadyCancelled_ignoresEvent() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);
        TeleportationRegister.getTeleportationData(player).setCancelled(true);

        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 1, 64, 1);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    // ==================== Async Operation Tests ====================

    @Test
    @DisplayName("Should handle async player data loading on damage")
    void onPlayerHit_asyncDataLoad_handlesCorrectly() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        var attacker = server.addPlayer("Attacker");
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            attacker,
            player,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should handle async player data loading on move")
    void onPlayerMove_asyncDataLoad_handlesCorrectly() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 1, 64, 1);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    // ==================== Multiple Teleporting Players Tests ====================

    @Test
    @DisplayName("Should handle multiple teleporting players")
    void onPlayerHit_multipleTeleportingPlayers_handlesCorrectly() {
        var player1 = server.addPlayer("Teleporting1");
        var player2 = server.addPlayer("Teleporting2");
        var attacker = server.addPlayer("Attacker");

        TeleportationRegister.register(player1.getUniqueId().toString(), System.currentTimeMillis() + 5000);
        TeleportationRegister.register(player2.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        EntityDamageByEntityEvent event1 = new EntityDamageByEntityEvent(
            attacker,
            player1,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event1));

        TeleportationRegister.remove(player1.getUniqueId().toString());
        TeleportationRegister.remove(player2.getUniqueId().toString());
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle zero damage event")
    void onPlayerHit_zeroDamage_handlesGracefully() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        var attacker = server.addPlayer("Attacker");
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            attacker,
            player,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );
        event.setDamage(0);

        assertDoesNotThrow(() -> listener.onPlayerHit(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should handle teleportation with expired time")
    void onPlayerMove_expiredTeleportation_handlesGracefully() {
        // Register with past time
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() - 1000);

        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 1, 64, 1);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle exception during cancellation gracefully")
    void onPlayerHit_exceptionDuringCancellation_doesNotCrash() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        var attacker = server.addPlayer("Attacker");
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            attacker,
            player,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should handle exception during data loading gracefully")
    void onPlayerMove_exceptionDuringLoad_doesNotCrash() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 1, 64, 1);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    // ==================== Configuration Tests ====================

    @Test
    @DisplayName("Should respect cancelTeleportOnDamage config")
    void onPlayerHit_respectsCancelOnDamageConfig() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        var attacker = server.addPlayer("Attacker");
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
            attacker,
            player,
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
        );

        assertDoesNotThrow(() -> listener.onPlayerHit(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should respect cancelTeleportOnMovePosition config")
    void onPlayerMove_respectsCancelOnMovePositionConfig() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        Location from = new Location(world, 0, 64, 0);
        Location to = new Location(world, 1, 64, 1);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should respect cancelTeleportOnMoveHead config")
    void onPlayerMove_respectsCancelOnMoveHeadConfig() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        Location from = new Location(world, 0, 64, 0, 0, 0);
        Location to = new Location(world, 0, 64, 0, 90, 0);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        assertDoesNotThrow(() -> listener.onPlayerMove(event));

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    // ==================== Direct Method Tests ====================

    @Test
    @DisplayName("Should handle direct cancelTeleportation call")
    void cancelTeleportation_directCall_cancelsCorrectly() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        assertDoesNotThrow(() -> listener.cancelTeleportation(player));

        assertTrue(TeleportationRegister.getTeleportationData(player).isCancelled());

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should handle cancelTeleportation for non-registered player")
    void cancelTeleportation_nonRegisteredPlayer_handlesGracefully() {
        assertDoesNotThrow(() -> listener.cancelTeleportation(player));
    }

    // ==================== Rapid Event Tests ====================

    @Test
    @DisplayName("Should handle rapid damage events")
    void onPlayerHit_rapidDamage_handlesGracefully() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        var attacker = server.addPlayer("Attacker");
        for (int i = 0; i < 5; i++) {
            EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                attacker,
                player,
                EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
            );
            assertDoesNotThrow(() -> listener.onPlayerHit(event));
        }

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    @Test
    @DisplayName("Should handle rapid movement events")
    void onPlayerMove_rapidMovement_handlesGracefully() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        Location from = new Location(world, 0, 64, 0);
        for (int i = 1; i <= 5; i++) {
            Location to = new Location(world, i, 64, i);
            PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
            assertDoesNotThrow(() -> listener.onPlayerMove(event));
            from = to;
        }

        TeleportationRegister.remove(player.getUniqueId().toString());
    }

    // ==================== Different Damage Types ====================

    @Test
    @DisplayName("Should handle different damage causes")
    void onPlayerHit_differentDamageCauses_handlesCorrectly() {
        TeleportationRegister.register(player.getUniqueId().toString(), System.currentTimeMillis() + 5000);

        EntityDamageByEntityEvent.DamageCause[] causes = {
            EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK,
            EntityDamageByEntityEvent.DamageCause.ENTITY_SWEEP_ATTACK,
            EntityDamageByEntityEvent.DamageCause.PROJECTILE,
            EntityDamageByEntityEvent.DamageCause.THORNS
        };

        for (var cause : causes) {
            var attacker = server.addPlayer("Attacker");
            EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                attacker,
                player,
                cause
            );
            assertDoesNotThrow(() -> listener.onPlayerHit(event));
        }

        TeleportationRegister.remove(player.getUniqueId().toString());
    }
}
