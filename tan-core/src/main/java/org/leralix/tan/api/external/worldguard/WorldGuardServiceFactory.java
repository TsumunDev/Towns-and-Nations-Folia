package org.leralix.tan.api.external.worldguard;

import org.bukkit.Bukkit;

/**
 * Factory that safely loads a {@link WorldGuardService} implementation.
 * Uses reflection so that {@code WorldGuardServiceImpl} (which contains hard
 * WorldGuard/WorldEdit imports) is never class-loaded unless WorldGuard is
 * actually present on the server.
 */
public class WorldGuardServiceFactory {

    private WorldGuardServiceFactory() {}

    public static WorldGuardService create() {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            return new NoOpWorldGuardService();
        }
        try {
            return (WorldGuardService) Class.forName(
                    "org.leralix.tan.api.external.worldguard.WorldGuardServiceImpl")
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            Bukkit.getLogger().warning(
                    "[TaN] WorldGuard is present but the integration class could not be loaded: " + e.getMessage());
            return new NoOpWorldGuardService();
        }
    }
}
