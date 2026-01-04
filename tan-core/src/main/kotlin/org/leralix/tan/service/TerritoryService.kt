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

/**
 * Kotlin service for territory (Town/Region) operations using coroutines.
 * Provides unified access to towns and regions with caching and deduplication.
 */
object TerritoryService {
    
    private val townStorage: TownDataStorage by lazy { TownDataStorage.getInstance() }
    private val regionStorage: RegionDataStorage by lazy { RegionDataStorage.getInstance() }
    
    // In-flight request deduplication
    private val inFlightTownRequests = ConcurrentHashMap<String, Deferred<TownData?>>()
    private val inFlightRegionRequests = ConcurrentHashMap<String, Deferred<RegionData?>>()
    
    // ============ Town Operations ============
    
    /**
     * Get town by ID (suspend function)
     */
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
    
    /**
     * Save town (suspend function)
     */
    suspend fun saveTown(town: TownData) {
        withContext(Dispatchers.IO) {
            townStorage.put(town.id, town)
        }
    }
    
    /**
     * Update town with transformer (suspend function)
     */
    suspend fun updateTown(id: String, transform: (TownData) -> TownData): TownData? {
        val town = getTown(id) ?: return null
        val updated = transform(town)
        saveTown(updated)
        return updated
    }
    
    /**
     * Get all towns (suspend function)
     */
    suspend fun getAllTowns(): Collection<TownData> = withContext(Dispatchers.IO) {
        townStorage.allAsync.join().values
    }
    
    /**
     * Delete town (suspend function)
     */
    suspend fun deleteTown(id: String) {
        withContext(Dispatchers.IO) {
            townStorage.delete(id)
        }
    }
    
    // ============ Region Operations ============
    
    /**
     * Get region by ID (suspend function)
     */
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
    
    /**
     * Save region (suspend function)
     */
    suspend fun saveRegion(region: RegionData) {
        withContext(Dispatchers.IO) {
            regionStorage.put(region.id, region)
        }
    }
    
    /**
     * Update region with transformer (suspend function)
     */
    suspend fun updateRegion(id: String, transform: (RegionData) -> RegionData): RegionData? {
        val region = getRegion(id) ?: return null
        val updated = transform(region)
        saveRegion(updated)
        return updated
    }
    
    /**
     * Get all regions (suspend function)
     */
    suspend fun getAllRegions(): Collection<RegionData> = withContext(Dispatchers.IO) {
        regionStorage.allAsync.join().values
    }
    
    /**
     * Delete region (suspend function)
     */
    suspend fun deleteRegion(id: String) {
        withContext(Dispatchers.IO) {
            regionStorage.delete(id)
        }
    }
    
    // ============ Unified Territory Operations ============
    
    /**
     * Get any territory (town or region) by ID (suspend function)
     */
    suspend fun getTerritory(id: String): TerritoryData? {
        // Try town first, then region
        return getTown(id) ?: getRegion(id)
    }
    
    /**
     * Save any territory (suspend function)
     */
    suspend fun saveTerritory(territory: TerritoryData) {
        when (territory) {
            is TownData -> saveTown(territory)
            is RegionData -> saveRegion(territory)
            else -> throw IllegalArgumentException("Unknown territory type: ${territory::class}")
        }
    }
    
    // ============ Java-friendly CompletableFuture API ============
    
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
    
    // ============ Batch Operations ============
    
    /**
     * Batch get multiple towns (suspend function)
     */
    suspend fun getTowns(ids: List<String>): Map<String, TownData?> = coroutineScope {
        ids.map { id -> async { id to getTown(id) } }.awaitAll().toMap()
    }
    
    /**
     * Batch get multiple regions (suspend function)
     */
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
