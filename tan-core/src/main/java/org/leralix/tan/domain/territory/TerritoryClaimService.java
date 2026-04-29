package org.leralix.tan.domain.territory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.enums.RolePermission;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.ClaimBlacklistStorage;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.upgrade.TerritoryStats;
import org.leralix.tan.upgrade.rewards.list.BiomeStat;
import org.leralix.tan.upgrade.rewards.numeric.ChunkCap;
import org.leralix.tan.upgrade.rewards.numeric.ChunkCost;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.utils.territory.ChunkUtil;
import org.leralix.tan.utils.text.TanChatUtils;

/**
 * Handles chunk claiming validation and enemy claim tracking for territories.
 */
public class TerritoryClaimService {

    public boolean canClaimChunkSync(TerritoryData territory, Player player, Chunk chunk, boolean ignoreAdjacent) {
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
        if (ClaimBlacklistStorage.cannotBeClaimed(chunk)) {
            TanChatUtils.message(player, Lang.CHUNK_IS_BLACKLISTED.get(player));
            return false;
        }
        if (!territory.doesPlayerHavePermission(tanPlayer, RolePermission.CLAIM_CHUNK)) {
            TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(player));
            return false;
        }
        TerritoryStats territoryStats = territory.getNewLevel();
        int nbOfClaimedChunks = territory.getNumberOfClaimedChunk();
        if (!territoryStats.getStat(BiomeStat.class).canClaimBiome(chunk)) {
            TanChatUtils.message(player, Lang.CHUNK_BIOME_NOT_ALLOWED.get(player));
            return false;
        }
        if (!territoryStats.getStat(ChunkCap.class).canDoAction(nbOfClaimedChunks)) {
            TanChatUtils.message(player, Lang.MAX_CHUNK_LIMIT_REACHED.get(player));
            return false;
        }
        int cost = getClaimCost(territory);
        if (territory.getBalance() < cost) {
            TanChatUtils.message(player,
                Lang.TERRITORY_NOT_ENOUGH_MONEY.get(
                    player, territory.getColoredName(), Double.toString(cost - territory.getBalance())));
            return false;
        }
        ClaimedChunk2 chunkData = NewClaimedChunkStorage.getInstance().get(chunk);
        if (!chunkData.canTerritoryClaim(player, territory)) {
            return false;
        }
        if (ignoreAdjacent) {
            return true;
        }
        if (territory.getNumberOfClaimedChunk() == 0) {
            if (ChunkUtil.isInBufferZone(chunkData, territory)) {
                TanChatUtils.message(player,
                    Lang.CHUNK_IN_BUFFER_ZONE.get(
                        player, Integer.toString(Constants.territoryClaimBufferZone())));
                return false;
            }
            return true;
        }
        if (!NewClaimedChunkStorage.getInstance()
            .isOneAdjacentChunkClaimedBySameTerritoryAsync(chunk, territory.getID())
            .join()) {
            TanChatUtils.message(player, Lang.CHUNK_NOT_ADJACENT.get(player));
            return false;
        }
        return true;
    }

    public CompletableFuture<Boolean> canClaimChunkAsync(TerritoryData territory, Player player, Chunk chunk, boolean ignoreAdjacent) {
        return PlayerDataStorage.getInstance().get(player).thenCompose(tanPlayer -> {
            if (ClaimBlacklistStorage.cannotBeClaimed(chunk)) {
                TanChatUtils.message(player, Lang.CHUNK_IS_BLACKLISTED.get(player));
                return CompletableFuture.completedFuture(false);
            }
            if (!territory.doesPlayerHavePermission(tanPlayer, RolePermission.CLAIM_CHUNK)) {
                TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(player));
                return CompletableFuture.completedFuture(false);
            }
            TerritoryStats territoryStats = territory.getNewLevel();
            int nbOfClaimedChunks = territory.getNumberOfClaimedChunk();
            if (!territoryStats.getStat(BiomeStat.class).canClaimBiome(chunk)) {
                TanChatUtils.message(player, Lang.CHUNK_BIOME_NOT_ALLOWED.get(player));
                return CompletableFuture.completedFuture(false);
            }
            if (!territoryStats.getStat(ChunkCap.class).canDoAction(nbOfClaimedChunks)) {
                TanChatUtils.message(player, Lang.MAX_CHUNK_LIMIT_REACHED.get(player));
                return CompletableFuture.completedFuture(false);
            }
            int cost = getClaimCost(territory);
            if (territory.getBalance() < cost) {
                TanChatUtils.message(player,
                    Lang.TERRITORY_NOT_ENOUGH_MONEY.get(
                        player, territory.getColoredName(), Double.toString(cost - territory.getBalance())));
                return CompletableFuture.completedFuture(false);
            }
            String chunkKey = chunk.getX() + "," + chunk.getZ() + "," + chunk.getWorld().getUID().toString();
            return NewClaimedChunkStorage.getInstance().get(chunkKey)
                .thenCompose(chunkData -> {
                    if (chunkData == null) {
                        chunkData = new org.leralix.tan.dataclass.chunk.WildernessChunk(chunk);
                    }
                    if (!chunkData.canTerritoryClaim(player, territory)) {
                        return CompletableFuture.completedFuture(false);
                    }
                    if (ignoreAdjacent) {
                        return CompletableFuture.completedFuture(true);
                    }
                    if (territory.getNumberOfClaimedChunk() == 0) {
                        if (ChunkUtil.isInBufferZone(chunkData, territory)) {
                            TanChatUtils.message(player,
                                Lang.CHUNK_IN_BUFFER_ZONE.get(
                                    player, Integer.toString(Constants.territoryClaimBufferZone())));
                            return CompletableFuture.completedFuture(false);
                        }
                        return CompletableFuture.completedFuture(true);
                    }
                    return NewClaimedChunkStorage.getInstance()
                        .isOneAdjacentChunkClaimedBySameTerritoryAsync(chunk, territory.getID())
                        .thenApply(isAdjacent -> {
                            if (!isAdjacent) {
                                TanChatUtils.message(player, Lang.CHUNK_NOT_ADJACENT.get(player));
                            }
                            return isAdjacent;
                        });
                });
        });
    }

    public int getClaimCost(TerritoryData territory) {
        return territory.getNewLevel().getStat(ChunkCost.class).getCost();
    }

    public boolean canConquerChunk(Map<String, Integer> availableClaims, ClaimedChunk2 chunk) {
        if (availableClaims.containsKey(chunk.getOwnerID())) {
            consumeEnemyClaim(availableClaims, chunk.getOwnerID());
            return true;
        }
        return false;
    }

    public Map<String, Integer> getAvailableEnemyClaims(Map<String, Integer> availableClaims) {
        if (availableClaims == null) return new HashMap<>();
        return availableClaims;
    }

    public void addAvailableClaims(Map<String, Integer> availableClaims, String territoryID, int amount) {
        availableClaims.merge(territoryID, amount, Integer::sum);
    }

    public void consumeEnemyClaim(Map<String, Integer> availableClaims, String territoryID) {
        availableClaims.merge(territoryID, -1, Integer::sum);
        if (availableClaims.get(territoryID) <= 0)
            availableClaims.remove(territoryID);
    }
}
