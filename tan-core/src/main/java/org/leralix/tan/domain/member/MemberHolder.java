package org.leralix.tan.domain.member;

import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Service holder for MemberService.
 * Provides lazy initialization and singleton access.
 *
 * <p>This holder follows the Service Holder Pattern to enable dependency injection
 * without a full DI framework. The service is initialized on first access.</p>
 *
 * @since 2.0.0
 */
public final class MemberHolder {

    private static MemberService instance;

    private MemberHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets or creates the MemberService instance.
     *
     * <p>Uses double-checked locking for thread-safe lazy initialization.</p>
     *
     * @return The MemberService instance
     */
    public static MemberService getService() {
        if (instance == null) {
            synchronized (MemberHolder.class) {
                if (instance == null) {
                    instance = new MemberServiceImpl(TownDataStorage.getInstance());
                }
            }
        }
        return instance;
    }

    /**
     * Sets the MemberService instance.
     * <p>Primarily used for testing to inject mock instances.</p>
     *
     * @param service The service instance to set
     */
    public static void setService(MemberService service) {
        synchronized (MemberHolder.class) {
            instance = service;
        }
    }

    /**
     * Clears the service instance.
     * <p>Primarily used for testing to reset state between tests.</p>
     */
    public static void clear() {
        synchronized (MemberHolder.class) {
            instance = null;
        }
    }
}
