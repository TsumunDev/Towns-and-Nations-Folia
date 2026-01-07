package org.leralix.tan.validation;

/**
 * Exception thrown when input validation fails.
 * <p>
 * This exception provides structured error information including an error code
 * for programmatic handling and a human-readable message for user feedback.
 * </p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * try {
 *     String name = InputValidator.validateTownName(userInput);
 * } catch (ValidationException e) {
 *     // Log with code
 *     logger.error("Validation failed: {} - {}", e.getCode(), e.getMessage());
 *
 *     // Show to user
 *     player.sendMessage(ChatColor.RED + e.getMessage());
 * }
 * }</pre>
 *
 * @see ErrorCode
 * @see InputValidator
 * @since 0.15.0
 */
public class ValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ErrorCode errorCode;
  private final String userInput;
  private final Object[] contextData;

  /**
   * Creates a new validation exception with error code and message.
   *
   * @param code the error code
   * @param message the error message
   */
  public ValidationException(ErrorCode code, String message) {
    super(message);
    this.errorCode = code;
    this.userInput = null;
    this.contextData = null;
  }

  /**
   * Creates a new validation exception with error code, message, and cause.
   *
   * @param code the error code
   * @param message the error message
   * @param cause the underlying cause
   */
  public ValidationException(ErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = code;
    this.userInput = null;
    this.contextData = null;
  }

  /**
   * Creates a new validation exception with full context.
   *
   * @param code the error code
   * @param message the error message
   * @param userInput the invalid user input (sanitized)
   * @param contextData additional context data
   */
  public ValidationException(ErrorCode code, String message, String userInput, Object... contextData) {
    super(message);
    this.errorCode = code;
    this.userInput = userInput;
    this.contextData = contextData;
  }

  /**
   * Returns the error code for this validation failure.
   *
   * @return the error code
   */
  public ErrorCode getCode() {
    return errorCode;
  }

  /**
   * Returns the sanitized user input that caused the failure.
   *
   * @return the user input, or null if not available
   */
  public String getUserInput() {
    return userInput;
  }

  /**
   * Returns additional context data for this error.
   *
   * @return the context data array, or null if not available
   */
  public Object[] getContextData() {
    return contextData != null ? contextData.clone() : null;
  }

  /**
   * Returns a formatted error message suitable for logging.
   *
   * @return the formatted log message
   */
  public String getLogMessage() {
    StringBuilder sb = new StringBuilder();
    sb.append("[").append(errorCode.getCode()).append("] ");
    sb.append(getMessage());

    if (userInput != null) {
      sb.append(" | Input: '").append(InputValidator.sanitizeForLog(userInput)).append("'");
    }

    return sb.toString();
  }

  /**
   * Returns a user-friendly error message (without technical details).
   *
   * @return the user-friendly message
   */
  public String getUserMessage() {
    return getMessage();
  }

  /**
   * Checks if this error is due to invalid input format.
   *
   * @return true if the error is format-related
   */
  public boolean isFormatError() {
    return errorCode == ErrorCode.INVALID_FORMAT
        || errorCode == ErrorCode.INVALID_UUID
        || errorCode == ErrorCode.INVALID_PLAYER_NAME
        || errorCode == ErrorCode.INVALID_NUMBER
        || errorCode == ErrorCode.INVALID_COORDINATES;
  }

  /**
   * Checks if this error is due to insufficient permissions.
   *
   * @return true if the error is permission-related
   */
  public boolean isPermissionError() {
    return errorCode == ErrorCode.PERMISSION_DENIED
        || errorCode == ErrorCode.NOT_LEADER
        || errorCode == ErrorCode.NOT_MEMBER;
  }

  /**
   * Checks if this error is recoverable (user can retry with corrected input).
   *
   * @return true if the error is recoverable
   */
  public boolean isRecoverable() {
    return !errorCode.equals(ErrorCode.AT_WAR)
        && !errorCode.equals(ErrorCode.LIMIT_REACHED)
        && !errorCode.equals(ErrorCode.DATABASE_ERROR);
  }

  @Override
  public String toString() {
    return String.format("ValidationException{code=%s, message='%s'}",
        errorCode.getCode(), getMessage());
  }

  // ========== Static Factory Methods ==========

  /**
   * Creates a validation exception for null input.
   *
   * @param fieldName the name of the field that was null
   * @return the exception
   */
  public static ValidationException nullInput(String fieldName) {
    return new ValidationException(ErrorCode.NULL_INPUT,
        fieldName + " cannot be null");
  }

  /**
   * Creates a validation exception for empty input.
   *
   * @param fieldName the name of the field that was empty
   * @return the exception
   */
  public static ValidationException emptyInput(String fieldName) {
    return new ValidationException(ErrorCode.EMPTY_INPUT,
        fieldName + " cannot be empty");
  }

  /**
   * Creates a validation exception for value too low.
   *
   * @param fieldName the field name
   * @param minimum the minimum allowed value
   * @return the exception
   */
  public static ValidationException tooLow(String fieldName, Number minimum) {
    return new ValidationException(ErrorCode.VALUE_TOO_LOW,
        fieldName + " must be at least " + minimum);
  }

  /**
   * Creates a validation exception for value too high.
   *
   * @param fieldName the field name
   * @param maximum the maximum allowed value
   * @return the exception
   */
  public static ValidationException tooHigh(String fieldName, Number maximum) {
    return new ValidationException(ErrorCode.VALUE_TOO_HIGH,
        fieldName + " cannot exceed " + maximum);
  }

  /**
   * Creates a validation exception for invalid format.
   *
   * @param fieldName the field name
   * @param expectedFormat the expected format description
   * @return the exception
   */
  public static ValidationException invalidFormat(String fieldName, String expectedFormat) {
    return new ValidationException(ErrorCode.INVALID_FORMAT,
        "Invalid " + fieldName + " format. Expected: " + expectedFormat);
  }

  /**
   * Creates a validation exception for insufficient funds.
   *
   * @param required the amount required
   * @param available the amount available
   * @return the exception
   */
  public static ValidationException insufficientFunds(double required, double available) {
    return new ValidationException(ErrorCode.INSUFFICIENT_FUNDS,
        "Insufficient funds. Required: " + required + ", Available: " + available);
  }

  /**
   * Creates a validation exception for permission denied.
   *
   * @param action the action that was denied
   * @return the exception
   */
  public static ValidationException permissionDenied(String action) {
    return new ValidationException(ErrorCode.PERMISSION_DENIED,
        "You don't have permission to " + action);
  }

  /**
   * Creates a validation exception for resource not found.
   *
   * @param resourceType the type of resource
   * @param resourceId the resource identifier
   * @return the exception
   */
  public static ValidationException notFound(String resourceType, String resourceId) {
    return new ValidationException(ErrorCode.NOT_FOUND,
        resourceType + " not found: " + resourceId);
  }

  /**
   * Creates a validation exception for name already taken.
   *
   * @param name the name that is already taken
   * @return the exception
   */
  public static ValidationException nameAlreadyTaken(String name) {
    return new ValidationException(ErrorCode.NAME_ALREADY_TAKEN,
        "The name '" + name + "' is already taken");
  }
}
