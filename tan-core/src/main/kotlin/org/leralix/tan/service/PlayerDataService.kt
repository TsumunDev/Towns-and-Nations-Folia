package org.leralix.tan.service
import kotlinx.coroutines.*
import org.bukkit.entity.Player
import org.leralix.tan.coroutines.TanCoroutines
import org.leralix.tan.dataclass.ITanPlayer
import org.leralix.tan.storage.stored.PlayerDataStorage
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
object PlayerDataService {
    private val storage: PlayerDataStorage by lazy { PlayerDataStorage.getInstance() }
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<ITanPlayer?>>()
    suspend fun getPlayer(uuid: UUID): ITanPlayer? = getPlayer(uuid.toString())
    suspend fun getPlayer(id: String): ITanPlayer? {
        inFlightRequests[id]?.let { deferred ->
            return deferred.await()
        }
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
    suspend fun getPlayer(player: Player): ITanPlayer? = getPlayer(player.uniqueId)
    suspend fun savePlayer(player: ITanPlayer) {
        withContext(Dispatchers.IO) {
            storage.put(player.id, player)
        }
    }
    suspend fun updatePlayer(id: String, transform: (ITanPlayer) -> ITanPlayer): ITanPlayer? {
        val player = getPlayer(id) ?: return null
        val updated = transform(player)
        savePlayer(updated)
        return updated
    }
    suspend fun updatePlayer(uuid: UUID, transform: (ITanPlayer) -> ITanPlayer): ITanPlayer? =
        updatePlayer(uuid.toString(), transform)
    suspend fun playerExists(id: String): Boolean = getPlayer(id) != null
    suspend fun deletePlayer(id: String) {
        withContext(Dispatchers.IO) {
            storage.delete(id)
        }
    }
    @JvmStatic
    fun getPlayerAsync(uuid: UUID): CompletableFuture<ITanPlayer?> =
        TanCoroutines.asFuture { getPlayer(uuid) }
    @JvmStatic
    fun getPlayerAsync(id: String): CompletableFuture<ITanPlayer?> =
        TanCoroutines.asFuture { getPlayer(id) }
    @JvmStatic
    fun getPlayerAsync(player: Player): CompletableFuture<ITanPlayer?> =
        TanCoroutines.asFuture { getPlayer(player) }
    @JvmStatic
    fun savePlayerAsync(player: ITanPlayer): CompletableFuture<Unit> =
        TanCoroutines.asFuture { savePlayer(player) }
    @JvmStatic
    fun playerExistsAsync(id: String): CompletableFuture<Boolean> =
        TanCoroutines.asFuture { playerExists(id) }
    @JvmStatic
    fun deletePlayerAsync(id: String): CompletableFuture<Unit> =
        TanCoroutines.asFuture { deletePlayer(id) }
    suspend fun getPlayers(ids: List<String>): Map<String, ITanPlayer?> = coroutineScope {
        ids.map { id ->
            async { id to getPlayer(id) }
        }.awaitAll().toMap()
    }
    @JvmStatic
    fun getPlayersAsync(ids: List<String>): CompletableFuture<Map<String, ITanPlayer?>> =
        TanCoroutines.asFuture { getPlayers(ids) }
}