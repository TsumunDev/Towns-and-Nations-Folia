package org.leralix.tan.api.external.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.leralix.tan.enums.permissions.ChunkPermissionType;

/**
 * Central manager for WorldGuard integration.
 * <p>
 * Uses {@link WorldGuardServiceFactory} to obtain a safe implementation that
 * never triggers {@code NoClassDefFoundError} when WorldGuard is absent.
 * All external consumers should go through this singleton rather than
 * referencing any implementation class directly.
 */
public class WorldGuardManager {

    private WorldGuardService service;
    private static WorldGuardManager instance;

    private WorldGuardManager() {}

    public static WorldGuardManager getInstance() {
        if (instance == null) {
            instance = new WorldGuardManager();
        }
        return instance;
    }

    /**
     * Attempt to load the WorldGuard integration.
     * If WorldGuard is not present or loading fails, a no-op fallback is used
     * and {@link #isEnabled()} will return {@code false}.
     */
    public void register() {
        service = WorldGuardServiceFactory.create();
    }

    public boolean isEnabled() {
        return service != null && service.isEnabled();
    }

    public boolean isHandledByWorldGuard(Location location) {
        if (!isEnabled()) return false;
        return service.isHandledByWorldGuard(location);
    }

    public boolean isActionAllowed(Player player, Location location, ChunkPermissionType actionType) {
        if (!isEnabled()) return false;
        return service.isActionAllowed(player, location, actionType);
    }
}
