package org.leralix.tan.gui.cosmetic.type;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.integration.NexoIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Icon type that loads Nexo custom items at runtime.
 * Falls back to Material.BARRIER if Nexo unavailable or item not found.
 *
 * <p>Usage in icons.yml:
 * <pre>{@code
 * TERRITORY_ICON: "nexo:custom_crown"
 * TREASURY_ICON: "nexo:gold_coin_stack"
 * WAR_ICON: "nexo:enchanted_sword"
 * }</pre>
 */
public class NexoIconType extends IconType {

    private static final Logger logger = LoggerFactory.getLogger(NexoIconType.class);
    
    private final String nexoItemId;
    private final Material fallbackMaterial;
    private ItemStack cachedItem = null;

    /**
     * Create a Nexo icon type with BARRIER as fallback.
     *
     * @param nexoItemId the Nexo item ID (without "nexo:" prefix)
     */
    public NexoIconType(String nexoItemId) {
        this(nexoItemId, Material.BARRIER);
    }

    /**
     * Create a Nexo icon type with custom fallback material.
     *
     * @param nexoItemId the Nexo item ID (without "nexo:" prefix)
     * @param fallbackMaterial the Material to use if Nexo unavailable
     */
    public NexoIconType(String nexoItemId, Material fallbackMaterial) {
        this.nexoItemId = nexoItemId;
        this.fallbackMaterial = fallbackMaterial;
    }

    @Override
    protected ItemStack getItemStack(Player player) {
        // Cache the item to avoid repeated Nexo API calls
        if (cachedItem != null) {
            return cachedItem.clone();
        }

        // Try to load from Nexo
        ItemStack nexoItem = NexoIntegration.getItemOrDefault(
            nexoItemId, 
            new ItemStack(fallbackMaterial)
        );

        // Cache successful load
        if (nexoItem.getType() != fallbackMaterial) {
            cachedItem = nexoItem;
            logger.debug("Cached Nexo item '{}'", nexoItemId);
        }

        return nexoItem.clone();
    }

    /**
     * Get the Nexo item ID.
     *
     * @return the item ID without prefix
     */
    public String getNexoItemId() {
        return nexoItemId;
    }

    /**
     * Get the fallback material.
     *
     * @return the Material used when Nexo unavailable
     */
    public Material getFallbackMaterial() {
        return fallbackMaterial;
    }
}
