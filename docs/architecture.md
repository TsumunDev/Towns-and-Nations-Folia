# Coconation (Towns & Nations) - Brownfield Architecture Document

## Introduction

This document captures the **CURRENT STATE** of the Coconation (formerly Towns & Nations) Minecraft plugin codebase, including technical debt, workarounds, and real-world patterns. It serves as a reference for AI agents working on enhancements, with a focus on achieving 90/100 quality score through code modernization, API updates, and future-proofing.

### Project Overview

**Coconation** is a comprehensive territorial, diplomatic, and economic management plugin for Minecraft Folia/Paper servers. Players can create towns, form regions (nations), engage in diplomacy, wage wars, and manage complex economies.

**Current Version**: 2.0.0 (Development)
**Minecraft Target**: Folia 1.21.1+ (⚠️ **NEEDS UPDATE**: currently 1.20.1 in build.gradle)
**Java Version**: 21 (LTS)
**Language**: Java 21 + Kotlin 2.1.0 (mixed codebase, ~600 source files)

### Document Scope

**Comprehensive documentation** of entire system with focus on:
- **Code Quality**: Identifying areas needing cleanup for 90/100 score
- **API Modernization**: Upgrading deprecated APIs and patterns
- **Future-Proofing**: Ensuring long-term maintainability and scalability

### Quality Goals

The project aims for:
- ✅ **90/100 quality score** (clean code, minimal bugs)
- ✅ **Zero deprecated methods** (all APIs current)
- ✅ **Future-proof architecture** (extensible, maintainable)
- ✅ **Folia-native design** (proper regionalization)
- ✅ **High test coverage** (currently 14%, progressive improvement planned)

### Change Log

| Date | Version | Description | Author |
| ------ | ------- | ----------- | ------ |
| 2025-01-11 | 1.0 | Initial brownfield analysis | Winston (Architect) |

---

## Quick Reference - Key Files and Entry Points

### Critical Files for Understanding the System

#### Core Plugin
- **Main Entry**: `tan-core/src/main/java/org/leralix/tan/TownsAndNations.java` - Plugin initialization, lifecycle, component wiring
- **Configuration**: `tan-core/src/main/resources/config.yml` - Main configuration
- **Language**: `tan-core/src/main/resources/lang/en/main.yml` - English localization (pattern for other languages)

#### Domain Models
- **Territory Data**: `tan-core/src/main/java/org/leralix/tan/dataclass/territory/TerritoryData.java` - Abstract base for towns/regions
- **Town Data**: `tan-core/src/main/java/org/leralix/tan/dataclass/territory/TownData.java` - Town-specific logic
- **Region Data**: `tan-core/src/main/java/org/leralix/tan/dataclass/territory/RegionData.java` - Region (nation) logic
- **Player Data**: `tan-core/src/main/java/org/leralix/tan/dataclass/PlayerData.java` - Player accounts

#### Storage Layer
- **Database Storage**: `tan-core/src/main/java/org/leralix/tan/storage/stored/DatabaseStorage.java` - Generic CRUD with caching
- **Player Storage**: `tan-core/src/main/java/org/leralix/tan/storage/stored/PlayerDataStorage.java`
- **Town Storage**: `tan-core/src/main/java/org/leralix/tan/storage/stored/TownDataStorage.java`
- **Region Storage**: `tan-core/src/main/java/org/leralix/tan/storage/stored/RegionDataStorage.java`
- **Database Handlers**:
  - `tan-core/src/main/java/org/leralix/tan/storage/database/MySqlHandler.java`
  - `tan-core/src/main/java/org/leralix/tan/storage/database/SQLiteHandler.java`

#### Async & Redis
- **Redis Manager**: `tan-core/src/main/java/org/leralix/tan/redis/RedisManager.java` - Multi-server synchronization
- **Cache Manager**: `tan-core/src/main/java/org/leralix/tan/redis/QueryCacheManager.java` - Two-tier caching (Guava + Redis)
- **Folia Scheduler**: `tan-core/src/main/java/org/leralix/tan/utils/FoliaScheduler.java` - Folia-aware scheduling abstraction
- **Virtual Threads**: `tan-core/src/main/java/org/leralix/tan/async/VirtualThreadExecutor.java` - Java 21 virtual thread support

#### Economy & Integration
- **Economy Util**: `tan-core/src/main/java/org/leralix/tan/economy/EconomyUtil.java` - Economy abstraction layer
- **Vault Manager**: `tan-core/src/main/java/org/leralix/tan/economy/VaultManager.java` - Vault API integration
- **PlaceholderAPI**: `tan-core/src/main/java/org/leralix/tan/api/external/papi/PlaceHolderAPI.java`
- **WorldGuard**: `tan-core/src/main/java/org/leralix/tan/api/external/worldguard/WorldGuardManager.java`
- **Nexo Integration**: `tan-core/src/main/java/org/leralix/tan/integration/nexo/NexoIntegration.kt` - Reflection-based item integration

#### GUI & Commands
- **Async GUI Helper**: `tan-core/src/main/java/org/leralix/tan/utils/AsyncGuiHelper.java` - Async GUI patterns
- **Base GUI**: `tan-core/src/main/java/org/leralix/tan/gui/BasicGui.java` - Abstract GUI base
- **Player Commands**: `tan-core/src/main/java/org/leralix/tan/commands/player/PlayerCommandManager.java`
- **Admin Commands**: `tan-core/src/main/java/org/leralix/tan/commands/admin/AdminCommandManager.java`

#### Testing
- **Test Base**: `tan-core/src/test/java/org/leralix/tan/` - JUnit 5 + MockBukkit tests
- **Coverage**: Currently 14% (progressive roadmap to 70%)

---

## High Level Architecture

### Technical Summary

Coconation is a **multi-module Gradle project** implementing a complex town/nation management system with **async-first architecture** designed for Folia's regionalized threading model.

**Architecture Patterns**:
- **Component Pattern**: Immutable components for thread-safe territory data
- **Repository Pattern**: DatabaseStorage abstraction for CRUD operations
- **Strategy Pattern**: Multiple economy implementations (Standalone/External/Vault)
- **Observer Pattern**: Event-driven GUI updates and Redis pub/sub
- **Async/Await**: CompletableFuture-based async operations throughout
- **Two-Tier Caching**: Local (Guava) + Distributed (Redis) for performance

### Actual Tech Stack (from build.gradle files)

| Category | Technology | Version | Notes |
| ---------- | ---------- | ------- | ----- |
| **Build System** | Gradle | 8.x | Kotlin DSL 2.1.0 |
| **Java** | OpenJDK | 21 | LTS, Virtual Threads enabled |
| **Kotlin** | Kotlin | 2.1.0 | Coroutines 1.9.0, JVM target 21 |
| **Minecraft Server** | Folia API | 1.20.1 | ⚠️ **NEEDS UPDATE to 1.21.1+** |
| **GUI Framework** | Triumph GUI | 3.1.11 | Comprehensive chest GUI system |
| **Database** | HikariCP | 5.1.0 | Connection pooling |
| **Database Drivers** | MySQL Connector | 8.4.0 | Production MySQL |
| | SQLite JDBC | 3.43.2.0 | Embedded SQLite |
| **Redis** | Redisson | 3.24.0 | Cluster/Sentinel/Single support |
| **JSON** | Gson | 2.11.0 | Serialization/deserialization |
| **Logging** | SLF4J + Logback | 2.0.17/1.5.20 | Structured logging |
| **Testing** | JUnit | 5.10.0 | Modern testing framework |
| | MockBukkit | v4.72.9 | Bukkit server mocking |
| | TestContainers | 1.19.3 | Real database integration tests |
| | Mockito | 5.7.0 | Mocking framework |
| **Circuit Breaker** | Resilience4j | 2.1.0 | Fault tolerance |
| **Monitoring** | bStats | 3.1.0 | Plugin metrics |
| **Math** | exp4j | 0.4.8 | Expression evaluation |

### Module Structure

**Multi-module Gradle layout**:

```
TownsAndNations-folia/
├── tan-api/              # Public API module (interfaces only)
│   └── build.gradle      # Java library, Folia API compile-only
├── tan-core/             # Main implementation (Java + Kotlin)
│   └── build.gradle      # Shadow plugin for fat JAR
├── build.gradle          # Root configuration
└── settings.gradle       # Module definitions
```

**Module Dependencies**:
- `tan-api` → Pure interface definitions, compile-only dependencies
- `tan-core` → Depends on `tan-api`, contains all implementation logic

### Repository Structure Reality Check

- **Type**: Monorepo with multi-module Gradle build
- **Package Manager**: Gradle with Kotlin DSL
- **Build Tool**: Gradle 8.x, Shadow plugin for fat JAR creation
- **Version Control**: Git with GitHub
- **Notable**:
  - Mixed Java/Kotlin codebase (gradual Kotlin transition)
  - Heavy use of compile-only dependencies for Bukkit ecosystem
  - Shadow plugin minimizes JAR by excluding unused dependencies

---

## Source Tree and Module Organization

### Project Structure (Actual)

```text
tan-core/src/main/java/org/leralix/tan/
├── TownsAndNations.java                 # Main plugin class (entry point)
├── api/                                 # External integrations
│   ├── external/
│   │   ├── papi/PlaceHolderAPI.java     # PlaceholderAPI expansion
│   │   └── worldguard/                  # WorldGuard integration
│   └── internal/InternalAPI.java        # Plugin API for other plugins
├── async/                               # Async execution utilities
│   └── VirtualThreadExecutor.java       # Java 21 virtual thread support
├── building/                            # Building/landmark system
├── chat/                                # Chat system
├── commands/                            # Command handlers
│   ├── admin/                           # Admin commands
│   ├── debug/                           # Debug commands
│   ├── player/                          # Player commands
│   └── server/                          # Server commands
├── coroutines/                          # Kotlin coroutine bridge
│   └── KotlinBridge.java                # Java/Kotlin async interoperability
├── dataclass/                           # Domain models
│   ├── territory/                       # Territory hierarchy
│   │   ├── TerritoryData.java           # Abstract base (COMPONENT PATTERN)
│   │   ├── TownData.java                # Town implementation
│   │   ├── RegionData.java              # Region (nation) implementation
│   │   ├── components/                  # Immutable territory components
│   │   │   ├── TownPropertyComponent.java
│   │   │   └── TownRecruitmentComponent.java
│   │   ├── cosmetic/                    # Icons, colors, visual customization
│   │   ├── diplomacy/                   # Relations, alliances, vassals
│   │   ├── economy/                     # Budget, taxes, salaries
│   │   ├── permission/                  # Chunk permissions
│   │   ├── tax/                         # Tax collection
│   │   ├── treasury/                    # Balance management
│   │   ├── war/                         # War goals and military
│   │   └── progression/                 # ⚠️ DEV: Town progression tiers (INCOMPLETE)
│   ├── chunk/                           # Chunk claim system
│   │   ├── ClaimedChunk2.java           # Abstract base for all chunks
│   │   ├── TownClaimedChunk.java
│   │   ├── RegionClaimedChunk.java
│   │   ├── LandmarkClaimedChunk.java
│   │   └── WildernessChunk.java
│   ├── property/                        # 3D property system
│   ├── newhistory/                      # Transaction history
│   ├── PlayerData.java                  # Player accounts
│   ├── PropertyData.java                # Property plots
│   └── RankData.java                    # Rank definitions
├── domain/                              # ⚠️ DEV: Domain layer (INCOMPLETE/REFACTOR NEEDED)
│   ├── quest/                           # Quest system (CAN BE REMOVED & RESTARTED)
│   ├── prestige/                        # Prestige points (CAN BE REMOVED & RESTARTED)
│   └── upgrade/                         # Upgrade shop (CAN BE REMOVED & RESTARTED)
├── economy/                             # Economy system
│   ├── AbstractTanEcon.java             # Abstract economy base
│   ├── TanEconomyStandalone.java        # Internal economy
│   ├── TanEconomyExternal.java          # External Vault wrapper
│   ├── TanEconomyVault.java             # Vault provider implementation
│   ├── EconomyUtil.java                 # Economy facade
│   └── VaultManager.java                # Vault integration
├── events/                              # Event handling
│   └── newsletter/                      # In-game newsletter system
├── gui/                                 # Triumph GUI system (116 GUI files!)
│   ├── user/                            # Player-facing GUIs
│   │   ├── player/                      # Player settings
│   │   ├── territory/                   # Town/region management
│   │   ├── property/                    # Property management
│   │   ├── ranks/                       # Rank management
│   │   ├── war/                         # War interfaces
│   │   ├── quest/                       # ⚠️ DEV: Quest GUIs (INCOMPLETE)
│   │   ├── progression/                 # ⚠️ DEV: Progression GUIs (INCOMPLETE)
│   │   └── prestige/                    # ⚠️ DEV: Prestige GUIs (INCOMPLETE)
│   ├── admin/                           # Admin tools
│   └── BasicGui.java                    # Abstract GUI base
├── integration/                         # Third-party integrations
│   └── nexo/                            # Nexo items (reflection-based)
│       ├── NexoIntegration.kt
│       ├── NexoExtensions.kt
│       └── NexoUpdateChecker.kt
├── listeners/                           # Bukkit event listeners
│   ├── chat/                            # Chat input handling
│   └── interact/                        # Right-click interactions
├── redis/                               # Multi-server synchronization
│   ├── RedisManager.java                # Pub/sub coordination
│   ├── QueryCacheManager.java           # Two-tier caching
│   ├── TerritoryLazyLoader.java         # High-performance loading
│   └── RedisClusterConfig.java          # Redis connection config
├── service/                             # ⚠️ DEV: Service layer (INCOMPLETE)
│   ├── quest/                           # Quest service
│   ├── prestige/                        # Prestige service
│   └── upgrade/                         # Upgrade service
├── storage/                             # Data persistence
│   ├── database/                        # Database handlers
│   │   ├── DatabaseHandler.java         # Abstract handler
│   │   ├── MySqlHandler.java            # MySQL implementation
│   │   ├── SQLiteHandler.java           # SQLite implementation
│   │   └── DatabaseHealthCheck.java     # Health monitoring
│   ├── stored/                          # Entity storage (Repository pattern)
│   │   ├── DatabaseStorage.java         # Generic CRUD base
│   │   ├── PlayerDataStorage.java
│   │   ├── TownDataStorage.java
│   │   └── RegionDataStorage.java
│   └── typeadapter/                     # Gson type adapters for complex types
├── tasks/                               # Scheduled tasks
├── utils/                               # Utility classes
│   ├── FoliaScheduler.java              # Folia-aware scheduling
│   ├── AsyncGuiHelper.java              # Async GUI operations
│   ├── deprecated/                      # ⚠️ DEPRECATED: Old utilities
│   │   ├── GuiUtil.java                 # DO NOT USE (remove in future)
│   │   └── HeadUtils.java               # DO NOT USE (remove in future)
│   └── gui/
│       └── GuiHelperBridge.java         # ⚠️ DEPRECATED: Use AsyncGuiHelper instead
└── wars/                                # War system implementation
```

### Key Modules and Their Purpose

#### Core Domain Logic
- **TerritoryData** (`dataclass/territory/TerritoryData.java`): Abstract base for all territories with **immutable component pattern** for thread safety
  - Components: TreasuryComponent, TaxComponent, CosmeticComponent, DiplomacyComponent, WarComponent
  - ~1000 lines, handles common territory logic (economy, diplomacy, ranks, chunks)

- **TownData** (`dataclass/territory/TownData.java`): Player-controlled towns
  - Features: Membership management, property system, progression system, prestige points
  - Extends TerritoryData with town-specific functionality

- **RegionData** (`dataclass/territory/RegionData.java`): Nations (collections of towns)
  - Features: Capital town, vassal towns, inter-town diplomacy, collective taxation
  - Extends TerritoryData with nation-specific logic

#### Storage Layer (HIGH QUALITY - Enterprise Patterns)
- **DatabaseStorage** (`storage/stored/DatabaseStorage.java`): Generic CRUD base with:
  - Built-in Guava caching (configurable size)
  - Async-first design (CompletableFuture<T>)
  - JSON serialization with Gson
  - Batch operations for performance
  - Query limiting and retry logic

- **Connection Pooling**:
  - MySQL: HikariCP with max pool 30, min idle 5, 30s timeout
  - SQLite: HikariCP with max pool 10, min idle 2, 120s timeout
  - Prepared statement caching (250 cache size)

- **Two-Tier Caching**:
  - **L1 (Local)**: Guava Cache (default 1000 entries, 3-min TTL)
  - **L2 (Redis)**: Redisson RMapCache with variable TTLs
  - Cache hit rate tracking and statistics

#### Async & Threading (EXCELLENT - Folia-Native Design)
- **FoliaScheduler** (`utils/FoliaScheduler.java`): Abstraction layer for Folia's regionalized threading
  - `runTask(plugin, location, task)` - Location-specific execution
  - `runTaskAsync(plugin, task)` - I/O operations
  - Proper separation of region-specific vs global tasks

- **VirtualThreadExecutor** (`async/VirtualThreadExecutor.java`): Java 21 virtual threads
  - Dynamic detection of Java 21+ capabilities
  - I/O-bound tasks: Virtual threads or ForkJoinPool fallback
  - CPU-bound tasks: Fixed thread pool (core count based)

- **Kotlin Coroutines Bridge** (`coroutines/KotlinBridge.java`):
  - `launchOnFolia()` extension functions
  - Structured concurrency with proper dispatchers
  - Seamless Java/Kotlin async interoperability

#### GUI System (COMPREHENSIVE - 116 GUI Files)
- **BasicGui** (`gui/BasicGui.java`): Abstract base using Triumph GUI
  - Standard click/drag cancellation
  - Layout glyphs support
  - Configurable row sizes

- **AsyncGuiHelper** (`utils/AsyncGuiHelper.java`): Async GUI patterns
  - `loadAsync()` - Basic async loading
  - `loadAsyncWithFallback()` - Async with fallback values
  - `prefetchPlayerData()` - Performance optimization
  - Circuit breaker pattern for error handling

- **GUI Categories**:
  - User GUIs: MainMenu, PlayerMenu, Territory menus
  - Admin GUIs: AdminMainMenu, admin tools
  - Property GUIs: Buy/rent/management
  - Rank GUIs: Permission and rank management
  - War GUIs: War planning and attacks

#### Economy & Integrations (CLEAN ABSTRACTIONS)
- **Economy System** (`economy/`):
  - `AbstractTanEcon` - Strategy pattern for economy providers
  - `TanEconomyStandalone` - Internal economy
  - `TanEconomyExternal` - Vault wrapper
  - `TanEconomyVault` - Vault provider for other plugins

- **External Integrations** (`api/external/`):
  - PlaceholderAPI: 26 placeholders registered
  - WorldGuard: Permission integration with flags
  - Nexo: Reflection-based item support (avoids version conflicts)

#### Redis & Multi-Server (PRODUCTION-READY)
- **RedisManager** (`redis/RedisManager.java`):
  - Multi-server synchronization with heartbeat (30s interval, 60s timeout)
  - Pub/sub channels: global, server-events, town-sync, player-sync
  - Server registry for cluster health

- **QueryCacheManager** (`redis/QueryCacheManager.java`):
  - Two-tier caching (Guava + Redis)
  - Variable TTLs by data type (1-30 minutes)
  - Cache invalidation patterns

- **RedisClusterConfig** (`redis/RedisClusterConfig.java`):
  - Single, Sentinel, and Cluster modes
  - 64 connection pool size, 10 minimum idle
  - Redis 6.0+ ACL support

---

## Data Models and APIs

### Data Models

The codebase uses a **hybrid approach**: Domain models with JSON serialization for database storage.

#### Core Domain Classes

**Territory Hierarchy**:
```
TerritoryData (Abstract)
├── TownData (Player-controlled)
└── RegionData (Nation - collection of towns)
```

**Key Models**:
- `PlayerData` - Player accounts, balances, town memberships
- `TownData` - Towns with economy, diplomacy, members, chunks
- `RegionData` - Nations with capital, vassals, inter-town relations
- `ClaimedChunk2` - Abstract chunk (location, permissions, owner)
- `PropertyData` - 3D property plots within towns
- `RankData` - Custom ranks with permissions

#### Component Pattern (Recent Refactoring)

**Immutable Components** for thread safety:
```java
// Territory data is now composed of immutable components
public class TerritoryData {
    private final TreasuryComponent treasury;
    private final TaxComponent tax;
    private final CosmeticComponent cosmetic;
    private final DiplomacyComponent diplomacy;
    private final WarComponent war;

    // Components are immutable, new instances created on changes
    public TerritoryData withTreasury(TreasuryComponent newTreasury) {
        return new TerritoryData(newTreasury, this.tax, ...);
    }
}
```

**Benefits**:
- Thread-safe by design (immutability)
- Clear separation of concerns
- Easy to test components independently
- Prevents concurrent modification issues

#### JSON Serialization

**Gson with Custom Type Adapters**:
- Located in `storage/typeadapter/`
- Handles complex types: EnumMap, CustomIcon, AbstractOwner
- Ensures proper round-trip serialization

**Storage Format**:
```sql
CREATE TABLE towns (
    id VARCHAR(36) PRIMARY KEY,
    town_name VARCHAR(255),
    data TEXT,  -- JSON serialized TownData
    -- Indexed columns for common queries
    created_at TIMESTAMP
);
```

### API Specifications

#### Public API (tan-api module)

**InternalAPI** (`api/internal/InternalAPI.java`):
- Plugin API for third-party developers
- Methods to query town/region data
- Economy access for other plugins
- Event hooks for territorial changes

**Note**: API is functional but could benefit from:
- Better Javadoc documentation
- More comprehensive event system
- API versioning strategy

#### PlaceholderAPI Integration

**26 Placeholders** registered with `tan_` prefix:

Economy placeholders:
- `%tan_player_balance%` - Player's current balance
- `%tan_player_town_balance%` - Town treasury balance
- `%tan_player_region_balance%` - Region treasury balance

Territory placeholders:
- `%tan_player_town_name%` - Town name
- `%tan_player_town_tag%` - Town tag (colored)
- `%tan_player_region_name%` - Region name
- `%tan_player_rank_name%` - Player's rank name

**Pattern**: Each placeholder is a separate class implementing `PapiEntry`

---

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

---

## Integration Points and External Dependencies

### External Services

| Service | Purpose | Integration Type | Key Files |
| -------- | ------- | ---------------- | --------- |
| **Vault** | Economy API | Service registration | `economy/VaultManager.java`, `economy/TanEconomyVault.java` |
| **PlaceholderAPI** | Placeholders | Expansion (26 placeholders) | `api/external/papi/PlaceHolderAPI.java` |
| **WorldGuard** | Region protection | Permission mapping | `api/external/worldguard/WorldGuardManager.java` |
| **Nexo** | Custom items | Reflection-based (avoids version conflicts) | `integration/nexo/NexoIntegration.kt` |
| **Redis** | Multi-server sync | Redisson client (cluster/sentinel/single) | `redis/RedisManager.java`, `redis/RedisClusterConfig.java` |
| **bStats** | Metrics | Anonymous usage stats | `TownsAndNations.java` initialization |

### Internal Integration Points

#### 1. **Async → Database → Cache**
```
Async Request
    ↓
FoliaScheduler.runTaskAsync() (Virtual Thread)
    ↓
DatabaseStorage.checkCache() (L1: Guava)
    ↓
Cache Miss? → Query Database
    ↓
RedisManager.checkRedisCache() (L2: Redis)
    ↓
Update both caches
    ↓
Return CompletableFuture<T>
```

#### 2. **GUI → Async → Main Thread**
```
Player opens GUI
    ↓
AsyncGuiHelper.loadAsync() (Data prefetching)
    ↓
FoliaScheduler.runTaskAsync() (Load data)
    ↓
FoliaScheduler.runTask() (Update GUI on region thread)
    ↓
Circuit breaker on failure (5 failures, 60s timeout)
```

#### 3. **Multi-Server Redis Pub/Sub**
```
Server A updates town data
    ↓
DatabaseStorage.saveAsync()
    ↓
RedisManager.publish("tan:town-sync", jsonData)
    ↓
Server B receives message (ignores own server ID)
    ↓
Invalidates local cache
    ↓
Next load fetches fresh data
```

### Dependency Management

**Soft Dependencies** (`plugin.yml`):
```yaml
softdepend:
  - PlaceholderAPI
  - WorldGuard
  - Vault
  - Nexo
```

**Strategy**: Plugin functions without these dependencies but with reduced capabilities:
- **No Vault**: Uses standalone internal economy
- **No PlaceholderAPI**: Placeholders simply don't work
- **No WorldGuard**: WorldGuard integration disabled
- **No Nexo**: Custom items unavailable (graceful fallback)

---

## Development and Deployment

### Local Development Setup

#### Prerequisites
- **Java 21** (OpenJDK or Oracle JDK)
- **Gradle 8.x** (included via Gradle Wrapper)
- **MySQL 8.0+** (for production testing) or SQLite (embedded)
- **Redis 6.0+** (optional, for multi-server testing)
- **Folia 1.21.1+** server (⚠️ **NEEDS UPDATE in build.gradle**)

#### Build Commands
```bash
# Clean build
./gradlew clean build

# Run tests (with code coverage)
./gradlew test jacocoTestReport

# Create plugin JAR (shaded with dependencies)
./gradlew shadowJar

# Output: tan-core/build/libs/Coconation.jar
```

#### Development Workflow
1. **Import**: Import project into IDE (IntelliJ IDEA recommended for Kotlin support)
2. **Dependencies**: Gradle will automatically download all dependencies
3. **Testing**: Run tests with `./gradlew test`
4. **Code Style**: No formal check configured yet (consider Spotless or Checkstyle)

#### Configuration Files
- **Main Config**: `tan-core/src/main/resources/config.yml`
- **Language**: `tan-core/src/main/resources/lang/en/main.yml` (English)
- **Plugin Metadata**: `tan-core/src/main/resources/plugin.yml`

### Build and Deployment Process

#### Build Pipeline
```bash
# 1. Clean previous builds
./gradlew clean

# 2. Run tests (fails if coverage < 14%)
./gradlew test

# 3. Create fat JAR with Shadow plugin
./gradlew :tan-core:shadowJar

# 4. Output JAR
tan-core/build/libs/Coconation.jar
```

#### Shadow JAR Configuration
**File**: `tan-core/build.gradle:37-82`

**Minimization**: Excludes unused classes from dependencies
- **Preserved**: SQLite/MySQL natives, logging, Kotlin runtime
- **Excluded platforms**: FreeBSD, Android ARM, Windows ARMv7 (reduces size)
- **Relocated dependencies**: Prevents conflicts
  - `net.objecthunter.exp4j` → `org.leralix.tan.libs.exp4j`
  - `com.mysql` → `org.leralix.shadow.mysql`
  - `com.zaxxer.hikari` → `org.leralix.shadow.hikari`
  - `com.google.gson` → `org.leralix.shadow.gson`
  - `io.prometheus` → `org.leralix.shadow.prometheus`

#### Deployment
**Target**: `plugins/` folder of Folia server
```bash
cp tan-core/build/libs/Coconation.jar /path/to/folia/plugins/
```

**First Run**: Plugin will generate:
- `plugins/Coconation/config.yml`
- `plugins/Coconation/lang.yml`
- `plugins/Coconation/database.db` (if using SQLite)

### Environments

**Development**:
- Local Folia server with SQLite
- Hot reload with IDE (IntelliJ)
- Debug logging enabled

**Staging**:
- Remote test server with MySQL
- Redis for multi-server testing
- Production-like configuration

**Production**:
- MySQL with connection pooling
- Redis cluster for multi-server sync
- Monitoring with bStats

---

## Testing Reality

### Current Test Coverage

**Overall**: 14% (Progressive improvement roadmap in place)

#### Covered Areas (✅)
- **Economy System**: `AbstractTanEconTest`, `EconomyUtilTest`, `TanEconomyStandaloneTest`, `TanEconomyVaultTest`
- **Territory Data**: `TownDataTest`, `RegionDataTest`
- **Components**: `TaxComponentTest`, `TreasuryComponentTest`, `CosmeticComponentTest`, `DiplomacyComponentTest`, `WarComponentTest`
- **Commands**: ~20 command tests (player and admin commands)
- **Listeners**: `PlayerJoinListenerTest`, `MobSpawnListenerTest`, `CommandBlockerTest`, `ChatListenerTest`
- **Utilities**: `CommandExceptionHandlerTest`, `RateLimiterTest`, `FoliaSchedulerTest`
- **Storage**: `DatabaseStorageTest`, `PlayerDataStorageCoverageTest`, `TownDataStorageCoverageTest`

#### Excluded Areas (❌ - Will be covered progressively)
- **GUI Tests**: Entire `gui/` package (excluded from coverage)
- **Deprecated Code**: `utils/deprecated/` (excluded)
- **Complex Integration Tests**: `listeners/interact/`, `wars/`
- **Storage Migration**: `storage/` (except explicitly included tests)

### Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests TownDataTest

# With coverage report
./gradlew test jacocoTestReport
# Report: tan-core/build/reports/jacoco/test/html/index.html
```

### Test Infrastructure

**Frameworks**:
- **JUnit 5** (`5.10.0`) - Modern testing
- **MockBukkit** (`v4.72.9`) - Bukkit server mocking
- **Mockito** (`5.7.0`) - Mocking framework
- **TestContainers** (`1.19.3`) - Real database integration tests

**Test Quality**: Tests are well-structured with:
- Proper `@BeforeEach` setup
- Clear test naming (should/when format)
- Mock isolation for unit tests
- Integration tests for database operations

**Coverage Enforcement**:
```gradle
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.14  // Currently 14%
            }
        }
    }
}
```

**Quality Gate**: Build fails if:
- Tests fail (`ignoreFailures = false`)
- Coverage below 14%

---

## Quality Improvement Roadmap

### Goal: Achieve 90/100 Quality Score

#### Priority 1: Code Cleanliness (CRITICAL)

**1.1 Remove Deprecated Code** ⚠️ HIGH PRIORITY
- [ ] Remove `GuiHelperBridge.java` and all usages
- [ ] Remove `utils/deprecated/` package
- [ ] Audit all 92 files with @Deprecated markers
- [ ] Create removal plan for each deprecated item

**1.2 Fix Folia API Version** ⚠️ HIGH PRIORITY
- [ ] Update `build.gradle`: `folia-api:1.20.1` → `1.21.1+`
- [ ] Test on latest Folia version
- [ ] Update documentation

**1.3 Standardize Code Style**
- [ ] Configure Spotless or Checkstyle
- [ ] Enforce consistent formatting
- [ ] Add import organization
- [ ] Set line length limits

#### Priority 2: API Modernization (HIGH)

**2.1 Review All API Usage**
- [ ] Audit for deprecated Bukkit/Paper APIs
- [ ] Replace with modern alternatives
- [ ] Update to latest Folia APIs

**2.2 Improve Type Safety**
- [ ] Add `@Nullable`/`@NonNull` annotations
- [ ] Use Java 21 records where appropriate
- [ ] Leverage Kotlin nullability

**2.3 Documentation**
- [ ] Javadoc all public APIs
- [ ] Document thread-safety guarantees
- [ ] Add usage examples

#### Priority 3: Future-Proofing (HIGH)

**3.1 Architecture Cleanup**
- [ ] Decide: Complete or remove incomplete features (quest/prestige/upgrade)
- [ ] If remove: Delete `domain/quest/`, `domain/prestige/`, `domain/upgrade/`
- [ ] If keep: Complete with proper architecture and tests

**3.2 Improve Test Coverage**
- [ ] Phase 2: 14% → 20% (expand storage and economy tests)
- [ ] Phase 3: 20% → 35% (cover command handlers)
- [ ] Phase 4: 35% → 50% (include GUI tests)
- [ ] Phase 5: 50% → 70%+ (full coverage)

**3.3 Performance Optimization**
- [ ] Profile GUI loading times
- [ ] Implement lazy loading for large datasets
- [ ] Optimize cache hit rates
- [ ] Add performance monitoring

**3.4 Error Handling**
- [ ] Standardize error handling patterns
- [ ] Improve error messages for users
- [ ] Add comprehensive logging
- [ ] Implement circuit breakers for external calls

### Success Metrics

**Code Quality**:
- Zero compiler warnings
- Zero deprecated API usage
- Consistent code style (Spotless passing)
- 70%+ test coverage

**API Quality**:
- All public APIs documented with Javadoc
- Thread-safety clearly documented
- Usage examples provided

**Future-Proofing**:
- Clean architecture (no incomplete features)
- Comprehensive test coverage
- Performance benchmarks passing
- Monitoring and alerting in place

---

## Appendix - Useful Commands and Scripts

### Frequently Used Commands

```bash
# Build
./gradlew clean build                    # Full build
./gradlew :tan-core:shadowJar           # Create plugin JAR

# Testing
./gradlew test                          # Run all tests
./gradlew test --tests *Test            # Run specific test
./gradlew jacocoTestReport              # Generate coverage report

# Development
./gradlew :tan-core:compileJava         # Compile Java
./gradlew :tan-core:compileKotlin       # Compile Kotlin
./gradlew clean                         # Clean build artifacts
```

### Debugging and Troubleshooting

**Logs**:
- Location: `tan-core/logs/tan.log`
- Level: Configured in `logback.xml`
- Format: SLF4J with structured logging

**Debug Mode**:
- Set `debug: true` in `config.yml`
- Enables verbose logging
- Logs all SQL queries
- Traces cache operations

**Common Issues**:

| Issue | Symptom | Solution |
| ----- | ------- | -------- |
| **Folia threading error** | "Not scheduled on region thread" | Use `FoliaScheduler.runTask()` instead of direct calls |
| **GUI not opening** | Player kicked or nothing happens | Check `AsyncGuiHelper` circuit breaker status |
| **Cache serving stale data** | Old town data displayed | Manually invalidate: `/ccn admin debug clearcache` |
| **Redis connection failed** | Multi-server sync not working | Check `config.yml` Redis settings, verify server is reachable |
| **Tests failing** | `./gradlew test` fails | Run `./gradlew clean test`, check test reports in `build/reports/tests/` |

### Performance Monitoring

**Cache Statistics**:
```java
// In-game command
/ccn admin debug cache

// Logs cache hit rates
L1 (Guava): 85.2% hit rate
L2 (Redis): 92.7% hit rate
Database queries: 1234
```

**Redis Health**:
```java
// Check Redis connection
/ccn admin debug redis

// Logs connection pool status
Active connections: 5/64
Idle connections: 10
Pub/sub channels: 4
```

**Database Performance**:
```java
// Connection pool stats
/ccn admin debug database

// Logs HikariCP stats
Active connections: 8/30
Idle connections: 5
Total connections: 13
```

---

## Conclusion

### Current State Summary

**Strengths**:
- ✅ Excellent async architecture (Folia-native)
- ✅ Enterprise-grade storage layer (HikariCP, two-tier caching)
- ✅ Comprehensive GUI system (116 GUI files with Triumph)
- ✅ Clean economy abstraction (Vault/Standalone)
- ✅ Production-ready Redis multi-server sync
- ✅ Good component pattern refactoring (immutable, thread-safe)

**Weaknesses** (Addressing 90/100 Goal):
- ⚠️ Outdated Folia API (1.20.1 instead of 1.21.1+)
- ⚠️ Low test coverage (14%, roadmap to 70%)
- ⚠️ Deprecated code not removed (92 files)
- ⚠️ Incomplete features (quest/prestige/progression)
- ⚠️ Inconsistent code style (no formatter configured)

### Recommended Next Steps

**Immediate** (Week 1):
1. Update Folia API to 1.21.1+
2. Remove all deprecated code (GuiHelperBridge, utils/deprecated/)
3. Configure code style formatter (Spotless)

**Short-term** (Month 1):
4. Decide: Complete or remove incomplete features
5. Increase test coverage to 20%
6. Document all public APIs with Javadoc

**Medium-term** (Quarter 1):
7. Achieve 50% test coverage
8. Implement performance monitoring
9. Standardize error handling patterns

**Long-term** (Quarter 2+):
10. Reach 70%+ test coverage
11. Complete architecture documentation
12. Achieve 90/100 quality score

### Architectural Philosophy

**Design Principles**:
1. **Async-First**: Never block region threads
2. **Immutability**: Use immutable components for thread safety
3. **Caching**: Two-tier cache (local + distributed)
4. **Graceful Degradation**: Works without optional dependencies
5. **Observability**: Comprehensive logging and metrics

**Future-Proofing Strategy**:
- Component pattern enables easy extension
- Async architecture scales with Folia
- Redis enables multi-server deployments
- Comprehensive test suite prevents regressions

### Final Assessment

This is a **well-architected, production-ready plugin** with excellent async patterns and enterprise-grade infrastructure. The main areas for improvement are:

1. **Code modernization** (update APIs, remove deprecated code)
2. **Testing expansion** (increase coverage progressively)
3. **Feature completion** (finish or remove incomplete systems)

With focused effort on these three areas, achieving 90/100 quality score is **realistic and attainable**.

---

**Document Version**: 1.0
**Last Updated**: 2025-01-11
**Maintained By**: Winston (Architect Agent)
**Status**: Complete brownfield analysis, ready for development planning
