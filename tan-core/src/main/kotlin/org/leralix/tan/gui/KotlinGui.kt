package org.leralix.tan.gui

import dev.triumphteam.gui.components.GuiType
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.leralix.tan.TownsAndNations
import org.leralix.tan.coroutines.FoliaDispatchers
import org.leralix.tan.coroutines.TanCoroutines
import org.leralix.tan.dataclass.ITanPlayer
import org.leralix.tan.gui.cosmetic.IconManager
import org.leralix.tan.lang.Lang
import org.leralix.tan.lang.LangType
import org.leralix.tan.service.PlayerDataService
import org.slf4j.LoggerFactory

/**
 * Kotlin base class for async GUI menus.
 * Provides coroutine-based data loading.
 */
abstract class KotlinGui(
    protected val player: Player,
    protected val tanPlayer: ITanPlayer,
    title: String,
    rows: Int
) {
    
    protected val gui: Gui = Gui.gui()
        .title(Component.text(title))
        .type(GuiType.CHEST)
        .rows(rows)
        .create()
    
    protected val langType: LangType = tanPlayer.lang
    protected val iconManager: IconManager = IconManager.getInstance()
    
    init {
        gui.setDefaultClickAction { event ->
            if (event.clickedInventory?.type != InventoryType.PLAYER) {
                event.isCancelled = true
            }
        }
        gui.setDragAction { it.isCancelled = true }
    }
    
    /**
     * Populate the GUI with items. Called before opening.
     */
    abstract suspend fun populate()
    
    /**
     * Open the GUI to the player.
     */
    fun openGui() {
        gui.open(player)
    }
    
    /**
     * Set an item at the specified position.
     */
    protected fun setItem(row: Int, col: Int, item: GuiItem) {
        gui.setItem(row, col, item)
    }
    
    /**
     * Set an item at a slot index.
     */
    protected fun setItem(slot: Int, item: GuiItem) {
        gui.setItem(slot, item)
    }
    
    /**
     * Create a simple item with name and click action.
     */
    protected fun simpleItem(
        material: Material,
        name: String,
        lore: List<String> = emptyList(),
        onClick: ((InventoryClickEvent) -> Unit)? = null
    ): GuiItem {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta?.setDisplayName(name)
        if (lore.isNotEmpty()) {
            meta?.lore = lore
        }
        item.itemMeta = meta
        
        return GuiItem(item) { event ->
            onClick?.invoke(event)
        }
    }
    
    /**
     * Create a back arrow button.
     */
    protected fun backArrow(onClick: () -> Unit): GuiItem {
        return simpleItem(
            Material.ARROW,
            Lang.GUI_BACK_ARROW.get(langType)
        ) { onClick() }
    }
    
    /**
     * Create a close button.
     */
    protected fun closeButton(): GuiItem {
        return simpleItem(
            Material.BARRIER,
            "§cClose"
        ) { player.closeInventory() }
    }
    
    /**
     * Fill border with glass panes.
     */
    protected fun fillBorder(material: Material = Material.BLACK_STAINED_GLASS_PANE) {
        val filler = simpleItem(material, " ")
        gui.filler.fillBorder(filler)
    }
    
    companion object {
        private val logger = LoggerFactory.getLogger(KotlinGui::class.java)
        
        /**
         * Helper to open a GUI with async data loading.
         * 
         * Usage:
         * ```kotlin
         * KotlinGui.openAsync(player) { tanPlayer ->
         *     MyMenu(player, tanPlayer, loadedData)
         * }
         * ```
         */
        fun openAsync(
            player: Player,
            factory: suspend (ITanPlayer) -> KotlinGui
        ) {
            TanCoroutines.launch {
                try {
                    val tanPlayer = PlayerDataService.getPlayer(player)
                        ?: throw IllegalStateException("Player data not found")
                    
                    val gui = factory(tanPlayer)
                    gui.populate()
                    
                    // Open on main thread via Folia scheduler
                    withContext(FoliaDispatchers.forEntity(TownsAndNations.getPlugin(), player)) {
                        gui.openGui()
                    }
                } catch (e: Exception) {
                    logger.error("Failed to open GUI for ${player.name}", e)
                    player.sendMessage("§cError opening menu: ${e.message}")
                }
            }
        }
        
        /**
         * Open a GUI when player data is already available.
         */
        fun openWithData(
            player: Player,
            tanPlayer: ITanPlayer,
            factory: suspend (ITanPlayer) -> KotlinGui
        ) {
            TanCoroutines.launch {
                try {
                    val gui = factory(tanPlayer)
                    gui.populate()
                    
                    withContext(FoliaDispatchers.forEntity(TownsAndNations.getPlugin(), player)) {
                        gui.openGui()
                    }
                } catch (e: Exception) {
                    logger.error("Failed to open GUI for ${player.name}", e)
                    player.sendMessage("§cError opening menu: ${e.message}")
                }
            }
        }
    }
}
