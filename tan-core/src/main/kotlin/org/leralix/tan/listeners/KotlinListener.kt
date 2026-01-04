package org.leralix.tan.listeners

import kotlinx.coroutines.*
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.Listener
import org.leralix.tan.TownsAndNations
import org.leralix.tan.coroutines.FoliaDispatchers
import org.leralix.tan.coroutines.TanCoroutines
import org.leralix.tan.dataclass.ITanPlayer
import org.leralix.tan.service.PlayerDataService
import org.leralix.tan.utils.FoliaScheduler
import org.slf4j.LoggerFactory

/**
 * Base class for Kotlin event listeners with coroutine support.
 * Provides async event handling with automatic error handling.
 * 
 * Usage:
 * ```kotlin
 * class MyListener : KotlinListener() {
 *     
 *     @EventHandler
 *     fun onPlayerJoin(event: PlayerJoinEvent) {
 *         launchAsync(event.player) { tanPlayer ->
 *             // Async operations here
 *             val town = TerritoryService.getTown(tanPlayer.townId)
 *             
 *             // Switch to main thread for player interaction
 *             onMainThread {
 *                 event.player.sendMessage("Welcome to ${town?.name}!")
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class KotlinListener : Listener {
    
    companion object {
        private val logger = LoggerFactory.getLogger(KotlinListener::class.java)
    }
    
    /**
     * Launch an async operation for a player event.
     * Automatically loads TanPlayer data.
     */
    protected fun launchAsync(
        player: Player,
        block: suspend (ITanPlayer) -> Unit
    ) {
        TanCoroutines.launch {
            try {
                val tanPlayer = PlayerDataService.getPlayer(player)
                if (tanPlayer == null) {
                    logger.warn("TanPlayer is null for ${player.name}")
                    return@launch
                }
                block(tanPlayer)
            } catch (e: Exception) {
                logger.error("Error in async listener for ${player.name}", e)
            }
        }
    }
    
    /**
     * Launch an async operation without player data.
     */
    protected fun launchAsync(block: suspend () -> Unit) {
        TanCoroutines.launch {
            try {
                block()
            } catch (e: Exception) {
                logger.error("Error in async listener", e)
            }
        }
    }
    
    /**
     * Run a block on the main thread (Folia-safe).
     */
    protected suspend fun onMainThread(block: () -> Unit) {
        withContext(FoliaDispatchers.global(TownsAndNations.getPlugin())) {
            block()
        }
    }
    
    /**
     * Run a block on the main thread for a specific player.
     */
    protected suspend fun onMainThread(player: Player, block: () -> Unit) {
        withContext(FoliaDispatchers.forEntity(TownsAndNations.getPlugin(), player)) {
            block()
        }
    }
    
    /**
     * Run a block on the main thread (non-suspend, fire-and-forget).
     */
    protected fun runOnMainThread(block: () -> Unit) {
        FoliaScheduler.runTask(TownsAndNations.getPlugin(), block)
    }
    
    /**
     * Run a block on the main thread for a specific player (non-suspend).
     */
    protected fun runOnMainThread(player: Player, block: () -> Unit) {
        FoliaScheduler.runEntityTask(TownsAndNations.getPlugin(), player, block)
    }
}

/**
 * Extension to cancel an event safely.
 */
fun <T> T.cancelEvent() where T : Event, T : Cancellable {
    isCancelled = true
}

/**
 * Extension to check if player is online before operations.
 */
inline fun Player.ifOnline(block: Player.() -> Unit) {
    if (isOnline) block()
}

/**
 * Helper object for registering Kotlin listeners.
 */
object ListenerRegistry {
    
    private val registeredListeners = mutableListOf<Listener>()
    
    /**
     * Register a listener with the plugin.
     */
    fun register(listener: Listener) {
        val plugin = TownsAndNations.getPlugin()
        plugin.server.pluginManager.registerEvents(listener, plugin)
        registeredListeners.add(listener)
    }
    
    /**
     * Register multiple listeners.
     */
    fun registerAll(vararg listeners: Listener) {
        listeners.forEach { register(it) }
    }
    
    /**
     * Get count of registered listeners.
     */
    fun count(): Int = registeredListeners.size
}
