package org.leralix.tan.dataclass.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.RankData;
import org.leralix.tan.dataclass.territory.NationData;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.storage.invitation.TownInviteDataStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Manages all territory-related logic for a player — town/region membership, ranks, diplomacy, and name caching.
 * Data fields (TownId, townRankID, regionRankID) remain in PlayerData for Gson serialization compatibility.
 */
public class PlayerTerritoryComponent {
    private final ITanPlayer player;

    // Performance cache for town/region names and overlord status
    private String cachedTownName;
    private String cachedNationName;
    private Boolean cachedIsOverlord;
    private long cacheTime;
    private static final long CACHE_TTL_MS = 5000;

    public PlayerTerritoryComponent(ITanPlayer player) {
        this.player = player;
    }

    // ── Town queries ──

    public CompletableFuture<TownData> getTown() {
        String townId = player.getTownId();
        if (townId == null) {
            return CompletableFuture.completedFuture(null);
        }
        return TownDataStorage.getInstance().get(townId);
    }

    public boolean hasTown() {
        return player.getTownId() != null;
    }

    public String getTownName() {
        if (!hasTown()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (cachedTownName != null && (now - cacheTime) < CACHE_TTL_MS) {
            return cachedTownName;
        }
        getTown().thenAccept(town -> {
            if (town != null) {
                cachedTownName = town.getName();
                cacheTime = now;
            }
        });
        return cachedTownName;
    }

    public CompletableFuture<String> getTownNameAsync() {
        if (!hasTown()) {
            return CompletableFuture.completedFuture(null);
        }
        return getTown().thenApply(town -> town != null ? town.getName() : null);
    }

    public boolean isTownOverlord() {
        if (!hasTown()) return false;
        long now = System.currentTimeMillis();
        if (cachedIsOverlord != null && (now - cacheTime) < CACHE_TTL_MS) {
            return cachedIsOverlord;
        }
        getTown().thenAccept(town -> {
            if (town != null) {
                cachedIsOverlord = town.isLeader(player.getID());
                cacheTime = now;
            }
        });
        return cachedIsOverlord != null && cachedIsOverlord;
    }

    public CompletableFuture<Boolean> isTownOverlordAsync() {
        if (!hasTown()) {
            return CompletableFuture.completedFuture(false);
        }
        return getTown().thenApply(town -> town != null && town.isLeader(player.getID()));
    }

    public RankData getTownRank() {
        if (!hasTown()) return null;
        TownData cachedTown = TownDataStorage.getInstance().getSync(player.getTownId());
        if (cachedTown != null) {
            return cachedTown.getRank(player.getTownRankID());
        }
        return null;
    }

    public CompletableFuture<RankData> getTownRankAsync() {
        if (!hasTown()) {
            return CompletableFuture.completedFuture(null);
        }
        return getTown().thenApply(town -> town != null ? town.getRank(player.getTownRankID()) : null);
    }

    // ── Region queries ──

    public boolean hasRegion() {
        if (!hasTown()) {
            return false;
        }
        TownData cachedTown = TownDataStorage.getInstance().getSync(player.getTownId());
        if (cachedTown != null) {
            return cachedTown.haveOverlord();
        }
        return false;
    }

    public CompletableFuture<Boolean> hasRegionAsync() {
        if (!hasTown()) {
            return CompletableFuture.completedFuture(false);
        }
        return getTown().thenApply(town -> town != null && town.haveOverlord());
    }

    public CompletableFuture<RegionData> getRegion() {
        if (!hasRegion()) return CompletableFuture.completedFuture(null);
        return getTown()
                .thenApply(town -> {
                    if (town == null) return null;
                    Optional<TerritoryData> overlord = town.getOverlord();
                    return overlord.map(territoryData -> (RegionData) territoryData).orElse(null);
                });
    }

    public String getNationName() {
        if (!hasRegion()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (cachedNationName != null && (now - cacheTime) < CACHE_TTL_MS) {
            return cachedNationName;
        }
        getRegion().thenAccept(region -> {
            if (region != null) {
                cachedNationName = region.getName();
                cacheTime = now;
            }
        });
        return cachedNationName;
    }

    public CompletableFuture<String> getNationNameAsync() {
        if (!hasRegion()) {
            return CompletableFuture.completedFuture(null);
        }
        return getRegion().thenApply(region -> region != null ? region.getName() : null);
    }

    public CompletableFuture<RankData> getRegionRankAsync() {
        if (!hasRegion()) return CompletableFuture.completedFuture(null);
        return getRegion().thenCombine(
                CompletableFuture.completedFuture(player.getRegionRankID()),
                (region, rankID) -> region != null ? region.getRank(rankID) : null
        );
    }

    public Integer getRegionRankID() {
        if (!hasRegion()) {
            return null;
        }
        return player.getRegionRankID();
    }

    public CompletableFuture<Integer> getRegionRankIDAsync() {
        if (!hasRegion()) {
            return CompletableFuture.completedFuture(null);
        }
        if (player.getRegionRankID() != null) {
            return CompletableFuture.completedFuture(player.getRegionRankID());
        }
        return getRegion().thenApply(region -> {
            if (region != null) {
                player.setRegionRankID(region.getDefaultRankID());
                return player.getRegionRankID();
            }
            return null;
        });
    }

    // ── Territory rank dispatch ──

    public Integer getRankID(TerritoryData territoryData) {
        if (territoryData instanceof TownData) {
            return player.getTownRankID();
        } else if (territoryData instanceof RegionData) {
            return getRegionRankID();
        } else if (territoryData instanceof org.leralix.tan.dataclass.territory.NationData) {
            return player.getNationRankID();
        }
        return null;
    }

    public RankData getRank(TerritoryData territoryData) {
        return territoryData.getRank(getRankID(territoryData));
    }

    public void setRankID(TerritoryData territoryData, Integer defaultRankID) {
        if (territoryData instanceof TownData) {
            player.setTownRankID(defaultRankID);
        }
        if (territoryData instanceof RegionData) {
            player.setRegionRankID(defaultRankID);
        }
        if (territoryData instanceof org.leralix.tan.dataclass.territory.NationData) {
            player.setNationRankID(defaultRankID);
        }
    }

    // ── Territory aggregation ──

    public CompletableFuture<List<TerritoryData>> getAllTerritoriesPlayerIsIn() {
        CompletableFuture<TownData> townFuture = getTown();
        CompletableFuture<RegionData> regionFuture = getRegion();
        CompletableFuture<NationData> nationFuture = getRegion().thenCompose(region -> {
            if (region == null) return CompletableFuture.completedFuture(null);
            return region.getNationAsync();
        });
        return CompletableFuture.allOf(townFuture, regionFuture, nationFuture)
                .thenApply(v -> {
                    List<TerritoryData> territories = new ArrayList<>();
                    TownData town = townFuture.join();
                    RegionData region = regionFuture.join();
                    NationData nation = nationFuture.join();
                    if (town != null) {
                        territories.add(town);
                    }
                    if (region != null) {
                        territories.add(region);
                    }
                    if (nation != null) {
                        territories.add(nation);
                    }
                    return territories;
                });
    }

    // ── Diplomacy ──

    public CompletableFuture<TownRelation> getRelationWithPlayer(Player otherPlayer) {
        return PlayerDataStorage.getInstance()
                .get(otherPlayer)
                .thenCompose(otherPlayerData -> getRelationWithPlayer(otherPlayerData));
    }

    public CompletableFuture<TownRelation> getRelationWithPlayer(ITanPlayer otherPlayer) {
        if (!hasTown() || !otherPlayer.hasTown())
            return CompletableFuture.completedFuture(TownRelation.NEUTRAL);
        return getTown()
                .thenCombine(
                        otherPlayer.getTown(),
                        (playerTown, otherPlayerTown) -> {
                            if (playerTown == null || otherPlayerTown == null) {
                                return TownRelation.NEUTRAL;
                            }
                            return playerTown.getRelationWith(otherPlayerTown);
                        });
    }

    public TownRelation getRelationWithPlayerSync(ITanPlayer otherPlayer) {
        if (!hasTown() || !otherPlayer.hasTown()) return TownRelation.NEUTRAL;
        return TownRelation.NEUTRAL;
    }

    // ── Town applications ──

    public void clearAllTownApplications() {
        String uuid = player.getID();
        TownInviteDataStorage.removeInvitation(uuid);
        for (TownData allTown : TownDataStorage.getInstance().getAllSync().values()) {
            allTown.removePlayerJoinRequest(uuid);
        }
    }
}
