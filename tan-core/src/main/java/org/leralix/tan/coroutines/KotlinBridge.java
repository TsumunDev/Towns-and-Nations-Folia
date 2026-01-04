package org.leralix.tan.coroutines;

/**
 * Java bridge for Kotlin coroutines initialization.
 * Call this from TownsAndNations.onEnable() and onDisable().
 * 
 * Usage in TownsAndNations.java:
 * <pre>
 * // In onEnable():
 * KotlinBridge.initializeCoroutines();
 * 
 * // In onDisable():
 * KotlinBridge.shutdownCoroutines();
 * </pre>
 */
public final class KotlinBridge {
    
    private KotlinBridge() {
        // Utility class
    }
    
    /**
     * Initialize Kotlin coroutines scope.
     * Call this early in plugin onEnable().
     */
    public static void initializeCoroutines() {
        TanCoroutines.initialize();
    }
    
    /**
     * Shutdown Kotlin coroutines gracefully.
     * Call this in plugin onDisable().
     */
    public static void shutdownCoroutines() {
        TanCoroutines.shutdown();
    }
}
