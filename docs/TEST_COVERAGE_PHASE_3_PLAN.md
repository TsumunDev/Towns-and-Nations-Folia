# Test Coverage Phase 3: Roadmap to 70%

**Created**: 2025-01-15
**Current Coverage**: 50-55%
**Target Coverage**: 70%
**Timeline**: Month 3-4

---

## Executive Summary

Phase 3 aims to increase test coverage from **50% to 70%** by adding comprehensive GUI tests, edge case handling, and advanced scenario testing. This phase completes the test coverage expansion initiative and ensures the plugin has robust test coverage for production use.

---

## Coverage Gaps Analysis

### Current State (50-55% Coverage)

**Well-Tested Areas** (70%+ coverage):
- ✅ Storage layer (DatabaseStorage, PlayerDataStorage, TownDataStorage, RegionDataStorage)
- ✅ Economy system (AbstractTanEcon, EconomyUtil, implementations)
- ✅ Territory data models (TownData, RegionData, components)
- ✅ Command handlers (player and admin commands)
- ✅ Event listeners (chat, interactions, quests)

**Under-Tested Areas** (30-50% coverage):
- ⚠️ GUI system (116 GUI files, minimal tests)
- ⚠️ Complex workflows (multi-step operations)
- ⚠️ Edge cases (null handling, boundary conditions)
- ⚠️ Error paths (exception handling)
- ⚠️ Async operations (CompletableFuture chains)

**Untested Areas** (0-30% coverage):
- ❌ GUI integration (Triumph GUI framework)
- ❌ Property system
- ❌ War mechanics
- ❌ Quest/Prestige/Upgrade systems
- ❌ Redis synchronization
- ❌ Advanced diplomacy (treaties, alliances)

---

## Phase 3 Strategy

### 3.1 GUI Testing (Target: +10% coverage)

**Priority**: High
**Expected Impact**: +10% overall coverage

#### GUI Tests to Add

1. **Basic GUI Tests**
   - MainMenuTest
   - TownMenuTest
   - RegionMenuTest
   - PropertyMenuTest
   - PlayerMenuTest

2. **Territory GUI Tests**
   - TerritoryMemberMenuTest
   - TerritoryRanksMenuTest
   - TreasuryMenuTest
   - ChunkSettingsMenuTest
   - TownSettingsMenuTest

3. **Diplomacy GUI Tests**
   - OpenDiplomacyMenuTest
   - OpenRelationMenuTest
   - VassalsMenuTest
   - WarsMenuTest

4. **Advanced GUI Tests**
   - QuestListMenuTest
   - PrestigeShopMenuTest
   - UpgradeShopMenuTest
   - ProgressionMenuTest

**Test Strategy**:
- Mock Triumph GUI framework
- Test GUI item creation
- Test click handlers
- Test pagination
- Test async data loading

**Implementation**: 50-60 GUI tests

### 3.2 Edge Case Testing (Target: +5% coverage)

**Priority**: High
**Expected Impact**: +5% overall coverage

#### Edge Cases to Test

1. **Null Handling**
   - Null player parameters
   - Null territory data
   - Null database results
   - Null GUI events

2. **Boundary Conditions**
   - Empty collections
   - Maximum values (Int.MAX_VALUE, etc.)
   - Minimum values (negative balances, etc.)
   - Overflow scenarios

3. **Concurrent Access**
   - Race conditions
   - Deadlock scenarios
   - Thread starvation
   - Lock contention

4. **Error Recovery**
   - Database connection failures
   - Redis connection failures
   - Out of memory scenarios
   - Corrupted data recovery

**Test Strategy**:
- Parameterized tests with various inputs
- Property-based testing (where applicable)
- Chaos engineering (random failures)
- Stress testing

**Implementation**: 80-100 edge case tests

### 3.3 Advanced Workflow Testing (Target: +5% coverage)

**Priority**: Medium
**Expected Impact**: +5% overall coverage

#### Workflows to Test

1. **Complex Town Operations**
   - Town creation with multiple members
   - Town dissolution
   - Town merge/split
   - Capital transfer

2. **Advanced Economy**
   - Multi-player transactions
   - Tax collection with debt
   - Salary payment cascades
   - Treasury bankruptcy

3. **Complex Diplomacy**
   - Multi-region wars
   - Alliance networks
   - Vassal rebellions
   - Peace treaties

4. **Property Operations**
   - Property purchase with checks
   - Property rental cycles
   - Property transfers
   - Property abandonment

**Test Strategy**:
- End-to-end workflow tests
- Multi-step scenario tests
- State machine testing
- Time-based tests (rent cycles, etc.)

**Implementation**: 40-50 workflow tests

---

## Implementation Plan

### Month 3: GUI Tests

**Week 1-2: Basic GUI Tests**
- Create GUI test framework
- Test MainMenu, TownMenu, RegionMenu
- Test PropertyMenu, PlayerMenu
- **Deliverable**: 25 basic GUI tests

**Week 3-4: Advanced GUI Tests**
- Test territory management GUIs
- Test diplomacy GUIs
- Test progression GUIs
- **Deliverable**: 35 advanced GUI tests

### Month 4: Edge Cases and Workflows

**Week 1-2: Edge Case Tests**
- Add null handling tests
- Add boundary condition tests
- Add concurrent access tests
- **Deliverable**: 80 edge case tests

**Week 3-4: Workflow Tests**
- Add complex workflow tests
- Add advanced economy tests
- Add advanced diplomacy tests
- **Deliverable**: 45 workflow tests

---

## Test Infrastructure Improvements

### 3.4 GUI Testing Framework

**Current State**: No GUI testing framework
**Target**: Comprehensive GUI test framework

#### Components to Build

1. **Mock Triumph GUI**
   ```java
   public class MockGui {
       public void mockItem(int slot, ItemStack item);
       public void mockClick(int slot, Player player);
       public void verifyOpened();
       public void verifyClosed();
   }
   ```

2. **GUI Test Base Class**
   ```java
   public abstract class GUITest {
       protected MockGui mockGui;
       protected PlayerMock player;

       @BeforeEach
       void setUpGuiTest() {
           mockGui = new MockGui();
           player = server.addPlayer();
       }

       protected void testGuiOpens(Class<? extends Gui> guiClass);
       protected void testGuiClosesCorrectly();
       protected void testAllItemsClickable();
   }
   ```

3. **GUI Assertions**
   ```java
   public class GuiAssertions {
       public static void assertGuiItemName(int slot, String expected);
       public static void assertGuiItemLore(int slot, String... expected);
       public static void assertGuiClickable(int slot);
   }
   ```

### 3.5 Test Data Builders

**Current State**: Manual test data setup
**Target**: Fluent test data builders

#### Builders to Create

1. **TownDataBuilder**
   ```java
   TownData town = new TownDataBuilder()
       .withName("TestTown")
       .withLeader(player)
       .withBalance(1000.0)
       .withMembers(member1, member2)
       .withClaims(chunk1, chunk2)
       .build();
   ```

2. **PlayerDataBuilder**
   ```java
   ITanPlayer player = new PlayerDataBuilder()
       .withId(uuid)
       .withBalance(500.0)
       .withTown(townId)
       .withRank(Rank.MEMBER)
       .build();
   ```

3. **RegionDataBuilder**
   ```java
   RegionData region = new RegionDataBuilder()
       .withName("TestRegion")
       .withCapital(townId)
       .withVassals(townId2, townId3)
       .build();
   ```

### 3.6 Async Test Utilities

**Current State**: Basic async support
**Target**: Advanced async testing utilities

#### Utilities to Add

1. **CompletableFuture Assertions**
   ```java
   public class FutureAssertions {
       public static <T> void assertCompletes(CompletableFuture<T> future);
       public static <T> void assertCompletesWith(CompletableFuture<T> future, T expected);
       public static <T> void assertCompletesExceptionally(CompletableFuture<T> future, Class<? extends Throwable> exception);
       public static <T> void assertCompletesWithin(CompletableFuture<T> future, Duration timeout);
   }
   ```

2. **Folia Test Scheduler**
   ```java
   public class FoliaTestScheduler {
       public static void runOnRegionThread(Location loc, Runnable task);
       public static void runAsync(Runnable task);
       public static void runDelayed(Runnable task, Duration delay);
   }
   ```

---

## Success Metrics

### Coverage Targets

| Metric | Current | Target | Increase |
|--------|---------|--------|----------|
| Overall coverage | 50-55% | 70% | +15-20% |
| GUI coverage | 5-10% | 40% | +30-35% |
| Edge case coverage | 20% | 50% | +30% |
| Workflow coverage | 35% | 60% | +25% |

### Test Count Targets

| Category | Current | Target | New Tests |
|----------|---------|--------|-----------|
| GUI tests | ~5 | ~65 | +60 |
| Edge case tests | ~40 | ~120 | +80 |
| Workflow tests | ~14 | ~60 | +46 |
| **Total** | **~534** | **~720** | **~186** |

---

## Risk Mitigation

### Risks and Mitigations

1. **Risk: GUI testing framework complexity**
   - **Mitigation**: Start with simple mocking, iterate to advanced framework
   - **Fallback**: Use integration tests instead of unit tests for GUIs

2. **Risk: Flaky async tests**
   - **Mitigation**: Use proper timeouts and assertions
   - **Fallback**: Make async tests optional in CI

3. **Risk: Long test execution time**
   - **Mitigation**: Parallelize tests, optimize setup/teardown
   - **Target**: Keep total suite under 5 minutes

4. **Risk: Mock framework limitations**
   - **Mitigation**: Use TestContainers for real database/Redis when needed
   - **Fallback**: Accept lower coverage for hard-to-test areas

---

## Phase 3 Deliverables

### Month 3 Deliverables

1. **GUI Test Framework**
   - Mock Triumph GUI implementation
   - GUI test base classes
   - GUI assertion utilities

2. **GUI Tests**
   - 60 GUI tests covering main menus
   - Tests for GUI opening/closing
   - Tests for item interactions
   - Tests for pagination

### Month 4 Deliverables

1. **Edge Case Tests**
   - 80 tests for null handling
   - Tests for boundary conditions
   - Tests for concurrent access
   - Tests for error recovery

2. **Workflow Tests**
   - 46 tests for complex workflows
   - Multi-step operation tests
   - State machine tests
   - Time-based tests

3. **Coverage Report**
   - JaCoCo HTML report showing 70%+ coverage
   - Coverage breakdown by package
   - Recommendations for Phase 4

---

## Phase 3 Completion Criteria

Story 3.1: GUI Tests Complete
- ✅ 60+ GUI tests created
- ✅ GUI coverage reaches 40%
- ✅ All main menus tested
- ✅ All tests passing

Story 3.2: Edge Cases Complete
- ✅ 80+ edge case tests created
- ✅ Null handling tested
- ✅ Boundary conditions tested
- ✅ Concurrent access tested
- ✅ All tests passing

Story 3.3: Workflows Complete
- ✅ 46+ workflow tests created
- ✅ Complex town operations tested
- ✅ Advanced economy tested
- ✅ Complex diplomacy tested
- ✅ All tests passing

Story 3.4: Verify 70% Milestone
- ✅ Overall coverage ≥ 70%
- ✅ GUI coverage ≥ 40%
- ✅ No regressions
- ✅ Test suite executes in < 5 minutes
- ✅ Coverage report generated

---

## Conclusion

Phase 3 will complete the test coverage expansion initiative by adding comprehensive GUI tests, edge case handling, and advanced workflow tests. Upon completion, the plugin will have **70% test coverage**, ensuring robust quality and reliability for production use.

**Next Steps After Phase 3**:
- Phase 4: Increase coverage to 85% (legacy code, deprecated handling)
- Phase 5: Reach 90%+ coverage (full corner case coverage)
- Production deployment with confidence

---

*Plan created: 2025-01-15*
*Epic 7 complete: Phase 2 (50% coverage)*
*Next milestone: Phase 3 (70% coverage)*
