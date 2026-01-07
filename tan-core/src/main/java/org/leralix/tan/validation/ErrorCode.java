package org.leralix.tan.validation;

/**
 * Error codes for validation failures.
 * <p>
 * Each code represents a specific category of validation error, allowing for
 * structured error handling and user-friendly error messages.
 * </p>
 *
 * @see ValidationException
 * @since 0.15.0
 */
public enum ErrorCode {

  // ========== NULL/EMPTY INPUTS ==========
  /**
   * Input value was null.
   */
  NULL_INPUT("VAL_001", "Input cannot be null"),

  /**
   * Input value was empty or blank.
   */
  EMPTY_INPUT("VAL_002", "Input cannot be empty"),

  // ========== FORMAT ERRORS ==========
  /**
   * Input does not match the required format.
   */
  INVALID_FORMAT("VAL_010", "Invalid input format"),

  /**
   * Invalid UUID format.
   */
  INVALID_UUID("VAL_011", "Invalid UUID format"),

  /**
   * Invalid player name format.
   */
  INVALID_PLAYER_NAME("VAL_012", "Invalid player name"),

  /**
   * Invalid numeric value (NaN or infinite).
   */
  INVALID_NUMBER("VAL_013", "Invalid number value"),

  /**
   * Invalid coordinates.
   */
  INVALID_COORDINATES("VAL_014", "Invalid coordinates"),

  // ========== VALUE RANGE ERRORS ==========
  /**
   * Value is below minimum allowed.
   */
  VALUE_TOO_LOW("VAL_020", "Value is too low"),

  /**
   * Value is negative.
   */
  NEGATIVE_VALUE("VAL_021", "Value cannot be negative"),

  /**
   * Value exceeds maximum allowed.
   */
  VALUE_TOO_HIGH("VAL_022", "Value is too high"),

  /**
   * Value is out of allowed range.
   */
  OUT_OF_RANGE("VAL_023", "Value is out of allowed range"),

  // ========== BUSINESS LOGIC ERRORS ==========
  /**
   * Insufficient funds for transaction.
   */
  INSUFFICIENT_FUNDS("VAL_030", "Insufficient funds"),

  /**
   * Name is reserved by the system.
   */
  RESERVED_NAME("VAL_031", "This name is reserved"),

  /**
   * Name is already taken.
   */
  NAME_ALREADY_TAKEN("VAL_032", "This name is already in use"),

  /**
   * ID already exists.
   */
  ID_ALREADY_EXISTS("VAL_033", "This ID already exists"),

  /**
   * Resource not found.
   */
  NOT_FOUND("VAL_034", "Resource not found"),

  // ========== PERMISSION ERRORS ==========
  /**
   * User lacks required permission.
   */
  PERMISSION_DENIED("VAL_040", "Permission denied"),

  /**
   * User is not the leader/owner.
   */
  NOT_LEADER("VAL_041", "You must be the leader to do this"),

  /**
   * User is not a member of the territory.
   */
  NOT_MEMBER("VAL_042", "You are not a member"),

  // ========== STATE ERRORS ==========
  /**
   * Operation invalid in current state.
   */
  INVALID_STATE("VAL_050", "Invalid state for this operation"),

  /**
   * Territory is at war.
   */
  AT_WAR("VAL_051", "Cannot perform this action while at war"),

  /**
   * Territory has pending actions.
   */
  HAS_PENDING_ACTIONS("VAL_052", "Complete pending actions first"),

  /**
   * Cooldown has not expired.
   */
  COOLDOWN_ACTIVE("VAL_053", "Action is on cooldown"),

  // ========== CONSTRAINT ERRORS ==========
  /**
   * Maximum limit reached.
   */
  LIMIT_REACHED("VAL_060", "Maximum limit reached"),

  /**
   * Required condition not met.
   */
  REQUIREMENT_NOT_MET("VAL_061", "Requirements not met"),

  /**
   * Chunk already claimed.
   */
  CHUNK_ALREADY_CLAIMED("VAL_062", "This chunk is already claimed"),

  /**
   * Chunk is not claimed.
   */
  CHUNK_NOT_CLAIMED("VAL_063", "This chunk is not claimed"),

  // ========== NETWORK/DATABASE ERRORS ==========
  /**
   * Database operation failed.
   */
  DATABASE_ERROR("VAL_070", "Database operation failed"),

  /**
   * Network operation failed.
   */
  NETWORK_ERROR("VAL_071", "Network operation failed"),

  /**
   * Transaction failed.
   */
  TRANSACTION_FAILED("VAL_072", "Transaction failed"),

  // ========== UNKNOWN ERRORS ==========
  /**
   * Unknown error occurred.
   */
  UNKNOWN("VAL_099", "Unknown error");

  private final String code;
  private final String defaultMessage;

  ErrorCode(String code, String defaultMessage) {
    this.code = code;
    this.defaultMessage = defaultMessage;
  }

  /**
   * Returns the error code (e.g., "VAL_001").
   *
   * @return the error code
   */
  public String getCode() {
    return code;
  }

  /**
   * Returns the default error message.
   *
   * @return the default message
   */
  public String getDefaultMessage() {
    return defaultMessage;
  }

  /**
   * Returns a formatted error message with context.
   *
   * @param context additional context information
   * @return the formatted message
   */
  public String getFormattedMessage(String context) {
    return String.format("[%s] %s: %s", code, defaultMessage, context);
  }
}
