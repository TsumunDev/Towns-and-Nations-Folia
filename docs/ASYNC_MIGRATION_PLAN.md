# Async Migration Plan - getSync() Removal

**Story**: 1.3 - Phase 2: Async Migration
**Created**: 2025-01-11
**Status**: Audit Phase

---

## Critical Issue Summary

**Problem**: 100+ blocking I/O calls (`getSync()`, `getAllSync()`) violate Folia's regionalized threading model

**Impact**:
- Blocks region threads during database I/O
- Causes server lag under load
- Prevents proper Folia optimization
- Violates async-first architecture principles

---

## Usage Analysis

### Total Call Count: ~100 occurrences

#### By Category:

| Category | Count | Priority | Risk |
|----------|-------|----------|------|
| **Economy operations** | 6 | CRITICAL | HIGH - Blocks player transactions |
| **TownData operations** | ~15 | HIGH | MEDIUM - Town management |
| **RegionData operations** | ~8 | HIGH | MEDIUM - Nation management |
| **TerritoryData operations** | ~12 | HIGH | MEDIUM - Territory logic |
| **Event handlers** | ~10 | CRITICAL | HIGH - Called on main thread |
| **Commands** | ~15 | MEDIUM | LOW - Player-initiated |
| **GUI updates** | ~8 | MEDIUM | LOW - Display only |
| **Newsletter events** | ~10 | LOW | LOW - Background tasks |
| **Tests** | ~30 | N/A | N/A - Tests allowed to be sync |

---

## Priority Matrix

### 🔴 CRITICAL - Must Fix First (Economy + Events)

**These block player interactions and server responsiveness:**

#### 1. EconomyUtil.java (6 calls)
```java
// Current - BLOCKING
public static double getBalance(Player player) {
    return econ.getBalance(PlayerDataStorage.getInstance().getSync(player));
}

// Target - ASYNC
public static CompletableFuture<Double> getBalance(Player player) {
    return PlayerDataStorage.getInstance().get(player.getID())
        .thenApply(econ::getBalance);
}
```

**Files**: `EconomyUtil.java:15,21,27,30,36,39`

**Risk**: HIGH - Economy core functionality
**Effort**: 2 hours (6 method signatures)
**Dependencies**: All economy-dependent code must update

#### 2. TanEconomyVault.java (4 calls)
```java
// Current - BLOCKING
@Override
public double getBalance(OfflinePlayer player) {
    return super.getBalance(PlayerDataStorage.getInstance().getSync(player));
}

// Target - ASYNC
public CompletableFuture<Double> getBalance(OfflinePlayer player) {
    return PlayerDataStorage.getInstance().get(player.getUniqueId())
        .thenCompose(tanPlayer -> CompletableFuture.supplyAsync(() -> super.getBalance(tanPlayer)));
}
```

**Files**: `TanEconomyVault.java:65,103,106,124`

**Risk**: HIGH - Vault API integration
**Effort**: 3 hours (Vault API is sync, must adapt)
**Dependencies**: External plugins using Vault

---

### 🟡 HIGH - Important Performance Wins

#### 3. TownData.java (~15 calls)

**Pattern**: Member/town loading during territory operations

```java
// Current - BLOCKING
public void removePlayer(UUID tanPlayerID) {
    ITanPlayer player = PlayerDataStorage.getInstance().getSync(tanPlayerID);
    // ... player logic
}

// Target - ASYNC
public CompletableFuture<Void> removePlayer(UUID tanPlayerID) {
    return PlayerDataStorage.getInstance().get(tanPlayerID)
        .thenAccept(player -> {
            // ... player logic
        });
}
```

**Files**: `TownData.java:142,220,234,276,351,380,398,400,653,679...`

**Risk**: MEDIUM - Core town functionality
**Effort**: 4 hours (~15 methods)
**Dependencies**: Town management commands

#### 4. TerritoryData.java (~12 calls)

**Pattern**: Player/territory operations

```java
// Current - BLOCKING
public boolean isPlayerIn(Player player) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    return members.contains(tanPlayer.getID());
}

// Target - ASYNC
public CompletableFuture<Boolean> isPlayerIn(Player player) {
    return PlayerDataStorage.getInstance().get(player.getUniqueId())
        .thenApply(tanPlayer -> members.contains(tanPlayer.getID()));
}
```

**Files**: `TerritoryData.java:496,576,639,695,738,796...`

**Risk**: MEDIUM - Permission checks
**Effort**: 3 hours (~12 methods)
**Dependencies**: GUI, commands, events

---

### 🟢 MEDIUM - Nice to Have

#### 5. Commands (~15 calls)
- Player commands can afford small delays
- Use `CompletableFuture.thenAcceptOnFolia()` pattern
- Effort: 2 hours

#### 6. GUI Updates (~8 calls)
- Display-only operations
- Use `AsyncGuiHelper` pattern (already exists!)
- Effort: 1 hour

#### 7. Newsletter/Events (~10 calls)
- Background notifications
- Can be fully async without player impact
- Effort: 1 hour

---

## Migration Strategy

### Phase 1: Foundation (CRITICAL - 5 hours)

**Target**: Economy layer + Folia async utilities

1. **Create FoliaAsyncHelper utility** (30 min)
   ```java
   public class FoliaAsyncHelper {
       public static <T> void runOnRegion(
           Location location,
           CompletableFuture<T> future,
           Consumer<T> consumer) {
           future.thenAcceptAsync(result -> {
               FoliaScheduler.runTask(plugin, location, () -> {
                   consumer.accept(result);
               });
           }, FoliaScheduler.asyncExecutor());
       }
   }
   ```

2. **Migrate EconomyUtil** (2 hours)
   - Change all 6 methods to async
   - Update all call sites

3. **Migrate TanEconomyVault** (3 hours)
   - Wrap Vault sync API with async bridge
   - Maintain backward compatibility

**Deliverable**: Economy operations no longer block region threads

---

### Phase 2: Core Territory Logic (HIGH - 7 hours)

**Target**: TownData + TerritoryData

1. **Migrate TownData** (4 hours)
   - Convert 15 methods to async
   - Update call sites in commands/GUI

2. **Migrate TerritoryData** (3 hours)
   - Convert 12 methods to async
   - Update permission checks

**Deliverable**: Territory operations respect Folia threading

---

### Phase 3: Commands and GUI (MEDIUM - 3 hours)

**Target**: Commands + GUI updates

1. **Migrate Commands** (2 hours)
   - Use async command pattern
   - Update 15 command handlers

2. **Migrate GUI Updates** (1 hour)
   - Use existing AsyncGuiHelper
   - Prefetch data in background

**Deliverable**: Player-initiated operations are async

---

### Phase 4: Cleanup (LOW - 2 hours)

**Target**: Newsletter, events, tests

1. **Migrate Newsletter** (1 hour)
   - Fully async notification system

2. **Update Tests** (1 hour)
   - Tests can remain sync if needed
   - Add async test patterns where relevant

**Deliverable**: All production code is async

---

## Code Patterns

### Pattern 1: Simple Query
```java
// Before
public Type result = SomeStorage.getInstance().getSync(id);

// After
SomeStorage.getInstance().get(id).thenAccept(result -> {
    // Use result on region thread
    FoliaScheduler.runTask(plugin, location, () -> {
        // Process result
    });
});
```

### Pattern 2: Chained Queries
```java
// Before
TownData town = TownDataStorage.getInstance().getSync(townId);
ITanPlayer player = PlayerDataStorage.getInstance().getSync(playerId);

// After
TownDataStorage.getInstance().get(townId)
    .thenCompose(town -> PlayerDataStorage.getInstance()
        .get(playerId)
        .thenApply(player -> Pair.of(town, player)))
    .thenAccept(pair -> {
        TownData town = pair.first();
        ITanPlayer player = pair.second();
        FoliaScheduler.runTask(plugin, location, () -> {
            // Process both
        });
    });
```

### Pattern 3: Return Value to Player
```java
// Before
public boolean someMethod(Player player) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    return tanPlayer.hasTown();
}

// After
public CompletableFuture<Boolean> someMethod(Player player) {
    return PlayerDataStorage.getInstance().get(player.getUniqueId())
        .thenApply(ITanPlayer::hasTown);
}
```

---

## Testing Strategy

### Unit Tests
- Mock storage with async return values
- Use `CompletableFuture.complete()` in tests

### Integration Tests
- Keep using `getSync()` in tests (acceptable)
- Add new tests for async paths

### Manual Testing
- Test economy operations under load
- Verify GUI responsiveness
- Monitor thread blocking with visualvm

---

## Risk Mitigation

### High Risk Areas

1. **Economy system**
   - Risk: Breaking player balance transactions
   - Mitigation: Thorough testing, feature flag

2. **Vault integration**
   - Risk: External plugin incompatibility
   - Mitigation: Maintain sync wrapper for Vault API

3. **Command handlers**
   - Risk: Commands not executing
   - Mitigation: Add error handling, fallback to sync

### Rollback Plan
- Keep `getSync()` methods but mark `@Deprecated`
- Add warning logs when sync methods are called
- Remove after 2 versions

---

## Success Metrics

### Completion Criteria
- [ ] Zero sync calls in economy operations
- [ ] Zero sync calls in territory operations
- [ ] Zero sync calls in event handlers
- [ ] All production code uses async patterns
- [ ] Tests still pass (tests can use sync)

### Performance Targets
- GUI response time: < 200ms (currently variable)
- Economy operations: < 100ms (currently blocks)
- No thread blocking warnings in logs

---

## Next Steps

1. **Review this plan** with team
2. **Create utility class** (FoliaAsyncHelper)
3. **Start Phase 1** with EconomyUtil migration
4. **Test thoroughly** before proceeding to Phase 2

---

**Document Version**: 1.0
**Last Updated**: 2025-01-11
