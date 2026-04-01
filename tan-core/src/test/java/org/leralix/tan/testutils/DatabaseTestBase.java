package org.leralix.tan.testutils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for database integration tests using TestContainers.
 * <p>
 * This class provides a real MySQL container for database integration testing.
 * The container is started once and reused across all tests that extend this class.
 * <p>
 * Benefits of using TestContainers:
 * <ul>
 *   <li>Tests run against a real database, not mocks</li>
 *   <li>Catches ORM and query issues that mocks miss</li>
 *   <li>Tests are deterministic and isolated</li>
 *   <li>No need for external database setup</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * @Testcontainers
 * class MyDatabaseTest extends DatabaseTestBase {
 *     @Test
 *     void testDataPersistence() {
 *         // Use mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()
 *     }
 * }
 * }</pre>
 */
@Testcontainers
public abstract class DatabaseTestBase {

    /**
     * MySQL container for integration testing.
     * <p>
     * This container is started once per test class and reused for all test methods.
     * The container uses MySQL 8.0 with the following configuration:
     * <ul>
     *   <li>Database: test_tan</li>
     *   <li>Username: test</li>
     *   <li>Password: test</li>
     * </ul>
     */
    @Container
    protected static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_tan")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);  // Reuse container across tests for performance

    /**
     * Sets up the database schema before each test.
     * <p>
     * Override this method to initialize test data or run database migrations.
     * Called automatically by JUnit before each test method.
     */
    @BeforeEach
    void setUpDatabase() {
        // Default implementation does nothing
        // Override in subclasses to run migrations or set up test data
    }

    /**
     * Cleans up test data after each test.
     * <p>
     * Override this method to clean up test data while preserving schema.
     * Called automatically by JUnit after each test method.
     */
    @AfterEach
    void cleanDatabase() {
        // Default implementation does nothing
        // Override in subclasses to clean up test data
    }

    /**
     * Gets the JDBC URL for connecting to the test database.
     *
     * @return JDBC URL string
     */
    protected String getJdbcUrl() {
        return mysql.getJdbcUrl();
    }

    /**
     * Gets the database username for the test database.
     *
     * @return Username string
     */
    protected String getUsername() {
        return mysql.getUsername();
    }

    /**
     * Gets the database password for the test database.
     *
     * @return Password string
     */
    protected String getPassword() {
        return mysql.getPassword();
    }
}
