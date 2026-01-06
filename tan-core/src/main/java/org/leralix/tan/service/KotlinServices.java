package org.leralix.tan.service;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
public final class KotlinServices {
    private KotlinServices() {
        throw new IllegalStateException("Utility class");
    }
    public static CompletableFuture<ITanPlayer> getPlayer(UUID uuid) {
        return PlayerDataService.INSTANCE.getPlayerAsync(uuid);
    }
    public static CompletableFuture<ITanPlayer> getPlayer(String id) {
        return PlayerDataService.INSTANCE.getPlayerAsync(id);
    }
    public static CompletableFuture<ITanPlayer> getPlayer(Player player) {
        return PlayerDataService.INSTANCE.getPlayerAsync(player);
    }
    public static CompletableFuture<Void> savePlayer(ITanPlayer player) {
        return PlayerDataService.INSTANCE.savePlayerAsync(player).thenApply(unit -> null);
    }
    public static CompletableFuture<TownData> getTown(String id) {
        return TerritoryService.INSTANCE.getTownAsync(id);
    }
    public static CompletableFuture<Void> saveTown(TownData town) {
        return TerritoryService.INSTANCE.saveTownAsync(town).thenApply(unit -> null);
    }
    public static CompletableFuture<RegionData> getRegion(String id) {
        return TerritoryService.INSTANCE.getRegionAsync(id);
    }
    public static CompletableFuture<Void> saveRegion(RegionData region) {
        return TerritoryService.INSTANCE.saveRegionAsync(region).thenApply(unit -> null);
    }
    public static CompletableFuture<TerritoryData> getTerritory(String id) {
        return TerritoryService.INSTANCE.getTerritoryAsync(id);
    }
    public static CompletableFuture<Void> saveTerritory(TerritoryData territory) {
        return TerritoryService.INSTANCE.saveTerritoryAsync(territory).thenApply(unit -> null);
    }
}