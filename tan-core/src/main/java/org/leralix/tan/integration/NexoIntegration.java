package org.leralix.tan.integration;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Lightweight Nexo integration using reflection to avoid compile-time dependency.
 * Works at runtime when Nexo plugin is installed, gracefully degrades when absent.
 *
 * <p>Usage in icons.yml:
 * <pre>{@code
 * TERRITORY_ICON: "nexo:custom_crown"
 * TREASURY_ICON: "nexo:gold_coin_stack"
 * }</pre>
 */
public class NexoIntegration {

    private static final Logger logger = LoggerFactory.getLogger(NexoIntegration.class);
    private static boolean enabled = false;
    private static boolean initialized = false;
    private static Class<?> nexoItemsClass = null;
    private static Method itemFromIdMethod = null;

    /**
     * Initialize Nexo integration using reflection (no compile dependency).
     * Called automatically by IconManager when parsing icons.yml.
     *
     * @return true if Nexo is available and reflection successful
     */
    public static boolean initialize() {
        if (initialized) {
            return enabled;
        }

        initialized = true;
        Plugin nexo = Bukkit.getPluginManager().getPlugin("Nexo");

        if (nexo == null || !nexo.isEnabled()) {
            logger.debug("Nexo plugin not found - nexo: prefix will fallback to Material");
            enabled = false;
            return false;
        }

        try {
            // Use reflection to load Nexo API classes
            nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            
            // Find itemFromId method
            itemFromIdMethod = nexoItemsClass.getMethod("itemFromId", String.class);
            
            enabled = true;
            logger.info("Nexo integration enabled via reflection - nexo: prefix available in icons.yml");
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            logger.warn("Nexo plugin found but API incompatible - nexo: prefix disabled");
            logger.debug("Nexo reflection error", e);
            enabled = false;
            return false;
        }
    }

    /**
     * Check if Nexo integration is active.
     *
     * @return true if Nexo plugin loaded and reflection successful
     */
    public static boolean isEnabled() {
        if (!initialized) {
            initialize();
        }
        return enabled;
    }

    /**
     * Get a Nexo custom item by its ID using reflection.
     *
     * <p>Example:
     * <pre>{@code
     * Optional<ItemStack> crown = NexoIntegration.getItem("custom_crown");
     * ItemStack icon = crown.orElse(new ItemStack(Material.GOLDEN_HELMET));
     * }</pre>
     *
     * @param itemId the Nexo item ID (e.g., "custom_crown", "gold_coin")
     * @return Optional containing the custom ItemStack if found, empty otherwise
     */
    public static Optional<ItemStack> getItem(String itemId) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            // Call NexoItems.itemFromId(itemId).build() via reflection
            Object itemBuilder = itemFromIdMethod.invoke(null, itemId);
            
            if (itemBuilder == null) {
                logger.debug("Nexo item '{}' not found", itemId);
                return Optional.empty();
            }
            
            // Call .build() method on the builder
            Method buildMethod = itemBuilder.getClass().getMethod("build");
            ItemStack item = (ItemStack) buildMethod.invoke(itemBuilder);
            
            if (item == null) {
                logger.debug("Nexo item '{}' build() returned null", itemId);
                return Optional.empty();
            }
            
            return Optional.of(item);
        } catch (Exception e) {
            logger.error("Error getting Nexo item '{}' via reflection", itemId, e);
            return Optional.empty();
        }
    }

    /**
     * Get a Nexo custom item with fallback to default ItemStack.
     *
     * @param itemId the Nexo item ID
     * @param fallback the fallback ItemStack if Nexo item not found
     * @return the custom item or fallback (never null)
     */
    public static ItemStack getItemOrDefault(String itemId, ItemStack fallback) {
        return getItem(itemId).orElse(fallback);
    }

    /**
     * Check if a specific Nexo item exists using reflection.
     *
     * @param itemId the Nexo item ID to check
     * @return true if the item exists in Nexo configuration
     */
    public static boolean itemExists(String itemId) {
        if (!isEnabled()) {
            return false;
        }

        try {
            Method existsMethod = nexoItemsClass.getMethod("exists", String.class);
            return (Boolean) existsMethod.invoke(null, itemId);
        } catch (Exception e) {
            logger.debug("Error checking Nexo item existence '{}'", itemId, e);
            return false;
        }
    }
}
