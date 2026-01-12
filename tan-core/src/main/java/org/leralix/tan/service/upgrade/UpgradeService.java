package org.leralix.tan.service.upgrade;

import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.domain.upgrade.model.TownUpgrade;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for town upgrade management.
 * <p>
 * Manages the purchasing and application of town upgrades using prestige points.
 * </p>
 */
public interface UpgradeService {

    /**
     * Result of an upgrade purchase attempt.
     */
    enum PurchaseResult {
        /** Purchase successful */
        SUCCESS,
        /** Not enough prestige points */
        INSUFFICIENT_PRESTIGE,
        /** Requirements not met */
        REQUIREMENTS_NOT_MET,
        /** Upgrade already purchased */
        ALREADY_PURCHASED,
        /** Upgrade not found */
        NOT_FOUND,
        /** Town not found */
        TOWN_NOT_FOUND,
        /** Unknown error */
        ERROR
    }

    /**
     * Gets the singleton instance.
     */
    static UpgradeService getInstance() {
        return UpgradeServiceImpl.getInstance();
    }

    /**
     * Initializes the upgrade service and loads upgrade definitions.
     */
    void initialize();

    /**
     * Gets all available upgrades for a town (filtered by requirements).
     *
     * @param townId The town ID
     * @return Future completing with list of available upgrades
     */
    CompletableFuture<List<TownUpgrade>> getAvailableUpgrades(String townId);

    /**
     * Gets all upgrades (not filtered by requirements).
     *
     * @return List of all registered upgrades
     */
    List<TownUpgrade> getAllUpgrades();

    /**
     * Gets a specific upgrade by ID.
     *
     * @param upgradeId The upgrade ID
     * @return Optional containing the upgrade, or empty if not found
     */
    Optional<TownUpgrade> getUpgrade(String upgradeId);

    /**
     * Purchases an upgrade for a town.
     *
     * @param player The player purchasing the upgrade
     * @param townId The town to purchase for
     * @param upgradeId The upgrade to purchase
     * @return Future completing with purchase result
     */
    CompletableFuture<PurchaseResult> purchaseUpgrade(
            Player player,
            String townId,
            String upgradeId
    );

    /**
     * Checks if a town can purchase an upgrade.
     *
     * @param townId The town ID
     * @param upgradeId The upgrade ID
     * @return Future completing with true if purchasable
     */
    CompletableFuture<Boolean> canPurchase(String townId, String upgradeId);

    /**
     * Gets the set of purchased upgrade IDs for a town.
     * TODO: Implement upgrade tracking in TownData
     *
     * @param townId The town ID
     * @return Future completing with set of purchased upgrade IDs
     */
    CompletableFuture<Set<String>> getPurchasedUpgrades(String townId);
}
