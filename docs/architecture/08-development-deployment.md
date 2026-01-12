## Development and Deployment

### Local Development Setup

#### Prerequisites
- **Java 21** (OpenJDK or Oracle JDK)
- **Gradle 8.x** (included via Gradle Wrapper)
- **MySQL 8.0+** (for production testing) or SQLite (embedded)
- **Redis 6.0+** (optional, for multi-server testing)
- **Folia 1.21.1+** server (⚠️ **NEEDS UPDATE in build.gradle**)

#### Build Commands
```bash
# Clean build
./gradlew clean build

# Run tests (with code coverage)
./gradlew test jacocoTestReport

# Create plugin JAR (shaded with dependencies)
./gradlew shadowJar

# Output: tan-core/build/libs/Coconation.jar
```

#### Development Workflow
1. **Import**: Import project into IDE (IntelliJ IDEA recommended for Kotlin support)
2. **Dependencies**: Gradle will automatically download all dependencies
3. **Testing**: Run tests with `./gradlew test`
4. **Code Style**: No formal check configured yet (consider Spotless or Checkstyle)

#### Configuration Files
- **Main Config**: `tan-core/src/main/resources/config.yml`
- **Language**: `tan-core/src/main/resources/lang/en/main.yml` (English)
- **Plugin Metadata**: `tan-core/src/main/resources/plugin.yml`

### Build and Deployment Process

#### Build Pipeline
```bash
# 1. Clean previous builds
./gradlew clean

# 2. Run tests (fails if coverage < 14%)
./gradlew test

# 3. Create fat JAR with Shadow plugin
./gradlew :tan-core:shadowJar

# 4. Output JAR
tan-core/build/libs/Coconation.jar
```

#### Shadow JAR Configuration
**File**: `tan-core/build.gradle:37-82`

**Minimization**: Excludes unused classes from dependencies
- **Preserved**: SQLite/MySQL natives, logging, Kotlin runtime
- **Excluded platforms**: FreeBSD, Android ARM, Windows ARMv7 (reduces size)
- **Relocated dependencies**: Prevents conflicts
  - `net.objecthunter.exp4j` → `org.leralix.tan.libs.exp4j`
  - `com.mysql` → `org.leralix.shadow.mysql`
  - `com.zaxxer.hikari` → `org.leralix.shadow.hikari`
  - `com.google.gson` → `org.leralix.shadow.gson`
  - `io.prometheus` → `org.leralix.shadow.prometheus`

#### Deployment
**Target**: `plugins/` folder of Folia server
```bash
cp tan-core/build/libs/Coconation.jar /path/to/folia/plugins/
```

**First Run**: Plugin will generate:
- `plugins/Coconation/config.yml`
- `plugins/Coconation/lang.yml`
- `plugins/Coconation/database.db` (if using SQLite)

### Environments

**Development**:
- Local Folia server with SQLite
- Hot reload with IDE (IntelliJ)
- Debug logging enabled

**Staging**:
- Remote test server with MySQL
- Redis for multi-server testing
- Production-like configuration

**Production**:
- MySQL with connection pooling
- Redis cluster for multi-server sync
- Monitoring with bStats

