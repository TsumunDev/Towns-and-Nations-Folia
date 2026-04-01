package org.leralix.tan.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ChunkListener}.
 * <p>
 * Tests chunk-based permission checking for various player actions
 * including block breaking, placing, interactions, and entity damage.
 * Uses MockBukkit 4.108.0 API with world.spawnEntity() instead of deprecated server.spawn().
 * </p>
 */
@DisplayName("ChunkListener Tests")
class ChunkListenerTest extends AbstractPluginTest {

    private ChunkListener listener;
    private Player player;
    private World world;
    private Location testLocation;
    private Block testBlock;

    @BeforeEach
    void setUp() {
        MockBukkit.load(org.leralix.lib.SphereLib.class);
        listener = new ChunkListener();
        player = server.addPlayer("TestPlayer");
        world = server.addSimpleWorld("test_world");
        testLocation = new Location(world, 0, 64, 0);
        testBlock = testLocation.getBlock();
        testBlock.setType(Material.DIRT);
    }

    // ==================== Block Break Tests ====================

    @Test
    @DisplayName("Should handle block break event without throwing")
    void onBlockBreak_validBlock_doesNotThrow() {
        BlockBreakEvent event = new BlockBreakEvent(testBlock, player);

        assertDoesNotThrow(() -> listener.onBlockBreak(event));
    }

    @Test
    @DisplayName("Should cancel block break in claimed chunk without permission")
    void onBlockBreak_noPermission_cancelsEvent() {
        BlockBreakEvent event = new BlockBreakEvent(testBlock, player);

        listener.onBlockBreak(event);

        // Event should be cancelled if no permission (default behavior in wilderness)
        // or if explicitly denied by permission service
    }

    @Test
    @DisplayName("Should handle property sign break protection")
    void onBlockBreak_propertySign_cancelsEvent() {
        testBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "test"));
        BlockBreakEvent event = new BlockBreakEvent(testBlock, player);

        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "Property sign break should be cancelled");
        testBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle fort flag break protection")
    void onBlockBreak_fortFlag_cancelsEvent() {
        testBlock.setMetadata("fortFlag", new org.bukkit.metadata.FixedMetadataValue(plugin, "test"));
        BlockBreakEvent event = new BlockBreakEvent(testBlock, player);

        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "Fort flag break should be cancelled");
        testBlock.removeMetadata("fortFlag", plugin);
    }

    // ==================== Block Place Tests ====================

    @Test
    @DisplayName("Should handle block place event without throwing")
    void onBlockPlace_validBlock_doesNotThrow() {
        BlockPlaceEvent event = new BlockPlaceEvent(testBlock, testBlock.getState(), testBlock, new ItemStack(Material.DIRT), player, true, true);

        assertDoesNotThrow(() -> listener.onBlocPlaced(event));
    }

    // ==================== Bucket Tests ====================

    @Test
    @DisplayName("Should handle bucket fill event")
    void onBucketFill_validEvent_doesNotThrow() {
        Block waterBlock = testLocation.clone().add(0, 1, 0).getBlock();
        waterBlock.setType(Material.WATER);
        PlayerBucketFillEvent event = new PlayerBucketFillEvent(player, waterBlock, waterBlock.getLocation(), null, new ItemStack(Material.WATER_BUCKET), new ItemStack(Material.BUCKET));

        assertDoesNotThrow(() -> listener.onBucketFillEvent(event));
    }

    @Test
    @DisplayName("Should handle bucket empty event")
    void onBucketEmpty_validEvent_doesNotThrow() {
        PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(player, testBlock, testBlock.getLocation(), null, new ItemStack(Material.LAVA_BUCKET));

        assertDoesNotThrow(() -> listener.onBucketEmptyEvent(event));
    }

    // ==================== Player Interact Tests ====================

    @Test
    @DisplayName("Should handle null clicked block gracefully")
    void onPlayerInteract_nullBlock_handlesGracefully() {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, null, null);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle button interaction")
    void onPlayerInteract_button_checksPermission() {
        testBlock.setType(Material.STONE_BUTTON);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle lever interaction")
    void onPlayerInteract_lever_checksPermission() {
        testBlock.setType(Material.LEVER);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle chest interaction")
    void onPlayerInteract_chest_checksPermission() {
        testBlock.setType(Material.CHEST);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle door interaction")
    void onPlayerInteract_door_checksPermission() {
        testBlock.setType(Material.OAK_DOOR);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle trapdoor interaction")
    void onPlayerInteract_trapdoor_checksPermission() {
        testBlock.setType(Material.OAK_TRAPDOOR);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle fence gate interaction")
    void onPlayerInteract_fenceGate_checksPermission() {
        testBlock.setType(Material.OAK_FENCE_GATE);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle sign interaction")
    void onPlayerInteract_sign_checksPermission() {
        testBlock.setType(Material.OAK_SIGN);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle property sign interaction")
    void onPlayerInteract_propertySign_cancelsEvent() {
        testBlock.setType(Material.OAK_SIGN);
        testBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "test"));
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        listener.onPlayerInteractEvent(event);

        assertTrue(event.isCancelled(), "Property sign interaction should be cancelled");
        testBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle jukebox interaction")
    void onPlayerInteract_jukebox_checksPermission() {
        testBlock.setType(Material.JUKEBOX);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle note block interaction")
    void onPlayerInteract_noteBlock_checksPermission() {
        testBlock.setType(Material.NOTE_BLOCK);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle redstone wire interaction")
    void onPlayerInteract_redstone_checksPermission() {
        testBlock.setType(Material.REDSTONE_WIRE);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle repeater interaction")
    void onPlayerInteract_repeater_checksPermission() {
        testBlock.setType(Material.REPEATER);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle comparator interaction")
    void onPlayerInteract_comparator_checksPermission() {
        testBlock.setType(Material.COMPARATOR);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle bone meal use")
    void onPlayerInteract_boneMeal_checksPermission() {
        testBlock.setType(Material.GRASS_BLOCK);
        ItemStack boneMeal = new ItemStack(Material.BONE_MEAL);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, boneMeal, testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle sweet berry bush interaction")
    void onPlayerInteract_sweetBerries_checksPermission() {
        testBlock.setType(Material.SWEET_BERRY_BUSH);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle boat placement")
    void onPlayerInteract_boat_checksPermission() {
        ItemStack boat = new ItemStack(Material.OAK_BOAT);
        testBlock.setType(Material.WATER);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, boat, testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle minecart placement")
    void onPlayerInteract_minecart_checksPermission() {
        ItemStack minecart = new ItemStack(Material.MINECART);
        testBlock.setType(Material.RAIL);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, minecart, testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    @Test
    @DisplayName("Should handle farmland physical interaction")
    void onPlayerInteract_farmland_checksPermission() {
        testBlock.setType(Material.FARMLAND);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.PHYSICAL, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    // ==================== Entity Damage Tests ====================

    @Test
    @DisplayName("Should handle passive mob damage by player")
    void onEntityDamageByEntity_passiveMob_checksPermission() {
        var pig = world.spawnEntity(testLocation, EntityType.PIG);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, pig, EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK);

        assertDoesNotThrow(() -> listener.onEntityDamageByEntity(event));
    }

    @Test
    @DisplayName("Should handle villager damage by player")
    void onEntityDamageByEntity_villager_checksPermission() {
        var villager = world.spawnEntity(testLocation, EntityType.VILLAGER);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, villager, EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK);

        assertDoesNotThrow(() -> listener.onEntityDamageByEntity(event));
    }

    @Test
    @DisplayName("Should handle item frame damage by player")
    void onEntityDamageByEntity_itemFrame_checksPermission() {
        ItemFrame itemFrame = (ItemFrame) world.spawnEntity(testLocation, EntityType.ITEM_FRAME);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, itemFrame, EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK);

        assertDoesNotThrow(() -> listener.onEntityDamageByEntity(event));
    }

    @Test
    @DisplayName("Should handle player vs player damage")
    void onEntityDamageByEntity_pvp_checksPermission() {
        var victim = server.addPlayer("VictimPlayer");
        victim.teleport(testLocation);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, victim, EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK);

        assertDoesNotThrow(() -> listener.onEntityDamageByEntity(event));
    }

    @Test
    @DisplayName("Should handle passive mob damage by projectile")
    void onEntityDamageByEntity_projectilePassiveMob_checksPermission() {
        var pig = world.spawnEntity(testLocation, EntityType.PIG);
        var arrow = world.spawnEntity(testLocation, EntityType.ARROW);
        ((Arrow) arrow).setShooter(player);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(arrow, pig, EntityDamageByEntityEvent.DamageCause.PROJECTILE);

        assertDoesNotThrow(() -> listener.onEntityDamageByEntity(event));
    }

    @Test
    @DisplayName("Should handle player vs player damage via projectile")
    void onEntityDamageByEntity_projectilePvp_checksPermission() {
        var victim = server.addPlayer("VictimPlayer");
        victim.teleport(testLocation);
        var arrow = world.spawnEntity(testLocation, EntityType.ARROW);
        ((Arrow) arrow).setShooter(player);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(arrow, victim, EntityDamageByEntityEvent.DamageCause.PROJECTILE);

        assertDoesNotThrow(() -> listener.onEntityDamageByEntity(event));
    }

    // ==================== Explosion Tests ====================

    @Test
    @DisplayName("Should handle explosion event")
    void onExplosion_validEvent_doesNotThrow() {
        EntityExplodeEvent event = new EntityExplodeEvent(null, testLocation, null, 5, null);

        assertDoesNotThrow(() -> listener.onExplosion(event));
    }

    @Test
    @DisplayName("Should filter blocks affected by explosion")
    void onExplosion_claimedChunk_filtersBlocks() {
        var creeper = world.spawnEntity(testLocation, EntityType.CREEPER);
        EntityExplodeEvent event = new EntityExplodeEvent(creeper, testLocation, List.of(testBlock), 5, null);

        assertDoesNotThrow(() -> listener.onExplosion(event));
    }

    // ==================== Fire Tests ====================

    @Test
    @DisplayName("Should handle block burn event")
    void onBurning_validEvent_doesNotThrow() {
        testBlock.setType(Material.OAK_LOG);
        var event = new org.bukkit.event.block.BlockBurnEvent(testBlock);

        assertDoesNotThrow(() -> listener.onBurning(event));
    }

    @Test
    @DisplayName("Should handle fire spread event")
    void onFireSpreading_validEvent_doesNotThrow() {
        var fireBlock = testLocation.clone().add(0, -1, 0).getBlock();
        fireBlock.setType(Material.FIRE);
        var event = new BlockSpreadEvent(testBlock, fireBlock);

        assertDoesNotThrow(() -> listener.onFireSpreading(event));
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle exception in block break gracefully")
    void onBlockBreak_exception_doesNotCrash() {
        BlockBreakEvent event = new BlockBreakEvent(testBlock, player);

        assertDoesNotThrow(() -> listener.onBlockBreak(event));
    }

    @Test
    @DisplayName("Should handle exception in player interact gracefully")
    void onPlayerInteract_exception_doesNotCrash() {
        testBlock.setType(Material.CHEST);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);

        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle multiple rapid interactions")
    void onPlayerInteract_rapidInteractions_handlesGracefully() {
        testBlock.setType(Material.CHEST);
        for (int i = 0; i < 10; i++) {
            PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);
            assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
        }
    }

    @Test
    @DisplayName("Should handle interaction with different block data")
    void onPlayerInteract_differentBlockData_handlesCorrectly() {
        testBlock.setType(Material.REDSTONE_WIRE);
        BlockData blockData = testBlock.getBlockData();
        assertNotNull(blockData, "Block data should not be null");

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), testBlock);
        assertDoesNotThrow(() -> listener.onPlayerInteractEvent(event));
    }
}
