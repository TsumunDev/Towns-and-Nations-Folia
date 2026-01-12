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

