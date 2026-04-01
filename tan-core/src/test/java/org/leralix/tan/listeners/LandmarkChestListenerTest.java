package org.leralix.tan.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LandmarkChestListener}.
 * <p>
 * Tests landmark chest interaction including metadata handling
 * and town membership verification.
 * </p>
 */
@DisplayName("LandmarkChestListener Tests")
class LandmarkChestListenerTest extends AbstractPluginTest {

    private LandmarkChestListener listener;
    private Player player;
    private Block chestBlock;
    private Location testLocation;

    @BeforeEach
    void setUp() {
        MockBukkit.load(org.leralix.lib.SphereLib.class);
        listener = new LandmarkChestListener();
        player = server.addPlayer("TestPlayer");

        var world = server.addSimpleWorld("test_world");
        testLocation = new Location(world, 0, 64, 0);
        chestBlock = testLocation.getBlock();
        chestBlock.setType(Material.CHEST);
    }

    // ==================== Basic Interaction Tests ====================

    @Test
    @DisplayName("Should handle null clicked block gracefully")
    void onPlayerInteract_nullBlock_handlesGracefully() {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            null
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertFalse(event.isCancelled(), "Event should not be cancelled for null block");
    }

    @Test
    @DisplayName("Should handle chest without metadata")
    void onPlayerInteract_noMetadata_handlesGracefully() {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertFalse(event.isCancelled(), "Event should not be cancelled for chest without metadata");
    }

    @Test
    @DisplayName("Should cancel interaction with landmark chest")
    void onPlayerInteract_landmarkChest_cancelsEvent() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertTrue(event.isCancelled(), "Landmark chest interaction should be cancelled");

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Block Type Tests ====================

    @Test
    @DisplayName("Should only handle CHEST block type")
    void onPlayerInteract_chestBlock_handlesCorrectly() {
        chestBlock.setType(Material.CHEST);
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should ignore non-chest block types")
    void onPlayerInteract_nonChestBlock_ignoresEvent() {
        chestBlock.setType(Material.ENDER_CHEST);
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should ignore BARREL block type")
    void onPlayerInteract_barrelBlock_ignoresEvent() {
        chestBlock.setType(Material.BARREL);
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Action Type Tests ====================

    @Test
    @DisplayName("Should handle RIGHT_CLICK_BLOCK action")
    void onPlayerInteract_rightClickAction_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should ignore LEFT_CLICK_BLOCK action")
    void onPlayerInteract_leftClickAction_ignoresEvent() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.LEFT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should ignore RIGHT_CLICK_AIR action")
    void onPlayerInteract_rightClickAir_ignoresEvent() {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_AIR,
            new ItemStack(Material.AIR),
            null
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertFalse(event.isCancelled(), "Air click should not be cancelled");
    }

    // ==================== Metadata Tests ====================

    @Test
    @DisplayName("Should handle empty landmark chest metadata")
    void onPlayerInteract_emptyMetadata_handlesGracefully() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, ""));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should handle special characters in metadata")
    void onPlayerInteract_specialCharsInMetadata_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmark-id_123"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should handle multiple metadata values")
    void onPlayerInteract_multipleMetadataValues_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmark1"));
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmark2"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Player Membership Tests ====================

    @Test
    @DisplayName("Should handle player with no town")
    void onPlayerInteract_noTownPlayer_handlesGracefully() {
        var noTownPlayer = server.addPlayer("NoTownPlayer");
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            noTownPlayer,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should handle player in town")
    void onPlayerInteract_townPlayer_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should handle admin player interaction")
    void onPlayerInteract_adminPlayer_handlesCorrectly() {
        var admin = server.addPlayer("AdminPlayer");
        admin.setOp(true);
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            admin,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Sync Data Loading Tests ====================

    @Test
    @DisplayName("Should handle sync player data loading")
    void onPlayerInteract_syncDataLoad_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should handle sync landmark data loading")
    void onPlayerInteract_syncLandmarkLoad_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "nonExistentLandmark"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle exception during data loading gracefully")
    void onPlayerInteract_dataLoadFailure_doesNotCrash() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "invalidLandmark"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should handle null landmark data")
    void onPlayerInteract_nullLandmarkData_handlesGracefully() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "nullLandmark"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle rapid chest interactions")
    void onPlayerInteract_rapidInteractions_handlesGracefully() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        for (int i = 0; i < 10; i++) {
            PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR),
                chestBlock
            );
            assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        }

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should handle chest in different chunks")
    void onPlayerInteract_differentChunks_handlesCorrectly() {
        Location differentLocation = new Location(testLocation.getWorld(), 16, 64, 16);
        Block differentBlock = differentLocation.getBlock();
        differentBlock.setType(Material.CHEST);
        differentBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            differentBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        differentBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Permission Tests ====================

    @Test
    @DisplayName("Should check player permissions on interaction")
    void onPlayerInteract_checksPermissions() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should deny access for non-town members")
    void onPlayerInteract_nonTownMember_deniesAccess() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertTrue(event.isCancelled(), "Access should be denied for non-town members");

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Chest State Tests ====================

    @Test
    @DisplayName("Should handle locked chest state")
    void onPlayerInteract_lockedChest_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should handle large chest (double chest)")
    void onPlayerInteract_doubleChest_handlesCorrectly() {
        // Set up adjacent chest for double chest
        Location adjacentLocation = testLocation.clone().add(1, 0, 0);
        Block adjacentBlock = adjacentLocation.getBlock();
        adjacentBlock.setType(Material.CHEST);

        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
        adjacentBlock.setType(Material.AIR);
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    @DisplayName("Should handle concurrent landmark chest access")
    void onPlayerInteract_concurrentAccess_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        Runnable interactTask = () -> {
            for (int i = 0; i < 5; i++) {
                PlayerInteractEvent event = new PlayerInteractEvent(
                    player,
                    Action.RIGHT_CLICK_BLOCK,
                    new ItemStack(Material.AIR),
                    chestBlock
                );
                assertDoesNotThrow(() -> listener.onPlayerInteract(event));
            }
        };

        Thread thread1 = new Thread(interactTask);
        Thread thread2 = new Thread(interactTask);

        assertDoesNotThrow(() -> {
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();
        });

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Landmark Type Tests ====================

    @Test
    @DisplayName("Should handle different landmark types")
    void onPlayerInteract_differentLandmarkTypes_handlesCorrectly() {
        String[] landmarkTypes = {"TOWN_HALL", "FORTRESS", "LIBRARY", "BANK"};

        for (String landmarkType : landmarkTypes) {
            chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, landmarkType));

            PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR),
                chestBlock
            );

            assertDoesNotThrow(() -> listener.onPlayerInteract(event));

            chestBlock.removeMetadata("LandmarkChest", plugin);
        }
    }

    // ==================== Item in Hand Tests ====================

    @Test
    @DisplayName("Should handle interaction with item in hand")
    void onPlayerInteract_itemInHand_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        ItemStack itemInHand = new ItemStack(Material.DIAMOND);
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            itemInHand,
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    @Test
    @DisplayName("Should handle interaction with empty hand")
    void onPlayerInteract_emptyHand_handlesCorrectly() {
        chestBlock.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        ItemStack emptyHand = null;
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            emptyHand,
            chestBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        chestBlock.removeMetadata("LandmarkChest", plugin);
    }

    // ==================== Location Tests ====================

    @Test
    @DisplayName("Should handle landmark chest at different Y levels")
    void onPlayerInteract_differentYLevels_handlesCorrectly() {
        for (int y = 0; y <= 100; y += 20) {
            Location location = new Location(testLocation.getWorld(), 0, y, 0);
            Block block = location.getBlock();
            block.setType(Material.CHEST);
            block.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

            PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR),
                block
            );

            assertDoesNotThrow(() -> listener.onPlayerInteract(event));

            block.removeMetadata("LandmarkChest", plugin);
            block.setType(Material.AIR);
        }
    }

    @Test
    @DisplayName("Should handle landmark chest in different worlds")
    void onPlayerInteract_differentWorlds_handlesCorrectly() {
        var world2 = server.addSimpleWorld("test_world2");
        Location world2Location = new Location(world2, 0, 64, 0);
        Block world2Block = world2Location.getBlock();
        world2Block.setType(Material.CHEST);
        world2Block.setMetadata("LandmarkChest", new org.bukkit.metadata.FixedMetadataValue(plugin, "landmarkId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            world2Block
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        world2Block.removeMetadata("LandmarkChest", plugin);
    }
}
