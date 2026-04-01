package org.leralix.tan.domain.gui;

import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Service locator/holder for TownGuiService.
 *
 * <p>This class provides lazy initialization of the TownGuiService singleton.
 * It follows the Service Holder pattern for dependency injection without
 * requiring a full DI framework.</p>
 *
 * <p>The service is only instantiated if the feature flag is enabled.</p>
 *
 * @see TownGuiService
 * @see TownGuiServiceImpl
 * @since 2.0.0
 */
public final class TownGuiHolder {

    private static TownGuiService instance;

    private TownGuiHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the TownGuiService instance.
     *
     * <p>If the service is not yet initialized, it will be created with
     * the default storage instances.</p>
     *
     * @return The TownGuiService instance
     */
    public static TownGuiService getService() {
        if (instance == null) {
            synchronized (TownGuiHolder.class) {
                if (instance == null) {
                    instance = new TownGuiServiceImpl(
                        TownDataStorage.getInstance(),
                        PlayerDataStorage.getInstance()
                    );
                }
            }
        }
        return instance;
    }

    /**
     * Sets a custom TownGuiService instance.
     *
     * <p>This method is primarily used for testing to inject mock services.</p>
     *
     * @param service The service to set
     */
    public static void setService(TownGuiService service) {
        synchronized (TownGuiHolder.class) {
            instance = service;
        }
    }

    /**
     * Clears the service instance.
     *
     * <p>This method is primarily used for testing to reset state between tests.</p>
     */
    public static void clear() {
        synchronized (TownGuiHolder.class) {
            instance = null;
        }
    }
}
