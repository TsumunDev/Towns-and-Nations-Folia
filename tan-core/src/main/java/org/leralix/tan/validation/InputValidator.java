package org.leralix.tan.validation;

import java.util.regex.Pattern;

/**
 * Centralized input validation for all user-provided data.
 * <p>
 * This class provides strict validation for all player inputs including names,
 * amounts, coordinates, and other user-controlled data. All validation methods
 * throw {@link ValidationException} with descriptive error messages on failure.
 * </p>
 * <p>
 * <b>Thread Safety:</b><br>
 * This class is thread-safe and can be used concurrently from multiple threads.
 * </p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * try {
 *     String safeName = InputValidator.validateTownName(userInput);
 *     double safeAmount = InputValidator.validatePositiveAmount(amountInput, 10000);
 * } catch (ValidationException e) {
 *     player.sendMessage(e.getMessage());
 * }
 * }</pre>
 *
 * @see ValidationException
 * @since 0.15.0
 */
public final class InputValidator {

  private InputValidator() {
    throw new IllegalStateException("Utility class");
  }

  // ========== PATTERNS ==========

  /**
   * Pattern for valid town/region names.
   * Allows: letters, numbers, spaces, hyphens, underscores. 1-32 characters.
   */
  private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\- \\p{L}]{1,32}$");

  /**
   * Pattern for UUID strings (standard format).
   */
  private static final Pattern UUID_PATTERN =
      Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  /**
   * Pattern for player names (Minecraft usernames).
   */
  private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

  /**
   * Pattern for town tags (short abbreviations).
   */
  private static final Pattern TAG_PATTERN = Pattern.compile("^[A-Z0-9]{1,5}$");

  // ========== CONSTRAINTS ==========

  /** Maximum amount for any monetary transaction. */
  public static final double MAX_MONEY_AMOUNT = 1_000_000_000.0;

  /** Maximum number of chunks a town can claim. */
  public static final int MAX_CHUNKS = 10_000;

  /** Maximum number of players in a town. */
  public static final int MAX_PLAYERS = 500;

  /** Maximum tax rate (100%). */
  public static final double MAX_TAX_RATE = 100.0;

  /** Maximum salary per payment. */
  public static final double MAX_SALARY = 100_000.0;

  /** Maximum description length. */
  public static final int MAX_DESCRIPTION_LENGTH = 256;

  /** Maximum message length. */
  public static final int MAX_MESSAGE_LENGTH = 128;

  // ========== NAME VALIDATION ==========

  /**
   * Validates a town or region name.
   *
   * @param name the name to validate
   * @return the validated name (trimmed)
   * @throws ValidationException if the name is invalid
   */
  public static String validateTownName(String name) {
    if (name == null) {
      throw new ValidationException(ErrorCode.NULL_INPUT, "Name cannot be null");
    }

    String trimmed = name.trim();

    if (trimmed.isEmpty()) {
      throw new ValidationException(ErrorCode.EMPTY_INPUT, "Name cannot be empty");
    }

    if (!NAME_PATTERN.matcher(trimmed).matches()) {
      throw new ValidationException(ErrorCode.INVALID_FORMAT,
          "Name must be 1-32 characters (letters, numbers, spaces, hyphens, underscores)");
    }

    if (trimmed.equalsIgnoreCase("admin") || trimmed.equalsIgnoreCase("console")) {
      throw new ValidationException(ErrorCode.RESERVED_NAME,
          "This name is reserved");
    }

    return trimmed;
  }

  /**
   * Validates a player name.
   *
   * @param name the player name to validate
   * @return the validated player name
   * @throws ValidationException if the name is invalid
   */
  public static String validatePlayerName(String name) {
    if (name == null) {
      throw new ValidationException(ErrorCode.NULL_INPUT, "Player name cannot be null");
    }

    String trimmed = name.trim();

    if (trimmed.isEmpty()) {
      throw new ValidationException(ErrorCode.EMPTY_INPUT, "Player name cannot be empty");
    }

    if (!PLAYER_NAME_PATTERN.matcher(trimmed).matches()) {
      throw new ValidationException(ErrorCode.INVALID_FORMAT,
          "Invalid player name format");
    }

    return trimmed;
  }

  /**
   * Validates a town tag (short abbreviation).
   *
   * @param tag the tag to validate
   * @return the validated tag (uppercase)
   * @throws ValidationException if the tag is invalid
   */
  public static String validateTag(String tag) {
    if (tag == null) {
      throw new ValidationException(ErrorCode.NULL_INPUT, "Tag cannot be null");
    }

    String trimmed = tag.trim().toUpperCase();

    if (trimmed.isEmpty()) {
      throw new ValidationException(ErrorCode.EMPTY_INPUT, "Tag cannot be empty");
    }

    if (!TAG_PATTERN.matcher(trimmed).matches()) {
      throw new ValidationException(ErrorCode.INVALID_FORMAT,
          "Tag must be 1-5 uppercase letters or numbers");
    }

    return trimmed;
  }

  // ========== UUID VALIDATION ==========

  /**
   * Validates and parses a UUID string.
   *
   * @param uuidString the UUID string to validate
   * @return the parsed UUID
   * @throws ValidationException if the UUID is invalid
   */
  public static java.util.UUID validateUUID(String uuidString) {
    if (uuidString == null) {
      throw new ValidationException(ErrorCode.NULL_INPUT, "UUID cannot be null");
    }

    if (!UUID_PATTERN.matcher(uuidString).matches()) {
      throw new ValidationException(ErrorCode.INVALID_FORMAT,
          "Invalid UUID format");
    }

    try {
      return java.util.UUID.fromString(uuidString);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(ErrorCode.INVALID_FORMAT,
          "Invalid UUID: " + uuidString, e);
    }
  }

  // ========== NUMERIC VALIDATION ==========

  /**
   * Validates a positive amount (money, items, etc.).
   *
   * @param amount the amount to validate
   * @param max the maximum allowed value
   * @return the validated amount
   * @throws ValidationException if the amount is invalid
   */
  public static double validatePositiveAmount(double amount, double max) {
    if (Double.isNaN(amount) || Double.isInfinite(amount)) {
      throw new ValidationException(ErrorCode.INVALID_NUMBER,
          "Amount must be a valid number");
    }

    if (amount < 0) {
      throw new ValidationException(ErrorCode.NEGATIVE_VALUE,
          "Amount cannot be negative");
    }

    if (amount > max) {
      throw new ValidationException(ErrorCode.VALUE_TOO_HIGH,
          "Amount cannot exceed " + max);
    }

    return amount;
  }

  /**
   * Validates a monetary amount.
   *
   * @param amount the amount to validate
   * @return the validated amount
   * @throws ValidationException if the amount is invalid
   */
  public static double validateMoneyAmount(double amount) {
    return validatePositiveAmount(amount, MAX_MONEY_AMOUNT);
  }

  /**
   * Validates a tax rate (0-100).
   *
   * @param rate the tax rate to validate
   * @return the validated rate
   * @throws ValidationException if the rate is invalid
   */
  public static double validateTaxRate(double rate) {
    if (Double.isNaN(rate) || Double.isInfinite(rate)) {
      throw new ValidationException(ErrorCode.INVALID_NUMBER,
          "Tax rate must be a valid number");
    }

    if (rate < 0) {
      throw new ValidationException(ErrorCode.NEGATIVE_VALUE,
          "Tax rate cannot be negative");
    }

    if (rate > MAX_TAX_RATE) {
      throw new ValidationException(ErrorCode.VALUE_TOO_HIGH,
          "Tax rate cannot exceed " + MAX_TAX_RATE + "%");
    }

    return rate;
  }

  /**
   * Validates an integer within a range.
   *
   * @param value the value to validate
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @return the validated integer
   * @throws ValidationException if the value is out of range
   */
  public static int validateIntRange(int value, int min, int max) {
    if (value < min) {
      throw new ValidationException(ErrorCode.VALUE_TOO_LOW,
          "Value must be at least " + min);
    }

    if (value > max) {
      throw new ValidationException(ErrorCode.VALUE_TOO_HIGH,
          "Value cannot exceed " + max);
    }

    return value;
  }

  /**
   * Validates coordinates.
   *
   * @param x the X coordinate
   * @param y the Y coordinate
   * @param z the Z coordinate
   * @throws ValidationException if coordinates are invalid
   */
  public static void validateCoordinates(int x, int y, int z) {
    if (y < -64 || y > 320) {
      throw new ValidationException(ErrorCode.INVALID_COORDINATES,
          "Y coordinate must be between -64 and 320");
    }
  }

  // ========== TEXT VALIDATION ==========

  /**
   * Validates a description string.
   *
   * @param description the description to validate
   * @return the validated description
   * @throws ValidationException if the description is invalid
   */
  public static String validateDescription(String description) {
    return validateTextLength(description, MAX_DESCRIPTION_LENGTH, "Description");
  }

  /**
   * Validates a message string.
   *
   * @param message the message to validate
   * @return the validated message
   * @throws ValidationException if the message is invalid
   */
  public static String validateMessage(String message) {
    return validateTextLength(message, MAX_MESSAGE_LENGTH, "Message");
  }

  /**
   * Validates text length.
   *
   * @param text the text to validate
   * @param maxLength the maximum allowed length
   * @param fieldName the field name for error messages
   * @return the validated text
   * @throws ValidationException if the text is too long
   */
  public static String validateTextLength(String text, int maxLength, String fieldName) {
    if (text == null) {
      throw new ValidationException(ErrorCode.NULL_INPUT, fieldName + " cannot be null");
    }

    if (text.length() > maxLength) {
      throw new ValidationException(ErrorCode.VALUE_TOO_HIGH,
          fieldName + " cannot exceed " + maxLength + " characters");
    }

    return text;
  }

  // ========== CHUNK VALIDATION ==========

  /**
   * Validates chunk count.
   *
   * @param count the chunk count to validate
   * @return the validated count
   * @throws ValidationException if the count is invalid
   */
  public static int validateChunkCount(int count) {
    if (count < 0) {
      throw new ValidationException(ErrorCode.NEGATIVE_VALUE,
          "Chunk count cannot be negative");
    }

    if (count > MAX_CHUNKS) {
      throw new ValidationException(ErrorCode.VALUE_TOO_HIGH,
          "Cannot exceed " + MAX_CHUNKS + " chunks");
    }

    return count;
  }

  // ========== PLAYER COUNT VALIDATION ==========

  /**
   * Validates player count.
   *
   * @param count the player count to validate
   * @return the validated count
   * @throws ValidationException if the count is invalid
   */
  public static int validatePlayerCount(int count) {
    if (count < 0) {
      throw new ValidationException(ErrorCode.NEGATIVE_VALUE,
          "Player count cannot be negative");
    }

    if (count > MAX_PLAYERS) {
      throw new ValidationException(ErrorCode.VALUE_TOO_HIGH,
          "Cannot exceed " + MAX_PLAYERS + " players");
    }

    return count;
  }

  // ========== COLOR CODE VALIDATION ==========

  /**
   * Validates a Minecraft color code.
   *
   * @param colorCode the color code to validate (e.g., "&a", "#FF0000")
   * @return the validated color code
   * @throws ValidationException if the color code is invalid
   */
  public static String validateColorCode(String colorCode) {
    if (colorCode == null || colorCode.isEmpty()) {
      throw new ValidationException(ErrorCode.EMPTY_INPUT, "Color code cannot be empty");
    }

    // Legacy format (&0-&f, &k-&r)
    if (colorCode.matches("^&[0-9a-fk-or]$")) {
      return colorCode;
    }

    // Hex format (&#RRGGBB)
    if (colorCode.matches("^&#[0-9a-fA-F]{6}$")) {
      return colorCode;
    }

    // Mini message format (simplified check)
    if (colorCode.matches("^<[a-z_]+>$")) {
      return colorCode;
    }

    throw new ValidationException(ErrorCode.INVALID_FORMAT,
        "Invalid color code format. Use &0-&f, &#RRGGBB, or <color>");
  }

  // ========== COMBINATION VALIDATORS ==========

  /**
   * Validates both name and description together.
   *
   * @param name the name to validate
   * @param description the description to validate
   * @return an array with [validatedName, validatedDescription]
   * @throws ValidationException if either value is invalid
   */
  public static String[] validateNameAndDescription(String name, String description) {
    String validName = validateTownName(name);
    String validDesc = validateDescription(description);
    return new String[]{validName, validDesc};
  }

  /**
   * Validates money amount with recipient check.
   *
   * @param amount the amount to validate
   * @param senderBalance the sender's balance
   * @return the validated amount
   * @throws ValidationException if the transaction is invalid
   */
  public static double validateTransaction(double amount, double senderBalance) {
    double validated = validateMoneyAmount(amount);

    if (validated > senderBalance) {
      throw new ValidationException(ErrorCode.INSUFFICIENT_FUNDS,
          "Insufficient funds. Needed: " + validated + ", Available: " + senderBalance);
    }

    return validated;
  }

  // ========== SANITIZATION ==========

  /**
   * Sanitizes input by removing potential malicious content.
   *
   * @param input the input to sanitize
   * @return the sanitized input
   */
  public static String sanitize(String input) {
    if (input == null) {
      return "";
    }

    // Remove null bytes and other control characters except newlines and tabs
    return input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
  }

  /**
   * Sanitizes a string for display (removes colors and special chars).
   *
   * @param input the input to sanitize
   * @return the sanitized string suitable for logging
   */
  public static String sanitizeForLog(String input) {
    if (input == null) {
      return "null";
    }

    return input.replaceAll("[&§][0-9a-fk-orx]", "") // Remove color codes
        .replaceAll("[\\p{C}]", "") // Remove control characters
        .trim();
  }
}
