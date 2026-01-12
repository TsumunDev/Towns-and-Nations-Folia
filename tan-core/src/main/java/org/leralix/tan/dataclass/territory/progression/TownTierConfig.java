package org.leralix.tan.dataclass.territory.progression;

/**
 * Configuration for town tier progression.
 * <p>
 * This class defines the requirements and caps for each tier level.
 * It determines how much XP is needed, what level to reach, and what
 * limitations are applied at each stage of civilization.
 * </p>
 *
 * <h2>Configuration Structure:</h2>
 * <ul>
 *   <li>{@link #maxLevel} - Maximum level within this tier</li>
 *   <li>{@link #requiredLevel} - Level required to ascend to next tier</li>
 *   <li>{@link #xpMultiplier} - XP cost multiplier for this tier</li>
 *   <li>{@link #playerCap} - Maximum players allowed</li>
 *   <li>{@link #claimCap} - Maximum chunks claimable</li>
 * </ul>
 *
 * @since 1.0
 */
public class TownTierConfig {

    private final TownTier tier;
    private final int maxLevel;
    private final int requiredLevel;
    private final double xpMultiplier;
    private final int playerCap;
    private final int claimCap;

    /**
     * Creates a new tier configuration.
     *
     * @param tier the tier this config applies to
     * @param maxLevel maximum level within this tier (e.g., 100 for Camping)
     * @param requiredLevel level required to ascend (e.g., 50 for Camping → Hamlet)
     * @param xpMultiplier XP cost multiplier (higher = harder to level)
     * @param playerCap maximum number of players in the town
     * @param claimCap maximum number of chunks claimable
     */
    public TownTierConfig(
            TownTier tier,
            int maxLevel,
            int requiredLevel,
            double xpMultiplier,
            int playerCap,
            int claimCap) {
        this.tier = tier;
        this.maxLevel = maxLevel;
        this.requiredLevel = requiredLevel;
        this.xpMultiplier = xpMultiplier;
        this.playerCap = playerCap;
        this.claimCap = claimCap;
    }

    /**
     * Gets the tier this configuration applies to.
     *
     * @return the town tier
     */
    public TownTier getTier() {
        return tier;
    }

    /**
     * Gets the maximum level achievable in this tier.
     *
     * @return the maximum level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Gets the level required to ascend to the next tier.
     *
     * @return the required ascension level
     */
    public int getRequiredLevel() {
        return requiredLevel;
    }

    /**
     * Gets the XP multiplier for this tier.
     * <p>
     * Higher tiers require more XP per level.
     * </p>
     *
     * @return the XP multiplier
     */
    public double getXpMultiplier() {
        return xpMultiplier;
    }

    /**
     * Gets the maximum number of players allowed in this tier.
     *
     * @return the player cap
     */
    public int getPlayerCap() {
        return playerCap;
    }

    /**
     * Gets the maximum number of chunks claimable in this tier.
     *
     * @return the claim cap
     */
    public int getClaimCap() {
        return claimCap;
    }

    /**
     * Checks if a given level meets the requirement for ascension.
     *
     * @param level the current level
     * @return true if the level is sufficient to ascend
     */
    public boolean canAscend(int level) {
        return level >= requiredLevel;
    }

    /**
     * Calculates the XP required for a specific level in this tier.
     *
     * @param level the target level
     * @return the XP required
     */
    public long getXpForLevel(int level) {
        return (long) (baseXpRequirement(tier) * level * xpMultiplier);
    }

    /**
     * Calculates the total XP required to reach a level in this tier.
     *
     * @param targetLevel the target level
     * @return the total XP needed
     */
    public long getTotalXpForLevel(int targetLevel) {
        long totalXp = 0;
        for (int level = 1; level <= targetLevel; level++) {
            totalXp += getXpForLevel(level);
        }
        return totalXp;
    }

    /**
     * Gets the base XP requirement for a tier.
     *
     * @param tier the tier
     * @return base XP per level
     */
    private static long baseXpRequirement(TownTier tier) {
        return switch (tier) {
            case CAMPING -> 100;      // Easy early game
            case HAMLET -> 250;       // Getting harder
            case VILLAGE -> 500;      // Mid-game challenge
            case CITY -> 1000;        // Significant commitment
            case METROPOLIS -> 2000;  // End-game grind
        };
    }

    /**
     * Creates default configurations for all tiers.
     *
     * @return an array of default tier configs
     */
    public static TownTierConfig[] createDefaults() {
        return new TownTierConfig[]{
                // Camping: Easy start, limited caps
                new TownTierConfig(TownTier.CAMPING, 100, 50, 1.0, 5, 10),

                // Hamlet: First expansion
                new TownTierConfig(TownTier.HAMLET, 150, 100, 1.2, 10, 25),

                // Village: Established settlement
                new TownTierConfig(TownTier.VILLAGE, 200, 150, 1.5, 15, 50),

                // City: Major urban center
                new TownTierConfig(TownTier.CITY, 250, 200, 1.8, 25, 100),

                // Metropolis: Peak development
                new TownTierConfig(TownTier.METROPOLIS, 300, 250, 2.0, 40, 200)
        };
    }
}
