package org.leralix.tan.dataclass.territory.progression;

/**
 * Immutable component managing town progression through civilization tiers.
 * <p>
 * This component tracks a town's current tier (Camping, Hamlet, Village, etc.),
 * its level within that tier, and accumulated experience points (XP).
 * </p>
 *
 * <h2>Progression System:</h2>
 * <ul>
 *   <li>Towns gain XP by completing quests, building structures, and activity</li>
 *   <li>Each tier has its own level range (e.g., Camping: 1-100)</li>
 *   <li>Reaching a threshold level enables ascension to the next tier</li>
 *   <li>Ascending resets level to 1 but unlocks new features and higher caps</li>
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * This component is immutable. All modifications return new instances.
 * Use {@link Builder} for constructing modified instances.
 *
 * @since 1.0
 * @see TownTier
 * @see TownTierConfig
 */
public final class TownProgressionComponent {

    private final TownTier currentTier;
    private final int currentLevel;
    private final long currentXp;
    private final long totalXpInTier;

    // Default values for new towns
    public static final TownTier DEFAULT_TIER = TownTier.CAMPING;
    public static final int DEFAULT_LEVEL = 1;
    public static final long DEFAULT_XP = 0;

    /**
     * Creates a new progression component with default values (Camping, level 1, 0 XP).
     */
    public TownProgressionComponent() {
        this(DEFAULT_TIER, DEFAULT_LEVEL, DEFAULT_XP, DEFAULT_XP);
    }

    /**
     * Creates a new progression component with specified values.
     *
     * @param currentTier the current tier
     * @param currentLevel the current level within the tier
     * @param currentXp current XP in the current level
     * @param totalXpInTier total XP accumulated in the current tier
     */
    public TownProgressionComponent(
            TownTier currentTier,
            int currentLevel,
            long currentXp,
            long totalXpInTier) {
        if (currentTier == null) {
            throw new IllegalArgumentException("Town tier cannot be null");
        }
        if (currentLevel < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        if (currentXp < 0) {
            throw new IllegalArgumentException("XP cannot be negative");
        }
        if (totalXpInTier < 0) {
            throw new IllegalArgumentException("Total XP in tier cannot be negative");
        }

        this.currentTier = currentTier;
        this.currentLevel = currentLevel;
        this.currentXp = currentXp;
        this.totalXpInTier = totalXpInTier;
    }

    /**
     * Gets the current civilization tier.
     *
     * @return the current tier
     */
    public TownTier getCurrentTier() {
        return currentTier;
    }

    /**
     * Gets the current level within the current tier.
     *
     * @return the current level (1+)
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Gets the current XP in the current level.
     *
     * @return the current XP
     */
    public long getCurrentXp() {
        return currentXp;
    }

    /**
     * Gets the total XP accumulated in the current tier.
     *
     * @return the total XP in this tier
     */
    public long getTotalXpInTier() {
        return totalXpInTier;
    }

    /**
     * Checks if the town is at maximum level for the current tier.
     *
     * @param config the tier configuration
     * @return true if at max level
     */
    public boolean isMaxLevel(TownTierConfig config) {
        return currentLevel >= config.getMaxLevel();
    }

    /**
     * Checks if the town meets requirements for ascension.
     *
     * @param config the tier configuration
     * @return true if ascension is possible
     */
    public boolean canAscend(TownTierConfig config) {
        if (currentTier.isLast()) {
            return false;
        }
        return currentLevel >= config.getRequiredLevel();
    }

    /**
     * Gets the XP required for the next level.
     *
     * @param config the tier configuration
     * @return XP needed for next level
     */
    public long getXpToNextLevel(TownTierConfig config) {
        if (isMaxLevel(config)) {
            return 0;
        }
        return config.getXpForLevel(currentLevel + 1) - currentXp;
    }

    /**
     * Gets the progress percentage to the next level.
     *
     * @param config the tier configuration
     * @return progress from 0.0 to 1.0
     */
    public double getLevelProgress(TownTierConfig config) {
        if (isMaxLevel(config)) {
            return 1.0;
        }
        long requiredXp = config.getXpForLevel(currentLevel);
        if (requiredXp == 0) {
            return 0.0;
        }
        return (double) currentXp / requiredXp;
    }

    /**
     * Creates a new instance with added XP.
     * <p>
     * This method automatically handles level-ups when XP thresholds are reached.
     * </p>
     *
     * @param xpToAdd the XP to add
     * @param config the tier configuration
     * @return a new component with updated XP/level
     */
    public TownProgressionComponent addXp(long xpToAdd, TownTierConfig config) {
        if (xpToAdd <= 0) {
            return this;
        }

        long newXp = currentXp + xpToAdd;
        long requiredXp = config.getXpForLevel(currentLevel);
        int newLevel = currentLevel;
        long newTotalXp = totalXpInTier + xpToAdd;

        // Handle level-ups
        while (newXp >= requiredXp && newLevel < config.getMaxLevel()) {
            newXp -= requiredXp;
            newLevel++;
            requiredXp = config.getXpForLevel(newLevel);
        }

        // Cap at max level
        if (newLevel >= config.getMaxLevel()) {
            newLevel = config.getMaxLevel();
            newXp = config.getXpForLevel(newLevel);
        }

        return new TownProgressionComponent(currentTier, newLevel, newXp, newTotalXp);
    }

    /**
     * Creates a new instance with an ascended tier.
     * <p>
     * Resets level to 1 and clears XP, but moves to the next tier.
     * </p>
     *
     * @return a new component at the next tier, or this if already at max
     */
    public TownProgressionComponent ascend() {
        if (currentTier.isLast()) {
            return this;
        }
        TownTier nextTier = currentTier.next();
        return new TownProgressionComponent(nextTier, 1, 0, 0);
    }

    /**
     * Creates a new component with the specified tier.
     *
     * @param tier the new tier
     * @return a new component with the updated tier
     */
    public TownProgressionComponent withTier(TownTier tier) {
        return new TownProgressionComponent(tier, currentLevel, currentXp, totalXpInTier);
    }

    /**
     * Creates a new component with the specified level.
     *
     * @param level the new level
     * @return a new component with the updated level
     */
    public TownProgressionComponent withLevel(int level) {
        return new TownProgressionComponent(currentTier, level, currentXp, totalXpInTier);
    }

    /**
     * Creates a new component with the specified XP.
     *
     * @param xp the new XP amount
     * @return a new component with the updated XP
     */
    public TownProgressionComponent withXp(long xp) {
        return new TownProgressionComponent(currentTier, currentLevel, xp, totalXpInTier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TownProgressionComponent other)) return false;
        return currentTier == other.currentTier &&
                currentLevel == other.currentLevel &&
                currentXp == other.currentXp &&
                totalXpInTier == other.totalXpInTier;
    }

    @Override
    public int hashCode() {
        int result = currentTier.hashCode();
        result = 31 * result + currentLevel;
        result = 31 * result + (int) (currentXp ^ (currentXp >>> 32));
        result = 31 * result + (int) (totalXpInTier ^ (totalXpInTier >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "TownProgressionComponent{" +
                "tier=" + currentTier +
                ", level=" + currentLevel +
                ", xp=" + currentXp +
                ", totalXpInTier=" + totalXpInTier +
                '}';
    }

    /**
     * Builder for constructing or modifying TownProgressionComponent instances.
     */
    public static final class Builder {
        private TownTier tier = DEFAULT_TIER;
        private int level = DEFAULT_LEVEL;
        private long xp = DEFAULT_XP;
        private long totalXpInTier = DEFAULT_XP;

        public Builder() {
        }

        public Builder tier(TownTier tier) {
            this.tier = tier;
            return this;
        }

        public Builder level(int level) {
            this.level = level;
            return this;
        }

        public Builder xp(long xp) {
            this.xp = xp;
            return this;
        }

        public Builder totalXpInTier(long totalXpInTier) {
            this.totalXpInTier = totalXpInTier;
            return this;
        }

        /**
         * Builds the progression component.
         *
         * @return a new TownProgressionComponent
         */
        public TownProgressionComponent build() {
            return new TownProgressionComponent(tier, level, xp, totalXpInTier);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return a fresh Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
