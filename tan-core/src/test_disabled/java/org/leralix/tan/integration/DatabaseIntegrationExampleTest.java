package org.leralix.tan.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.leralix.tan.testutils.DatabaseTestBase;

/**
 * Example integration test demonstrating DatabaseTestBase usage.
 *
 * <p>This test shows how to use TestContainers for real MySQL integration testing without mocking.
 * The test creates a simple table, inserts data, and verifies CRUD operations work correctly.
 *
 * <p><b>Running this test:</b>
 *
 * <pre>
 * ./gradlew test --tests "org.leralix.tan.integration.DatabaseIntegrationExampleTest"
 * </pre>
 *
 * <p><b>Requirements:</b>
 * <ul>
 *   <li>Docker must be running (TestContainers needs it)
 *   <li>First run downloads MySQL 8.0 image (~200MB)
 *   <li>Subsequent runs reuse cached container (~2-5s startup)
 * </ul>
 */
class DatabaseIntegrationExampleTest extends DatabaseTestBase {

  /**
   * Creates test schema before each test.
   *
   * <p>This ensures each test starts with a fresh, isolated database state.
   */
  @BeforeEach
  void setupTestSchema() throws SQLException {
    executeSql(
        """
        CREATE TABLE IF NOT EXISTS test_towns (
            id VARCHAR(36) PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            balance DOUBLE DEFAULT 0.0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """);
  }

  /**
   * Cleans up test data after each test.
   *
   * <p>Ensures no data pollution between test methods.
   */
  @AfterEach
  void cleanupTestData() throws SQLException {
    truncateTable("test_towns");
  }

  /** Test: Insert and retrieve a town record. */
  @Test
  void testInsertAndSelect() throws SQLException {
    // Given: A new town record
    String townId = "town-uuid-123";
    String townName = "TestTown";
    double balance = 1000.0;

    // When: Inserting the town into database
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "INSERT INTO test_towns (id, name, balance) VALUES (?, ?, ?)")) {
      stmt.setString(1, townId);
      stmt.setString(2, townName);
      stmt.setDouble(3, balance);
      int rowsAffected = stmt.executeUpdate();
      assertEquals(1, rowsAffected, "Should insert exactly 1 row");
    }

    // Then: The town can be retrieved with correct data
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM test_towns WHERE id = ?")) {
      stmt.setString(1, townId);
      ResultSet rs = stmt.executeQuery();

      assertTrue(rs.next(), "Town should exist in database");
      assertEquals(townId, rs.getString("id"));
      assertEquals(townName, rs.getString("name"));
      assertEquals(balance, rs.getDouble("balance"), 0.01);
      assertFalse(rs.next(), "Should only have one result");
    }
  }

  /** Test: Update town balance. */
  @Test
  void testUpdateBalance() throws SQLException {
    // Given: An existing town
    String townId = "town-uuid-456";
    executeSql(
        String.format("INSERT INTO test_towns (id, name, balance) VALUES ('%s', 'UpdateTown', 500.0)", townId));

    // When: Updating the balance
    double newBalance = 1500.0;
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("UPDATE test_towns SET balance = ? WHERE id = ?")) {
      stmt.setDouble(1, newBalance);
      stmt.setString(2, townId);
      int rowsUpdated = stmt.executeUpdate();
      assertEquals(1, rowsUpdated, "Should update exactly 1 row");
    }

    // Then: The balance is updated correctly
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT balance FROM test_towns WHERE id = ?")) {
      stmt.setString(1, townId);
      ResultSet rs = stmt.executeQuery();
      assertTrue(rs.next());
      assertEquals(newBalance, rs.getDouble("balance"), 0.01);
    }
  }

  /** Test: Delete town record. */
  @Test
  void testDelete() throws SQLException {
    // Given: An existing town
    String townId = "town-uuid-789";
    executeSql(
        String.format("INSERT INTO test_towns (id, name, balance) VALUES ('%s', 'DeleteTown', 300.0)", townId));

    // When: Deleting the town
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM test_towns WHERE id = ?")) {
      stmt.setString(1, townId);
      int rowsDeleted = stmt.executeUpdate();
      assertEquals(1, rowsDeleted, "Should delete exactly 1 row");
    }

    // Then: The town no longer exists
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM test_towns WHERE id = ?")) {
      stmt.setString(1, townId);
      ResultSet rs = stmt.executeQuery();
      assertFalse(rs.next(), "Town should not exist after deletion");
    }
  }

  /** Test: Query all towns (batch operations). */
  @Test
  void testBatchInsertAndSelectAll() throws SQLException {
    // Given: Multiple towns inserted via batch
    String[] townIds = {"town-1", "town-2", "town-3"};
    String[] townNames = {"Alpha", "Beta", "Gamma"};

    try (Connection conn = getDataSource().getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "INSERT INTO test_towns (id, name, balance) VALUES (?, ?, ?)")) {
      for (int i = 0; i < townIds.length; i++) {
        stmt.setString(1, townIds[i]);
        stmt.setString(2, townNames[i]);
        stmt.setDouble(3, 100.0 * (i + 1));
        stmt.addBatch();
      }
      int[] results = stmt.executeBatch();
      assertEquals(townIds.length, results.length, "Should insert all towns");
    }

    // Then: All towns can be retrieved
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as count FROM test_towns")) {
      ResultSet rs = stmt.executeQuery();
      assertTrue(rs.next());
      assertEquals(3, rs.getInt("count"), "Should have 3 towns in database");
    }
  }

  /** Test: Transaction rollback on error. */
  @Test
  void testTransactionRollback() throws SQLException {
    String townId = "town-rollback";

    try (Connection conn = getDataSource().getConnection()) {
      conn.setAutoCommit(false); // Start transaction

      try {
        // Insert a valid town
        try (PreparedStatement stmt =
            conn.prepareStatement(
                "INSERT INTO test_towns (id, name, balance) VALUES (?, ?, ?)")) {
          stmt.setString(1, townId);
          stmt.setString(2, "RollbackTown");
          stmt.setDouble(3, 500.0);
          stmt.executeUpdate();
        }

        // Simulate an error (constraint violation)
        try (PreparedStatement stmt =
            conn.prepareStatement(
                "INSERT INTO test_towns (id, name, balance) VALUES (?, ?, ?)")) {
          stmt.setString(1, townId); // Duplicate primary key - will fail!
          stmt.setString(2, "DuplicateTown");
          stmt.setDouble(3, 1000.0);
          stmt.executeUpdate();
        }

        conn.commit(); // This should not be reached
        fail("Should have thrown SQLException for duplicate key");

      } catch (SQLException e) {
        conn.rollback(); // Rollback on error
      }
    }

    // Verify: Town was NOT inserted (transaction rolled back)
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM test_towns WHERE id = ?")) {
      stmt.setString(1, townId);
      ResultSet rs = stmt.executeQuery();
      assertFalse(rs.next(), "Town should not exist after rollback");
    }
  }
}
