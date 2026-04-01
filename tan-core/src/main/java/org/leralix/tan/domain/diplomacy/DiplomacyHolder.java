package org.leralix.tan.domain.diplomacy;

import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Service locator/holder for DiplomacyService.
 *
 * <p>This class provides lazy initialization of the DiplomacyService singleton.
 * It follows the Service Holder pattern for dependency injection without
 * requiring a full DI framework.</p>
 *
 * <p>The service is only instantiated if the feature flag is enabled.</p>
 *
 * @see DiplomacyService
 * @see DiplomacyServiceImpl
 * @since 2.0.0
 */
public final class DiplomacyHolder {

    private static DiplomacyService instance;

    private DiplomacyHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the DiplomacyService instance.
     *
     * <p>If the service is not yet initialized, it will be created with
     * the default storage instance.</p>
     *
     * @return The DiplomacyService instance
     */
    public static DiplomacyService getService() {
        if (instance == null) {
            synchronized (DiplomacyHolder.class) {
                if (instance == null) {
                    instance = new DiplomacyServiceImpl(
                        TownDataStorage.getInstance()
                    );
                }
            }
        }
        return instance;
    }

    /**
     * Sets a custom DiplomacyService instance.
     *
     * <p>This method is primarily used for testing to inject mock services.</p>
     *
     * @param service The service to set
     */
    public static void setService(DiplomacyService service) {
        synchronized (DiplomacyHolder.class) {
            instance = service;
        }
    }

    /**
     * Clears the service instance.
     *
     * <p>This method is primarily used for testing to reset state between tests.</p>
     */
    public static void clear() {
        synchronized (DiplomacyHolder.class) {
            instance = null;
        }
    }
}
