package org.leralix.tan.dataclass.territory.progression;

import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.territory.TownData;

/**
 * Service managing town progression through civilization tiers.
 * <p>
 * This service provides the business logic for:
 * <ul>
 *   <li>Adding XP and handling level-ups</li>
 *   <li>Checking and executing ascensions</li>
 *   <li>Calculating tier requirements and caps</li>
 *   <li>Retrieving progression data</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety:</h2>
 * All operations are designed to work with the immutable {@link TownProgressionComponent}.
 * The service itself is stateless and thread-safe.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Add XP to a town
 * TownProgressionService service = TownProgressionService.getInstance();
 * service.addXp(town, 500);
 *
 * // Try to ascend
 * if (service.canAscend(town)) {
 *     service.ascend(town);
 * }
 * }</pre>
 *
 * @since 1.0
 * @see TownProgressionComponent
 * @see TownTier
 */
public class TownProgressionService {

    private static TownProgressionService instance;
    private final TownTierConfig[] tierConfigs;

    private TownProgressionService() {
        this.tierConfigs = TownTierConfig.createDefaults();
    }

    /**
     * Gets the singleton instance of the progression service.
     *
     * @return the service instance
     */
    public static synchronized TownProgressionService getInstance() {
        if (instance == null) {
            instance = new TownProgressionService();
        }
        return instance;
    }

    /**
     * Gets the configuration for a specific tier.
     *
     * @param tier the tier
     * @return the tier configuration, or Camping config if not found
     */
    public TownTierConfig getConfig(TownTier tier) {
        for (TownTierConfig config : tierConfigs) {
            if (config.getTier() == tier) {
                return config;
            }
        }
        // Default to first config (Camping)
        return tierConfigs[0];
    }

    /**
     * Adds XP to a town and handles level-ups automatically.
     * <p>
     * This method updates the town's progression component, saves it to storage,
     * and notifies players of level-ups.
     * </p>
     *
     * @param town the town to add XP to
     * @param amount the amount of XP to add
     * @return true if the town leveled up, false otherwise
     */
    public boolean addXp(TownData town, long amount) {
        if (amount <= 0) {
            return false;
        }

        // TODO: Integrate with TownData once progression component is added
        // For now, this is a placeholder showing the intended logic

        TownProgressionComponent progression = getProgression(town);
        TownTierConfig config = getConfig(progression.getCurrentTier());

        int oldLevel = progression.getCurrentLevel();
        TownProgressionComponent newProgression = progression.addXp(amount, config);
        int newLevel = newProgression.getCurrentLevel();

        boolean leveledUp = newLevel > oldLevel;

        // Update town progression
        // town.setProgression(newProgression);
        // TownDataStorage.getInstance().update(town);

        // Notify players if leveled up
        if (leveledUp) {
            notifyLevelUp(town, oldLevel, newLevel);
        }

        return leveledUp;
    }

    /**
     * Checks if a town can ascend to the next tier.
     *
     * @param town the town to check
     * @return true if ascension is possible
     */
    public boolean canAscend(TownData town) {
        TownProgressionComponent progression = getProgression(town);
        TownTierConfig config = getConfig(progression.getCurrentTier());

        // Check level requirement
        if (!progression.canAscend(config)) {
            return false;
        }

        // TODO: Add additional checks (e.g., required buildings, player count)
        // This will be implemented when integrating with the upgrade system

        return true;
    }

    /**
     * Attempts to ascend a town to the next tier.
     * <p>
     * If successful, this will:
     * <ul>
     *   <li>Move the town to the next tier</li>
     *   <li>Reset level to 1</li>
     *   <li>Unlock new features and caps</li>
     *   <li>Notify all town members</li>
     * </ul>
     * </p>
     *
     * @param town the town to ascend
     * @return true if ascension succeeded, false otherwise
     */
    public boolean ascend(TownData town) {
        if (!canAscend(town)) {
            return false;
        }

        TownProgressionComponent progression = getProgression(town);
        TownTier oldTier = progression.getCurrentTier();
        TownProgressionComponent newProgression = progression.ascend();

        // Update town progression
        // town.setProgression(newProgression);
        // TownDataStorage.getInstance().update(town);

        // Notify players
        notifyAscension(town, oldTier, newProgression.getCurrentTier());

        return true;
    }

    /**
     * Gets the current progression component for a town.
     * <p>
     * TODO: This will be implemented when the progression component is added to TownData
     * </p>
     *
     * @param town the town
     * @return the progression component
     */
    private TownProgressionComponent getProgression(TownData town) {
        // TODO: Return town.getProgression() once integrated
        // For now, return a default component
        return new TownProgressionComponent();
    }

    /**
     * Notifies town members of a level-up.
     *
     * @param town the town that leveled up
     * @param oldLevel the previous level
     * @param newLevel the new level
     */
    private void notifyLevelUp(TownData town, int oldLevel, int newLevel) {
        // TODO: Implement notification system
        // This will use the town's broadcast methods with localized messages
        TownsAndNations.getPlugin().getLogger().info(
                String.format("Town %s leveled up from %d to %d",
                        town.getID(), oldLevel, newLevel));
    }

    /**
     * Notifies town members of a tier ascension.
     *
     * @param town the town that ascended
     * @param oldTier the previous tier
     * @param newTier the new tier
     */
    private void notifyAscension(TownData town, TownTier oldTier, TownTier newTier) {
        // TODO: Implement notification system with effects (sounds, particles)
        TownsAndNations.getPlugin().getLogger().info(
                String.format("Town %s ascended from %s to %s",
                        town.getID(), oldTier.getName(), newTier.getName()));
    }

    /**
     * Calculates the total XP a town has accumulated across all tiers.
     *
     * @param town the town
     * @return total XP
     */
    public long getTotalXp(TownData town) {
        // TODO: Implement when progression is integrated
        return 0;
    }

    /**
     * Gets the player cap for a town based on its tier.
     *
     * @param town the town
     * @return maximum players allowed
     */
    public int getPlayerCap(TownData town) {
        TownProgressionComponent progression = getProgression(town);
        TownTierConfig config = getConfig(progression.getCurrentTier());
        return config.getPlayerCap();
    }

    /**
     * Gets the claim cap for a town based on its tier.
     *
     * @param town the town
     * @return maximum chunks claimable
     */
    public int getClaimCap(TownData town) {
        TownProgressionComponent progression = getProgression(town);
        TownTierConfig config = getConfig(progression.getCurrentTier());
        return config.getClaimCap();
    }

    /**
     * Gets the XP required to reach the next level.
     *
     * @param town the town
     * @return XP needed for next level, or 0 if at max level
     */
    public long getXpToNextLevel(TownData town) {
        TownProgressionComponent progression = getProgression(town);
        TownTierConfig config = getConfig(progression.getCurrentTier());
        return progression.getXpToNextLevel(config);
    }

    /**
     * Gets the progress percentage to the next level.
     *
     * @param town the town
     * @return progress from 0.0 to 1.0
     */
    public double getLevelProgress(TownData town) {
        TownProgressionComponent progression = getProgression(town);
        TownTierConfig config = getConfig(progression.getCurrentTier());
        return progression.getLevelProgress(config);
    }

    /**
     * Resets the service instance (for testing purposes).
     */
    public static void reset() {
        instance = null;
    }
}
