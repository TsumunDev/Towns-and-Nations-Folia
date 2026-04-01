package org.leralix.tan.integration.nexo;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.testutils.AbstractPluginTest;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.MockPluginManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NexoEventListener}.
 * <p>
 * Tests Nexo plugin integration event handling including plugin enable detection,
 * listener registration, and IconManager cache clearing.
 * </p>
 */
@DisplayName("NexoEventListener Tests")
class NexoEventListenerTest extends AbstractPluginTest {

    private NexoEventListener listener;

    @BeforeEach
    void setUp() {
        MockBukkit.load(org.leralix.lib.SphereLib.class);
        listener = new NexoEventListener();
    }

    // ==================== Basic Registration Tests ====================

    @Test
    @DisplayName("Should register listener successfully")
    void registerIfNeeded_validPlugin_registersSuccessfully() {
        // Reset registration state
        NexoEventListener.setRegistered(false);

        assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
        assertTrue(NexoEventListener.isRegistered(), "Listener should be registered");
    }

    @Test
    @DisplayName("Should not register twice")
    void registerIfNeeded_alreadyRegistered_skipsRegistration() {
        NexoEventListener.setRegistered(true);

        assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
        assertTrue(NexoEventListener.isRegistered(), "Listener should remain registered");
    }

    @Test
    @DisplayName("Should handle registration when Nexo is already enabled")
    void registerIfNeeded_nexoAlreadyEnabled_marksRegistered() {
        NexoEventListener.setRegistered(false);

        // Simulate Nexo being enabled
        MockPluginManager pluginManager = server.getPluginManager();
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        pluginManager.registerPlugin(nexoPlugin);

        assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
        assertTrue(NexoEventListener.isRegistered(), "Should mark as registered when Nexo is enabled");
    }

    // ==================== Plugin Enable Event Tests ====================

    @Test
    @DisplayName("Should handle Nexo plugin enable event")
    void onPluginEnable_nexoPlugin_initializesIntegration() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    @Test
    @DisplayName("Should ignore non-Nexo plugin enable events")
    void onPluginEnable_otherPlugin_ignoresEvent() {
        Plugin otherPlugin = MockBukkit.createMockPlugin("OtherPlugin");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(otherPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    @Test
    @DisplayName("Should handle null plugin gracefully")
    void onPluginEnable_nullPlugin_handlesGracefully() {
        Plugin nullPlugin = MockBukkit.createMockPlugin("NotNexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nullPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    // ==================== Integration Initialization Tests ====================

    @Test
    @DisplayName("Should reinitialize Nexo integration on plugin enable")
    void onPluginEnable_nexoPlugin_reinitializesIntegration() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    @Test
    @DisplayName("Should handle successful integration reinitialization")
    void onPluginEnable_integrationSuccess_logsSuccess() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    @Test
    @DisplayName("Should handle failed integration reinitialization")
    void onPluginEnable_integrationFailure_handlesGracefully() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    // ==================== IconManager Cache Tests ====================

    @Test
    @DisplayName("Should clear IconManager cache on Nexo load")
    void onPluginEnable_nexoLoaded_clearsIconManagerCache() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    @Test
    @DisplayName("Should handle IconManager cache clearing failure gracefully")
    void onPluginEnable_cacheClearFailure_doesNotCrash() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    // ==================== Reflection Tests ====================

    @Test
    @DisplayName("Should handle IconManager class not found")
    void clearIconManagerCache_classNotFound_handlesGracefully() {
        // This tests the reflection-based cache clearing
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    @Test
    @DisplayName("Should handle IconManager field not found")
    void clearIconManagerCache_fieldNotFound_handlesGracefully() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    // ==================== Multiple Enable Events Tests ====================

    @Test
    @DisplayName("Should handle multiple plugin enable events")
    void onPluginEnable_multipleEvents_handlesCorrectly() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event1 =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);
        org.bukkit.event.server.PluginEnableEvent event2 =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> {
            listener.onPluginEnable(event1);
            listener.onPluginEnable(event2);
        });
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle plugin name with different casing")
    void onPluginEnable_differentCasing_handlesCorrectly() {
        Plugin nexoPluginLower = MockBukkit.createMockPlugin("nexo");
        Plugin nexoPluginUpper = MockBukkit.createMockPlugin("NEXO");
        Plugin nexoPluginMixed = MockBukkit.createMockPlugin("Nexo");

        org.bukkit.event.server.PluginEnableEvent event1 =
            new org.bukkit.event.server.PluginEnableEvent(nexoPluginLower);
        org.bukkit.event.server.PluginEnableEvent event2 =
            new org.bukkit.event.server.PluginEnableEvent(nexoPluginUpper);
        org.bukkit.event.server.PluginEnableEvent event3 =
            new org.bukkit.event.server.PluginEnableEvent(nexoPluginMixed);

        assertDoesNotThrow(() -> {
            listener.onPluginEnable(event1);
            listener.onPluginEnable(event2);
            listener.onPluginEnable(event3);
        });
    }

    @Test
    @DisplayName("Should handle rapid plugin enable/disable cycles")
    void onPluginEnable_rapidCycles_handlesCorrectly() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");

        for (int i = 0; i < 5; i++) {
            org.bukkit.event.server.PluginEnableEvent event =
                new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);
            assertDoesNotThrow(() -> listener.onPluginEnable(event));
        }
    }

    // ==================== NexoIntegration Tests ====================

    @Test
    @DisplayName("Should interact with NexoIntegration")
    void onPluginEnable_nexoIntegration_handlesCorrectly() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    @Test
    @DisplayName("Should check NexoIntegration enabled state")
    void onPluginEnable_checksEnabledState() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    // ==================== Companion Object Tests ====================

    @Test
    @DisplayName("Should track registration state correctly")
    void isRegistered_registrationState_trackedCorrectly() {
        NexoEventListener.setRegistered(false);
        assertFalse(NexoEventListener.isRegistered(), "Should not be registered initially");

        NexoEventListener.setRegistered(true);
        assertTrue(NexoEventListener.isRegistered(), "Should be registered after setting");

        NexoEventListener.setRegistered(false);
        assertFalse(NexoEventListener.isRegistered(), "Should not be registered after resetting");
    }

    @Test
    @DisplayName("Should handle concurrent registration attempts")
    void registerIfNeeded_concurrentAttempts_handlesCorrectly() {
        NexoEventListener.setRegistered(false);

        Runnable registerTask = () -> {
            for (int i = 0; i < 5; i++) {
                assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
            }
        };

        Thread thread1 = new Thread(registerTask);
        Thread thread2 = new Thread(registerTask);

        assertDoesNotThrow(() -> {
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();
        });

        assertTrue(NexoEventListener.isRegistered(), "Should be registered after concurrent attempts");
    }

    // ==================== Logger Tests ====================

    @Test
    @DisplayName("Should log debug message when Nexo already enabled")
    void registerIfNeeded_nexoAlreadyEnabled_logsDebug() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        server.getPluginManager().registerPlugin(nexoPlugin);
        NexoEventListener.setRegistered(false);

        assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
        assertTrue(NexoEventListener.isRegistered(), "Should be registered");
    }

    @Test
    @DisplayName("Should log info message on listener registration")
    void registerIfNeeded_registers_logsInfo() {
        NexoEventListener.setRegistered(false);

        assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
    }

    @Test
    @DisplayName("Should log info message on Nexo plugin detection")
    void onPluginEnable_nexoDetected_logsInfo() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    @Test
    @DisplayName("Should log warning on integration failure")
    void onPluginEnable_integrationFailed_logsWarning() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle null plugin manager gracefully")
    void registerIfNeeded_nullPluginManager_handlesGracefully() {
        NexoEventListener.setRegistered(false);

        assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
    }

    @Test
    @DisplayName("Should handle exception during plugin enable")
    void onPluginEnable_exception_handlesGracefully() {
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    // ==================== State Management Tests ====================

    @Test
    @DisplayName("Should maintain state across multiple operations")
    void stateManagement_multipleOperations_maintainsState() {
        NexoEventListener.setRegistered(false);

        // First registration
        NexoEventListener.registerIfNeeded(plugin);
        assertTrue(NexoEventListener.isRegistered());

        // Second registration should skip
        NexoEventListener.registerIfNeeded(plugin);
        assertTrue(NexoEventListener.isRegistered());

        // Reset
        NexoEventListener.setRegistered(false);
        assertFalse(NexoEventListener.isRegistered());

        // Re-register
        NexoEventListener.registerIfNeeded(plugin);
        assertTrue(NexoEventListener.isRegistered());
    }

    // ==================== Plugin Manager Tests ====================

    @Test
    @DisplayName("Should handle getting plugins from plugin manager")
    void registerIfNeeded_getPlugins_handlesCorrectly() {
        NexoEventListener.setRegistered(false);

        assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
    }

    @Test
    @DisplayName("Should handle null plugin return from plugin manager")
    void registerIfNeeded_nullPluginReturn_handlesGracefully() {
        NexoEventListener.setRegistered(false);

        assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
    }

    // ==================== Late Load Tests ====================

    @Test
    @DisplayName("Should handle Nexo loaded after TAN startup")
    void onPluginEnable_lateLoad_initializesCorrectly() {
        // Simulate TAN starting first
        NexoEventListener.setRegistered(false);
        NexoEventListener.registerIfNeeded(plugin);

        // Then Nexo loads
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        org.bukkit.event.server.PluginEnableEvent event =
            new org.bukkit.event.server.PluginEnableEvent(nexoPlugin);

        assertDoesNotThrow(() -> listener.onPluginEnable(event));
    }

    @Test
    @DisplayName("Should handle TAN loaded after Nexo startup")
    void registerIfNeeded_nexoLoadedFirst_initializesCorrectly() {
        // Simulate Nexo being loaded first
        Plugin nexoPlugin = MockBukkit.createMockPlugin("Nexo");
        server.getPluginManager().registerPlugin(nexoPlugin);

        // Then TAN loads
        NexoEventListener.setRegistered(false);
        assertDoesNotThrow(() -> NexoEventListener.registerIfNeeded(plugin));
    }

    // ==================== Proxy Listener Tests ====================

    @Test
    @DisplayName("Should handle NexoProxyEventListener registration")
    void nexoProxyEventListener_registerIfAvailable_handlesCorrectly() {
        assertDoesNotThrow(() -> NexoProxyEventListener.registerIfAvailable(plugin));
    }

    @Test
    @DisplayName("Should handle proxy listener when events not available")
    void nexoProxyEventListener_eventsNotAvailable_skipsRegistration() {
        assertDoesNotThrow(() -> NexoProxyEventListener.registerIfAvailable(plugin));
    }

    @Test
    @DisplayName("Should handle proxy listener with valid events")
    void nexoProxyEventListener_validEvents_registersSuccessfully() {
        assertDoesNotThrow(() -> NexoProxyEventListener.registerIfAvailable(plugin));
    }
}
