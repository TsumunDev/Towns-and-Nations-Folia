package org.leralix.tan.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;

/**
 * Java bridge to access Kotlin services.
 * Provides CompletableFuture-based API for Java code.
 * 
 * <p>Usage:
 * <pre>{@code
 * KotlinServices.getPlayer(player).thenAccept(tanPlayer -> {
 *     // Use tanPlayer
 * });
 * 
 * KotlinServices.getTown(townId).thenAccept(town -> {
 *     // Use town
 * });
 * }</pre>
 */
public final class KotlinServices {
    
    private KotlinServices() {
        throw new IllegalStateException("Utility class");
    }
    
    // ============ Player Services ============
    
    /**
     * Get player data by UUID.
     */
    public static CompletableFuture<ITanPlayer> getPlayer(UUID uuid) {
        return PlayerDataService.INSTANCE.getPlayerAsync(uuid);
    }
    
    /**
     * Get player data by string ID.
     */
    public static CompletableFuture<ITanPlayer> getPlayer(String id) {
        return PlayerDataService.INSTANCE.getPlayerAsync(id);
    }
    
    /**
     * Get player data by Bukkit Player.
     */
    public static CompletableFuture<ITanPlayer> getPlayer(Player player) {
        return PlayerDataService.INSTANCE.getPlayerAsync(player);
    }
    
    /**
     * Save player data.
     */
    public static CompletableFuture<Void> savePlayer(ITanPlayer player) {
        return PlayerDataService.INSTANCE.savePlayerAsync(player).thenApply(unit -> null);
    }
    
    // ============ Territory Services ============
    
    /**
     * Get town by ID.
     */
    public static CompletableFuture<TownData> getTown(String id) {
        return TerritoryService.INSTANCE.getTownAsync(id);
    }
    
    /**
     * Save town.
     */
    public static CompletableFuture<Void> saveTown(TownData town) {
        return TerritoryService.INSTANCE.saveTownAsync(town).thenApply(unit -> null);
    }
    
    /**
     * Get region by ID.
     */
    public static CompletableFuture<RegionData> getRegion(String id) {
        return TerritoryService.INSTANCE.getRegionAsync(id);
    }
    
    /**
     * Save region.
     */
    public static CompletableFuture<Void> saveRegion(RegionData region) {
        return TerritoryService.INSTANCE.saveRegionAsync(region).thenApply(unit -> null);
    }
    
    /**
     * Get any territory by ID.
     */
    public static CompletableFuture<TerritoryData> getTerritory(String id) {
        return TerritoryService.INSTANCE.getTerritoryAsync(id);
    }
    
    /**
     * Save any territory.
     */
    public static CompletableFuture<Void> saveTerritory(TerritoryData territory) {
        return TerritoryService.INSTANCE.saveTerritoryAsync(territory).thenApply(unit -> null);
    }
}
