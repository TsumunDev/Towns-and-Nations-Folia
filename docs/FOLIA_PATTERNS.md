# Folia Compliance Patterns

**Version:** 1.0
**Last Updated:** 2026-03-26
**Target:** Folia 1.21+

---

## Overview

This document outlines the Folia-safe patterns used in Towns-and-Nations. Folia is a regionalized multithreading version of Paper that **does not have a global main thread**. All scheduled tasks must be region-specific, entity-specific, or asynchronous.

---

## Critical Rules

### 1. NEVER Use `Bukkit.getScheduler()`

```java
// ❌ FORBIDDEN - Will crash on Folia
Bukkit.getScheduler().runTask(plugin, runnable);
Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
Bukkit.getScheduler().cancelTask(taskId);
```

### 2. Use the Correct Scheduler for Each Context

| Context | API | Usage |
|---------|-----|-------|
| Region-specific | `Bukkit.getRegionScheduler()` | Operations at a specific location |
| Entity-specific | `entity.getScheduler()` | Operations tied to an entity |
| Global async | `Bukkit.getAsyncScheduler()` | Off-thread operations (DB, network) |
| Global region | `Bukkit.getGlobalRegionScheduler()` | Server-wide region operations |

---

## Folia-Safe Patterns

### Pattern 1: Region-Specific Scheduling

```java
// ✅ CORRECT - Run on the region thread for a location
Bukkit.getRegionScheduler().execute(plugin, location, () -> {
    // Safe to access Bukkit APIs here
    player.sendMessage("Region operation");
});

// ✅ CORRECT - Schedule delayed task on region
Bukkit.getRegionScheduler().runDelayed(plugin, location, (task) -> {
    // Region-specific delayed operation
}, delayTicks);

// ✅ CORRECT - Schedule repeating task on region
Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, (task) -> {
    // Repeating region operation
}, delayTicks, periodTicks);
```

### Pattern 2: Entity-Specific Scheduling

```java
// ✅ CORRECT - Run on entity's region thread
entity.getScheduler().run(plugin, (task) -> {
    // Safe to access this entity and related data
    player.sendMessage("Entity operation");
}, null); // retired = null

// ✅ CORRECT - Delayed entity task
entity.getScheduler().runDelayed(plugin, (task) -> {
    // Delayed entity-specific operation
}, null, delayTicks);
```

### Pattern 3: Async Scheduling

```java
// ✅ CORRECT - Async operation (uses milliseconds, not ticks)
Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
    // DB query, HTTP call, etc.
    // DO NOT access Bukkit APIs here
});

// ✅ CORRECT - Delayed async (milliseconds)
Bukkit.getAsyncScheduler().runDelayed(plugin, (task) -> {
    // Delayed async operation
}, delayMs, TimeUnit.MILLISECONDS);

// ✅ CORRECT - Repeating async (milliseconds)
Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
    // Repeating async operation (e.g., heartbeat)
}, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
```

### Pattern 4: Using FoliaScheduler Utility

The plugin provides a wrapper utility for common operations:

```java
// Region-based
FoliaScheduler.runTaskAtLocation(plugin, location, () -> {
    // Region-thread operation
});

FoliaScheduler.runTaskLaterAtLocation(plugin, location, () -> {
    // Delayed region-thread operation
}, delay);

// Entity-based
FoliaScheduler.runEntityTask(plugin, entity, () -> {
    // Entity-thread operation
});

FoliaScheduler.runEntityTaskLater(plugin, entity, () -> {
    // Delayed entity-thread operation
}, delay);

// Async
FoliaScheduler.runTaskAsynchronously(plugin, () -> {
    // Async operation
});

FoliaScheduler.runTaskLaterAsynchronously(plugin, () -> {
    // Delayed async operation
}, delay);
```

### Pattern 5: Async → Region Chain with FoliaAsyncHelper

For database queries that need to update Bukkit state:

```java
FoliaAsyncHelper.supplyAsync(plugin, () -> {
    // Async: Load from database
    return PlayerDataStorage.load(playerId);
})
.thenRunOnRegion(plugin, player.getLocation(), playerData -> {
    // Back on region thread: Use data safely
    player.setBalance(playerData.getBalance());
    player.sendMessage("Balance loaded");
});
```

### Pattern 6: Kotlin Coroutines with FoliaDispatchers

```kotlin
// Global async (IO operations)
scope.launch(FoliaDispatchers.global(plugin)) {
    // Async work
}

// Region-specific
scope.launch(FoliaDispatchers.forLocation(plugin, location)) {
    // Region-thread work
}

// Entity-specific
scope.launch(FoliaDispatchers.forEntity(plugin, entity)) {
    // Entity-thread work
}

// Extension functions
plugin.launchOnFolia(plugin, location) {
    // Region-thread coroutine
}

plugin.withFoliaContext(plugin, entity) {
    // Entity-thread coroutine context
}
```

---

## Common Mistakes

### Mistake 1: Assuming Global Thread Exists

```java
// ❌ WRONG - No global main thread on Folia
Bukkit.getScheduler().runTask(plugin, () -> {
    broadcastMessage("Hello everyone");
});

// ✅ CORRECT - Use GlobalRegionScheduler
Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
    Bukkit.broadcast(Component.text("Hello everyone"));
});
```

### Mistake 2: Using Ticks for AsyncScheduler

```java
// ❌ WRONG - AsyncScheduler uses milliseconds
Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {}, 20L, TimeUnit.MILLISECONDS);

// ✅ CORRECT - Convert ticks to milliseconds
long delayMs = 20L * 50; // 20 ticks = 1000ms
Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {}, delayMs, TimeUnit.MILLISECONDS);
```

### Mistake 3: Accessing Bukkit APIs in Async Context

```java
// ❌ WRONG - Bukkit APIs not thread-safe in async
Bukkit.getAsyncScheduler().runNow(plugin, task -> {
    player.sendMessage("Async message"); // CRASH
});

// ✅ CORRECT - Chain to region thread
Bukkit.getAsyncScheduler().runNow(plugin, task -> {
    String message = loadMessageFromDb();
    Bukkit.getRegionScheduler().execute(plugin, player.getLocation(), () -> {
        player.sendMessage(message);
    });
});
```

---

## Task Cancellation

```java
// ❌ WRONG - Bukkit.getScheduler() doesn't exist
Bukkit.getScheduler().cancelTask(taskId);

// ✅ CORRECT - Each ScheduledTask has cancel()
ScheduledTask task = Bukkit.getAsyncScheduler()
    .runAtFixedRate(plugin, callback, ...);
task.cancel(); // Cancel the task
```

---

## Testing Notes

- **MockBukkit** does not fully support Folia schedulers
- Integration tests should be run on a real Folia test server
- Unit tests should verify signature correctness at compile time
- Tests requiring region threading should be marked `@Disabled`

---

## References

- [Folia Documentation](https://docs.papermc.io/folia/)
- [Folia API Javadoc](https://jd.papermc.io/folia/)
- [Regionalized Threading Guide](https://docs.papermc.io/folia/developers/regionalized-tasks)
