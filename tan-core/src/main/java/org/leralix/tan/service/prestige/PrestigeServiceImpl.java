package org.leralix.tan.service.prestige;

import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.domain.prestige.model.PrestigePoints;
import org.leralix.tan.domain.prestige.model.PrestigeSource;
import org.leralix.tan.domain.prestige.model.PrestigeTransaction;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Implementation of the prestige service.
 * <p>
 * Manages prestige points with proper async operations and thread safety.
 * </p>
 */
public class PrestigeServiceImpl implements PrestigeService {

    private static PrestigeServiceImpl instance;
    private final Logger logger;

    private PrestigeServiceImpl() {
        this.logger = TownsAndNations.getPlugin().getLogger();
    }

    /**
     * Gets the singleton instance.
     */
    public static PrestigeServiceImpl getInstance() {
        if (instance == null) {
            instance = new PrestigeServiceImpl();
        }
        return instance;
    }

    @Override
    public void initialize() {
        logger.info("Initializing PrestigeService...");
        // No special initialization needed for now
        logger.info("PrestigeService initialized.");
    }

    @Override
    public CompletableFuture<Long> getBalance(String townId) {
        return TownDataStorage.getInstance().get(townId)
                .thenApply(town -> {
                    if (town == null) {
                        logger.warning("Town not found: " + townId);
                        return 0L;
                    }
                    return town.getPrestigeBalance();
                });
    }

    @Override
    public CompletableFuture<PrestigePoints> getPrestigePoints(String townId) {
        return TownDataStorage.getInstance().get(townId)
                .thenApply(town -> {
                    if (town == null) {
                        logger.warning("Town not found: " + townId);
                        return PrestigePoints.create();
                    }
                    return town.getPrestigePoints();
                });
    }

    @Override
    public CompletableFuture<Void> addPrestige(
            String townId,
            long amount,
            PrestigeSource source,
            String description
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        return TownDataStorage.getInstance().get(townId)
                .thenAccept(town -> {
                    if (town == null) {
                        logger.warning("Town not found: " + townId);
                        return;
                    }

                    PrestigePoints currentPoints = town.getPrestigePoints();
                    PrestigePoints newPoints = currentPoints.add(amount, source, description);

                    // Update town with new prestige points
                    town.setPrestigePoints(newPoints);

                    // Save to database
                    TownDataStorage.getInstance().update(town);

                    logger.fine(String.format(
                            "Added %d prestige to town %s (source: %s, new balance: %d)",
                            amount, townId, source, newPoints.currentBalance()
                    ));
                });
    }

    @Override
    public CompletableFuture<Boolean> spendPrestige(
            String townId,
            long amount,
            String description
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        return TownDataStorage.getInstance().get(townId)
                .thenApply(town -> {
                    if (town == null) {
                        logger.warning("Town not found: " + townId);
                        return false;
                    }

                    PrestigePoints currentPoints = town.getPrestigePoints();

                    // Check if town can afford
                    if (!currentPoints.canAfford(amount)) {
                        logger.fine(String.format(
                                "Town %s cannot afford %d prestige (balance: %d)",
                                townId, amount, currentPoints.currentBalance()
                        ));
                        return false;
                    }

                    // Spend the prestige
                    PrestigePoints newPoints = currentPoints.spend(amount, description);

                    // Update town
                    town.setPrestigePoints(newPoints);

                    // Save to database
                    TownDataStorage.getInstance().update(town);

                    logger.fine(String.format(
                            "Spent %d prestige from town %s (new balance: %d)",
                            amount, townId, newPoints.currentBalance()
                    ));

                    return true;
                });
    }

    @Override
    public CompletableFuture<List<PrestigeTransaction>> getHistory(String townId, int count) {
        return TownDataStorage.getInstance().get(townId)
                .thenApply(town -> {
                    if (town == null) {
                        logger.warning("Town not found: " + townId);
                        return List.of();
                    }

                    return town.getPrestigePoints().getRecentTransactions(count);
                });
    }
}
