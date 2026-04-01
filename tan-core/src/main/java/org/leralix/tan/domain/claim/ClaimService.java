package org.leralix.tan.domain.claim;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.leralix.lib.position.Vector2D;
import org.leralix.tan.dataclass.chunk.TownClaimedChunk;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing town land claims.
 *
 * <p>This service extracts claim-related logic from TownData following the
 * Strangler Fig Pattern. It provides async operations for claim management,
 * suitable for Folia's region-based threading.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Chunk claiming with validation</li>
 *   <li>Chunk unclaiming</li>
 *   <li>Claim count and limit tracking</li>
 *   <li>Claim cost calculation</li>
 *   <li>Capital location management</li>
 * </ul>
 *
 * @see ClaimServiceImpl
 * @since 2.0.0
 */
public interface ClaimService {

    /**
     * Gets the number of claimed chunks for a town.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the claim count (0 if town not found)
     */
    CompletableFuture<Integer> getNumberOfClaimedChunks(String townId);

    /**
     * Gets the claim cost for a town.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the claim cost (0 if town not found)
     */
    CompletableFuture<Integer> getClaimCost(String townId);

    /**
     * Gets the capital location for a town.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the capital location (empty if town not found or no capital set)
     */
    CompletableFuture<Optional<Vector2D>> getCapitalLocation(String townId);

    /**
     * Sets the capital location for a town.
     *
     * @param townId The town ID
     * @param location The new capital location
     * @return CompletableFuture that completes when the update is done
     */
    CompletableFuture<Void> setCapitalLocation(String townId, Vector2D location);

    /**
     * Claims a chunk for a town.
     *
     * <p>This method handles the core claim logic:
     * <ul>
     *   <li>Deducts claim cost from town balance</li>
     *   <li>Unclaims any existing chunk at the location</li>
     *   <li>Creates new town claim</li>
     *   <li>Sets capital if this is the first claim</li>
     * </ul></p>
     *
     * @param townId The town ID
     * @param chunk The chunk to claim
     * @return CompletableFuture containing the claimed chunk (null if town not found)
     */
    CompletableFuture<TownClaimedChunk> claimChunk(String townId, Chunk chunk);

    /**
     * Checks if this service is enabled via feature flag.
     *
     * @return true if the new claim service is enabled
     */
    boolean isEnabled();
}
