package org.leralix.tan.exception;

/**
 * Exception thrown when a war-related operation cannot be completed.
 * <p>
 * This includes scenarios such as:
 * <ul>
 *   <li>Declaring war on a territory with an existing war</li>
 *   <li>Executing war actions during peacetime</li>
 *   <li>Invalid war goal combinations</li>
 *   <li>Attempting to join a war as a non-participant</li>
 * </ul>
 * </p>
 *
 * @since 0.15.0
 */
public class WarException extends TanException {

  private static final long serialVersionUID = 1L;

  private final String warId;
  private final String reason;

  /**
   * Creates a new war exception.
   *
   * @param warId the war ID (if applicable)
   * @param reason the reason for the exception
   */
  public WarException(String warId, String reason) {
    super("WAR_001", "War operation failed: " + reason, null);
    this.warId = warId;
    this.reason = reason;

    if (warId != null) {
      addContext("warId", warId);
    }
    addContext("reason", reason);
  }

  /**
   * Creates a war exception without a specific war ID.
   *
   * @param reason the reason for the exception
   */
  public WarException(String reason) {
    this(null, reason);
  }

  /**
   * Creates an exception for attempting an action during peacetime.
   *
   * @param action the action that was attempted
   * @return the exception
   */
  public static WarException notAtWar(String action) {
    return new WarException(String.format(
        "Cannot %s: territory is not at war", action));
  }

  /**
   * Creates an exception for invalid war timing.
   *
   * @param reason the reason the timing is invalid
   * @return the exception
   */
  public static WarException invalidTiming(String reason) {
    return new WarException("Invalid war timing: " + reason);
  }

  /**
   * Creates an exception for invalid war goal.
   *
   * @param goal the invalid goal
   * @return the exception
   */
  public static WarException invalidGoal(String goal) {
    return new WarException("Invalid war goal: " + goal);
  }

  public String getWarId() {
    return warId;
  }

  public String getReason() {
    return reason;
  }

  @Override
  public String getUserMessage() {
    return "War operation failed: " + reason;
  }
}
