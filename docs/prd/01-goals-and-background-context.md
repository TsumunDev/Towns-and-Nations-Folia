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

