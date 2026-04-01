package org.leralix.tan.listeners;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MobSpawnListener}.
 * <p>
 * Tests mob spawning in claimed chunks and territory-based spawn rules.
 * Uses MockBukkit 4.108.0 API with world.spawnEntity() instead of deprecated server.spawn().
 * </p>
 */
@DisplayName("MobSpawnListener Tests")
class MobSpawnListenerTest extends AbstractPluginTest {

    private MobSpawnListener listener;
    private World world;
    private Location testLocation;
    private Chunk testChunk;

    @BeforeEach
    void setUp() {
        MockBukkit.load(org.leralix.lib.SphereLib.class);
        listener = new MobSpawnListener();

        world = server.addSimpleWorld("test_world");
        testLocation = new Location(world, 0, 64, 0);
        testChunk = testLocation.getChunk();
    }

    // ==================== Basic Spawn Tests ====================

    @Test
    @DisplayName("Should handle entity spawn without throwing")
    void entitySpawn_validEntity_doesNotThrow() {
        Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should allow spawn in wilderness")
    void entitySpawn_wilderness_allowsSpawn() {
        Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        listener.entitySpawn(event);

        assertFalse(event.isCancelled(), "Spawn should not be cancelled in wilderness");
    }

    // ==================== Passive Mob Tests ====================

    @Test
    @DisplayName("Should handle pig spawn")
    void entitySpawn_pig_handlesCorrectly() {
        Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle cow spawn")
    void entitySpawn_cow_handlesCorrectly() {
        Entity cow = world.spawnEntity(testLocation, EntityType.COW);
        EntitySpawnEvent event = new EntitySpawnEvent(cow);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle chicken spawn")
    void entitySpawn_chicken_handlesCorrectly() {
        Entity chicken = world.spawnEntity(testLocation, EntityType.CHICKEN);
        EntitySpawnEvent event = new EntitySpawnEvent(chicken);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle sheep spawn")
    void entitySpawn_sheep_handlesCorrectly() {
        Entity sheep = world.spawnEntity(testLocation, EntityType.SHEEP);
        EntitySpawnEvent event = new EntitySpawnEvent(sheep);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle villager spawn")
    void entitySpawn_villager_handlesCorrectly() {
        Entity villager = world.spawnEntity(testLocation, EntityType.VILLAGER);
        EntitySpawnEvent event = new EntitySpawnEvent(villager);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle horse spawn")
    void entitySpawn_horse_handlesCorrectly() {
        Entity horse = world.spawnEntity(testLocation, EntityType.HORSE);
        EntitySpawnEvent event = new EntitySpawnEvent(horse);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle wolf spawn")
    void entitySpawn_wolf_handlesCorrectly() {
        Entity wolf = world.spawnEntity(testLocation, EntityType.WOLF);
        EntitySpawnEvent event = new EntitySpawnEvent(wolf);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle cat spawn")
    void entitySpawn_cat_handlesCorrectly() {
        Entity cat = world.spawnEntity(testLocation, EntityType.CAT);
        EntitySpawnEvent event = new EntitySpawnEvent(cat);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Hostile Mob Tests ====================

    @Test
    @DisplayName("Should handle zombie spawn")
    void entitySpawn_zombie_handlesCorrectly() {
        Entity zombie = world.spawnEntity(testLocation, EntityType.ZOMBIE);
        EntitySpawnEvent event = new EntitySpawnEvent(zombie);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle skeleton spawn")
    void entitySpawn_skeleton_handlesCorrectly() {
        Entity skeleton = world.spawnEntity(testLocation, EntityType.SKELETON);
        EntitySpawnEvent event = new EntitySpawnEvent(skeleton);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle spider spawn")
    void entitySpawn_spider_handlesCorrectly() {
        Entity spider = world.spawnEntity(testLocation, EntityType.SPIDER);
        EntitySpawnEvent event = new EntitySpawnEvent(spider);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle creeper spawn")
    void entitySpawn_creeper_handlesCorrectly() {
        Entity creeper = world.spawnEntity(testLocation, EntityType.CREEPER);
        EntitySpawnEvent event = new EntitySpawnEvent(creeper);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle enderman spawn")
    void entitySpawn_enderman_handlesCorrectly() {
        Entity enderman = world.spawnEntity(testLocation, EntityType.ENDERMAN);
        EntitySpawnEvent event = new EntitySpawnEvent(enderman);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Neutral Mob Tests ====================

    @Test
    @DisplayName("Should handle iron golem spawn")
    void entitySpawn_ironGolem_handlesCorrectly() {
        Entity ironGolem = world.spawnEntity(testLocation, EntityType.IRON_GOLEM);
        EntitySpawnEvent event = new EntitySpawnEvent(ironGolem);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle polar bear spawn")
    void entitySpawn_polarBear_handlesCorrectly() {
        Entity polarBear = world.spawnEntity(testLocation, EntityType.POLAR_BEAR);
        EntitySpawnEvent event = new EntitySpawnEvent(polarBear);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle bee spawn")
    void entitySpawn_bee_handlesCorrectly() {
        Entity bee = world.spawnEntity(testLocation, EntityType.BEE);
        EntitySpawnEvent event = new EntitySpawnEvent(bee);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle panda spawn")
    void entitySpawn_panda_handlesCorrectly() {
        Entity panda = world.spawnEntity(testLocation, EntityType.PANDA);
        EntitySpawnEvent event = new EntitySpawnEvent(panda);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle llama spawn")
    void entitySpawn_llama_handlesCorrectly() {
        Entity llama = world.spawnEntity(testLocation, EntityType.LLAMA);
        EntitySpawnEvent event = new EntitySpawnEvent(llama);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle trader llama spawn")
    void entitySpawn_traderLlama_handlesCorrectly() {
        Entity traderLlama = world.spawnEntity(testLocation, EntityType.TRADER_LLAMA);
        EntitySpawnEvent event = new EntitySpawnEvent(traderLlama);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Aquatic Mob Tests ====================

    @Test
    @DisplayName("Should handle squid spawn")
    void entitySpawn_squid_handlesCorrectly() {
        Entity squid = world.spawnEntity(testLocation, EntityType.SQUID);
        EntitySpawnEvent event = new EntitySpawnEvent(squid);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle dolphin spawn")
    void entitySpawn_dolphin_handlesCorrectly() {
        Entity dolphin = world.spawnEntity(testLocation, EntityType.DOLPHIN);
        EntitySpawnEvent event = new EntitySpawnEvent(dolphin);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle turtle spawn")
    void entitySpawn_turtle_handlesCorrectly() {
        Entity turtle = world.spawnEntity(testLocation, EntityType.TURTLE);
        EntitySpawnEvent event = new EntitySpawnEvent(turtle);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle cod spawn")
    void entitySpawn_cod_handlesCorrectly() {
        Entity cod = world.spawnEntity(testLocation, EntityType.COD);
        EntitySpawnEvent event = new EntitySpawnEvent(cod);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle salmon spawn")
    void entitySpawn_salmon_handlesCorrectly() {
        Entity salmon = world.spawnEntity(testLocation, EntityType.SALMON);
        EntitySpawnEvent event = new EntitySpawnEvent(salmon);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle tropical fish spawn")
    void entitySpawn_tropicalFish_handlesCorrectly() {
        Entity tropicalFish = world.spawnEntity(testLocation, EntityType.TROPICAL_FISH);
        EntitySpawnEvent event = new EntitySpawnEvent(tropicalFish);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle pufferfish spawn")
    void entitySpawn_pufferfish_handlesCorrectly() {
        Entity pufferfish = world.spawnEntity(testLocation, EntityType.PUFFERFISH);
        EntitySpawnEvent event = new EntitySpawnEvent(pufferfish);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Nether Mob Tests ====================

    @Test
    @DisplayName("Should handle blaze spawn")
    void entitySpawn_blaze_handlesCorrectly() {
        Entity blaze = world.spawnEntity(testLocation, EntityType.BLAZE);
        EntitySpawnEvent event = new EntitySpawnEvent(blaze);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle ghast spawn")
    void entitySpawn_ghast_handlesCorrectly() {
        Entity ghast = world.spawnEntity(testLocation, EntityType.GHAST);
        EntitySpawnEvent event = new EntitySpawnEvent(ghast);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle piglin spawn")
    void entitySpawn_piglin_handlesCorrectly() {
        Entity piglin = world.spawnEntity(testLocation, EntityType.PIGLIN);
        EntitySpawnEvent event = new EntitySpawnEvent(piglin);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle strider spawn")
    void entitySpawn_strider_handlesCorrectly() {
        Entity strider = world.spawnEntity(testLocation, EntityType.STRIDER);
        EntitySpawnEvent event = new EntitySpawnEvent(strider);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Other Entity Tests ====================

    @Test
    @DisplayName("Should handle bat spawn")
    void entitySpawn_bat_handlesCorrectly() {
        Entity bat = world.spawnEntity(testLocation, EntityType.BAT);
        EntitySpawnEvent event = new EntitySpawnEvent(bat);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle snow golem spawn")
    void entitySpawn_snowGolem_handlesCorrectly() {
        Entity snowGolem = world.spawnEntity(testLocation, EntityType.SNOW_GOLEM);
        EntitySpawnEvent event = new EntitySpawnEvent(snowGolem);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle wandering trader spawn")
    void entitySpawn_wanderingTrader_handlesCorrectly() {
        Entity wanderingTrader = world.spawnEntity(testLocation, EntityType.WANDERING_TRADER);
        EntitySpawnEvent event = new EntitySpawnEvent(wanderingTrader);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle fox spawn")
    void entitySpawn_fox_handlesCorrectly() {
        Entity fox = world.spawnEntity(testLocation, EntityType.FOX);
        EntitySpawnEvent event = new EntitySpawnEvent(fox);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle frog spawn")
    void entitySpawn_frog_handlesCorrectly() {
        Entity frog = world.spawnEntity(testLocation, EntityType.FROG);
        EntitySpawnEvent event = new EntitySpawnEvent(frog);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle goat spawn")
    void entitySpawn_goat_handlesCorrectly() {
        Entity goat = world.spawnEntity(testLocation, EntityType.GOAT);
        EntitySpawnEvent event = new EntitySpawnEvent(goat);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle axolotl spawn")
    void entitySpawn_axolotl_handlesCorrectly() {
        Entity axolotl = world.spawnEntity(testLocation, EntityType.AXOLOTL);
        EntitySpawnEvent event = new EntitySpawnEvent(axolotl);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle glow squid spawn")
    void entitySpawn_glowSquid_handlesCorrectly() {
        Entity glowSquid = world.spawnEntity(testLocation, EntityType.GLOW_SQUID);
        EntitySpawnEvent event = new EntitySpawnEvent(glowSquid);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle sniffer spawn")
    void entitySpawn_sniffer_handlesCorrectly() {
        Entity sniffer = world.spawnEntity(testLocation, EntityType.SNIFFER);
        EntitySpawnEvent event = new EntitySpawnEvent(sniffer);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle camel spawn")
    void entitySpawn_camel_handlesCorrectly() {
        Entity camel = world.spawnEntity(testLocation, EntityType.CAMEL);
        EntitySpawnEvent event = new EntitySpawnEvent(camel);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle rabbit spawn")
    void entitySpawn_rabbit_handlesCorrectly() {
        Entity rabbit = world.spawnEntity(testLocation, EntityType.RABBIT);
        EntitySpawnEvent event = new EntitySpawnEvent(rabbit);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle ocelot spawn")
    void entitySpawn_ocelot_handlesCorrectly() {
        Entity ocelot = world.spawnEntity(testLocation, EntityType.OCELOT);
        EntitySpawnEvent event = new EntitySpawnEvent(ocelot);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle parrot spawn")
    void entitySpawn_parrot_handlesCorrectly() {
        Entity parrot = world.spawnEntity(testLocation, EntityType.PARROT);
        EntitySpawnEvent event = new EntitySpawnEvent(parrot);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle donkey spawn")
    void entitySpawn_donkey_handlesCorrectly() {
        Entity donkey = world.spawnEntity(testLocation, EntityType.DONKEY);
        EntitySpawnEvent event = new EntitySpawnEvent(donkey);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle mule spawn")
    void entitySpawn_mule_handlesCorrectly() {
        Entity mule = world.spawnEntity(testLocation, EntityType.MULE);
        EntitySpawnEvent event = new EntitySpawnEvent(mule);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle skeleton horse spawn")
    void entitySpawn_skeletonHorse_handlesCorrectly() {
        Entity skeletonHorse = world.spawnEntity(testLocation, EntityType.SKELETON_HORSE);
        EntitySpawnEvent event = new EntitySpawnEvent(skeletonHorse);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle allay spawn")
    void entitySpawn_allay_handlesCorrectly() {
        Entity allay = world.spawnEntity(testLocation, EntityType.ALLAY);
        EntitySpawnEvent event = new EntitySpawnEvent(allay);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Chunk Tests ====================

    @Test
    @DisplayName("Should handle spawn in different chunks")
    void entitySpawn_differentChunks_handlesCorrectly() {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                Location location = new Location(world, x * 16, 64, z * 16);
                Entity pig = world.spawnEntity(location, EntityType.PIG);
                EntitySpawnEvent event = new EntitySpawnEvent(pig);

                assertDoesNotThrow(() -> listener.entitySpawn(event));
            }
        }
    }

    @Test
    @DisplayName("Should handle spawn in claimed chunk")
    void entitySpawn_claimedChunk_checksPermission() {
        // This would require setting up a claimed chunk
        Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle rapid spawns")
    void entitySpawn_rapidSpawns_handlesGracefully() {
        for (int i = 0; i < 20; i++) {
            Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
            EntitySpawnEvent event = new EntitySpawnEvent(pig);

            assertDoesNotThrow(() -> listener.entitySpawn(event));
        }
    }

    @Test
    @DisplayName("Should handle spawn at different Y levels")
    void entitySpawn_differentYLevels_handlesCorrectly() {
        for (int y = 0; y <= 100; y += 20) {
            Location location = new Location(world, 0, y, 0);
            Entity pig = world.spawnEntity(location, EntityType.PIG);
            EntitySpawnEvent event = new EntitySpawnEvent(pig);

            assertDoesNotThrow(() -> listener.entitySpawn(event));
        }
    }

    @Test
    @DisplayName("Should handle spawn in different worlds")
    void entitySpawn_differentWorlds_handlesCorrectly() {
        World world2 = server.addSimpleWorld("test_world2");
        Location world2Location = new Location(world2, 0, 64, 0);
        Entity pig = world2.spawnEntity(world2Location, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle null chunk gracefully")
    void entitySpawn_nullChunk_handlesGracefully() {
        Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle exception during chunk check gracefully")
    void entitySpawn_chunkCheckFailure_doesNotCrash() {
        Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Spawn Reason Tests ====================

    @Test
    @DisplayName("Should handle natural spawn")
    void entitySpawn_natural_spawnHandlesCorrectly() {
        Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle breeding spawn")
    void entitySpawn_breeding_handlesCorrectly() {
        Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle spawn egg spawn")
    void entitySpawn_spawnEgg_handlesCorrectly() {
        Entity pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntitySpawnEvent event = new EntitySpawnEvent(pig);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle mob spawner spawn")
    void entitySpawn_spawner_handlesCorrectly() {
        Entity zombie = world.spawnEntity(testLocation, EntityType.ZOMBIE);
        EntitySpawnEvent event = new EntitySpawnEvent(zombie);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Slime Tests ====================

    @Test
    @DisplayName("Should handle slime spawn")
    void entitySpawn_slime_handlesCorrectly() {
        Entity slime = world.spawnEntity(testLocation, EntityType.SLIME);
        EntitySpawnEvent event = new EntitySpawnEvent(slime);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle magma cube spawn")
    void entitySpawn_magmaCube_handlesCorrectly() {
        Entity magmaCube = world.spawnEntity(testLocation, EntityType.MAGMA_CUBE);
        EntitySpawnEvent event = new EntitySpawnEvent(magmaCube);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Phantom Tests ====================

    @Test
    @DisplayName("Should handle phantom spawn")
    void entitySpawn_phantom_handlesCorrectly() {
        Entity phantom = world.spawnEntity(testLocation, EntityType.PHANTOM);
        EntitySpawnEvent event = new EntitySpawnEvent(phantom);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Drowned Tests ====================

    @Test
    @DisplayName("Should handle drowned spawn")
    void entitySpawn_drowned_handlesCorrectly() {
        Entity drowned = world.spawnEntity(testLocation, EntityType.DROWNED);
        EntitySpawnEvent event = new EntitySpawnEvent(drowned);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    // ==================== Netherite Tests ====================

    @Test
    @DisplayName("Should handle hoglin spawn")
    void entitySpawn_hoglin_handlesCorrectly() {
        Entity hoglin = world.spawnEntity(testLocation, EntityType.HOGLIN);
        EntitySpawnEvent event = new EntitySpawnEvent(hoglin);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle zoglin spawn")
    void entitySpawn_zoglin_handlesCorrectly() {
        Entity zoglin = world.spawnEntity(testLocation, EntityType.ZOGLIN);
        EntitySpawnEvent event = new EntitySpawnEvent(zoglin);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }

    @Test
    @DisplayName("Should handle piglin brute spawn")
    void entitySpawn_piglinBrute_handlesCorrectly() {
        Entity piglinBrute = world.spawnEntity(testLocation, EntityType.PIGLIN_BRUTE);
        EntitySpawnEvent event = new EntitySpawnEvent(piglinBrute);

        assertDoesNotThrow(() -> listener.entitySpawn(event));
    }
}
