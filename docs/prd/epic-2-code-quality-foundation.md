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

