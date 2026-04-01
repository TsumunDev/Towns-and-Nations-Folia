package org.leralix.tan.domain.claim;

import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Service locator/holder for ClaimService.
 *
 * <p>This class provides lazy initialization of the ClaimService singleton.
 * It follows the Service Holder pattern for dependency injection without
 * requiring a full DI framework.</p>
 *
 * <p>The service is only instantiated if the feature flag is enabled.</p>
 *
 * @see ClaimService
 * @see ClaimServiceImpl
 * @since 2.0.0
 */
public final class ClaimHolder {

    private static ClaimService instance;

    private ClaimHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the ClaimService instance.
     *
     * <p>If the service is not yet initialized, it will be created with
     * the default storage instance.</p>
     *
     * @return The ClaimService instance
     */
    public static ClaimService getService() {
        if (instance == null) {
            synchronized (ClaimHolder.class) {
                if (instance == null) {
                    instance = new ClaimServiceImpl(
                        TownDataStorage.getInstance()
                    );
                }
            }
        }
        return instance;
    }

    /**
     * Sets a custom ClaimService instance.
     *
     * <p>This method is primarily used for testing to inject mock services.</p>
     *
     * @param service The service to set
     */
    public static void setService(ClaimService service) {
        synchronized (ClaimHolder.class) {
            instance = service;
        }
    }

    /**
     * Clears the service instance.
     *
     * <p>This method is primarily used for testing to reset state between tests.</p>
     */
    public static void clear() {
        synchronized (ClaimHolder.class) {
            instance = null;
        }
    }
}
