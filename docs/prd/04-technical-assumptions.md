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

