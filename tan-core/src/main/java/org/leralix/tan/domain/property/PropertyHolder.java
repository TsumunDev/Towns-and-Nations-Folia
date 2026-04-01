package org.leralix.tan.domain.property;

import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Service locator/holder for PropertyService.
 *
 * <p>This class provides lazy initialization of the PropertyService singleton.
 * It follows the Service Holder pattern for dependency injection without
 * requiring a full DI framework.</p>
 *
 * <p>The service is only instantiated if the feature flag is enabled.</p>
 *
 * @see PropertyService
 * @see PropertyServiceImpl
 * @since 2.0.0
 */
public final class PropertyHolder {

    private static PropertyService instance;

    private PropertyHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the PropertyService instance.
     *
     * <p>If the service is not yet initialized, it will be created with
     * the default storage instance.</p>
     *
     * @return The PropertyService instance
     */
    public static PropertyService getService() {
        if (instance == null) {
            synchronized (PropertyHolder.class) {
                if (instance == null) {
                    instance = new PropertyServiceImpl(
                        TownDataStorage.getInstance()
                    );
                }
            }
        }
        return instance;
    }

    /**
     * Sets a custom PropertyService instance.
     *
     * <p>This method is primarily used for testing to inject mock services.</p>
     *
     * @param service The service to set
     */
    public static void setService(PropertyService service) {
        synchronized (PropertyHolder.class) {
            instance = service;
        }
    }

    /**
     * Clears the service instance.
     *
     * <p>This method is primarily used for testing to reset state between tests.</p>
     */
    public static void clear() {
        synchronized (PropertyHolder.class) {
            instance = null;
        }
    }
}
