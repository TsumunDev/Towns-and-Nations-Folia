## Technical Debt and Known Issues

### Critical Technical Debt

#### 1. ⚠️ **Outdated Folia API Version**
**File**: `tan-core/build.gradle:163`
```gradle
compileOnly 'dev.folia:folia-api:1.20.1-R0.1-SNAPSHOT'
```
**Issue**: Using Folia 1.20.1 instead of 1.21.1+
**Impact**: Missing new APIs, potential compatibility issues
**Fix Required**: Update to latest Folia API version
**Priority**: HIGH (blocks future-proofing)

#### 2. ⚠️ **Deprecated Code - Not Yet Removed**
**Files**:
- `utils/gui/GuiHelperBridge.java` - Marked for removal since v0.16.0
- `utils/deprecated/GuiUtil.java` - Old GUI utilities
- `utils/deprecated/HeadUtils.java` - Old head texture utilities

**Issue**: 92 files contain @Deprecated, TODO, or FIXME markers
**Impact**: Code bloat, confusion for developers, maintenance burden
**Fix Required**:
- Remove GuiHelperBridge and all usage (replace with AsyncGuiHelper)
- Remove utils/deprecated/ package entirely
- Create cleanup plan for remaining deprecated items
**Priority**: MEDIUM (code cleanliness)

#### 3. ⚠️ **Incomplete Features (Development Zone)**
**Packages** (CAN BE REMOVED & RESTARTED):
- `domain/quest/` - Quest system implementation (incomplete)
- `domain/prestige/` - Prestige points system (incomplete)
- `domain/upgrade/` - Upgrade shop system (incomplete)
- `dataclass/territory/progression/` - Town progression tiers (incomplete)
- `gui/user/quest/` - Quest GUIs (incomplete)
- `gui/user/progression/` - Progression GUIs (incomplete)
- `gui/user/prestige/` - Prestige GUIs (incomplete)
- `service/quest/`, `service/prestige/`, `service/upgrade/` - Service layers (incomplete)

**Issue**: These systems are partially implemented but not production-ready
**Impact**: Code complexity without functional benefit, unclear architecture
**Recommended Action**:
- Option A: Complete these systems with proper architecture
- Option B: Remove entirely and redesign from scratch with clean architecture
**Priority**: MEDIUM (architectural decision needed)

#### 4. ⚠️ **Low Test Coverage**
**File**: `tan-core/build.gradle:337`
```gradle
minimum = 0.14  // Currently 14% coverage
```
**Issue**: Only 14% test coverage (many GUI, storage, and listener tests excluded)
**Impact**: High risk of regressions, difficult refactoring
**Planned Roadmap**:
- Phase 1 (✅ Done): 14% → 15% - Baseline with critical tests
- Phase 2: 20% - Expand storage and economy tests
- Phase 3: 35% - Cover command handlers
- Phase 4: 50% - Include GUI tests (currently excluded)
- Phase 5: 70%+ - Full coverage including legacy/deprecated (remove exclusions)

**Priority**: HIGH (quality gate enforcement)

### Workarounds and Gotchas

#### 1. **Folia Regionalization**
**Gotcha**: Never block region threads with I/O operations
**Solution**: Always use `FoliaScheduler.runTaskAsync()` for database/Redis operations
**Example**:
```java
// ❌ WRONG - Blocks region thread
TownData town = database.loadTown(id);

// ✅ CORRECT - Async operation
database.loadTownAsync(id).thenAccept(town -> {
    FoliaScheduler.runTask(plugin, player.getLocation(), () -> {
        // Update GUI on region thread
    });
});
```

#### 2. **GUI State Management**
**Gotcha**: Players can close GUIs at any time, causing race conditions
**Solution**: Always check player online status before GUI updates
**File**: `utils/AsyncGuiHelper.java`
```java
if (!player.isOnline()) {
    return; // Player left, don't update GUI
}
```

#### 3. **Cache Invalidation**
**Gotcha**: Two-tier cache can serve stale data if not properly invalidated
**Solution**:
- Local cache: 3-minute TTL
- Redis cache: Variable TTLs (1-30 minutes)
- Pub/sub invalidation for cross-server updates

#### 4. **Kotlin Interoperability**
**Gotcha**: Kotlin nullability annotations not always respected from Java
**Solution**: Use `@Nullable` and `@NonNull` annotations consistently
**File**: `coroutines/KotlinBridge.java`

### Known Performance Bottlenecks

#### 1. **Large GUI Updates**
**Issue**: Loading entire town/region data for every GUI open
**Impact**: Slow GUI response times for large towns
**Solution**: Implement pagination, lazy loading, or data prefetching

#### 2. **Territory Lazy Loading**
**File**: `redis/TerritoryLazyLoader.java`
**Issue**: Synchronized blocks for loading coordination can cause contention
**Mitigation**: Already implemented with proper coordination, but monitor under high load

#### 3. **Batch Write Optimization**
**File**: `storage/stored/DatabaseStorage.java`
**Issue**: Single-row writes by default
**Solution**: Use `QueryBatchExecutor` for bulk operations (50 items, 100ms delay)

