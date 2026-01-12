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

