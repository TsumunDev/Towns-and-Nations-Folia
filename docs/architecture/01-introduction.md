## Introduction

This document captures the **CURRENT STATE** of the Coconation (formerly Towns & Nations) Minecraft plugin codebase, including technical debt, workarounds, and real-world patterns. It serves as a reference for AI agents working on enhancements, with a focus on achieving 90/100 quality score through code modernization, API updates, and future-proofing.

### Project Overview

**Coconation** is a comprehensive territorial, diplomatic, and economic management plugin for Minecraft Folia/Paper servers. Players can create towns, form regions (nations), engage in diplomacy, wage wars, and manage complex economies.

**Current Version**: 2.0.0 (Development)
**Minecraft Target**: Folia 1.21.1+ (⚠️ **NEEDS UPDATE**: currently 1.20.1 in build.gradle)
**Java Version**: 21 (LTS)
**Language**: Java 21 + Kotlin 2.1.0 (mixed codebase, ~600 source files)

### Document Scope

**Comprehensive documentation** of entire system with focus on:
- **Code Quality**: Identifying areas needing cleanup for 90/100 score
- **API Modernization**: Upgrading deprecated APIs and patterns
- **Future-Proofing**: Ensuring long-term maintainability and scalability

### Quality Goals

The project aims for:
- ✅ **90/100 quality score** (clean code, minimal bugs)
- ✅ **Zero deprecated methods** (all APIs current)
- ✅ **Future-proof architecture** (extensible, maintainable)
- ✅ **Folia-native design** (proper regionalization)
- ✅ **High test coverage** (currently 14%, progressive improvement planned)

### Change Log

| Date | Version | Description | Author |
| ------ | ------- | ----------- | ------ |
| 2025-01-11 | 1.0 | Initial brownfield analysis | Winston (Architect) |

