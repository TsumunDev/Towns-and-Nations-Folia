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

