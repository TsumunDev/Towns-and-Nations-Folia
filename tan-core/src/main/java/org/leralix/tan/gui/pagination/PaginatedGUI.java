package org.leralix.tan.gui.pagination;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.leralix.tan.gui.cosmetic.IconKey;
import org.leralix.tan.gui.cosmetic.IconManager;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.utils.gui.GuiUtil;

/**
 * Base class for paginated GUIs.
 *
 * <p>This class provides pagination support for GUIs that display large lists of items,
 * such as member lists, property lists, or chunk lists.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Automatic page navigation (previous/next buttons)</li>
 *   <li>Lazy loading of pages</li>
 *   <li>Customizable page size</li>
 *   <li>Page counter display</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * public class MemberMenu extends PaginatedGUI {
 *     public MemberMenu(Player player, TownData town) {
 *         super(player, "Members", 6, 45, () -> loadMembers(town));
 *     }
 *
 *     private static PageLoader loadMembers(TownData town) {
 *         return (page, pageSize) -> {
 *             int offset = page * pageSize;
 *             return AsyncGuiHelper.loadTownMembersPaginated(town, offset, pageSize);
 *         };
 *     }
 * }
 * }</pre>
 */
public abstract class PaginatedGUI extends Gui {

    protected final Player player;
    protected final String title;
    protected final int pageSize;
    protected int currentPage = 0;
    protected int totalPages = 1;

    protected final PageLoader pageLoader;
    protected List<?> currentPageItems = new ArrayList<>();

    /**
     * Functional interface for loading a page of items.
     */
    @FunctionalInterface
    public interface PageLoader {
        /**
         * Loads a page of items.
         *
         * @param page the page number (0-indexed)
         * @param pageSize the number of items per page
         * @return a CompletableFuture that completes with the list of items
         */
        java.util.concurrent.CompletableFuture<List<?>> loadPage(int page, int pageSize);
    }

    /**
     * Creates a new paginated GUI.
     *
     * @param player the player viewing the GUI
     * @param title the GUI title
     * @param rows the number of rows in the GUI
     * @param pageSize the number of items per page
     * @param pageLoader the function to load pages
     */
    public PaginatedGUI(Player player, String title, int rows, int pageSize, PageLoader pageLoader) {
        super(rows, title, new dev.triumphteam.gui.guis.GuiItem[0]);
        this.player = player;
        this.title = title;
        this.pageSize = pageSize;
        this.pageLoader = pageLoader;
    }

    /**
     * Opens the GUI and loads the first page.
     */
    @Override
    public void open(Player player) {
        this.currentPage = 0;
        loadPage(0);
    }

    /**
     * Loads a specific page of items.
     *
     * @param page the page number to load
     */
    protected void loadPage(int page) {
        this.currentPage = page;
        currentPageItems.clear();

        pageLoader.loadPage(page, pageSize)
            .thenAccept(items -> {
                currentPageItems = items;
                updatePage();
            })
            .exceptionally(e -> {
                org.slf4j.LoggerFactory.getLogger(PaginatedGUI.class)
                    .error("Failed to load page {} for GUI: {}", page, title, e);
                return null;
            });
    }

    /**
     * Updates the GUI with the current page's items.
     */
    protected void updatePage() {
        // Clear all items
        getInventory().clear();

        // Add items for current page
        int slot = 0;
        for (Object item : currentPageItems) {
            if (slot >= pageSize) break; // Don't exceed page size
            setItem(slot, createItemForObject(item));
            slot++;
        }

        // Add navigation buttons
        addNavigationButtons();

        // Update the GUI
        update();
    }

    /**
     * Creates a GUI item for an object in the list.
     *
     * <p>Subclasses must override this method to create the appropriate item.</p>
     *
     * @param obj the object to create an item for
     * @return the GUI item
     */
    protected abstract GuiItem createItemForObject(Object obj);

    /**
     * Adds navigation buttons (previous, next, page counter).
     */
    protected void addNavigationButtons() {
        int lastRow = getRows() - 1;
        int backButtonSlot = lastRow * 9;
        int prevButtonSlot = backButtonSlot - 2;
        int nextButtonSlot = backButtonSlot - 1;
        int pageCounterSlot = backButtonSlot - 5;

        // Previous page button
        if (currentPage > 0) {
            setItem(prevButtonSlot, createPreviousPageButton());
        }

        // Next page button
        if (currentPage < totalPages - 1) {
            setItem(nextButtonSlot, createNextPageButton());
        }

        // Page counter
        setItem(pageCounterSlot, createPageCounter());

        // Back button
        setItem(backButtonSlot, GuiUtil.createBackArrow(player, HumanEntity::closeInventory));
    }

    /**
     * Creates the previous page button.
     */
    protected GuiItem createPreviousPageButton() {
        return IconManager.getInstance()
            .get(IconKey.PREVIOUS_PAGE_ICON)
            .setName(Lang.PREVIOUS_PAGE.get())
            .setAction(event -> {
                currentPage--;
                loadPage(currentPage);
            })
            .asGuiItem(player);
    }

    /**
     * Creates the next page button.
     */
    protected GuiItem createNextPageButton() {
        return IconManager.getInstance()
            .get(IconKey.NEXT_PAGE_ICON)
            .setName(Lang.NEXT_PAGE.get())
            .setAction(event -> {
                currentPage++;
                loadPage(currentPage);
            })
            .asGuiItem(player);
    }

    /**
     * Creates the page counter item.
     */
    protected GuiItem createPageCounter() {
        return IconManager.getInstance()
            .get(Material.PAPER)
            .setName(String.format("Page %d/%d", currentPage + 1, totalPages))
            .setDescription(String.format("Showing %d items", currentPageItems.size()))
            .asGuiItem(player);
    }

    /**
     * Sets the total number of pages.
     *
     * @param totalPages the total number of pages
     */
    protected void setTotalPages(int totalPages) {
        this.totalPages = Math.max(1, totalPages);
    }

    /**
     * Gets the current page number.
     *
     * @return the current page (0-indexed)
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Gets the total number of pages.
     *
     * @return the total number of pages
     */
    public int getTotalPages() {
        return totalPages;
    }
}
