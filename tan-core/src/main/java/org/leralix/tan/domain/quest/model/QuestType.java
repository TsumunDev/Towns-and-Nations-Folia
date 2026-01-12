package org.leralix.tan.domain.quest.model;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Types of quests available in the game.
 * Each type corresponds to a specific Bukkit event.
 */
public enum QuestType {
    /**
     * Farming quests - breaking crops (wheat, carrots, potatoes, etc.)
     */
    FARM(BlockBreakEvent.class, "Farming"),

    /**
     * Mining quests - mining ores (coal, iron, gold, diamond, etc.)
     */
    MINING(BlockBreakEvent.class, "Mining"),

    /**
     * Combat quests - killing mobs/players
     */
    COMBAT(EntityDeathEvent.class, "Combat"),

    /**
     * Building quests - placing blocks
     */
    BUILD(BlockPlaceEvent.class, "Building"),

    /**
     * Activity quests - passive activity (online time, etc.)
     */
    ACTIVITY(null, "Activity");

    private final Class<? extends Event> bukkitEventClass;
    private final String displayName;

    QuestType(Class<? extends Event> bukkitEventClass, String displayName) {
        this.bukkitEventClass = bukkitEventClass;
        this.displayName = displayName;
    }

    /**
     * Gets the corresponding Bukkit event class for this quest type.
     */
    public Class<? extends Event> getBukkitEventClass() {
        return bukkitEventClass;
    }

    /**
     * Gets the display name for this quest type.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this quest type is triggered by a specific event.
     */
    public boolean isTriggeredBy(Event event) {
        return bukkitEventClass != null && bukkitEventClass.isInstance(event);
    }
}
