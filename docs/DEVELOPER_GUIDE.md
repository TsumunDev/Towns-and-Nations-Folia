# Towns and Nations - Developer Guide

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Pattern](#component-pattern)
3. [Async Patterns](#async-patterns)
4. [Storage Layer](#storage-layer)
5. [Code Style Guide](#code-style-guide)
6. [Testing Guide](#testing-guide)
7. [Contributing Guide](#contributing-guide)
8. [Troubleshooting](#troubleshooting)
9. [API Documentation](#api-documentation)

---

## Architecture Overview

### High-Level System Design

Towns and Nations (TAN) is a modular Minecraft plugin built on **Folia/Paper 1.21.1+** using a multi-module Maven architecture:

```
tan-api/          # Public API interfaces, POJOs, events
tan-core/         # Core implementation, database access, listeners, commands
```

### Key Architectural Principles

1. **Async-First Design**: All I/O operations (database, Redis, network) use `CompletableFuture` to avoid blocking Folia region threads
2. **Component Pattern**: Territory data uses composition instead of deep inheritance hierarchies
3. **Event-Driven**: Internal communication via Bukkit events + custom TAN API events
4. **Thread-Safety**: Lock-free reads with `ConcurrentHashMap`, proper synchronization for writes
5. **Cache-First**: Double-layer caching (Guava local + Redis distributed) for performance

### Module Structure

#### `tan-api` Module
- **Purpose**: Public API for third-party plugin integration
- **Contents**:
  - Interfaces: `ITanPlayer`, `ITown`, `IRegion`
  - Events: `TownClaimEvent`, `RegionCreatedEvent`, etc.
  - Data Objects: Records and DTOs for API contracts
- **Stability**: Semantic versioning, backward compatibility maintained

#### `tan-core` Module
- **Purpose**: Core plugin implementation
- **Contents**:
  - Database storage layer (`DatabaseStorage`, `PlayerDataStorage`, `TownDataStorage`, `RegionDataStorage`)
  - Economy system (`AbstractTanEcon`, `TanEconomyStandalone`, `TanEconomyVault`)
  - GUI system (Triumph GUI-based)
  - Command handlers
  - Event listeners
- **Dependencies**: `tan-api`, Paper API, HikariCP, Redisson

### Threading Model (Folia)

**CRITICAL**: Folia regionalizes the tick loop. Each region has its own thread.

- **NEVER** block a region thread with database I/O
- **NEVER** use `Bukkit.getScheduler()` for region-specific operations
- **ALWAYS** use `FoliaScheduler` for region-aware scheduling
- **ALWAYS** use `CompletableFuture` for I/O operations

---

## Component Pattern

### Why Composition Over Inheritance?

Towns and Regions have many cross-cutting concerns:
- Economy (treasury, taxes)
- Territory (chunk claims)
- Diplomacy (alliances, wars)
- Progression (prestige, upgrades)

Using inheritance would create deep hierarchies like:
```
TerritoryData -> TownData -> EconomicTown -> DiplomaticTown -> PrestigeTown
```

This is brittle and hard to maintain.

### Component-Based Design

Instead, we use **composition** with immutable components:

```java
public class TownData extends TerritoryData {
    private final TreasuryComponent treasury;      // Economy
    private final TaxComponent tax;                // Taxation
    private final DiplomacyComponent diplomacy;    // Relations
    private final ChunkComponent chunks;           // Claims
    private final WarComponent war;                // War mechanics

    // No deep inheritance, just composed behaviors
}
```

### Component Rules

1. **Immutability**: Components are immutable. To modify, create a new instance.
2. **Thread-Safety**: Components use concurrent collections for shared state
3. **Encapsulation**: Each component manages its own data and logic
4. **Testability**: Components can be tested in isolation

### Example: Creating a Town with Components

```java
public class TownData extends TerritoryData {
    public TownData(String id, String name, ITanPlayer leader) {
        super(id, name, leader);

        // Initialize components
        this.treasury = new TreasuryComponent(0.0);
        this.tax = new TaxComponent(10.0);
        this.diplomacy = new DiplomacyComponent();
        this.chunks = new ChunkComponent();
        this.war = new WarComponent();
    }

    public void addToTreasury(double amount) {
        treasury.deposit(amount);
        markDirty(); // Triggers async save
    }

    public void collectTaxes() {
        double taxAmount = tax.calculate(this);
        treasury.deposit(taxAmount);
    }
}
```

---

## Async Patterns

### The Golden Rule of Folia

**Never block a region thread.**

Blocking a region thread causes lag for all players and entities in that region.

### Async Pattern: I/O Pipeline

Standard pattern for database operations:

```java
public CompletableFuture<TownData> loadTownAsync(String townId) {
    // Step 1: Async database read (off region thread)
    return supplyAsync(() -> {
        return database.query("SELECT * FROM towns WHERE id = ?", townId);
    })

    // Step 2: Process data (still off region)
    .thenApplyAsync(json -> {
        return gson.fromJson(json, TownData.class);
    })

    // Step 3: Return to region thread for game state updates
    .thenAcceptAsync(town -> {
        FoliaScheduler.runTask(plugin, town.getSpawnLocation(), () -> {
            // Now safe to modify game state
            town.markDirty();
        });
    });
}
```

### Async Pattern: GUI Loading

GUIs must be opened on the region thread:

```java
public static void openTownGUI(Player player, String townId) {
    Location loc = player.getLocation();

    // Load data off-thread
    supplyAsync(() -> {
        return TownDataStorage.getInstance().getSync(townId);
    })

    // Open GUI on region thread
    .thenAccept(town -> {
        FoliaScheduler.runTask(plugin, loc, () -> {
            new TownGUI(player, town).open();
        });
    });
}
```

### Virtual Threads (Project Loom)

Virtual threads can be used for pure I/O (database, Redis) **only if** they don't interact with game state:

```java
// ✅ GOOD: Pure database I/O
public CompletableFuture<ITanPlayer> loadPlayer(String playerId) {
    return CompletableFuture.supplyAsync(() -> {
        return database.queryPlayer(playerId);
    }, virtualThreadExecutor);
}

// ❌ BAD: Virtual thread modifying game state
public void unsafeModify(Player player) {
    CompletableFuture.runAsync(() -> {
        player.getInventory().addItem(item); // UNSAFE!
    }, virtualThreadExecutor);
}
```

### FoliaScheduler Utility

Use `FoliaScheduler` for region-aware scheduling:

```java
// Run task on specific region
FoliaScheduler.runTask(plugin, location, () -> {
    // Safe to access game state here
});

// Run task delayed (region-specific)
FoliaScheduler.runTaskLater(plugin, location, () -> {
    // Delayed task
}, Duration.ofSeconds(5));

// Run task async (off-region)
FoliaScheduler.runTaskAsync(plugin, () -> {
    // I/O operations here
});
```

---

## Storage Layer

### Architecture Overview

The storage layer uses a **generic CRUD abstraction** with **double-layer caching**:

```
Application
    ↓
Guava Cache (L1 - in-memory)
    ↓
Redis Cache (L2 - distributed, optional)
    ↓
Database (SQLite/MySQL)
```

### DatabaseStorage Class

`DatabaseStorage<T>` is the abstract base class for all storage:

```java
public abstract class DatabaseStorage<T> {
    protected final Gson gson;
    protected final DataSource dataSource;
    protected final Cache<String, T> cache;

    // CRUD operations
    public abstract void create(T object);
    public abstract CompletableFuture<T> get(String id);
    public abstract void update(T object);
    public abstract void delete(String id);

    // Cache management
    protected void invalidateCache(String id);
    protected void clearCache();
}
```

### Concrete Storage Classes

1. **PlayerDataStorage**: Stores player balances, town membership
2. **TownDataStorage**: Stores town data, economy, claims
3. **RegionDataStorage**: Stores regions, vassal relationships

### Data Flow Example

```java
// READ operation
public CompletableFuture<ITanPlayer> getPlayer(String playerId) {
    // 1. Check L1 cache (Guava)
    T cached = cache.getIfPresent(playerId);
    if (cached != null) {
        return CompletableFuture.completedFuture(cached);
    }

    // 2. Check L2 cache (Redis, optional)
    // 3. Query database
    // 4. Populate caches
    // 5. Return result
}

// WRITE operation
public void updatePlayer(ITanPlayer player) {
    // 1. Update database
    // 2. Invalidate L1 cache
    // 3. Invalidate L2 cache
}
```

### Batch Write Optimizer

For bulk writes, use the `BatchWriteOptimizer`:

```java
BatchWriteOptimizer optimizer = new BatchWriteOptimizer();

optimizer.scheduleUpdate(player1);
optimizer.scheduleUpdate(player2);
optimizer.flush(); // Executes all updates in batch
```

### Thread-Safety

- **Reads**: Lock-free via `ConcurrentHashMap`
- **Writes**: Synchronized via `ReentrantLock` per entity ID
- **Cache**: `ConcurrentHashMap` for thread-safe access

---

## Code Style Guide

### Spotless Configuration

We use **Spotless** for automatic code formatting:

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <configuration>
        <java>
            <googleJavaFormat/>
            <removeUnusedImports/>
        </java>
    </configuration>
</plugin>
```

### Formatting Rules

1. **Indentation**: 4 spaces (no tabs)
2. **Line length**: 120 characters max
3. **Imports**: Sorted, unused imports removed
4. **Braces**: K&R style (opening brace on same line)

### Code Conventions

#### Naming

- **Classes**: `PascalCase` (e.g., `TownDataStorage`)
- **Methods**: `camelCase` (e.g., `getPlayerBalance`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_CLAIMS`)
- **Private fields**: `camelCase` (e.g., `playerCache`)

#### Javadoc

All public methods must have Javadoc:

```java
/**
 * Gets the balance of a player.
 *
 * @param player the player to query
 * @return the player's balance, may be negative
 * @throws IllegalArgumentException if player is null
 */
public double getBalance(ITanPlayer player) {
    // implementation
}
```

#### Modern Java (Java 21+)

Use modern Java features:

```java
// ✅ GOOD: Pattern matching
if (obj instanceof String s) {
    System.out.println(s.toUpperCase());
}

// ✅ GOOD: Switch expressions
String result = switch (value) {
    case 1 -> "one";
    case 2 -> "two";
    default -> "other";
};

// ✅ GOOD: Records
public record Point(int x, int y) {}

// ✅ GOOD: Var for local variables
var player = Bukkit.getPlayer(uuid);

// ✅ GOOD: Text blocks for multi-line strings
String json = """
    {
        "name": "Town",
        "id": "123"
    }
    """;
```

### Running Spotless

```bash
# Check code style
mvn spotless:check

# Auto-format code
mvn spotless:apply
```

---

## Testing Guide

### Test Framework

We use **JUnit 5** with **MockBukkit** for testing:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.papermc</groupId>
    <artifactId>paper-mockbukkit</artifactId>
    <version>1.21.1</version>
    <scope>test</scope>
</dependency>
```

### Writing Tests

#### Basic Test Structure

```java
@DisplayName("TownData storage tests")
class TownDataStorageTest {

    private TownDataStorage storage;
    private TanPlugin mockPlugin;

    @BeforeEach
    void setUp() {
        mockPlugin = MockBukkit.mock(TanPlugin.class);
        storage = new TownDataStorage(mockPlugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Create town should persist to database")
    void testCreateTown() {
        // Arrange
        TownData town = new TownData("town1", "MyTown", mockPlayer);

        // Act
        storage.createSync(town);

        // Assert
        TownData loaded = storage.getSync("town1");
        assertNotNull(loaded);
        assertEquals("MyTown", loaded.getName());
    }
}
```

#### Async Testing

```java
@Test
@DisplayName("Async load should complete successfully")
void testAsyncLoad() {
    CompletableFuture<ITanPlayer> future = storage.get(playerId);

    ITanPlayer player = future.join(); // Block for test
    assertNotNull(player);
}
```

#### Coverage Goals

- **Phase 1**: 20% coverage (current: 14%)
- **Phase 2**: 50% coverage
- **Phase 3**: 70% coverage

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TownDataStorageTest

# Run with coverage
mvn test jacoco:report
```

---

## Contributing Guide

### Workflow

1. **Fork** the repository
2. **Create branch**: `git checkout -b feature/my-feature`
3. **Make changes** following code style guidelines
4. **Write tests** for new functionality
5. **Run tests**: `mvn test`
6. **Format code**: `mvn spotless:apply`
7. **Commit** with clear message
8. **Push** to fork
9. **Create Pull Request**

### Commit Message Format

```
<type>: <description>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring
- `test`: Test updates
- `docs`: Documentation
- `chore`: Build/configuration changes

**Examples**:
```
feat: add town rename command
fix: prevent claiming chunks in war zones
refactor: migrate TownData to component pattern
test: add comprehensive storage tests
docs: update developer guide
```

### Pull Request Checklist

- [ ] Code follows style guidelines (`mvn spotless:check`)
- [ ] All tests pass (`mvn test`)
- [ ] New tests added for new functionality
- [ ] Javadoc added for public APIs
- [ ] Commit messages follow format
- [ ] No merge conflicts

### Code Review Process

1. **Automated checks**: CI runs tests and style checks
2. **Peer review**: At least one maintainer approval
3. **Integration**: Tested on staging server
4. **Merge**: Squash and merge to main branch

---

## Troubleshooting

### Common Issues

#### 1. "Region thread blocked" warning

**Symptom**: Console shows "Blocking region thread" warnings

**Cause**: Performing I/O on region thread

**Solution**:
```java
// ❌ BAD: Database call on region thread
public void onPlayerJoin(PlayerJoinEvent event) {
    ITanPlayer player = storage.getSync(id); // BLOCKS!
}

// ✅ GOOD: Async database call
public void onPlayerJoin(PlayerJoinEvent event) {
    storage.get(id).thenAccept(player -> {
        // Handle async result
    });
}
```

#### 2. "ConcurrentModificationException"

**Symptom**: Crash during iteration

**Cause**: Modifying collection while iterating

**Solution**:
```java
// ❌ BAD: Modify during iteration
for (Town town : towns) {
    if (town.isInactive()) {
        towns.remove(town); // CRASH!
    }
}

// ✅ GOOD: Use iterator
Iterator<Town> it = towns.iterator();
while (it.hasNext()) {
    Town town = it.next();
    if (town.isInactive()) {
        it.remove();
    }
}

// ✅ GOOD: Use concurrent collection
ConcurrentHashMap<String, Town> towns = new ConcurrentHashMap<>();
towns.forEach((id, town) -> {
    if (town.isInactive()) {
        towns.remove(id); // Safe
    }
});
```

#### 3. "ClassNotFoundException: Gson"

**Symptom**: Plugin fails to load

**Cause**: Missing dependency

**Solution**: Ensure dependencies are shaded in `pom.xml`:
```xml
<plugin>
    <artifactId>maven-shade-plugin</artifactId>
    <configuration>
        <relocations>
            <relocation>
                <pattern>com.google.gson</pattern>
                <shadedPattern>com.townsandnations.libs.gson</shadedPattern>
            </relocation>
        </relocations>
    </configuration>
</plugin>
```

#### 4. Tests failing with "MockBukkit not initialized"

**Symptom**: Test throws NPE on Bukkit methods

**Cause**: MockBukkit not set up

**Solution**:
```java
@BeforeEach
void setUp() {
    MockBukkit.mock(); // Initialize mocks
}

@AfterEach
void tearDown() {
    MockBukkit.unmock(); // Clean up
}
```

### Debug Mode

Enable debug logging in `config.yml`:

```yaml
debug:
  enabled: true
  log-database: true
  log-redis: true
  log-gui: false
```

### Performance Profiling

Use built-in profiling:

```bash
/tan profile start
# ... perform actions ...
/tan profile stop
```

Results saved to `profiles/` directory.

---

## API Documentation

### Generating Javadoc

```bash
# Generate Javadoc for all modules
mvn javadoc:javadoc

# Open in browser
open tan-api/target/site/apidocs/index.html
```

### Key Packages

- **`com.townsandnations.api`**: Public API interfaces
- **`com.townsandnations.api.events`**: Custom events
- **`com.townsandnations.core.storage`**: Storage layer
- **`com.townsandnations.core.economy`**: Economy system
- **`com.townsandnations.core.territory`**: Territory data models

### Example Code

See `JAVADOC_README.md` for comprehensive usage examples.

### Third-Party Integration

For integrating your plugin with TAN:

```java
// Listen to TAN events
@EventHandler
public void onTownClaim(TownClaimEvent event) {
    Town town = event.getTown();
    Chunk chunk = event.getChunk();

    // Your plugin logic here
}

// Access TAN API
ITanPlayer player = PlayerDataStorage.getInstance()
    .getSync(playerId);

if (player.hasTown()) {
    Town town = player.getTown();
    double balance = EconomyUtil.getBalance(player);
}
```

---

## Getting Help

- **GitHub Issues**: Bug reports and feature requests
- **Discord**: Community support
- **Documentation**: `docs/` folder
- **Javadoc**: API reference

---

*Last Updated: 2025-01-15*
