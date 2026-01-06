package org.leralix.tan.coroutines;
public final class KotlinBridge {
    private KotlinBridge() {
    }
    public static void initializeCoroutines() {
        TanCoroutines.initialize();
    }
    public static void shutdownCoroutines() {
        TanCoroutines.shutdown();
    }
}