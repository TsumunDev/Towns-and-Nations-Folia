package org.leralix.tan.api.external.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.leralix.tan.enums.permissions.ChunkPermissionType;

/**
 * No-operation fallback used when WorldGuard is not installed or failed to load.
 * Every method returns a safe default that effectively disables WorldGuard checks.
 */
public class NoOpWorldGuardService implements WorldGuardService {

    @Override
    public boolean isActionAllowed(Player player, Location location, ChunkPermissionType actionType) {
        return false;
    }

    @Override
    public boolean isHandledByWorldGuard(Location location) {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
