package org.leralix.tan.domain.claim;

import org.bukkit.Chunk;
import org.leralix.lib.position.Vector2D;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.dataclass.chunk.TownClaimedChunk;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of ClaimService.
 * Extracts claim-related logic from TownData following the Strangler Fig Pattern.
 *
 * <p>This service is enabled via the feature flag {@code development.use-new-claim-service}.</p>
 *
 * @see ClaimService
 * @since 2.0.0
 */
public class ClaimServiceImpl implements ClaimService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimServiceImpl.class);

    private final TownDataStorage townStorage;
    private NewClaimedChunkStorage chunkStorage;

    /**
     * Creates a new ClaimServiceImpl.
     *
     * @param townStorage The town data storage
     */
    public ClaimServiceImpl(TownDataStorage townStorage) {
        this.townStorage = townStorage;
        // Defer initialization to avoid issues during tests/plugin loading
        this.chunkStorage = null;
    }

    /**
     * Gets the chunk storage (lazy initialization).
     */
    private NewClaimedChunkStorage getChunkStorage() {
        if (chunkStorage == null) {
            chunkStorage = NewClaimedChunkStorage.getInstance();
        }
        return chunkStorage;
    }

    @Override
    public CompletableFuture<Integer> getNumberOfClaimedChunks(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("ClaimService: Town not found: " + townId);
                    return 0;
                }
                // Use the storage to get the actual count
                return getChunkStorage().getAllChunkFrom(town).size();
            });
    }

    @Override
    public CompletableFuture<Integer> getClaimCost(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("ClaimService: Town not found: " + townId);
                    return 0;
                }
                return town.getClaimCost();
            });
    }

    @Override
    public CompletableFuture<Optional<Vector2D>> getCapitalLocation(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("ClaimService: Town not found: " + townId);
                    return Optional.empty();
                }
                return town.getCapitalLocation();
            });
    }

    @Override
    public CompletableFuture<Void> setCapitalLocation(String townId, Vector2D location) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("ClaimService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.setCapitalLocation(location);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public CompletableFuture<TownClaimedChunk> claimChunk(String townId, Chunk chunk) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("ClaimService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }

                // Get the current claim count before claiming
                int currentClaims = getChunkStorage().getAllChunkFrom(town).size();

                // Deduct claim cost from town balance
                int claimCost = town.getClaimCost();
                town.removeFromBalance(claimCost);

                // Unclaim any existing chunk at this location
                ClaimedChunk2 existingChunk = getChunkStorage().get(chunk);
                getChunkStorage().unclaimChunkAndUpdate(existingChunk);

                // Create the new claim
                return getChunkStorage().claimTownChunkAsync(chunk, townId)
                    .thenApply(claimedChunk -> {
                        // Set capital if this is the first claim
                        if (currentClaims == 0) {
                            town.setCapitalLocation(claimedChunk.getVector2D());
                            // Save the updated town with capital location
                            townStorage.updateAsync(town);
                        }
                        return claimedChunk;
                    });
            });
    }

    @Override
    public boolean isEnabled() {
        TownsAndNations plugin = TownsAndNations.getPlugin();
        if (plugin == null) {
            return false; // Plugin not initialized (e.g., during tests)
        }
        return plugin.getConfig()
            .getBoolean("development.use-new-claim-service", false);
    }
}
