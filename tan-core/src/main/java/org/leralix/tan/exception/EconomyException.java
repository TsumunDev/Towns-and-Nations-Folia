package org.leralix.tan.exception;

/**
 * Exception thrown when an economy-related operation fails.
 * <p>
 * Economy exceptions encompass issues with:
 * <ul>
 *   <li>Insufficient funds for transactions</li>
 *   <li>Invalid amounts (negative, NaN, infinite)</li>
 *   <li>Tax calculation failures</li>
 *   <li>Treasury operations</li>
 *   <li>Salary payments</li>
 * </ul>
 * </p>
 *
 * @see InsufficientFundsException
 * @since 0.15.0
 */
public class EconomyException extends TanException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates an economy exception with a message.
   *
   * @param message the error message
   */
  public EconomyException(String message) {
    super("ECON_000", message, null);
  }

  /**
   * Creates an economy exception with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public EconomyException(String message, Throwable cause) {
    super("ECON_000", message, cause);
  }

  /**
   * Creates an economy exception with full error information.
   *
   * @param errorCode the specific error code
   * @param message the error message
   * @param cause the underlying cause
   */
  public EconomyException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  /**
   * Creates an exception for an invalid amount.
   *
   * @param amount the invalid amount
   * @return the exception
   */
  public static EconomyException invalidAmount(double amount) {
    return new EconomyException("Invalid amount: " + amount);
  }

  /**
   * Creates an exception for a negative amount.
   *
   * @param amount the negative amount
   * @return the exception
   */
  public static EconomyException negativeAmount(double amount) {
    return new EconomyException("Amount cannot be negative: " + amount);
  }

  /**
   * Creates an exception for a transaction failure.
   *
   * @param reason the failure reason
   * @return the exception
   */
  public static EconomyException transactionFailed(String reason) {
    return new EconomyException("Transaction failed: " + reason);
  }

  /**
   * Creates an exception for treasury depletion.
   *
   * @param entityName the entity with depleted treasury
   * @return the exception
   */
  public static EconomyException treasuryDepleted(String entityName) {
    return new EconomyException(entityName + "'s treasury is depleted");
  }
}
