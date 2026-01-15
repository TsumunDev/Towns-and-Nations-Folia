# Towns and Nations - Javadoc Documentation

This directory contains the Javadoc API documentation for the Towns and Nations plugin.

## Generating Documentation

To generate the Javadoc HTML documentation:

```bash
./gradlew javadoc
```

The documentation will be generated in:
- `tan-api/build/docs/javadoc/` - API module documentation
- `tan-core/build/docs/javadoc/` - Core module documentation

## Viewing Documentation

Open the documentation in your web browser:

```bash
# On Linux/macOS
open tan-core/build/docs/javadoc/index.html

# On Windows
start tan-core/build/docs/javadoc/index.html
```

Or navigate manually to:
```
tan-core/build/docs/javadoc/index.html
```

## Key Packages

### Territory API (`org.leralix.tan.dataclass.territory`)

The core territory management classes:

- **[TerritoryData](../tan-core/build/docs/javadoc/org/leralix/tan/dataclass/territory/TerritoryData.html)** - Abstract base class for all territories
  - Component pattern architecture
  - Member management
  - Economy integration
  - Diplomacy system

- **[TownData](../tan-core/build/docs/javadoc/org/leralix/tan/dataclass/territory/TownData.html)** - Town-specific implementation
  - Chunk claiming
  - Property ownership
  - Member ranks
  - Tax collection

- **[RegionData](../tan-core/build/docs/javadoc/org/leralix/tan/dataclass/territory/RegionData.html)** - Region/nation implementation
  - Vassal management
  - Capital system
  - Inter-town diplomacy

### Economy API (`org.leralix.tan.economy`)

Economy system integration:

- **[AbstractTanEcon](../tan-core/build/docs/javadoc/org/leralix/tan/economy/AbstractTanEcon.html)** - Economy abstraction layer
- **[EconomyUtil](../tan-core/build/docs/javadoc/org/leralix/tan/economy/EconomyUtil.html)** - Economy facade
- **[TanEconomyStandalone](../tan-core/build/docs/javadoc/org/leralix/tan/economy/TanEconomyStandalone.html)** - Internal TAN economy
- **[TanEconomyVault](../tan-core/build/docs/javadoc/org/leralix/tan/economy/TanEconomyVault.html)** - Vault API integration

### Storage API (`org.leralix.tan.storage.stored`)

Data persistence layer:

- **[TownDataStorage](../tan-core/build/docs/javadoc/org/leralix/tan/storage/stored/TownDataStorage.html)** - Town storage operations
- **[RegionDataStorage](../tan-core/build/docs/javadoc/org/leralix/tan/storage/stored/RegionDataStorage.html)** - Region storage operations
- **[PlayerDataStorage](../tan-core/build/docs/javadoc/org/leralix/tan/storage/stored/PlayerDataStorage.html)** - Player storage operations

## Code Examples

See [`TerritoryAPIExamples.java`](../tan-core/src/main/java/org/leralix/tan/examples/TerritoryAPIExamples.java) for comprehensive usage examples:

### Creating a Town

```java
// Get the player's ITanPlayer representation
ITanPlayer tanPlayer = PlayerDataStorage.getInstance()
    .getSync(player.getUniqueId().toString());

// Create the town asynchronously
TownDataStorage.getInstance().newTown(townName, tanPlayer)
    .thenAccept(town -> {
        town.setTax(10.0); // Set tax rate to 10%
    });
```

### Adding Members

```java
// Get the player's ITanPlayer representation
ITanPlayer tanMember = PlayerDataStorage.getInstance()
    .getSync(member.getUniqueId().toString());

// Add to town
town.addPlayer(tanMember);

// Verify membership
if (town.isPlayerInTown(tanMember)) {
    member.sendMessage("You have joined " + town.getName());
}
```

### Claiming Chunks

```java
// Claim a chunk
Location claimLocation = new Location(world, x, y, z);
town.claimChunk(player, claimLocation);

// Verify ownership
if (town.hasClaimedChunk(claimLocation)) {
    town.removeFromBalance(claimCost);
}
```

### Collecting Taxes

```java
// Collect taxes from all members
town.collectTax().thenAccept(result -> {
    double totalCollected = town.getBalance();
    System.out.println("Collected " + totalCollected + " in taxes");
});
```

## Component Pattern

TerritoryData uses composition with specialized components:

- **TreasuryComponent** - Balance, income, expenses
- **TaxComponent** - Tax collection and rates
- **CosmeticComponent** - Icon, color, description
- **DiplomacyComponent** - Relations, alliances, vassals
- **WarComponent** - War goals and military actions

## Thread Safety

Territory operations are thread-safe:

- All storage operations use `CompletableFuture` for async access
- Use `getAsync()` methods for non-blocking reads
- Balance operations are atomic
- Member management is synchronized

## Async Patterns

### Correct: Async Access

```java
// Good: Non-blocking async access
TownDataStorage.getInstance().get(townId)
    .thenAccept(town -> {
        if (town != null) {
            double balance = town.getBalance();
        }
    });
```

### Incorrect: Blocking Access

```java
// Bad: Blocks the thread
TownData town = TownDataStorage.getInstance().getSync(townId);
```

## Best Practices

1. **Always use async methods** for storage operations to avoid blocking
2. **Check for null** when retrieving territories from storage
3. **Use transactions** for multi-step operations
4. **Handle exceptions** in async pipelines with `.exceptionally()`
5. **Validate permissions** before modifying territory data

## Contributing

When adding new public APIs:

1. Add comprehensive Javadoc with `@param`, `@return`, `@throws`
2. Include usage examples for complex operations
3. Document thread-safety guarantees
4. Update this README with new examples
5. Run `./gradlew javadoc` to verify no warnings

## Additional Resources

- [TerritoryAPIExamples.java](../tan-core/src/main/java/org/leralix/tan/examples/TerritoryAPIExamples.java) - Complete code examples
- [PRD](prd.md) - Product Requirements Document
- [Migration Report](MIGRATION_REPORT.md) - Async migration guide
