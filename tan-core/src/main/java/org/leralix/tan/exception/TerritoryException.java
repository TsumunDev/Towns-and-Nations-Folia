package org.leralix.tan.exception;

/**
 * Exception thrown when a territory-related operation fails.
 * <p>
 * Territory exceptions encompass issues with towns, regions, land claims,
 * and territorial operations such as claiming, unclaiming, and managing.
 * </p>
 *
 * @since 0.15.0
 */
public class TerritoryException extends TanException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a territory exception with a message.
   *
   * @param message the error message
   */
  public TerritoryException(String message) {
    super("TERR_000", message, null);
  }

  /**
   * Creates a territory exception with a message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public TerritoryException(String message, Throwable cause) {
    super("TERR_000", message, cause);
  }

  /**
   * Creates a territory exception with full error information.
   *
   * @param errorCode the specific error code
   * @param message the error message
   * @param cause the underlying cause
   */
  public TerritoryException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  /**
   * Creates an exception for territory not found.
   *
   * @param territoryId the territory ID
   * @return the exception
   */
  public static TerritoryException notFound(String territoryId) {
    return new TerritoryException("Territory not found: " + territoryId);
  }

  /**
   * Creates an exception for town not found.
   *
   * @param townName the town name
   * @return the exception
   */
  public static TerritoryException townNotFound(String townName) {
    return new TerritoryException("Town not found: " + townName);
  }

  /**
   * Creates an exception for region not found.
   *
   * @param regionName the region name
   * @return the exception
   */
  public static TerritoryException regionNotFound(String regionName) {
    return new TerritoryException("Region not found: " + regionName);
  }

  /**
   * Creates an exception for player not in a town.
   *
   * @param playerName the player name
   * @return the exception
   */
  public static TerritoryException notInTown(String playerName) {
    return new TerritoryException(playerName + " is not a member of any town");
  }

  /**
   * Creates an exception for player not the leader.
   *
   * @param playerName the player name
   * @param territoryName the territory name
   * @return the exception
   */
  public static TerritoryException notLeader(String playerName, String territoryName) {
    return new TerritoryException(playerName + " is not the leader of " + territoryName);
  }
}