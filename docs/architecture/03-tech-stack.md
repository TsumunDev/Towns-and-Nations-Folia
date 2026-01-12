## High Level Architecture

### Technical Summary

Coconation is a **multi-module Gradle project** implementing a complex town/nation management system with **async-first architecture** designed for Folia's regionalized threading model.

**Architecture Patterns**:
- **Component Pattern**: Immutable components for thread-safe territory data
- **Repository Pattern**: DatabaseStorage abstraction for CRUD operations
- **Strategy Pattern**: Multiple economy implementations (Standalone/External/Vault)
- **Observer Pattern**: Event-driven GUI updates and Redis pub/sub
- **Async/Await**: CompletableFuture-based async operations throughout
- **Two-Tier Caching**: Local (Guava) + Distributed (Redis) for performance

### Actual Tech Stack (from build.gradle files)

| Category | Technology | Version | Notes |
| ---------- | ---------- | ------- | ----- |
| **Build System** | Gradle | 8.x | Kotlin DSL 2.1.0 |
| **Java** | OpenJDK | 21 | LTS, Virtual Threads enabled |
| **Kotlin** | Kotlin | 2.1.0 | Coroutines 1.9.0, JVM target 21 |
| **Minecraft Server** | Folia API | 1.20.1 | ⚠️ **NEEDS UPDATE to 1.21.1+** |
| **GUI Framework** | Triumph GUI | 3.1.11 | Comprehensive chest GUI system |
| **Database** | HikariCP | 5.1.0 | Connection pooling |
| **Database Drivers** | MySQL Connector | 8.4.0 | Production MySQL |
| | SQLite JDBC | 3.43.2.0 | Embedded SQLite |
| **Redis** | Redisson | 3.24.0 | Cluster/Sentinel/Single support |
| **JSON** | Gson | 2.11.0 | Serialization/deserialization |
| **Logging** | SLF4J + Logback | 2.0.17/1.5.20 | Structured logging |
| **Testing** | JUnit | 5.10.0 | Modern testing framework |
| | MockBukkit | v4.72.9 | Bukkit server mocking |
| | TestContainers | 1.19.3 | Real database integration tests |
| | Mockito | 5.7.0 | Mocking framework |
| **Circuit Breaker** | Resilience4j | 2.1.0 | Fault tolerance |
| **Monitoring** | bStats | 3.1.0 | Plugin metrics |
| **Math** | exp4j | 0.4.8 | Expression evaluation |

### Module Structure

**Multi-module Gradle layout**:

```
TownsAndNations-folia/
├── tan-api/              # Public API module (interfaces only)
│   └── build.gradle      # Java library, Folia API compile-only
├── tan-core/             # Main implementation (Java + Kotlin)
│   └── build.gradle      # Shadow plugin for fat JAR
├── build.gradle          # Root configuration
└── settings.gradle       # Module definitions
```

**Module Dependencies**:
- `tan-api` → Pure interface definitions, compile-only dependencies
- `tan-core` → Depends on `tan-api`, contains all implementation logic

### Repository Structure Reality Check

- **Type**: Monorepo with multi-module Gradle build
- **Package Manager**: Gradle with Kotlin DSL
- **Build Tool**: Gradle 8.x, Shadow plugin for fat JAR creation
- **Version Control**: Git with GitHub
- **Notable**:
  - Mixed Java/Kotlin codebase (gradual Kotlin transition)
  - Heavy use of compile-only dependencies for Bukkit ecosystem
  - Shadow plugin minimizes JAR by excluding unused dependencies

