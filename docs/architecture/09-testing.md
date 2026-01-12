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

