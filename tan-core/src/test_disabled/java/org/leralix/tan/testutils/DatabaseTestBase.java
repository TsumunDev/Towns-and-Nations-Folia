package org.leralix.tan.testutils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for database integration tests using TestContainers.
 *
 * <p>Provides a real MySQL container for testing database operations without mocking. Automatically
 * starts a MySQL 8.0 container before tests and stops it after all tests complete.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * class MyDatabaseTest extends DatabaseTestBase {
 *     @Test
 *     void testDatabaseOperation() throws SQLException {
 *         try (Connection conn = getDataSource().getConnection()) {
 *             // Run your test queries
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Real MySQL behavior - no mocking inconsistencies
 *   <li>Isolated environment - each test run gets fresh database
 *   <li>Fast startup - container caching, reusable across tests
 *   <li>Production-like testing - same schema, same queries
 * </ul>
 *
 * @see org.testcontainers.containers.MySQLContainer
 * @see com.zaxxer.hikari.HikariDataSource
 */
@Testcontainers
public abstract class DatabaseTestBase {

  /** MySQL 8.0 container with standard configuration. Reused across all test methods. */
  @Container
  protected static final MySQLContainer<?> mysqlContainer =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName("tan_test")
          .withUsername("test")
          .withPassword("test")
          .withReuse(true); // Reuse container for faster tests

  /** HikariCP connection pool configured for test database. */
  protected static HikariDataSource dataSource;

  /**
   * Starts MySQL container and initializes connection pool before any tests run.
   *
   * <p>Container startup takes ~2-5 seconds on first run, then cached for subsequent runs.
   */
  @BeforeAll
  static void setupDatabase() {
    // Container auto-starts via @Testcontainers annotation
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(mysqlContainer.getJdbcUrl());
    config.setUsername(mysqlContainer.getUsername());
    config.setPassword(mysqlContainer.getPassword());
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(5000);

    dataSource = new HikariDataSource(config);
  }

  /**
   * Stops MySQL container and closes connection pool after all tests complete.
   *
   * <p>Resources are automatically cleaned up by TestContainers framework.
   */
  @AfterAll
  static void teardownDatabase() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
    // Explicitly stop container to avoid resource leak
    if (mysqlContainer != null && mysqlContainer.isRunning()) {
      mysqlContainer.stop();
    }
  }

  /**
   * Gets the HikariCP connection pool for test database.
   *
   * <p>Use this to obtain connections for your test queries:
   *
   * <pre>{@code
   * try (Connection conn = getDataSource().getConnection()) {
   *     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM towns");
   *     ResultSet rs = stmt.executeQuery();
   *     // ... assertions
   * }
   * }</pre>
   *
   * @return Active HikariCP data source connected to MySQL container
   */
  protected static HikariDataSource getDataSource() {
    return dataSource;
  }

  /**
   * Gets MySQL container instance for advanced configuration.
   *
   * <p>Useful for retrieving connection details or executing raw SQL:
   *
   * <pre>{@code
   * String jdbcUrl = getMySQLContainer().getJdbcUrl();
   * int port = getMySQLContainer().getMappedPort(3306);
   * }</pre>
   *
   * @return Running MySQL container instance
   */
  protected static MySQLContainer<?> getMySQLContainer() {
    return mysqlContainer;
  }

  /**
   * Executes raw SQL statements on the test database.
   *
   * <p>Useful for setting up test data or creating schema:
   *
   * <pre>{@code
   * executeSql("CREATE TABLE towns (id VARCHAR(36) PRIMARY KEY, name VARCHAR(255))");
   * executeSql("INSERT INTO towns VALUES ('uuid-123', 'TestTown')");
   * }</pre>
   *
   * @param sql SQL statement to execute
   * @throws SQLException if SQL execution fails
   */
  protected void executeSql(String sql) throws SQLException {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
    }
  }

  /**
   * Clears all data from specified table while preserving schema.
   *
   * <p>Useful for test cleanup:
   *
   * <pre>{@code
   * @AfterEach
   * void cleanup() throws SQLException {
   *     truncateTable("towns");
   *     truncateTable("regions");
   * }
   * }</pre>
   *
   * @param tableName Name of table to truncate
   * @throws SQLException if truncation fails
   */
  protected void truncateTable(String tableName) throws SQLException {
    executeSql("TRUNCATE TABLE " + tableName);
  }
}
