# Deprecated Code Removal Plan

**Story**: 1.3 - Audit and Clean Remaining Deprecated Code
**Generated**: 2025-01-11
**Status**: Draft - Ready for Review

---

## Executive Summary

**Audit Results**:
- **@Deprecated markers found**: 11 occurrences
- **TODO markers found**: 15 occurrences
- **FIXME markers found**: 0 occurrences
- **Total files affected**: 27 files

**Note**: The original estimate of "92 files" in architecture docs was inaccurate. The actual count is significantly lower.

---

## Priority Classification

### Priority Levels

| Priority | Description | Criteria |
|----------|-------------|----------|
| **HIGH** | Blocks 90/100 goal | API usage, critical paths, blocking operations |
| **MEDIUM** | Code quality issues | Style, inconsistency, deprecated patterns |
| **LOW** | Nice-to-have | Minor improvements, legacy compatibility |

---

## HIGH Priority Items

### 1. Synchronous Storage Methods (`getSync`, `getAllSync`)

**Status**: ⚠️ **CRITICAL - Blocks Folia Compliance**

**Locations**:
- `DatabaseStorage.java:155-158` - `getAll()` method
- `NewClaimedChunkStorage.java:47-57` - `getSync()` method

**Problem**: These methods block the calling thread while waiting for database I/O, violating Folia's regionalized threading model.

**Impact**: 100+ usage locations across the codebase

**Usage Breakdown**:
- Production code: ~70 usages (EconomyUtil, TownData, RegionData, etc.)
- Test code: ~30 usages (Integration tests, storage tests)

**Removal Strategy**:
See detailed migration plan in `docs/ASYNC_MIGRATION_PLAN.md`

1. Phase 1: Economy layer + Folia async utilities (5 hours) - CRITICAL
2. Phase 2: Core territory logic (7 hours) - HIGH
3. Phase 3: Commands and GUI (3 hours) - MEDIUM
4. Phase 4: Newsletter, events, tests (2 hours) - LOW

**Target Epic/Story**: Epic 1 (Critical Infrastructure) → Story 1.2
**Estimated Effort**: 17 hours (4 phases)
**Risk**: HIGH
**Quick Win**: No
**Status**: ✅ Audit complete, ready for implementation

---

### 2. `NumberUtil` (Kotlin duplicate utility)

**Status**: ⚠️ **HIGH - Code duplication**

**Location**: `tan-core/src/main/kotlin/org/leralix/tan/utils/text/NumberUtil.kt:3`

**Problem**: Duplicate utility class. `NumberUtil` (Kotlin) delegates to `NumberUtils` (Java).

**Usage Count**: 6 files

**Replacement**: Replace all `NumberUtil.` with `NumberUtils.`

**Target Epic/Story**: Epic 1 → Story 1.2
**Estimated Effort**: 1 hour
**Risk**: LOW
**Quick Win**: YES

---

## MEDIUM Priority Items

### 3. `PropertyData.owningPlayerID` Field

**Status**: ⚠️ **MEDIUM - Legacy field**

**Location**: `PropertyData.java:52-53`

**Problem**: Legacy field replaced by `AbstractOwner owner` pattern.

**Target Epic/Story**: Epic 2 → New story
**Estimated Effort**: 2 hours
**Quick Win**: YES

---

### 4. `CustomIcon` Legacy Fields

**Status**: ⚠️ **MEDIUM - Icon system refactoring**

**Location**: `CustomIcon.java:8-11`

**Problem**: Legacy icon storage replaced by base64 serialization.

**Target Epic/Story**: Epic 2 → New story
**Estimated Effort**: 3 hours
**Quick Win**: No

---

## TODO Markers Analysis

### Incomplete Feature Systems (15 TODOs)

**Location**: `domain/` packages (quest, prestige, upgrade, progression)

- Quest System: 2 TODOs
- Upgrade System: 3 TODOs
- Progression System: 9 TODOs

**Recommendation**: Handle as part of Epic 3 (Feature Completion Decision).

---

## Summary Table

| Item | Priority | Effort | Quick Win | Target Story | Status |
|------|----------|--------|-----------|--------------|--------|
| NumberUtil.kt | HIGH | 1h | YES | 1.2 | ✅ COMPLETED |
| getSync/getAllSync | HIGH | 17h | NO | ASYNC_MIGRATION_PLAN.md | ✅ Audited |
| owningPlayerID | MEDIUM | 2h | YES | 1.3 | ✅ COMPLETED |
| CustomIcon fields | MEDIUM | 3h | NO | 2.X | Pending |
| Incomplete features | LOW | TBD | NO | 3.2 | Epic 3 decision |

---

**Progress**: 2/5 quick wins completed (40%)

**Next Steps**:
1. Implement async migration (see ASYNC_MIGRATION_PLAN.md)
2. Or proceed with CustomIcon fields cleanup
3. Or defer to Epic 3 decision

---

**Document Version**: 1.1
**Last Updated**: 2025-01-11
