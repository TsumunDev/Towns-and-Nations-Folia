package org.leralix.tan.domain.economy;

import org.leralix.tan.dataclass.territory.progression.TownTier;
import org.leralix.tan.domain.prestige.model.PrestigePoints;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service for economy-related operations on towns.
 * Extracted from TownData as part of the refactoring to separate concerns.
 *
 * <p>This service handles:</p>
 * <ul>
 *   <li>Getting town tier and level</li>
 *   <li>Managing prestige balance</li>
 *   <li>Managing purchased upgrades</li>
 * </ul>
 *
 * @see org.leralix.tan.dataclass.territory.TownData
 * @since 2.0.0
 */
public interface TownEconomyService {

    /**
     * Gets the current town tier.
     *
     * @param townId The ID of the town
     * @return CompletableFuture containing the town tier
     */
    CompletableFuture<TownTier> getTownTier(String townId);

    /**
     * Gets the current town level within the tier.
     *
     * @param townId The ID of the town
     * @return CompletableFuture containing the town level
     */
    CompletableFuture<Integer> getTownLevel(String townId);

    /**
     * Gets the current prestige balance.
     *
     * @param townId The ID of the town
     * @return CompletableFuture containing the prestige balance
     */
    CompletableFuture<Long> getPrestigeBalance(String townId);

    /**
     * Gets the prestige points for the town.
     *
     * @param townId The ID of the town
     * @return CompletableFuture containing the prestige points
     */
    CompletableFuture<PrestigePoints> getPrestigePoints(String townId);

    /**
     * Checks if the town has purchased a specific upgrade.
     *
     * @param townId The ID of the town
     * @param upgradeId The ID of the upgrade to check
     * @return CompletableFuture containing true if the upgrade is purchased
     */
    CompletableFuture<Boolean> hasPurchasedUpgrade(String townId, String upgradeId);

    /**
     * Gets all purchased upgrades for the town.
     *
     * @param townId The ID of the town
     * @return CompletableFuture containing the set of purchased upgrade IDs
     */
    CompletableFuture<Set<String>> getPurchasedUpgrades(String townId);

    /**
     * Adds a purchased upgrade to the town.
     *
     * @param townId The ID of the town
     * @param upgradeId The ID of the upgrade to add
     * @return CompletableFuture that completes when the upgrade is added
     */
    CompletableFuture<Void> addPurchasedUpgrade(String townId, String upgradeId);

    /**
     * Checks if this service is enabled via feature flag.
     *
     * @return true if the new economy service should be used
     */
    boolean isEnabled();
}
