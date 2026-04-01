package org.leralix.tan.domain.gui;

import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.lang.LangType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for GUI-related operations on towns.
 * Extracted from TownData as part of the refactoring to separate concerns.
 *
 * <p>This service handles:</p>
 * <ul>
 *   <li>Creating town icons with names</li>
 *   <li>Creating town icons with detailed information</li>
 *   <li>Generating ordered member lists for GUIs</li>
 * </ul>
 *
 * @see org.leralix.tan.dataclass.territory.TownData
 * @since 2.0.0
 */
public interface TownGuiService {

    /**
     * Creates an ItemStack icon with the town's name as display name.
     *
     * @param townId The ID of the town
     * @return An ItemStack with the town's head and name
     */
    CompletableFuture<ItemStack> getIconWithName(String townId);

    /**
     * Creates an ItemStack icon with detailed information about the town.
     *
     * <p>The lore includes:</p>
 * <ul>
     *   <li>Town description</li>
     *   <li>Leader name</li>
     *   <li>Number of members</li>
     *   <li>Number of claimed chunks</li>
     *   <li>Overlord region (if any)</li>
     * </ul>
     *
     * @param townId The ID of the town
     * @param langType The language type for translations
     * @return An ItemStack with the town's head and detailed lore
     */
    CompletableFuture<ItemStack> getIconWithInformations(String townId, LangType langType);

    /**
     * Creates an ordered list of GUI items for town members.
     *
     * <p>Each member is represented as a clickable head with:</p>
 * <ul>
     *   <li>Player's head</li>
     *   <li>Rank information</li>
     *   <li>Balance information</li>
     *   <li>Click action (if viewer has permission)</li>
     * </ul>
     *
     * @param townId The ID of the town
     * @param viewer The player viewing the GUI
     * @return A list of GuiItem representing each member
     */
    CompletableFuture<List<GuiItem>> getOrderedMemberList(String townId, ITanPlayer viewer);

    /**
     * Checks if this service is enabled via feature flag.
     *
     * @return true if the new GUI service should be used
     */
    boolean isEnabled();
}
