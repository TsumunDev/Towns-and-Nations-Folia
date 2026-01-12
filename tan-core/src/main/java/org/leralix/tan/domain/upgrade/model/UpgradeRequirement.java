package org.leralix.tan.domain.upgrade.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.lang.LangType;

/**
 * Requirements that must be met to purchase an upgrade.
 * <p>
 * Implementations can check tier, level, other upgrades, etc.
 * </p>
 */
public interface UpgradeRequirement {

    /**
     * Checks if this requirement is satisfied by the given town.
     *
     * @param town The town to check
     * @return true if the requirement is satisfied
     */
    boolean isSatisfied(TownData town);

    /**
     * Gets a human-readable description of this requirement.
     *
     * @param lang The language type
     * @return Description component
     */
    Component getDescription(LangType lang);

    /**
     * Requirement for a specific civilization tier.
     */
    record TierRequirement(org.leralix.tan.dataclass.territory.progression.TownTier tier) implements UpgradeRequirement {
        @Override
        public boolean isSatisfied(TownData town) {
            return town.getTownTier().getLevel() >= tier.getLevel();
        }

        @Override
        public Component getDescription(LangType lang) {
            return Component.text("Required Tier: " + tier.getName(), NamedTextColor.RED);
        }
    }

    /**
     * Requirement for a specific level within the current tier.
     */
    record LevelRequirement(int level) implements UpgradeRequirement {
        @Override
        public boolean isSatisfied(TownData town) {
            return town.getTownLevel() >= level;
        }

        @Override
        public Component getDescription(LangType lang) {
            return Component.text("Required Level: " + level, NamedTextColor.RED);
        }
    }

    /**
     * Requirement that another upgrade must be purchased first.
     */
    record UpgradePrerequisiteRequirement(String upgradeId) implements UpgradeRequirement {
        @Override
        public boolean isSatisfied(TownData town) {
            // TODO: Check if town has purchased this upgrade
            // This will be implemented when we add purchased upgrades tracking to TownData
            return false; // Placeholder
        }

        @Override
        public Component getDescription(LangType lang) {
            return Component.text("Requires upgrade: " + upgradeId, NamedTextColor.RED);
        }
    }

    /**
     * Composite requirement - all sub-requirements must be satisfied (AND logic).
     */
    record AndRequirement(UpgradeRequirement[] requirements) implements UpgradeRequirement {
        @Override
        public boolean isSatisfied(TownData town) {
            for (UpgradeRequirement req : requirements) {
                if (!req.isSatisfied(town)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Component getDescription(LangType lang) {
            Component result = Component.text("All of:", NamedTextColor.GRAY);
            for (UpgradeRequirement req : requirements) {
                result = result.append(Component.newline())
                        .append(Component.text("• ", NamedTextColor.GRAY))
                        .append(req.getDescription(lang));
            }
            return result;
        }
    }
}
