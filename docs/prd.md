# Coconation - Product Requirements Document (PRD)
## Brownfield Quality Improvement Initiative

---

## Goals and Background Context

### Goals

- **Achieve 90/100 quality score** through systematic code modernization and cleanup
- **Eliminate all deprecated APIs and methods** to ensure long-term maintainability
- **Update to latest Folia 1.21.1+ API** for future-proofing and access to new features
- **Establish consistent code style** with automated formatting (Spotless)
- **Increase test coverage to 70%+** with progressive improvement roadmap
- **Complete or remove incomplete features** (quest/prestige/upgrade/progression systems)
- **Document all public APIs** with comprehensive Javadoc for third-party developers
- **Optimize performance bottlenecks** identified in production usage

### Background Context

Coconation (formerly Towns & Nations) is a mature Minecraft plugin (v2.0.0 in development) providing comprehensive territorial, diplomatic, and economic management for Folia/Paper servers. The plugin currently runs on **600+ source files** with mixed Java/Kotlin codebase and powers production servers with multi-server Redis synchronization.

**Current State Assessment** (from brownfield architecture analysis):
- **Strengths**: Excellent async architecture (Folia-native), enterprise-grade storage layer, comprehensive GUI system, production-ready Redis sync, recent component pattern refactoring
- **Weaknesses**: Outdated Folia API (1.20.1 vs required 1.21.1+), low test coverage (14%), 92 files with deprecated markers, incomplete features (quest/prestige systems), no code style enforcement

**Business Impact**:
- Plugin remains functional but **technical debt is accumulating**
- Outdated API blocks adoption of latest Folia features
- Low test coverage increases regression risk for refactoring
- Incomplete features create architectural confusion

**Why Now**:
The plugin is at a critical juncture where addressing technical debt now will prevent major rewrites later. With the recent component pattern refactoring (last 4 commits), the codebase structure is solid and ready for quality-focused improvements.

### Change Log

| Date | Version | Description | Author |
| ---- | ------- | ----------- | ------ |
| 2025-01-11 | 1.0 | Initial brownfield PRD for quality improvements | Winston (Architect) |

---

## Requirements

### Functional Requirements

**FR1**: The plugin MUST compile and run successfully on Folia 1.21.1+ API without warnings or errors
**FR2**: All deprecated methods, classes, and packages MUST be removed from the codebase (target: 0 deprecated items)
**FR3**: Code style MUST be automatically enforced with Spotless formatter for consistent formatting across all files
**FR4**: Test coverage MUST increase from 14% to minimum 20% in Phase 1, with roadmap to 70%+
**FR5**: Incomplete feature systems (quest/prestige/upgrade/progression) MUST be either completed with proper architecture OR completely removed
**FR6**: All public API methods MUST have Javadoc documentation with clear usage examples
**FR7**: Build process MUST enforce code quality gates (tests pass, coverage minimum, style checks pass)
**FR8**: Performance bottlenecks in GUI loading and territory operations MUST be identified and optimized
**FR9**: All external integrations (Vault, PlaceholderAPI, WorldGuard, Nexo) MUST use latest stable APIs
**FR10**: Developer documentation MUST be updated with new architecture patterns and best practices

### Non Functional Requirements

**NFR1**: Code Quality - Zero compiler warnings across entire codebase
**NFR2**: Maintainability - Any junior developer familiar with Java/Kotlin can understand and modify code within 30 minutes
**NFR3**: Testability - All new code must have unit tests before merge (enforced via PR checks)
**NFR4**: Performance - GUI operations must complete within 200ms for cached data, 500ms for database queries
**NFR5**: Backward Compatibility - Existing town/region/player data must migrate without manual intervention
**NFR6**: Folia Compliance - All operations must respect Folia's regionalized threading model (no blocking region threads)
**NFR7**: Multi-Server Sync - Redis synchronization must maintain consistency across servers with eventual consistency model
**NFR8**: API Stability - Public API changes must follow semantic versioning with deprecation periods
**NFR9**: Documentation - Every public class, method, and field must have Javadoc with @param, @return, @throws where applicable
**NFR10**: Build Time - Full build with tests must complete under 5 minutes on modern hardware

---

## User Interface Design Goals

### Overall UX Vision

**No UI/UX Changes Required**: This PRD focuses entirely on **internal code quality, technical debt removal, and developer experience improvements**. End-users (players and server admins) will experience:
- Faster GUI loading times (performance optimization)
- Fewer bugs (better test coverage)
- No breaking changes (backward compatibility maintained)

**Developer Experience Improvements**:
- Consistent code style across all files (Spotless)
- Clear API documentation (Javadoc)
- Comprehensive tests prevent regressions
- Modern APIs with up-to-date dependencies

### Core Screens and Views

**No Changes**: Existing GUI system (116 GUI files with Triumph GUI) remains unchanged. Performance optimizations will be transparent to users.

### Accessibility

Not applicable - This is a backend code quality improvement PRD with no UI/UX changes.

### Branding

Not applicable - No visual changes to plugin.

### Target Device and Platforms

**Platform**: Minecraft Folia/Paper 1.21.1+ servers
**Language**: Java 21 + Kotlin 2.1.0
**Build Tool**: Gradle 8.x with Kotlin DSL

---

## Technical Assumptions

### Repository Structure

**Monorepo with Multi-Module Gradle Layout**:
```
TownsAndNations-folia/
├── tan-api/          # Public API module (interfaces only)
├── tan-core/         # Main implementation (Java + Kotlin)
├── docs/             # Architecture and PRD documentation
└── build.gradle      # Root build configuration
```

**Rationale**: Monorepo simplifies refactoring across API and implementation modules while maintaining clear separation of concerns.

### Service Architecture

**Monolithic Plugin with Component Pattern**:
- Single plugin JAR with modular internal architecture
- Component pattern for territory data (immutable, thread-safe)
- Repository pattern for data access (DatabaseStorage abstraction)
- Async-first design for Folia compatibility

**Rationale**: Minecraft plugins are inherently monolithic (single JAR loaded by server). Component pattern provides internal modularity without microservice complexity.

### Testing Requirements

**Full Testing Pyramid**:
- **Unit Tests**: 70%+ target coverage (currently 14%)
- **Integration Tests**: Database operations, Redis synchronization
- **Manual Testing**: GUI workflows, complex multi-player scenarios
- **Test Infrastructure**: JUnit 5, MockBukkit, Mockito, TestContainers

**Progressive Roadmap**:
- Phase 1 (Week 1): 14% → 20% - Critical path tests
- Phase 2 (Month 1): 20% → 35% - Storage and economy tests
- Phase 3 (Month 2): 35% → 50% - Command handler tests
- Phase 4 (Month 3): 50% → 70% - GUI tests (currently excluded)
- Phase 5 (Quarter 2): 70%+ - Full coverage including edge cases

**Quality Gates**:
- Build fails if tests fail (`ignoreFailures = false`)
- Build fails if coverage below minimum (progressive thresholds)
- All PRs must pass tests before merge

### Additional Technical Assumptions and Requests

**Build Tool**:
- Gradle 8.x with Kotlin DSL (existing)
- Shadow plugin for fat JAR creation (existing)
- JaCoCo for code coverage measurement (existing)

**Code Quality Tools**:
- **Spotless** (NEW): Automatic code formatting
  - Google Java Format for Java files
  - ktlint for Kotlin files
  - Import ordering
  - Line length limits (120 characters)
- **Checkstyle** (OPTIONAL): Additional static analysis if Spotless insufficient

**Dependency Management**:
- Keep all dependencies up-to-date (dependabot or Renovate)
- Folia API: Update to 1.21.1+ immediately
- Review other dependencies for security updates

**Documentation**:
- **Javadoc** (ENHANCED): All public APIs documented
- **Architecture docs** (EXISTING): `docs/architecture.md` comprehensive
- **Developer guide** (NEW): Onboarding and contribution guidelines

**Performance Monitoring**:
- bStats integration (existing)
- Add performance benchmarking for critical paths (GUI loading, territory queries)

**Backward Compatibility**:
- Maintain database schema compatibility
- Migrate existing data without manual intervention
- Preserve public API stability (semantic versioning)

---

## Epic List

### Epic 1: Critical Infrastructure Fixes (Week 1)
**Goal**: Update dependencies and remove deprecated code to unblock future development

**Focus**: High-priority, high-impact fixes that clear technical debt blocking progress

**Epic 2: Code Quality Foundation (Week 2)
**Goal**: Establish automated code style enforcement and developer experience improvements

**Focus**: Tooling and standards that prevent future technical debt accumulation

**Epic 3: Feature Completion Decision (Week 3)
**Goal**: Complete or remove incomplete feature systems (quest/prestige/upgrade/progression)

**Focus**: Architectural decision and implementation for clean codebase

**Epic 4: Test Coverage Expansion - Phase 1 (Month 1)
**Goal**: Increase test coverage from 14% to 20% with critical path tests

**Focus**: Storage, economy, and territory data tests (foundational components)

**Epic 5: API Documentation (Month 1-2)
**Goal**: Document all public APIs with comprehensive Javadoc

**Focus**: Developer experience and third-party integrations

**Epic 6: Performance Optimization (Month 2)
**Goal**: Identify and optimize performance bottlenecks in GUI and territory operations

**Focus**: User-facing performance improvements (GUI loading, cache hit rates)

**Epic 7: Test Coverage Expansion - Phase 2 (Month 2-3)
**Goal**: Increase test coverage from 20% to 50% with command and integration tests

**Focus**: Command handlers, listeners, and integration tests

---

## Epic 1: Critical Infrastructure Fixes

### Expanded Goal

Update outdated dependencies and remove all deprecated code to eliminate technical debt blocking future development. This epic unblocks the ability to use latest Folia features, reduces codebase complexity, and ensures long-term maintainability. The fixes are high-impact but low-risk, addressing well-understood issues with clear solutions.

---

### Story 1.1: Update Folia API to 1.21.1+

**As a** plugin developer,
**I want** the plugin to compile and run on Folia 1.21.1+ API,
**so that** I can use the latest features and ensure future compatibility.

#### Acceptance Criteria

1. **Update build.gradle**: Change `folia-api:1.20.1-R0.1-SNAPSHOT` to `folia-api:1.21.1-R0.1-SNAPSHOT` (or latest)
2. **Successful compilation**: `./gradlew clean build` completes without errors
3. **No deprecated API usage**: Code audit confirms no use of deprecated 1.20.1 APIs
4. **Tests pass**: All existing tests pass after update
5. **Runtime compatibility**: Plugin loads successfully on Folia 1.21.1+ server
6. **Documentation updated**: README.md and build.gradle comments reflect new version

---

### Story 1.2: Remove GuiHelperBridge and Deprecated Utils Package

**As a** plugin maintainer,
**I want** all deprecated GUI utility code removed from the codebase,
**so that** developers don't accidentally use obsolete patterns.

#### Acceptance Criteria

1. **Remove GuiHelperBridge**: Delete `utils/gui/GuiHelperBridge.java`
2. **Remove all usages**: Replace all `GuiHelperBridge` calls with `AsyncGuiHelper`
3. **Remove deprecated package**: Delete `utils/deprecated/` directory entirely
   - `utils/deprecated/GuiUtil.java`
   - `utils/deprecated/HeadUtils.java`
4. **Tests updated**: Any tests using deprecated code updated to use new utilities
5. **No references remain**: Grep search confirms no references to removed classes
6. **Build succeeds**: Clean build completes without errors

---

### Story 1.3: Audit and Clean Remaining Deprecated Code

**As a** plugin developer,
**I want** a comprehensive audit of all remaining @Deprecated markers in the codebase,
**so that** we can create a removal plan for each deprecated item.

#### Acceptance Criteria

1. **Complete audit**: List all 92 files with @Deprecated/TODO/FIXME markers
2. **Categorize by priority**:
   - HIGH: Blocks 90/100 goal (API usage, critical paths)
   - MEDIUM: Code quality issues (style, inconsistency)
   - LOW: Nice-to-have improvements
3. **Create removal plan**: Document each deprecated item with:
   - File name and line number
   - Reason for deprecation
   - Recommended replacement
   - Estimated effort (hours)
   - Target epic/story for removal
4. **Document in wiki**: Add removal plan to `docs/DEPRECATED_REMOVAL_PLAN.md`
5. **Identify quick wins**: Mark items that can be removed in < 1 hour
6. **Present findings**: Review plan with stakeholders for prioritization

---

## Epic 2: Code Quality Foundation

### Expanded Goal

Establish automated code style enforcement and developer experience improvements that prevent future technical debt accumulation. This epic focuses on tooling and standards that make code quality automatic rather than manual, ensuring consistency across the 600+ file codebase and making onboarding easier for new contributors.

---

### Story 2.1: Configure Spotless Code Formatter

**As a** developer,
**I want** automatic code formatting on every build,
**so that** code style is consistent across all files without manual effort.

#### Acceptance Criteria

1. **Spotless plugin added**: Add to `build.gradle` with proper configuration
2. **Java formatting**: Google Java Format applied (4-space indentation, 120 char line limit)
3. **Kotlin formatting**: ktlint applied (consistent with Java style)
4. **Import ordering**: Automatic import organization and removal of unused imports
5. **License header**: Enforce consistent license header (if desired)
6. **Check task**: `./gradlew spotlessCheck` verifies formatting
7. **Apply task**: `./gradlew spotlessApply` auto-fixes formatting issues
8. **Git hooks** (optional): Pre-commit hook runs `spotlessApply`
9. **CI integration**: Build fails if formatting incorrect
10. **Documentation**: Update README with Spotless usage instructions

---

### Story 2.2: Enforce Code Style in Build Process

**As a** maintainer,
**I want** the build to fail if code style is incorrect,
**so that** all merged code follows consistent formatting.

#### Acceptance Criteria

1. **Build integration**: `./gradlew build` depends on `spotlessCheck`
2. **Clear error messages**: Spotless violations show file paths and suggested fixes
3. **CI/CD integration**: GitHub Actions / GitLab CI runs Spotless checks
4. **PR blocking**: Pull requests fail style checks before merge review
5. **Quick fix command**: Documented `./gradlew spotlessApply` for developers
6. **Gradle task performance**: Spotless checks complete in < 30 seconds
7. **Exclude list**: Document any files excluded from formatting (if necessary)
8. **Team notification**: Notify team of new style enforcement via README

---

### Story 2.3: Establish Code Review Checklist

**As a** code reviewer,
**I want** a standardized checklist for pull request reviews,
**so that** all code quality aspects are consistently evaluated.

#### Acceptance Criteria

1. **Checklist created**: Document PR review checklist in `docs/CODE_REVIEW_CHECKLIST.md`
2. **Checklist items include**:
   - Code style (Spotless passing)
   - Test coverage (new code has tests)
   - No deprecated API usage
   - Javadoc on public APIs
   - Folia compliance (no blocking region threads)
   - Backward compatibility (data migration considered)
   - Performance impact (no obvious regressions)
3. **Template created**: GitHub PR template includes checklist
4. **Team training**: Brief document explaining checklist usage
5. **Integration**: Checklist linked in CONTRIBUTING.md
6. **Enforcement**: PRs not meeting checklist criteria are not merged

---

## Epic 3: Feature Completion Decision

### Expanded Goal

Make an architectural decision and execute completion or removal of incomplete feature systems (quest/prestige/upgrade/progression). These systems are partially implemented but not production-ready, creating architectural confusion. This epic resolves the ambiguity by either completing them with proper architecture and tests, or removing them entirely for a clean codebase.

---

### Story 3.1: Assess Incomplete Features

**As a** project maintainer,
**I want** a comprehensive assessment of incomplete feature systems,
**so that** I can make an informed completion/removal decision.

#### Acceptance Criteria

1. **Analyze quest system**: Review `domain/quest/`, `gui/user/quest/`, `service/quest/`
   - Current functionality: What works?
   - Missing functionality: What's incomplete?
   - Code quality: Is architecture sound?
   - Test coverage: What tests exist?
   - Integration points: What depends on this?
   - Effort to complete: Estimated hours
2. **Analyze prestige system**: Same assessment for `domain/prestige/`, `gui/user/prestige/`
3. **Analyze upgrade system**: Same assessment for `domain/upgrade/`, `gui/user/upgrade/`
4. **Analyze progression system**: Same assessment for `dataclass/territory/progression/`
5. **User demand**: Research if players/requesters want these features
6. **Strategic value**: Assess if features align with plugin's core goals
7. **Recommendation document**: Create `docs/FEATURE_ASSESSMENT.md` with:
   - Pros/cons of completion
   - Pros/cons of removal
   - Recommended path for each system
   - Effort estimates
8. **Stakeholder review**: Present assessment for decision

---

### Story 3.2: Execute Decision - Complete or Remove Features

**As a** project lead,
**I want** to either complete incomplete features or remove them entirely,
**so that** the codebase has no half-implemented systems.

#### Acceptance Criteria (Option A: Complete Features)

1. **Architecture designed**: Proper architecture for each system (domain, service, storage, GUI)
2. **Tests written**: Unit tests for domain logic, integration tests for persistence
3. **GUI implemented**: Complete, tested GUI for each system
4. **Documentation**: User guide and developer docs for new features
5. **Code review**: All code passes review checklist
6. **Tests pass**: 100% coverage for new code
7. **Performance**: No performance regressions
8. **Release notes**: Document new features in changelog

#### Acceptance Criteria (Option B: Remove Features)

1. **Delete directories**: Remove all feature code:
   - `domain/quest/`, `domain/prestige/`, `domain/upgrade/`
   - `gui/user/quest/`, `gui/user/prestige/`, `gui/user/upgrade/`
   - `service/quest/`, `service/prestige/`, `service/upgrade/`
   - `dataclass/territory/progression/`
2. **Remove references**: Grep and remove any imports or references
3. **Tests updated**: Remove any tests for deleted features
4. **Build succeeds**: Clean build completes
5. **Tests pass**: All remaining tests pass
6. **Documentation updated**: Remove feature mentions from docs
7. **Commit message**: Clear commit explaining removal rationale

**Note**: This story will be executed based on decision from Story 3.1. Either Option A or Option B acceptance criteria will apply, not both.

---

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

---

## Epic 5: API Documentation (Month 1-2)

### Expanded Goal

Document all public APIs with comprehensive Javadoc to improve developer experience and support third-party integrations. Well-documented APIs reduce support burden, attract contributors, and enable plugin ecosystem growth.

---

### Story 5.1: Document Core Territory APIs

**As a** third-party developer,
**I want** Javadoc on all territory-related public APIs,
**so that** I can integrate with town/region data correctly.

#### Acceptance Criteria

1. **TerritoryData documented**: All public methods with Javadoc
   - Class-level explanation of component pattern
   - @param, @return, @throws where applicable
   - Thread-safety guarantees documented
   - Usage examples for common operations
2. **TownData documented**: Town-specific methods documented
3. **RegionData documented**: Region-specific methods documented
4. **Component interfaces documented**: All component APIs
5. **Examples provided**: Code examples for:
   - Creating a town
   - Adding members
   - Claiming chunks
   - Collecting taxes
6. **Javadoc compiles**: `./gradlew javadoc` succeeds without warnings
7. **HTML generated**: Javadoc HTML viewable and readable

---

### Story 5.2: Document Economy APIs

**As a** third-party developer,
**I want** Javadoc on all economy-related public APIs,
**so that** I can integrate with the economy system correctly.

#### Acceptance Criteria

1. **AbstractTanEcon documented**: All methods with Javadoc
2. **EconomyUtil documented**: Facade methods documented
3. **Vault integration documented**: How to use TAN economy with Vault
4. **Examples provided**: Code examples for:
   - Checking player balance
   - Transferring money
   - Creating custom transactions
   - Integrating with external economies
5. **Thread-safety documented**: When synchronization is needed
6. **Javadoc compiles**: No warnings
7. **HTML generated**: Documentation viewable

---

### Story 5.3: Document Storage and Data Access APIs

**As a** third-party developer,
**I want** Javadoc on all storage-related public APIs,
**so that** I can query and modify plugin data correctly.

#### Acceptance Criteria

1. **DatabaseStorage documented**: Generic CRUD methods
2. **PlayerDataStorage documented**: Player-specific queries
3. **TownDataStorage documented**: Town-specific queries
4. **RegionDataStorage documented**: Region-specific queries
5. **Async patterns documented**: How to use CompletableFuture correctly
6. **Cache behavior documented**: How caching works, when to bypass
7. **Examples provided**: Code examples for:
   - Loading player data async
   - Querying towns by name
   - Batching queries for performance
8. **Javadoc compiles**: No warnings
9. **HTML generated**: Documentation viewable

---

### Story 5.4: Create Developer Guide

**As a** new contributor,
**I want** a comprehensive developer guide,
**so that** I can understand the plugin architecture and contribute effectively.

#### Acceptance Criteria

1. **Developer guide created**: `docs/DEVELOPER_GUIDE.md`
2. **Architecture overview**: High-level system design
3. **Component pattern explained**: How immutable components work
4. **Async patterns explained**: Folia threading model and best practices
5. **Storage layer explained**: Database, caching, and data flow
6. **Code style guide**: Spotless configuration and conventions
7. **Testing guide**: How to write and run tests
8. **Contributing guide**: PR process, code review checklist
9. **Troubleshooting**: Common issues and solutions
10. **Links to Javadoc**: Hyperlinked API references

---

## Epic 6: Performance Optimization (Month 2)

### Expanded Goal

Identify and optimize performance bottlenecks in GUI loading and territory operations to improve user experience. Performance improvements are transparent to end-users but noticeable in faster GUI response times and smoother server performance under load.

---

### Story 6.1: Profile GUI Performance

**As a** developer,
**I want** to identify performance bottlenecks in GUI operations,
**so that** I can optimize the slowest paths.

#### Acceptance Criteria

1. **Profiling tool set up**: Java Flight Recorder or VisualVM configured
2. **Benchmarks created**: Measure GUI opening times for:
   - MainMenu
   - TownMenu (large town, 100+ members)
   - RegionMenu (large region, 10+ towns)
   - PropertyMenu (many properties)
3. **Bottlenecks identified**: List top 5 slowest operations
4. **Database queries identified**: Identify N+1 query problems
5. **Cache hit rates measured**: Current cache effectiveness
6. **Profiling report**: Document findings in `docs/PERFORMANCE_PROFILING.md`
7. **Baseline metrics**: Establish performance baseline for comparison

---

### Story 6.2: Optimize GUI Data Loading

**As a** player,
**I want** GUI menus to open quickly,
**so that** I don't experience lag when managing my town.

#### Acceptance Criteria

1. **Implement lazy loading**: Load GUI data progressively, not all at once
2. **Add pagination**: Large lists (members, chunks) paginated
3. **Prefetch data**: AsyncGuiHelper prefetches commonly accessed data
4. **Reduce database queries**: Batch queries, use JOINs where possible
5. **Optimize cache strategy**: Increase cache size, adjust TTLs
6. **Performance targets**:
   - Small GUI (< 10 items): < 100ms
   - Medium GUI (10-50 items): < 200ms
   - Large GUI (50+ items): < 500ms
7. **Before/after benchmarks**: Document performance improvements
8. **No regressions**: All functionality still works correctly

---

### Story 6.3: Optimize Territory Loading

**As a** server administrator,
**I want** territory data to load efficiently,
**so that** server performance is good under load.

#### Acceptance Criteria

1. **Review TerritoryLazyLoader**: Identify optimization opportunities
2. **Reduce synchronized blocks**: Minimize lock contention
3. **Implement read-write locks**: Allow concurrent reads
4. **Optimize lazy loading**: Batch territory preloading
5. **Cache warming**: Preload commonly accessed territories
6. **Memory optimization**: Reduce memory footprint per territory
7. **Performance targets**:
   - Territory load from cache: < 10ms
   - Territory load from database: < 100ms
   - Batch load (10 territories): < 500ms
8. **Load testing**: Test with 1000+ territories
9. **Before/after benchmarks**: Document improvements

---

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

---

## Checklist Results Report

**Note**: This section will be populated after executing the PM checklist, prior to finalizing the PRD.

---

## Next Steps

### UX Expert Prompt

**Not Required**: This PRD focuses on internal code quality with no UI/UX changes. UX expertise not needed for this brownfield quality improvement initiative.

### Architect Prompt

**Create Brownfield Architecture Document**

Based on this PRD, create a brownfield architecture document (`docs/brownfield-architecture.md`) that addresses the quality improvement roadmap:

1. **Critical Infrastructure Fixes**:
   - Document Folia API 1.21.1+ upgrade path
   - Detail deprecated code removal strategy
   - Identify potential breaking changes

2. **Code Quality Foundation**:
   - Specify Spotless configuration details
   - Define code style standards (indentation, line length, imports)
   - Design code review checklist integration

3. **Feature Completion Decision**:
   - Analyze incomplete systems (quest/prestige/upgrade)
   - Provide architecture for completion OR removal plan
   - Assess impact on existing codebase

4. **Test Coverage Expansion**:
   - Identify test infrastructure needs (mocking, test data)
   - Design test patterns for async/Folia code
   - Plan integration test architecture

5. **Performance Optimization**:
   - Profile current bottlenecks
   - Design caching improvements
   - Specify lazy loading strategy

6. **Maintain backward compatibility** throughout all improvements
7. **Ensure Folia compliance** in all architectural changes
8. **Document migration paths** for any breaking changes

Focus on practical, implementation-ready guidance that enables developers to execute this PRD efficiently.
