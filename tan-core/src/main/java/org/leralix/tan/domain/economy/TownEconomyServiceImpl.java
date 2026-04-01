package org.leralix.tan.domain.economy;

import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.progression.TownTier;
import org.leralix.tan.domain.prestige.model.PrestigePoints;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Implementation of TownEconomyService.
 * Extracts economy-related logic from TownData following the Strangler Fig Pattern.
 *
 * <p>This service is enabled via the feature flag {@code development.use-new-economy-service}.</p>
 *
 * @see TownEconomyService
 * @since 2.0.0
 */
public class TownEconomyServiceImpl implements TownEconomyService {

    private static final Logger LOGGER = Logger.getLogger(TownEconomyServiceImpl.class.getName());

    private final TownDataStorage townStorage;

    /**
     * Creates a new TownEconomyServiceImpl.
     *
     * @param townStorage The town data storage
     */
    public TownEconomyServiceImpl(TownDataStorage townStorage) {
        this.townStorage = townStorage;
    }

    @Override
    public CompletableFuture<TownTier> getTownTier(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("TownEconomyService: Town not found: " + townId);
                    return TownTier.CAMPING; // Default fallback
                }
                return town.getTownTier();
            });
    }

    @Override
    public CompletableFuture<Integer> getTownLevel(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("TownEconomyService: Town not found: " + townId);
                    return 1; // Default fallback
                }
                return town.getTownLevel();
            });
    }

    @Override
    public CompletableFuture<Long> getPrestigeBalance(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("TownEconomyService: Town not found: " + townId);
                    return 0L; // Default fallback
                }
                return town.getPrestigeBalance();
            });
    }

    @Override
    public CompletableFuture<PrestigePoints> getPrestigePoints(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("TownEconomyService: Town not found: " + townId);
                    return PrestigePoints.create(); // Default fallback
                }
                return town.getPrestigePoints();
            });
    }

    @Override
    public CompletableFuture<Boolean> hasPurchasedUpgrade(String townId, String upgradeId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("TownEconomyService: Town not found: " + townId);
                    return false;
                }
                return town.hasPurchasedUpgrade(upgradeId);
            });
    }

    @Override
    public CompletableFuture<Set<String>> getPurchasedUpgrades(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("TownEconomyService: Town not found: " + townId);
                    return Set.of();
                }
                return town.getPurchasedUpgrades();
            });
    }

    @Override
    public CompletableFuture<Void> addPurchasedUpgrade(String townId, String upgradeId) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warning("TownEconomyService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.addPurchasedUpgrade(upgradeId);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public boolean isEnabled() {
        return TownsAndNations.getPlugin()
            .getConfig()
            .getBoolean("development.use-new-economy-service", false);
    }
}
