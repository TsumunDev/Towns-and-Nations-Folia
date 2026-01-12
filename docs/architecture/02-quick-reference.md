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

