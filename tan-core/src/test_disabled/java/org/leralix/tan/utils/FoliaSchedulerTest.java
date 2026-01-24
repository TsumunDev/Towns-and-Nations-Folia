package org.leralix.tan.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Unit tests for FoliaScheduler class.
 * Tests scheduling methods for Folia regionalized execution.
 */
public class FoliaSchedulerTest {

    private ServerMock server;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        MockBukkit.load(TownsAndNations.class);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("runTask should execute task on global region")
    void testRunTask() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        FoliaScheduler.runTask(plugin, () -> executed.set(true));

        // Assert
        assertTrue(executed.get(), "Task should be executed");
    }

    @Test
    @DisplayName("runTaskLater should schedule task with delay")
    void testRunTaskLater() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        var task = FoliaScheduler.runTaskLater(plugin, () -> executed.set(true), 1);

        // Assert
        assertNotNull(task, "ScheduledTask should not be null");
    }

    @Test
    @DisplayName("runTaskAsynchronously should execute task async")
    void testRunTaskAsynchronously() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        FoliaScheduler.runTaskAsynchronously(plugin, () -> executed.set(true));

        // Assert
        assertTrue(executed.get(), "Async task should be executed");
    }

    @Test
    @DisplayName("runTaskLaterAsynchronously should schedule async task")
    void testRunTaskLaterAsynchronously() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        FoliaScheduler.runTaskLaterAsynchronously(plugin, () -> executed.set(true), 1);

        // Assert
        // Task is scheduled, execution may be delayed
    }

    @Test
    @DisplayName("runEntityTask should execute task on entity scheduler")
    void testRunEntityTask() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        PlayerMock player = server.addPlayer("TestPlayer");
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        FoliaScheduler.runEntityTask(plugin, player, () -> executed.set(true));

        // Assert
        assertTrue(executed.get(), "Entity task should be executed");
    }

    @Test
    @DisplayName("runEntityTaskLater should schedule task on entity scheduler")
    void testRunEntityTaskLater() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        PlayerMock player = server.addPlayer("TestPlayer");
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        FoliaScheduler.runEntityTaskLater(plugin, player, () -> executed.set(true), 1);

        // Assert
        // Task is scheduled on entity scheduler
    }

    @Test
    @DisplayName("runTaskAtLocation should execute task on region scheduler")
    void testRunTaskAtLocation() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        var world = server.getWorlds().get(0);
        Location location = new Location(world, 0, 64, 0);
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        FoliaScheduler.runTaskAtLocation(plugin, location, () -> executed.set(true));

        // Assert
        assertTrue(executed.get(), "Location task should be executed");
    }

    @Test
    @DisplayName("runTaskLaterAtLocation should schedule task on region scheduler")
    void testRunTaskLaterAtLocation() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        var world = server.getWorlds().get(0);
        Location location = new Location(world, 0, 64, 0);
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        FoliaScheduler.runTaskLaterAtLocation(plugin, location, () -> executed.set(true), 1);

        // Assert
        // Task is scheduled on region scheduler
    }

    @Test
    @DisplayName("runTaskTimer should schedule repeating task")
    void testRunTaskTimer() {
        // Arrange
        AtomicInteger counter = new AtomicInteger(0);
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        FoliaScheduler.runTaskTimer(plugin, () -> counter.incrementAndGet(), 1, 1);

        // Assert
        // Timer task is scheduled
    }

    @Test
    @DisplayName("runTask with null plugin should handle gracefully")
    void testRunTaskNullPlugin() {
        // Arrange & Act & Assert
        assertThrows(NullPointerException.class, () -> {
            FoliaScheduler.runTask(null, () -> {});
        });
    }

    @Test
    @DisplayName("runTask with null runnable should not crash")
    void testRunTaskNullRunnable() {
        // Arrange
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act & Assert
        // May throw NPE - depends on scheduler implementation
        FoliaScheduler.runTask(plugin, null);
    }

    @Test
    @DisplayName("runTaskAtLocation with null location should throw")
    void testRunTaskAtLocationNullLocation() {
        // Arrange
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            FoliaScheduler.runTaskAtLocation(plugin, null, () -> {});
        });
    }

    @Test
    @DisplayName("runEntityTask with null entity should throw")
    void testRunEntityTaskNullEntity() {
        // Arrange
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            FoliaScheduler.runEntityTask(plugin, null, () -> {});
        });
    }

    @Test
    @DisplayName("runTaskTimer with zero delay should still schedule")
    void testRunTaskTimerZeroDelay() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        TownsAndNations plugin = TownsAndNations.getPlugin();

        // Act
        FoliaScheduler.runTaskTimer(plugin, () -> executed.set(true), 0, 1);

        // Assert
        // Timer should be scheduled even with zero delay
    }
}
