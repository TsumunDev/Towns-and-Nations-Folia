package org.leralix.tan.api.external.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.leralix.tan.enums.permissions.ChunkPermissionType;

/**
 * Abstraction over WorldGuard integration.
 * Implementations are loaded via {@link WorldGuardServiceFactory} to avoid
 * hard class-loading when WorldGuard is not present on the server.
 */
public interface WorldGuardService {

    /**
     * Check whether the given action is allowed for the player at the location,
     * according to WorldGuard region flags and ownership.
     */
    boolean isActionAllowed(Player player, Location location, ChunkPermissionType actionType);

    /**
     * Check whether the location falls inside at least one WorldGuard region.
     */
    boolean isHandledByWorldGuard(Location location);

    /**
     * @return true if a real WorldGuard-backed implementation is active.
     */
    boolean isEnabled();
}
