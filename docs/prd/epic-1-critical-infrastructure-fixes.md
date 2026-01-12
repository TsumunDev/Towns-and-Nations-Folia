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

