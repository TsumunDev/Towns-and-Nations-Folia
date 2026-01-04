package org.leralix.tan.gui

import kotlinx.coroutines.*
import org.bukkit.entity.Player
import org.leralix.tan.TownsAndNations
import org.leralix.tan.coroutines.FoliaDispatchers
import org.leralix.tan.coroutines.TanCoroutines
import org.leralix.tan.dataclass.ITanPlayer
import org.leralix.tan.service.PlayerDataService
import org.slf4j.LoggerFactory

/**
 * Kotlin async GUI helper utilities.
 * Provides coroutine-based alternatives to AsyncGuiHelper.java.
 */
object GuiCoroutines {
    
    private val logger = LoggerFactory.getLogger(GuiCoroutines::class.java)
    
    /**
     * Load data asynchronously and run callback on main thread.
     */
    fun <T> loadAndRun(
        player: Player,
        loader: suspend () -> T,
        onMain: (T) -> Unit
    ) {
        TanCoroutines.launch {
            try {
                val data = loader()
                
                withContext(FoliaDispatchers.forEntity(TownsAndNations.getPlugin(), player)) {
                    if (player.isOnline) {
                        onMain(data)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in loadAndRun for ${player.name}", e)
            }
        }
    }
    
    /**
     * Load player data and additional data, then run callback.
     */
    fun <T> withPlayerData(
        player: Player,
        loader: suspend (ITanPlayer) -> T,
        onMain: (T) -> Unit
    ) {
        TanCoroutines.launch {
            try {
                val tanPlayer = PlayerDataService.getPlayer(player)
                    ?: throw IllegalStateException("Player data not found")
                
                val data = loader(tanPlayer)
                
                withContext(FoliaDispatchers.forEntity(TownsAndNations.getPlugin(), player)) {
                    if (player.isOnline) {
                        onMain(data)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in withPlayerData for ${player.name}", e)
                player.sendMessage("§cError: ${e.message}")
            }
        }
    }
    
    /**
     * Run a block on the main thread for a specific player.
     * Uses Folia-safe entity scheduling.
     */
    suspend fun <T> onMainThread(player: Player, block: () -> T): T {
        return withContext(FoliaDispatchers.forEntity(TownsAndNations.getPlugin(), player)) {
            block()
        }
    }
    
    /**
     * Run a block asynchronously.
     */
    suspend fun <T> async(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }
    
    /**
     * Delay execution (Folia-safe).
     */
    suspend fun delay(millis: Long) {
        kotlinx.coroutines.delay(millis)
    }
    
    /**
     * Run two async operations in parallel and collect results.
     */
    suspend fun <A, B> parallel(
        first: suspend () -> A,
        second: suspend () -> B
    ): Pair<A, B> = coroutineScope {
        val a = async { first() }
        val b = async { second() }
        Pair(a.await(), b.await())
    }
    
    /**
     * Run three async operations in parallel.
     */
    suspend fun <A, B, C> parallel(
        first: suspend () -> A,
        second: suspend () -> B,
        third: suspend () -> C
    ): Triple<A, B, C> = coroutineScope {
        val a = async { first() }
        val b = async { second() }
        val c = async { third() }
        Triple(a.await(), b.await(), c.await())
    }
}

/**
 * Map a collection in parallel with async operations.
 */
suspend fun <T, R> Collection<T>.parallelMap(
    transform: suspend (T) -> R
): List<R> = coroutineScope {
    map { async { transform(it) } }.awaitAll()
}

/**
 * Filter a collection with async predicate in parallel.
 */
suspend fun <T> Collection<T>.parallelFilter(
    predicate: suspend (T) -> Boolean
): List<T> = coroutineScope {
    map { item -> async { if (predicate(item)) item else null } }
        .awaitAll()
        .filterNotNull()
}
