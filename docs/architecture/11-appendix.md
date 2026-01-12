## Appendix - Useful Commands and Scripts

### Frequently Used Commands

```bash
# Build
./gradlew clean build                    # Full build
./gradlew :tan-core:shadowJar           # Create plugin JAR

# Testing
./gradlew test                          # Run all tests
./gradlew test --tests *Test            # Run specific test
./gradlew jacocoTestReport              # Generate coverage report

# Development
./gradlew :tan-core:compileJava         # Compile Java
./gradlew :tan-core:compileKotlin       # Compile Kotlin
./gradlew clean                         # Clean build artifacts
```

### Debugging and Troubleshooting

**Logs**:
- Location: `tan-core/logs/tan.log`
- Level: Configured in `logback.xml`
- Format: SLF4J with structured logging

**Debug Mode**:
- Set `debug: true` in `config.yml`
- Enables verbose logging
- Logs all SQL queries
- Traces cache operations

**Common Issues**:

| Issue | Symptom | Solution |
| ----- | ------- | -------- |
| **Folia threading error** | "Not scheduled on region thread" | Use `FoliaScheduler.runTask()` instead of direct calls |
| **GUI not opening** | Player kicked or nothing happens | Check `AsyncGuiHelper` circuit breaker status |
| **Cache serving stale data** | Old town data displayed | Manually invalidate: `/ccn admin debug clearcache` |
| **Redis connection failed** | Multi-server sync not working | Check `config.yml` Redis settings, verify server is reachable |
| **Tests failing** | `./gradlew test` fails | Run `./gradlew clean test`, check test reports in `build/reports/tests/` |

### Performance Monitoring

**Cache Statistics**:
```java
// In-game command
/ccn admin debug cache

// Logs cache hit rates
L1 (Guava): 85.2% hit rate
L2 (Redis): 92.7% hit rate
Database queries: 1234
```

**Redis Health**:
```java
// Check Redis connection
/ccn admin debug redis

// Logs connection pool status
Active connections: 5/64
Idle connections: 10
Pub/sub channels: 4
```

**Database Performance**:
```java
// Connection pool stats
/ccn admin debug database

// Logs HikariCP stats
Active connections: 8/30
Idle connections: 5
Total connections: 13
```

