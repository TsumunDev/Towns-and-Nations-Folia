## Quality Improvement Roadmap

### Goal: Achieve 90/100 Quality Score

#### Priority 1: Code Cleanliness (CRITICAL)

**1.1 Remove Deprecated Code** ⚠️ HIGH PRIORITY
- [ ] Remove `GuiHelperBridge.java` and all usages
- [ ] Remove `utils/deprecated/` package
- [ ] Audit all 92 files with @Deprecated markers
- [ ] Create removal plan for each deprecated item

**1.2 Fix Folia API Version** ⚠️ HIGH PRIORITY
- [ ] Update `build.gradle`: `folia-api:1.20.1` → `1.21.1+`
- [ ] Test on latest Folia version
- [ ] Update documentation

**1.3 Standardize Code Style**
- [ ] Configure Spotless or Checkstyle
- [ ] Enforce consistent formatting
- [ ] Add import organization
- [ ] Set line length limits

#### Priority 2: API Modernization (HIGH)

**2.1 Review All API Usage**
- [ ] Audit for deprecated Bukkit/Paper APIs
- [ ] Replace with modern alternatives
- [ ] Update to latest Folia APIs

**2.2 Improve Type Safety**
- [ ] Add `@Nullable`/`@NonNull` annotations
- [ ] Use Java 21 records where appropriate
- [ ] Leverage Kotlin nullability

**2.3 Documentation**
- [ ] Javadoc all public APIs
- [ ] Document thread-safety guarantees
- [ ] Add usage examples

#### Priority 3: Future-Proofing (HIGH)

**3.1 Architecture Cleanup**
- [ ] Decide: Complete or remove incomplete features (quest/prestige/upgrade)
- [ ] If remove: Delete `domain/quest/`, `domain/prestige/`, `domain/upgrade/`
- [ ] If keep: Complete with proper architecture and tests

**3.2 Improve Test Coverage**
- [ ] Phase 2: 14% → 20% (expand storage and economy tests)
- [ ] Phase 3: 20% → 35% (cover command handlers)
- [ ] Phase 4: 35% → 50% (include GUI tests)
- [ ] Phase 5: 50% → 70%+ (full coverage)

**3.3 Performance Optimization**
- [ ] Profile GUI loading times
- [ ] Implement lazy loading for large datasets
- [ ] Optimize cache hit rates
- [ ] Add performance monitoring

**3.4 Error Handling**
- [ ] Standardize error handling patterns
- [ ] Improve error messages for users
- [ ] Add comprehensive logging
- [ ] Implement circuit breakers for external calls

### Success Metrics

**Code Quality**:
- Zero compiler warnings
- Zero deprecated API usage
- Consistent code style (Spotless passing)
- 70%+ test coverage

**API Quality**:
- All public APIs documented with Javadoc
- Thread-safety clearly documented
- Usage examples provided

**Future-Proofing**:
- Clean architecture (no incomplete features)
- Comprehensive test coverage
- Performance benchmarks passing
- Monitoring and alerting in place

