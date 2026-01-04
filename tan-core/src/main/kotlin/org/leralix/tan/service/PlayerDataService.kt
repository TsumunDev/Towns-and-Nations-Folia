package org.leralix.tan.service

import kotlinx.coroutines.*
import org.bukkit.entity.Player
import org.leralix.tan.coroutines.TanCoroutines
import org.leralix.tan.dataclass.ITanPlayer
import org.leralix.tan.storage.stored.PlayerDataStorage
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Kotlin service for player data operations using coroutines.
 * Wraps PlayerDataStorage with suspend functions for cleaner async code.
 * 
 * Usage in Kotlin:
 * ```kotlin
 * val player = PlayerDataService.getPlayer(uuid)
 * PlayerDataService.updatePlayer(player) { it.apply { balance += 100.0 } }
 * ```
 * 
 * Usage from Java (via CompletableFuture):
 * ```java
 * PlayerDataService.getPlayerAsync(uuid).thenAccept(player -> { ... });
 * ```
 */
object PlayerDataService {
    
    private val storage: PlayerDataStorage by lazy { PlayerDataStorage.getInstance() }
    
    // In-flight request deduplication to prevent duplicate DB queries
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<ITanPlayer?>>()
    
    /**
     * Get player data by UUID (suspend function)
     */
    suspend fun getPlayer(uuid: UUID): ITanPlayer? = getPlayer(uuid.toString())
    
    /**
     * Get player data by string ID (suspend function)
     */
    suspend fun getPlayer(id: String): ITanPlayer? {
        // Check for in-flight request to deduplicate
        inFlightRequests[id]?.let { deferred ->
            return deferred.await()
        }
        
        // Create new deferred and register it
        val deferred = TanCoroutines.async {
            withContext(Dispatchers.IO) {
                storage.get(id).join()
            }
        }
        
        inFlightRequests[id] = deferred
        
        return try {
            deferred.await()
        } finally {
            inFlightRequests.remove(id)
        }
    }
    
    /**
     * Get player data by Bukkit Player (suspend function)
     */
    suspend fun getPlayer(player: Player): ITanPlayer? = getPlayer(player.uniqueId)
    
    /**
     * Save player data (suspend function)
     */
    suspend fun savePlayer(player: ITanPlayer) {
        withContext(Dispatchers.IO) {
            storage.put(player.id, player)
        }
    }
    
    /**
     * Update player data with a transformer function (suspend function)
     * Atomic read-modify-write pattern
     */
    suspend fun updatePlayer(id: String, transform: (ITanPlayer) -> ITanPlayer): ITanPlayer? {
        val player = getPlayer(id) ?: return null
        val updated = transform(player)
        savePlayer(updated)
        return updated
    }
    
    /**
     * Update player data with a transformer function (suspend function)
     */
    suspend fun updatePlayer(uuid: UUID, transform: (ITanPlayer) -> ITanPlayer): ITanPlayer? =
        updatePlayer(uuid.toString(), transform)
    
    /**
     * Check if player exists (suspend function)
     */
    suspend fun playerExists(id: String): Boolean = getPlayer(id) != null
    
    /**
     * Delete player data (suspend function)
     */
    suspend fun deletePlayer(id: String) {
        withContext(Dispatchers.IO) {
            storage.delete(id)
        }
    }
    
    // ============ Java-friendly CompletableFuture API ============
    
    /**
     * Get player data by UUID (Java-friendly)
     */
    @JvmStatic
    fun getPlayerAsync(uuid: UUID): CompletableFuture<ITanPlayer?> = 
        TanCoroutines.asFuture { getPlayer(uuid) }
    
    /**
     * Get player data by string ID (Java-friendly)
     */
    @JvmStatic
    fun getPlayerAsync(id: String): CompletableFuture<ITanPlayer?> = 
        TanCoroutines.asFuture { getPlayer(id) }
    
    /**
     * Get player data by Bukkit Player (Java-friendly)
     */
    @JvmStatic
    fun getPlayerAsync(player: Player): CompletableFuture<ITanPlayer?> = 
        TanCoroutines.asFuture { getPlayer(player) }
    
    /**
     * Save player data (Java-friendly)
     */
    @JvmStatic
    fun savePlayerAsync(player: ITanPlayer): CompletableFuture<Unit> = 
        TanCoroutines.asFuture { savePlayer(player) }
    
    /**
     * Check if player exists (Java-friendly)
     */
    @JvmStatic
    fun playerExistsAsync(id: String): CompletableFuture<Boolean> = 
        TanCoroutines.asFuture { playerExists(id) }
    
    /**
     * Delete player data (Java-friendly)
     */
    @JvmStatic
    fun deletePlayerAsync(id: String): CompletableFuture<Unit> = 
        TanCoroutines.asFuture { deletePlayer(id) }
    
    /**
     * Batch get multiple players (suspend function)
     */
    suspend fun getPlayers(ids: List<String>): Map<String, ITanPlayer?> = coroutineScope {
        ids.map { id -> 
            async { id to getPlayer(id) }
        }.awaitAll().toMap()
    }
    
    /**
     * Batch get multiple players (Java-friendly)
     */
    @JvmStatic
    fun getPlayersAsync(ids: List<String>): CompletableFuture<Map<String, ITanPlayer?>> =
        TanCoroutines.asFuture { getPlayers(ids) }
}
