package org.leralix.tan.domain.upgrade.model;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.lang.LangType;

import java.util.List;
import java.util.Objects;

/**
 * Represents a town upgrade that can be purchased with prestige points.
 * <p>
 * Town upgrades provide various benefits such as increased caps,
 * unlocked features, or improved capabilities.
 * </p>
 *
 * @param id Unique identifier (e.g., "town_hall_2")
 * @param name Human-readable name
 * @param description Detailed description
 * @param cost Cost in prestige points
 * @param requirements Requirements to purchase
 * @param rewards Rewards granted when purchased
 * @param category Category for GUI organization
 * @param icon Icon material for GUI
 */
public record TownUpgrade(
        String id,
        String name,
        String description,
        int cost,
        List<UpgradeRequirement> requirements,
        List<UpgradeReward> rewards,
        UpgradeCategory category,
        Material icon
) {

    public TownUpgrade {
        Objects.requireNonNull(id, "Upgrade ID cannot be null");
        Objects.requireNonNull(name, "Upgrade name cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(requirements, "Requirements cannot be null");
        Objects.requireNonNull(rewards, "Rewards cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Upgrade ID cannot be blank");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("Upgrade must have at least one requirement (use TierRequirement.CAMPING for no requirements)");
        }
        if (rewards.isEmpty()) {
            throw new IllegalArgumentException("Upgrade must have at least one reward");
        }
    }

    /**
     * Checks if this upgrade can be purchased by the given town.
     *
     * @param town The town to check
     * @return true if all requirements are satisfied
     */
    public boolean canPurchase(TownData town) {
        return requirements.stream().allMatch(req -> req.isSatisfied(town));
    }

    /**
     * Gets a human-readable description of requirements.
     *
     * @param lang The language type
     * @return List of requirement descriptions
     */
    public List<Component> getRequirementsDescriptions(LangType lang) {
        return requirements.stream()
                .map(req -> req.getDescription(lang))
                .toList();
    }

    /**
     * Gets a human-readable description of rewards.
     *
     * @param lang The language type
     * @return List of reward descriptions
     */
    public List<Component> getRewardsDescriptions(LangType lang) {
        return rewards.stream()
                .map(reward -> reward.getDescription(lang))
                .toList();
    }

    /**
     * Creates the icon ItemStack for GUI display.
     *
     * @return ItemStack with display name and lore
     */
    public org.bukkit.inventory.ItemStack createIcon(LangType lang) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(icon);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Display name
            meta.displayName(Component.text(name, net.kyori.adventure.text.format.NamedTextColor.GOLD));

            // Lore
            List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text(description, net.kyori.adventure.text.format.NamedTextColor.GRAY));
            lore.add(Component.empty()); // Empty line
            lore.add(Component.text("Cost: " + cost + " Prestige",
                    net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            lore.add(Component.empty()); // Empty line
            lore.add(Component.text("Requirements:", net.kyori.adventure.text.format.NamedTextColor.RED));
            getRequirementsDescriptions(lang).forEach(desc ->
                    lore.add(Component.text("• ", net.kyori.adventure.text.format.NamedTextColor.RED).append(desc))
            );
            lore.add(Component.empty()); // Empty line
            lore.add(Component.text("Rewards:", net.kyori.adventure.text.format.NamedTextColor.GREEN));
            getRewardsDescriptions(lang).forEach(desc ->
                    lore.add(Component.text("• ", net.kyori.adventure.text.format.NamedTextColor.GREEN).append(desc))
            );

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates a TownUpgrade with default values.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing TownUpgrade instances.
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private int cost;
        private final List<UpgradeRequirement> requirements = new java.util.ArrayList<>();
        private final List<UpgradeReward> rewards = new java.util.ArrayList<>();
        private UpgradeCategory category = UpgradeCategory.GENERAL;
        private Material icon = Material.DIAMOND;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder cost(int cost) {
            this.cost = cost;
            return this;
        }

        public Builder addRequirement(UpgradeRequirement requirement) {
            this.requirements.add(requirement);
            return this;
        }

        public Builder addReward(UpgradeReward reward) {
            this.rewards.add(reward);
            return this;
        }

        public Builder category(UpgradeCategory category) {
            this.category = category;
            return this;
        }

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Builds the TownUpgrade instance.
         *
         * @throws IllegalStateException if required fields are missing
         */
        public TownUpgrade build() {
            if (id == null || name == null || description == null) {
                throw new IllegalStateException("ID, name, and description are required");
            }
            if (requirements.isEmpty()) {
                throw new IllegalStateException("At least one requirement is required");
            }
            if (rewards.isEmpty()) {
                throw new IllegalStateException("At least one reward is required");
            }

            return new TownUpgrade(
                    id,
                    name,
                    description,
                    cost,
                    List.copyOf(requirements),
                    List.copyOf(rewards),
                    category,
                    icon
            );
        }
    }
}
