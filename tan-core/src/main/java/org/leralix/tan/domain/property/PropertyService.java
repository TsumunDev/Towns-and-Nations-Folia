package org.leralix.tan.domain.property;

import org.bukkit.Location;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.lib.position.Vector3D;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing properties within towns.
 *
 * <p>This service extracts property-related logic from TownData following the
 * Strangler Fig Pattern. It provides async operations for property management,
 * suitable for Folia's region-based threading.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Property registration (town-owned and player-owned)</li>
 *   <li>Property lookup by ID or location</li>
 *   <li>Property removal</li>
 *   <li>Automatic ID generation</li>
 * </ul>
 *
 * @see PropertyData
 * @see PropertyServiceImpl
 * @since 2.0.0
 */
public interface PropertyService {

    /**
     * Gets all properties for a town.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the property collection (empty if town not found)
     */
    CompletableFuture<Collection<PropertyData>> getProperties(String townId);

    /**
     * Gets the property data map for a town.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the property map (empty if town not found)
     */
    CompletableFuture<Map<String, PropertyData>> getPropertyDataMap(String townId);

    /**
     * Finds a property by its ID within a town.
     *
     * @param townId The town ID
     * @param propertyId The property ID (e.g., "P0", "P1")
     * @return CompletableFuture containing the property, or null if not found
     */
    CompletableFuture<PropertyData> getProperty(String townId, String propertyId);

    /**
     * Finds a property at a specific location within a town.
     *
     * @param townId The town ID
     * @param location The location to search
     * @return CompletableFuture containing the property, or null if not found
     */
    CompletableFuture<PropertyData> getPropertyAtLocation(String townId, Location location);

    /**
     * Removes a property from the town.
     *
     * @param townId The town ID
     * @param propertyData The property to remove
     * @return CompletableFuture that completes when removal is done
     */
    CompletableFuture<Void> removeProperty(String townId, PropertyData propertyData);

    /**
     * Checks if this service is enabled via feature flag.
     *
     * @return true if the new property service is enabled
     */
    boolean isEnabled();
}
