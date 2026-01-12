## Epic 4: Test Coverage Expansion - Phase 1 (Month 1)

### Expanded Goal

Increase test coverage from 14% to 20% by focusing on critical path components: storage layer, economy system, and territory data models. These foundational components are used throughout the codebase, so testing them provides high value and enables safer refactoring in future epics.

---

### Story 4.1: Expand Storage Layer Tests

**As a** developer,
**I want** comprehensive tests for the storage layer,
**so that** I can refactor database code with confidence.

#### Acceptance Criteria

1. **DatabaseStorage tests**: Test generic CRUD operations
   - Create, read, update, delete operations
   - Cache hit/miss scenarios
   - Async operations (CompletableFuture)
   - Batch operations
   - Error handling (connection failures)
2. **PlayerDataStorage tests**: Test player-specific storage
   - Balance updates
   - Town membership changes
   - Cache invalidation
3. **TownDataStorage tests**: Test town storage
   - Town creation/deletion
   - Component updates (economy, diplomacy)
   - Chunk claim updates
4. **RegionDataStorage tests**: Test region storage
   - Region creation/deletion
   - Vassal relationship updates
   - Capital changes
5. **Integration tests**: Use TestContainers for real MySQL/SQLite
6. **Coverage target**: Storage package achieves 60%+ coverage
7. **All tests pass**: New tests don't break existing tests
8. **Performance**: Tests complete in < 2 minutes

---

### Story 4.2: Expand Economy System Tests

**As a** developer,
**I want** comprehensive tests for the economy system,
**so that** financial transactions are bug-free.

#### Acceptance Criteria

1. **AbstractTanEcon tests**: Test economy abstraction
   - Deposit/withdraw operations
   - Balance checks
   - Currency formatting
2. **TanEconomyStandalone tests**: Test internal economy
   - Player balance management
   - Transaction history
   - Overflow/underflow protection
3. **TanEconomyExternal tests**: Test Vault wrapper
   - Vault API integration
   - Graceful fallback when Vault unavailable
4. **TanEconomyVault tests**: Test Vault provider
   - Economy provider registration
   - External plugin access
5. **EconomyUtil tests**: Test economy facade
   - Proper delegation to implementations
   - Null safety
6. **Edge cases**: Negative balances, max money, concurrent transactions
7. **Coverage target**: Economy package achieves 50%+ coverage
8. **All tests pass**: No regressions in existing tests

---

### Story 4.3: Expand Territory Data Model Tests

**As a** developer,
**I want** comprehensive tests for territory data models,
**so that** territorial logic is correct and thread-safe.

#### Acceptance Criteria

1. **TerritoryData tests**: Test abstract territory base
   - Component pattern immutability
   - Thread safety of concurrent access
   - Component updates create new instances
2. **TownData tests**: Test town-specific logic
   - Member management
   - Rank assignments
   - Property ownership
3. **RegionData tests**: Test region-specific logic
   - Vassal relationships
   - Capital management
   - Inter-town diplomacy
4. **Component tests**: Test each component type
   - TreasuryComponent: Balance calculations
   - TaxComponent: Tax collection logic
   - DiplomacyComponent: Relation management
   - WarComponent: War goal tracking
5. **Concurrency tests**: Multi-threaded territory access
6. **Serialization tests**: Gson round-trip serialization
7. **Coverage target**: Territory package achieves 40%+ coverage
8. **All tests pass**: No regressions

---

### Story 4.4: Verify Coverage Milestone (20%)

**As a** project maintainer,
**I want** to verify that we've achieved 20% test coverage,
**so that** we can mark Phase 1 complete.

#### Acceptance Criteria

1. **Run JaCoCo**: `./gradlew test jacocoTestReport`
2. **Coverage measured**: Overall coverage is ≥ 20%
3. **No regressions**: All previously passing tests still pass
4. **New tests documented**: List new test classes in coverage report
5. **Coverage report**: Generate HTML report at `tan-core/build/reports/jacoco/test/html/index.html`
6. **Update build.gradle**: Adjust minimum coverage to 0.20
7. **Milestone celebrated**: Document achievement in project changelog
8. **Plan Phase 2**: Identify next areas for coverage improvement

