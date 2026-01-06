package org.leralix.tan.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all Towns and Nations plugin exceptions.
 * <p>
 * This exception class provides structured error information including error codes,
 * context data, and user-friendly messages. All custom exceptions should extend this class.
 * </p>
 * <p>
 * <b>Error Handling Pattern:</b><br>
 * <pre>{@code
 * try {
 *     town.claimChunk(player);
 * } catch (TerritoryException e) {
 *     logger.error("Failed to claim chunk: {}", e.getLogMessage(), e);
 *     player.sendMessage(e.getUserMessage());
 * }
 * }</pre>
 * </p>
 *
 * @see EconomyException
 * @see TerritoryException
 * @see PermissionException
 * @see StorageException
 * @since 0.15.0
 */
public class TanException extends Exception {

  private static final long serialVersionUID = 1L;

  private final String errorCode;
  private final Map<String, Object> context;

  /**
   * Creates a new TanException with a message.
   *
   * @param message the error message
   */
  public TanException(String message) {
    this("TAN_000", message, null);
  }

  /**
   * Creates a new TanException with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public TanException(String message, Throwable cause) {
    this("TAN_000", message, cause);
  }

  /**
   * Creates a new TanException with full error information.
   *
   * @param errorCode the error code (e.g., "TERR_001", "ECON_001")
   * @param message the error message
   * @param cause the underlying cause
   */
  public TanException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.context = new HashMap<>();
  }

  /**
   * Returns the error code for this exception.
   *
   * @return the error code
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * Adds context information to this exception.
   *
   * @param key the context key
   * @param value the context value
   * @return this exception for chaining
   */
  public TanException addContext(String key, Object value) {
    this.context.put(key, value);
    return this;
  }

  /**
   * Returns the context information for this exception.
   *
   * @return the context map
   */
  public Map<String, Object> getContext() {
    return new HashMap<>(context);
  }

  /**
   * Returns a user-friendly error message.
   *
   * @return the user-friendly message
   */
  public String getUserMessage() {
    return getMessage();
  }

  /**
   * Returns a formatted log message with error code and context.
   *
   * @return the formatted log message
   */
  public String getLogMessage() {
    StringBuilder sb = new StringBuilder();
    sb.append("[").append(errorCode).append("] ");
    sb.append(getMessage());

    if (!context.isEmpty()) {
      sb.append(" | Context: ").append(context);
    }

    return sb.toString();
  }

  /**
   * Checks if this exception is recoverable.
   *
   * @return true by default, subclasses can override
   */
  public boolean isRecoverable() {
    return true;
  }

  @Override
  public String toString() {
    return String.format("%s{code=%s, message='%s'}",
        getClass().getSimpleName(), errorCode, getMessage());
  }
}