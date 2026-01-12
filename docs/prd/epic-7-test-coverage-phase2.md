## Epic 7: Test Coverage Expansion - Phase 2 (Month 2-3)

### Expanded Goal

Increase test coverage from 20% to 50% by adding tests for command handlers, event listeners, and integration scenarios. This expands coverage beyond foundational components into user-facing functionality, ensuring commands work correctly and events are handled properly.

---

### Story 7.1: Add Command Handler Tests

**As a** developer,
**I want** tests for all player and admin commands,
**so that** command changes don't introduce bugs.

#### Acceptance Criteria

1. **Player command tests**: Test all `/ccn` commands
   - `ccn gui`, `ccn claim`, `ccn unclaim`
   - `ccn town`, `ccn invite`, `ccn join`
   - `ccn pay`, `ccn balance`
   - `ccn map`, `ccn spawn`
2. **Admin command tests**: Test all `/ccnadmin` commands
   - `ccnadmin addmoney`, `ccnadmin setmoney`
   - `ccnadmin sudo`, `ccnadmin unclaim`
3. **Error cases**: Test invalid arguments, permissions, edge cases
4. **Integration with Folia**: Test thread-safety of command execution
5. **Coverage target**: Commands package achieves 40%+ coverage
6. **All tests pass**: No regressions

---

### Story 7.2: Add Listener Tests

**As a** developer,
**I want** tests for all event listeners,
**so that** event handling is correct.

#### Acceptance Criteria

1. **PlayerJoinListener tests**: Test player join logic
2. **ChatListener tests**: Test chat input handling
3. **RightClickListener tests**: Test right-click interactions
4. **MobSpawnListener tests**: Test mob spawn protection
5. **Quest listeners** (if keeping): Test quest event handling
6. **Error cases**: Test null checks, edge cases
7. **Coverage target**: Listeners package achieves 40%+ coverage
8. **All tests pass**: No regressions

---

### Story 7.3: Add Integration Tests

**As a** developer,
**I want** integration tests for complex workflows,
**so that** multi-component scenarios work correctly.

#### Acceptance Criteria

1. **Town creation workflow**: Test end-to-end town creation
2. **Economy workflow**: Test money transfer, taxes, salaries
3. **Diplomacy workflow**: Test alliance creation, wars
4. **Multi-server sync**: Test Redis synchronization (TestContainers for Redis)
5. **Database integration**: Test with real MySQL/SQLite (TestContainers)
6. **Coverage target**: Overall coverage reaches 50%+
7. **All tests pass**: No regressions
8. **Performance tests**: Complete in reasonable time

---

### Story 7.4: Verify Coverage Milestone (50%)

**As a** project maintainer,
**I want** to verify that we've achieved 50% test coverage,
**so that** we can mark Phase 2 complete.

#### Acceptance Criteria

1. **Run JaCoCo**: `./gradlew test jacocoTestReport`
2. **Coverage measured**: Overall coverage is ≥ 50%
3. **No regressions**: All previously passing tests still pass
4. **New tests documented**: List new test classes
5. **Coverage report**: Generate HTML report
6. **Update build.gradle**: Adjust minimum coverage to 0.50
7. **Milestone celebrated**: Document achievement
8. **Plan Phase 3**: Roadmap to 70% coverage (GUI tests, edge cases)

