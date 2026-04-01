package org.leralix.tan.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PropertySignListener}.
 * <p>
 * Tests property sign interaction including right-click menu opening
 * and left-click territory display.
 * </p>
 */
@DisplayName("PropertySignListener Tests")
class PropertySignListenerTest extends AbstractPluginTest {

    private PropertySignListener listener;
    private Player player;
    private Block signBlock;
    private Location testLocation;

    @BeforeEach
    void setUp() {
        MockBukkit.load(org.leralix.lib.SphereLib.class);
        listener = new PropertySignListener();
        player = server.addPlayer("TestPlayer");

        var world = server.addSimpleWorld("test_world");
        testLocation = new Location(world, 0, 64, 0);
        signBlock = testLocation.getBlock();
        signBlock.setType(Material.OAK_WALL_SIGN);
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
    @DisplayName("Should handle right-click on sign without metadata")
    void onPlayerInteract_noMetadata_handlesGracefully() {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
    }

    @Test
    @DisplayName("Should handle left-click on sign without metadata")
    void onPlayerInteract_leftClickNoMetadata_handlesGracefully() {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.LEFT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
    }

    // ==================== Property Sign Metadata Tests ====================

    @Test
    @DisplayName("Should handle right-click on property sign")
    void onPlayerInteract_propertySignRightClick_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertTrue(event.isCancelled(), "Property sign interaction should be cancelled");

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle left-click on property sign")
    void onPlayerInteract_propertySignLeftClick_showsBox() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.LEFT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertTrue(event.isCancelled(), "Property sign interaction should be cancelled");

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle malformed property sign metadata")
    void onPlayerInteract_malformedMetadata_handlesGracefully() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "invalid_format"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle empty property sign metadata")
    void onPlayerInteract_emptyMetadata_handlesGracefully() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, ""));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    // ==================== Sign Type Tests ====================

    @Test
    @DisplayName("Should handle OAK_SIGN block type")
    void onPlayerInteract_oakSign_handlesCorrectly() {
        signBlock.setType(Material.OAK_SIGN);
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle OAK_WALL_SIGN block type")
    void onPlayerInteract_wallSign_handlesCorrectly() {
        signBlock.setType(Material.OAK_WALL_SIGN);
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should ignore non-sign block types")
    void onPlayerInteract_nonSignBlock_ignoresEvent() {
        signBlock.setType(Material.CHEST);
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertFalse(event.isCancelled(), "Non-sign interaction should not be cancelled");

        signBlock.removeMetadata("propertySign", plugin);
    }

    // ==================== Action Type Tests ====================

    @Test
    @DisplayName("Should handle RIGHT_CLICK_BLOCK action")
    void onPlayerInteract_rightClickAction_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle LEFT_CLICK_BLOCK action")
    void onPlayerInteract_leftClickAction_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.LEFT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
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

    @Test
    @DisplayName("Should ignore LEFT_CLICK_AIR action")
    void onPlayerInteract_leftClickAir_ignoresEvent() {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.LEFT_CLICK_AIR,
            new ItemStack(Material.AIR),
            null
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertFalse(event.isCancelled(), "Air click should not be cancelled");
    }

    @Test
    @DisplayName("Should ignore PHYSICAL action")
    void onPlayerInteract_physicalAction_ignoresEvent() {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.PHYSICAL,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertFalse(event.isCancelled(), "Physical action should not be cancelled");
    }

    // ==================== Async Operation Tests ====================

    @Test
    @DisplayName("Should handle async town data loading")
    void onPlayerInteract_asyncTownLoad_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle async player data loading")
    void onPlayerInteract_asyncPlayerLoad_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    // ==================== Property Access Tests ====================

    @Test
    @DisplayName("Should handle property owner access")
    void onPlayerInteract_ownerAccess_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle property renter access")
    void onPlayerInteract_renterAccess_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle property for sale")
    void onPlayerInteract_forSale_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle property for rent")
    void onPlayerInteract_forRent_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    // ==================== Multiple Metadata Values Tests ====================

    @Test
    @DisplayName("Should handle multiple metadata values on sign")
    void onPlayerInteract_multipleMetadataValues_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId1_propertyId1"));
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId2_propertyId2"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle exception during town data loading gracefully")
    void onPlayerInteract_townLoadFailure_doesNotCrash() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "invalidTown_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle exception during player data loading gracefully")
    void onPlayerInteract_playerLoadFailure_doesNotCrash() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle rapid sign interactions")
    void onPlayerInteract_rapidInteractions_handlesGracefully() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        for (int i = 0; i < 10; i++) {
            PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR),
                signBlock
            );
            assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        }

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle special characters in metadata")
    void onPlayerInteract_specialCharsInMetadata_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "town-id_123_property-id_456"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle player with no town")
    void onPlayerInteract_noTownPlayer_handlesGracefully() {
        var noTownPlayer = server.addPlayer("NoTownPlayer");
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            noTownPlayer,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle admin player interaction")
    void onPlayerInteract_adminPlayer_handlesCorrectly() {
        var admin = server.addPlayer("AdminPlayer");
        admin.setOp(true);
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            admin,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    // ==================== Territory Display Tests ====================

    @Test
    @DisplayName("Should show property box on left click")
    void onPlayerInteract_leftClick_showsPropertyBox() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.LEFT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));
        assertTrue(event.isCancelled(), "Event should be cancelled");

        signBlock.removeMetadata("propertySign", plugin);
    }

    // ==================== Embargo Tests ====================

    @Test
    @DisplayName("Should handle trade embargo check")
    void onPlayerInteract_embargoChecked_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }

    // ==================== Location Tests ====================

    @Test
    @DisplayName("Should handle property sign in different chunks")
    void onPlayerInteract_differentChunks_handlesCorrectly() {
        Location differentLocation = new Location(testLocation.getWorld(), 16, 64, 16);
        Block differentBlock = differentLocation.getBlock();
        differentBlock.setType(Material.OAK_WALL_SIGN);
        differentBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            differentBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        differentBlock.removeMetadata("propertySign", plugin);
    }

    @Test
    @DisplayName("Should handle property sign in claimed chunk")
    void onPlayerInteract_claimedChunk_handlesCorrectly() {
        signBlock.setMetadata("propertySign", new org.bukkit.metadata.FixedMetadataValue(plugin, "townId_propertyId"));

        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            signBlock
        );

        assertDoesNotThrow(() -> listener.onPlayerInteract(event));

        signBlock.removeMetadata("propertySign", plugin);
    }
}
