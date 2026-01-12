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

