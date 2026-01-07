package org.leralix.tan.exception;

/**
 * Exception thrown when a transaction cannot be completed due to insufficient funds.
 * <p>
 * This exception provides detailed information about the transaction failure,
 * including the required amount, available balance, and the entity involved.
 * </p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * if (playerBalance < amount) {
 *     throw new InsufficientFundsException(townName, amount, playerBalance);
 * }
 * }</pre>
 *
 * @since 0.15.0
 */
public class InsufficientFundsException extends EconomyException {

  private static final long serialVersionUID = 1L;

  private final double required;
  private final double available;
  private final String entityName;

  /**
   * Creates an exception for insufficient funds.
   *
   * @param entityName the name of the entity lacking funds (player/town)
   * @param required the amount required
   * @param available the amount available
   */
  public InsufficientFundsException(String entityName, double required, double available) {
    super("ECON_001", String.format("Insufficient funds for %s. Required: %.2f, Available: %.2f",
        entityName, required, available), null);
    this.required = required;
    this.available = available;
    this.entityName = entityName;

    addContext("entity", entityName);
    addContext("required", required);
    addContext("available", available);
    addContext("shortage", required - available);
  }

  /**
   * Creates an exception with a custom message.
   *
   * @param message the error message
   */
  public InsufficientFundsException(String message) {
    super("ECON_001", message, null);
    this.required = 0;
    this.available = 0;
    this.entityName = "unknown";
  }

  /**
   * Returns the amount required.
   *
   * @return the required amount
   */
  public double getRequired() {
    return required;
  }

  /**
   * Returns the amount available.
   *
   * @return the available amount
   */
  public double getAvailable() {
    return available;
  }

  /**
   * Returns the name of the entity that lacks funds.
   *
   * @return the entity name
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * Returns the shortage amount.
   *
   * @return the amount needed (required - available)
   */
  public double getShortage() {
    return required - available;
  }

  @Override
  public String getUserMessage() {
    return String.format("You need %.2f but only have %.2f available (shortage: %.2f)",
        required, available, getShortage());
  }
}
