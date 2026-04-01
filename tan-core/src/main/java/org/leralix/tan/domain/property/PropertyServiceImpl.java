package org.leralix.tan.domain.property;

import org.bukkit.Location;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Implementation of PropertyService.
 * Extracts property-related logic from TownData following the Strangler Fig Pattern.
 *
 * <p>This service is enabled via the feature flag {@code development.use-new-property-service}.</p>
 *
 * @see PropertyService
 * @since 2.0.0
 */
public class PropertyServiceImpl implements PropertyService {

    private static final Logger LOGGER = Logger.getLogger(PropertyServiceImpl.class.getName());

    private final TownDataStorage townStorage;

    /**
     * Creates a new PropertyServiceImpl.
     *
     * @param townStorage The town data storage
     */
    public PropertyServiceImpl(TownDataStorage townStorage) {
        this.townStorage = townStorage;
    }

    @Override
    public CompletableFuture<Collection<PropertyData>> getProperties(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("PropertyService: Town not found: " + townId);
                    return Collections.emptyList();
                }
                return town.getProperties();
            });
    }

    @Override
    public CompletableFuture<Map<String, PropertyData>> getPropertyDataMap(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("PropertyService: Town not found: " + townId);
                    return Map.of();
                }
                return town.getPropertyDataMap();
            });
    }

    @Override
    public CompletableFuture<PropertyData> getProperty(String townId, String propertyId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("PropertyService: Town not found: " + townId);
                    return null;
                }
                return town.getProperty(propertyId);
            });
    }

    @Override
    public CompletableFuture<PropertyData> getPropertyAtLocation(String townId, Location location) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warning("PropertyService: Town not found: " + townId);
                    return null;
                }
                return town.getProperty(location);
            });
    }

    @Override
    public CompletableFuture<Void> removeProperty(String townId, PropertyData propertyData) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warning("PropertyService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.removeProperty(propertyData);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public boolean isEnabled() {
        TownsAndNations plugin = TownsAndNations.getPlugin();
        if (plugin == null) {
            return false; // Plugin not initialized (e.g., during tests)
        }
        return plugin.getConfig()
            .getBoolean("development.use-new-property-service", false);
    }
}
