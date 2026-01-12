package org.leralix.tan.dataclass.territory.progression;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;

/**
 * Represents the civilization tier of a town.
 * <p>
 * Tiers represent the developmental stage of a town, from a small camping
 * to a powerful metropolis. Each tier unlocks new features and increases caps.
 * </p>
 *
 * <h2>Tier Progression:</h2>
 * <ol>
 *   <li>{@link #CAMPING} - Initial stage, minimal features</li>
 *   <li>{@link #HAMLET} - Small community, basic unlockables</li>
 *   <li>{@link #VILLAGE} - Growing settlement, advanced features</li>
 *   <li>{@link #CITY} - Major urban center, significant bonuses</li>
 *   <li>{@link #METROPOLIS} - Peak development, maximum capabilities</li>
 * </ol>
 *
 * @since 1.0
 */
public enum TownTier {
    /**
     * Stage 1: Camping - The beginning of civilization.
     * <p>
     * Features: Basic town creation, minimal claims.
     * Icon: Campfire (represents the starting point).
     * </p>
     */
    CAMPING(1, "Camping", Material.CAMPFIRE, NamedTextColor.GRAY),

    /**
     * Stage 2: Hamlet - A small emerging community.
     * <p>
     * Features: Expanded claims, basic economy unlocks.
     * Icon: Oak Planks (represents basic construction).
     * </p>
     */
    HAMLET(2, "Hamlet", Material.OAK_PLANKS, NamedTextColor.GREEN),

    /**
     * Stage 3: Village - A growing settlement.
     * <p>
     * Features: Advanced economy, diplomatic options, special buildings.
     * Icon: Iron Block (represents established infrastructure).
     * </p>
     */
    VILLAGE(3, "Village", Material.IRON_BLOCK, NamedTextColor.GOLD),

    /**
     * Stage 4: City - A major urban center.
     * <p>
     * Features: Extensive capabilities, trade dominance, military strength.
     * Icon: Gold Block (represents wealth and power).
     * </p>
     */
    CITY(4, "City", Material.GOLD_BLOCK, NamedTextColor.AQUA),

    /**
     * Stage 5: Metropolis - The peak of civilization.
     * <p>
     * Features: Maximum potential, regional influence, legendary status.
     * Icon: Diamond Block (represents ultimate achievement).
     * </p>
     */
    METROPOLIS(5, "Metropolis", Material.DIAMOND_BLOCK, NamedTextColor.LIGHT_PURPLE);

    private final int level;
    private final String name;
    private final Material iconMaterial;
    private final NamedTextColor color;

    TownTier(int level, String name, Material iconMaterial, NamedTextColor color) {
        this.level = level;
        this.name = name;
        this.iconMaterial = iconMaterial;
        this.color = color;
    }

    /**
     * Gets the numeric level of this tier.
     *
     * @return the tier level (1-5)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the display name of this tier.
     *
     * @return the tier name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the icon material for this tier.
     *
     * @return the material representing this tier
     */
    public Material getIconMaterial() {
        return iconMaterial;
    }

    /**
     * Gets the color associated with this tier.
     *
     * @return the named text color
     */
    public NamedTextColor getColor() {
        return color;
    }

    /**
     * Creates an ItemStack representing this tier.
     *
     * @return an icon ItemStack
     */
    public ItemStack getIcon() {
        return new ItemStack(iconMaterial);
    }

    /**
     * Gets the colored display name as a Component.
     *
     * @return a colored component
     */
    public Component getColoredName() {
        return Component.text(name, color);
    }

    /**
     * Gets the next tier in the progression.
     *
     * @return the next tier, or null if this is the maximum tier
     */
    public TownTier next() {
        int nextLevel = level + 1;
        for (TownTier tier : values()) {
            if (tier.level == nextLevel) {
                return tier;
            }
        }
        return null;
    }

    /**
     * Gets the previous tier in the progression.
     *
     * @return the previous tier, or null if this is the first tier
     */
    public TownTier previous() {
        int prevLevel = level - 1;
        for (TownTier tier : values()) {
            if (tier.level == prevLevel) {
                return tier;
            }
        }
        return null;
    }

    /**
     * Checks if this is the first (lowest) tier.
     *
     * @return true if this is CAMPING
     */
    public boolean isFirst() {
        return this == CAMPING;
    }

    /**
     * Checks if this is the last (highest) tier.
     *
     * @return true if this is METROPOLIS
     */
    public boolean isLast() {
        return this == METROPOLIS;
    }

    /**
     * Gets a tier by its numeric level.
     *
     * @param level the tier level (1-5)
     * @return the corresponding tier, or CAMPING if invalid
     */
    public static TownTier fromLevel(int level) {
        for (TownTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return CAMPING;
    }

    /**
     * Gets the localized name for this tier.
     *
     * @param langType the language type
     * @return the localized tier name
     */
    public String getLocalizedName(LangType langType) {
        // TODO: Add to Lang.java when implementing localization
        // For now, return the English name
        return name;
    }
}
