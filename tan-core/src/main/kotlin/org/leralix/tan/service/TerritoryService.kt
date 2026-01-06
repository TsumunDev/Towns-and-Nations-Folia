package org.leralix.tan.service
import kotlinx.coroutines.*
import org.leralix.tan.coroutines.TanCoroutines
import org.leralix.tan.dataclass.territory.RegionData
import org.leralix.tan.dataclass.territory.TerritoryData
import org.leralix.tan.dataclass.territory.TownData
import org.leralix.tan.storage.stored.RegionDataStorage
import org.leralix.tan.storage.stored.TownDataStorage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
object TerritoryService {
    private val townStorage: TownDataStorage by lazy { TownDataStorage.getInstance() }
    private val regionStorage: RegionDataStorage by lazy { RegionDataStorage.getInstance() }
    private val inFlightTownRequests = ConcurrentHashMap<String, Deferred<TownData?>>()
    private val inFlightRegionRequests = ConcurrentHashMap<String, Deferred<RegionData?>>()
    suspend fun getTown(id: String): TownData? {
        inFlightTownRequests[id]?.let { return it.await() }
        val deferred = TanCoroutines.async {
            withContext(Dispatchers.IO) {
                townStorage.get(id).join()
            }
        }
        inFlightTownRequests[id] = deferred
        return try {
            deferred.await()
        } finally {
            inFlightTownRequests.remove(id)
        }
    }
    suspend fun saveTown(town: TownData) {
        withContext(Dispatchers.IO) {
            townStorage.put(town.id, town)
        }
    }
    suspend fun updateTown(id: String, transform: (TownData) -> TownData): TownData? {
        val town = getTown(id) ?: return null
        val updated = transform(town)
        saveTown(updated)
        return updated
    }
    suspend fun getAllTowns(): Collection<TownData> = withContext(Dispatchers.IO) {
        townStorage.allAsync.join().values
    }
    suspend fun deleteTown(id: String) {
        withContext(Dispatchers.IO) {
            townStorage.delete(id)
        }
    }
    suspend fun getRegion(id: String): RegionData? {
        inFlightRegionRequests[id]?.let { return it.await() }
        val deferred = TanCoroutines.async {
            withContext(Dispatchers.IO) {
                regionStorage.get(id).join()
            }
        }
        inFlightRegionRequests[id] = deferred
        return try {
            deferred.await()
        } finally {
            inFlightRegionRequests.remove(id)
        }
    }
    suspend fun saveRegion(region: RegionData) {
        withContext(Dispatchers.IO) {
            regionStorage.put(region.id, region)
        }
    }
    suspend fun updateRegion(id: String, transform: (RegionData) -> RegionData): RegionData? {
        val region = getRegion(id) ?: return null
        val updated = transform(region)
        saveRegion(updated)
        return updated
    }
    suspend fun getAllRegions(): Collection<RegionData> = withContext(Dispatchers.IO) {
        regionStorage.allAsync.join().values
    }
    suspend fun deleteRegion(id: String) {
        withContext(Dispatchers.IO) {
            regionStorage.delete(id)
        }
    }
    suspend fun getTerritory(id: String): TerritoryData? {
        return getTown(id) ?: getRegion(id)
    }
    suspend fun saveTerritory(territory: TerritoryData) {
        when (territory) {
            is TownData -> saveTown(territory)
            is RegionData -> saveRegion(territory)
            else -> throw IllegalArgumentException("Unknown territory type: ${territory::class}")
        }
    }
    @JvmStatic
    fun getTownAsync(id: String): CompletableFuture<TownData?> =
        TanCoroutines.asFuture { getTown(id) }
    @JvmStatic
    fun saveTownAsync(town: TownData): CompletableFuture<Unit> =
        TanCoroutines.asFuture { saveTown(town) }
    @JvmStatic
    fun getAllTownsAsync(): CompletableFuture<Collection<TownData>> =
        TanCoroutines.asFuture { getAllTowns() }
    @JvmStatic
    fun deleteTownAsync(id: String): CompletableFuture<Unit> =
        TanCoroutines.asFuture { deleteTown(id) }
    @JvmStatic
    fun getRegionAsync(id: String): CompletableFuture<RegionData?> =
        TanCoroutines.asFuture { getRegion(id) }
    @JvmStatic
    fun saveRegionAsync(region: RegionData): CompletableFuture<Unit> =
        TanCoroutines.asFuture { saveRegion(region) }
    @JvmStatic
    fun getAllRegionsAsync(): CompletableFuture<Collection<RegionData>> =
        TanCoroutines.asFuture { getAllRegions() }
    @JvmStatic
    fun deleteRegionAsync(id: String): CompletableFuture<Unit> =
        TanCoroutines.asFuture { deleteRegion(id) }
    @JvmStatic
    fun getTerritoryAsync(id: String): CompletableFuture<TerritoryData?> =
        TanCoroutines.asFuture { getTerritory(id) }
    @JvmStatic
    fun saveTerritoryAsync(territory: TerritoryData): CompletableFuture<Unit> =
        TanCoroutines.asFuture { saveTerritory(territory) }
    suspend fun getTowns(ids: List<String>): Map<String, TownData?> = coroutineScope {
        ids.map { id -> async { id to getTown(id) } }.awaitAll().toMap()
    }
    suspend fun getRegions(ids: List<String>): Map<String, RegionData?> = coroutineScope {
        ids.map { id -> async { id to getRegion(id) } }.awaitAll().toMap()
    }
    @JvmStatic
    fun getTownsAsync(ids: List<String>): CompletableFuture<Map<String, TownData?>> =
        TanCoroutines.asFuture { getTowns(ids) }
    @JvmStatic
    fun getRegionsAsync(ids: List<String>): CompletableFuture<Map<String, RegionData?>> =
        TanCoroutines.asFuture { getRegions(ids) }
}