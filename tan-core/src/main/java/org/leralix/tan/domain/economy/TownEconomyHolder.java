package org.leralix.tan.domain.economy;

import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Service locator/holder for TownEconomyService.
 *
 * <p>This class provides lazy initialization of the TownEconomyService singleton.
 * It follows the Service Holder pattern for dependency injection without
 * requiring a full DI framework.</p>
 *
 * <p>The service is only instantiated if the feature flag is enabled.</p>
 *
 * @see TownEconomyService
 * @see TownEconomyServiceImpl
 * @since 2.0.0
 */
public final class TownEconomyHolder {

    private static TownEconomyService instance;

    private TownEconomyHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the TownEconomyService instance.
     *
     * <p>If the service is not yet initialized, it will be created with
     * the default storage instance.</p>
     *
     * @return The TownEconomyService instance
     */
    public static TownEconomyService getService() {
        if (instance == null) {
            synchronized (TownEconomyHolder.class) {
                if (instance == null) {
                    instance = new TownEconomyServiceImpl(
                        TownDataStorage.getInstance()
                    );
                }
            }
        }
        return instance;
    }

    /**
     * Sets a custom TownEconomyService instance.
     *
     * <p>This method is primarily used for testing to inject mock services.</p>
     *
     * @param service The service to set
     */
    public static void setService(TownEconomyService service) {
        synchronized (TownEconomyHolder.class) {
            instance = service;
        }
    }

    /**
     * Clears the service instance.
     *
     * <p>This method is primarily used for testing to reset state between tests.</p>
     */
    public static void clear() {
        synchronized (TownEconomyHolder.class) {
            instance = null;
        }
    }
}
