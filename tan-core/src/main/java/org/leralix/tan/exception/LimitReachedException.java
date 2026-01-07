package org.leralix.tan.exception;

/**
 * Exception thrown when a territory or player has reached a maximum limit.
 * <p>
 * This includes scenarios such as:
 * <ul>
 *   <li>Town has reached maximum chunks</li>
 *   <li>Town has reached maximum players</li>
 *   <li>Player has reached maximum property count</li>
 *   <li>Rank count limit reached</li>
 * </ul>
 * </p>
 *
 * @since 0.15.0
 */
public class LimitReachedException extends TerritoryException {

  private static final long serialVersionUID = 1L;

  private final String limitType;
  private final int current;
  private final int maximum;
  private final String entityName;

  /**
   * Creates an exception for a reached limit.
   *
   * @param limitType the type of limit (e.g., "chunks", "players", "properties")
   * @param current the current value
   * @param maximum the maximum allowed value
   * @param entityName the name of the entity that reached the limit
   */
  public LimitReachedException(String limitType, int current, int maximum, String entityName) {
    super("TERR_002", String.format("%s has reached maximum %s: %d/%d",
        entityName, limitType, current, maximum), null);
    this.limitType = limitType;
    this.current = current;
    this.maximum = maximum;
    this.entityName = entityName;

    addContext("limitType", limitType);
    addContext("current", current);
    addContext("maximum", maximum);
    addContext("entityName", entityName);
  }

  /**
   * Creates an exception for maximum chunks reached.
   *
   * @param townName the town name
   * @param current the current chunk count
   * @param maximum the maximum allowed chunks
   * @return the exception
   */
  public static LimitReachedException maxChunks(String townName, int current, int maximum) {
    return new LimitReachedException("chunks", current, maximum, townName);
  }

  /**
   * Creates an exception for maximum players reached.
   *
   * @param townName the town name
   * @param current the current player count
   * @param maximum the maximum allowed players
   * @return the exception
   */
  public static LimitReachedException maxPlayers(String townName, int current, int maximum) {
    return new LimitReachedException("players", current, maximum, townName);
  }

  /**
   * Creates an exception for maximum properties reached.
   *
   * @param playerName the player name
   * @param current the current property count
   * @param maximum the maximum allowed properties
   * @return the exception
   */
  public static LimitReachedException maxProperties(String playerName, int current, int maximum) {
    return new LimitReachedException("properties", current, maximum, playerName);
  }

  public String getLimitType() {
    return limitType;
  }

  public int getCurrent() {
    return current;
  }

  public int getMaximum() {
    return maximum;
  }

  public String getEntityName() {
    return entityName;
  }

  @Override
  public String getUserMessage() {
    return String.format("%s has reached the maximum %s (%d/%d)",
        entityName, limitType, current, maximum);
  }

  @Override
  public boolean isRecoverable() {
    return false; // Cannot proceed without upgrading or removing items
  }
}
