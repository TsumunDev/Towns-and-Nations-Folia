package org.leralix.tan.service.prestige;

import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.domain.prestige.model.PrestigePoints;
import org.leralix.tan.domain.prestige.model.PrestigeSource;
import org.leralix.tan.domain.prestige.model.PrestigeTransaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for prestige points management.
 * <p>
 * Prestige points are the currency used to purchase town upgrades.
 * They can be earned through quests, level milestones, tier ascension, etc.
 * </p>
 */
public interface PrestigeService {

    /**
     * Gets the singleton instance.
     */
    static PrestigeService getInstance() {
        return PrestigeServiceImpl.getInstance();
    }

    /**
     * Initializes the prestige service.
     */
    void initialize();

    /**
     * Gets the current prestige balance for a town.
     *
     * @param townId The town ID
     * @return Future completing with prestige balance
     */
    CompletableFuture<Long> getBalance(String townId);

    /**
     * Gets the full prestige points component for a town.
     *
     * @param townId The town ID
     * @return Future completing with prestige points
     */
    CompletableFuture<PrestigePoints> getPrestigePoints(String townId);

    /**
     * Adds prestige points to a town.
     *
     * @param townId The town ID
     * @param amount Amount to add (must be positive)
     * @param source Source of the prestige
     * @param description Optional description
     * @return Future completing when added
     */
    CompletableFuture<Void> addPrestige(
            String townId,
            long amount,
            PrestigeSource source,
            String description
    );

    /**
     * Spends prestige points from a town.
     *
     * @param townId The town ID
     * @param amount Amount to spend (must be positive)
     * @param description Description of the purchase
     * @return Future completing with true if successful, false if insufficient balance
     */
    CompletableFuture<Boolean> spendPrestige(
            String townId,
            long amount,
            String description
    );

    /**
     * Gets transaction history for a town.
     *
     * @param townId The town ID
     * @param count Maximum number of recent transactions
     * @return Future completing with transaction list
     */
    CompletableFuture<List<PrestigeTransaction>> getHistory(String townId, int count);

    /**
     * Calculates prestige decay for an inactive town.
     * (Post-MVP feature - returns 0 for now)
     *
     * @param townId The town ID
     * @param inactiveDays Number of days inactive
     * @return Amount of prestige to decay
     */
    default long calculateDecay(String townId, long inactiveDays) {
        return 0; // No decay in MVP
    }
}
