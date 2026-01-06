package org.leralix.tan.exception;

/**
 * Exception thrown when a storage operation fails.
 * <p>
 * Storage exceptions encompass issues with:
 * <ul>
 *   <li>Database connection failures</li>
 *   <li>Query execution errors</li>
 *   <li>Transaction failures</li>
 *   <li>Cache inconsistencies</li>
 * </ul>
 * </p>
 *
 * @since 0.15.0
 */
public class StorageException extends TanException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a storage exception with a message.
   *
   * @param message the error message
   */
  public StorageException(String message) {
    super("STOR_000", message, null);
  }

  /**
   * Creates a storage exception with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public StorageException(String message, Throwable cause) {
    super("STOR_000", message, cause);
  }

  /**
   * Creates a storage exception with a cause only.
   *
   * @param cause the underlying cause
   */
  public StorageException(Throwable cause) {
    super("STOR_000", "Storage operation failed: " + cause.getMessage(), cause);
  }

  /**
   * Creates an exception for database connection failure.
   *
   * @param details the connection details
   * @return the exception
   */
  public static StorageException connectionFailed(String details) {
    return new StorageException("Database connection failed: " + details);
  }

  /**
   * Creates an exception for query execution failure.
   *
   * @param query the query that failed
   * @param cause the underlying cause
   * @return the exception
   */
  public static StorageException queryFailed(String query, Throwable cause) {
    return new StorageException("Query execution failed: " + query, cause);
  }

  /**
   * Creates an exception for transaction failure.
   *
   * @param transactionId the transaction identifier
   * @return the exception
   */
  public static StorageException transactionFailed(String transactionId) {
    return new StorageException("Transaction failed: " + transactionId);
  }

  /**
   * Creates an exception for data not found.
   *
   * @param dataType the type of data (e.g., "player", "town")
   * @param id the data identifier
   * @return the exception
   */
  public static StorageException notFound(String dataType, String id) {
    return new StorageException(dataType + " not found: " + id);
  }
}
