package org.leralix.tan.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.exception.TanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Structured logging utility for the Towns and Nations plugin.
 * <p>
 * This class provides consistent, structured logging with context information
 * suitable for production environments. All log messages include:
 * <ul>
 *   <li><b>Timestamp</b> - ISO-8601 format</li>
 *   <li><b>Level</b> - TRACE, DEBUG, INFO, WARN, ERROR</li>
 *   <li><b>Component</b> - Plugin component (economy, territory, war, etc.)</li>
 *   <li><b>Context</b> - Additional structured data (player IDs, town IDs, etc.)</li>
 *   <li><b>Exception</b> - Stack traces with full context</li>
 * </ul>
 * </p>
 * <p>
 * <b>Thread Safety:</b><br>
 * This class is thread-safe and can be used concurrently. MDC (Mapped Diagnostic Context)
 * is used for per-thread context information.
 * </p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Simple logging
 * TanLogger.info("economy", "Player {} deposited {}", playerName, amount);
 *
 * // With context
 * TanLogger.withContext("townId", townId)
 *     .withContext("playerId", playerId)
 *     .info("economy", "Town deposit processed");
 *
 * // With exception
 * try {
 *     town.claimChunk(chunk);
 * } catch (TerritoryException e) {
 *     TanLogger.error("territory", "Failed to claim chunk", e)
 *         .addContext("townId", townId)
 *         .addContext("chunk", chunkX + "," + chunkZ);
 * }
 * }</pre>
 *
 * @since 0.15.0
 */
public final class TanLogger {

  private static final Logger ROOT_LOGGER = LoggerFactory.getLogger("TAN");
  private static final String PLUGIN_NAME = "TownsAndNations";
  private static final String VERSION = getVersion();

  // Component-specific loggers
  private static final Map<String, Logger> COMPONENT_LOGGERS = new ConcurrentHashMap<>();

  static {
    COMPONENT_LOGGERS.put("economy", LoggerFactory.getLogger("TAN.ECONOMY"));
    COMPONENT_LOGGERS.put("territory", LoggerFactory.getLogger("TAN.TERRITORY"));
    COMPONENT_LOGGERS.put("war", LoggerFactory.getLogger("TAN.WAR"));
    COMPONENT_LOGGERS.put("storage", LoggerFactory.getLogger("TAN.STORAGE"));
    COMPONENT_LOGGERS.put("gui", LoggerFactory.getLogger("TAN.GUI"));
    COMPONENT_LOGGERS.put("command", LoggerFactory.getLogger("TAN.COMMAND"));
    COMPONENT_LOGGERS.put("redis", LoggerFactory.getLogger("TAN.REDIS"));
    COMPONENT_LOGGERS.put("network", LoggerFactory.getLogger("TAN.NETWORK"));
  }

  private TanLogger() {
    throw new IllegalStateException("Utility class");
  }

  // ========== Basic Logging Methods ==========

  /**
   * Logs a TRACE level message.
   *
   * @param component the component name
   * @param message the message to log
   * @param args the message arguments
   */
  public static void trace(String component, String message, Object... args) {
    getLogger(component).trace(message, args);
  }

  /**
   * Logs a DEBUG level message.
   *
   * @param component the component name
   * @param message the message to log
   * @param args the message arguments
   */
  public static void debug(String component, String message, Object... args) {
    getLogger(component).debug(message, args);
  }

  /**
   * Logs an INFO level message.
   *
   * @param component the component name
   * @param message the message to log
   * @param args the message arguments
   */
  public static void info(String component, String message, Object... args) {
    getLogger(component).info(message, args);
  }

  /**
   * Logs a WARN level message.
   *
   * @param component the component name
   * @param message the message to log
   * @param args the message arguments
   */
  public static void warn(String component, String message, Object... args) {
    getLogger(component).warn(message, args);
  }

  /**
   * Logs an ERROR level message.
   *
   * @param component the component name
   * @param message the message to log
   * @param args the message arguments
   */
  public static void error(String component, String message, Object... args) {
    getLogger(component).error(message, args);
  }

  /**
   * Logs an ERROR level message with an exception.
   *
   * @param component the component name
   * @param message the message to log
   * @param throwable the exception to log
   */
  public static void error(String component, String message, Throwable throwable) {
    getLogger(component).error(message, throwable);
  }

  // ========== Structured Logging with Context ==========

  /**
   * Creates a log entry builder with context.
   *
   * @param key the context key
   * @param value the context value
   * @return a LogEntry builder
   */
  public static LogEntry withContext(String key, Object value) {
    return new LogEntry().addContext(key, value);
  }

  /**
   * Creates a log entry builder for command execution.
   *
   * @param sender the command sender
   * @param command the command name
   * @return a LogEntry builder pre-populated with command context
   */
  public static LogEntry forCommand(CommandSender sender, String command) {
    return new LogEntry()
        .addContext("eventType", "command")
        .addContext("command", command)
        .addContext("sender", sender.getName())
        .addContext("senderType", sender instanceof Player ? "player" : "console")
        .addContext("server", getServerName());
  }

  /**
   * Creates a log entry builder for player actions.
   *
   * @param player the player
   * @param action the action being performed
   * @return a LogEntry builder pre-populated with player context
   */
  public static LogEntry forPlayerAction(Player player, String action) {
    return new LogEntry()
        .addContext("eventType", "player_action")
        .addContext("action", action)
        .addContext("player", player.getName())
        .addContext("playerId", player.getUniqueId().toString())
        .addContext("world", player.getWorld().getName())
        .addContext("location", formatLocation(player.getLocation()));
  }

  /**
   * Creates a log entry builder for economy operations.
   *
   * @param operation the operation type (deposit, withdraw, tax, etc.)
   * @param amount the amount involved
   * @return a LogEntry builder pre-populated with economy context
   */
  public static LogEntry forEconomy(String operation, double amount) {
    return new LogEntry()
        .addContext("eventType", "economy")
        .addContext("operation", operation)
        .addContext("amount", amount);
  }

  /**
   * Creates a log entry builder for territory operations.
   *
   * @param territoryId the territory ID
   * @param operation the operation type
   * @return a LogEntry builder pre-populated with territory context
   */
  public static LogEntry forTerritory(String territoryId, String operation) {
    return new LogEntry()
        .addContext("eventType", "territory")
        .addContext("territoryId", territoryId)
        .addContext("operation", operation);
  }

  /**
   * Creates a log entry builder for errors.
   *
   * @param component the component where the error occurred
   * @param throwable the exception
   * @return a LogEntry builder pre-populated with error context
   */
  public static LogEntry forError(String component, Throwable throwable) {
    LogEntry entry = new LogEntry()
        .addContext("eventType", "error")
        .addContext("component", component)
        .addContext("exception", throwable.getClass().getSimpleName())
        .addContext("message", throwable.getMessage());

    if (throwable instanceof TanException tanEx) {
      entry.addContext("errorCode", tanEx.getErrorCode());
    }

    return entry;
  }

  // ========== Specialized Logging Methods ==========

  /**
   * Logs a command execution with full context.
   *
   * @param sender the command sender
   * @param command the command name
   * @param args the command arguments
   * @param durationMs the execution duration in milliseconds
   */
  public static void logCommand(CommandSender sender, String command, String[] args, long durationMs) {
    withContext("eventType", "command")
        .addContext("command", command)
        .addContext("sender", sender.getName())
        .addContext("senderType", sender instanceof Player ? "player" : "console")
        .addContext("args", String.join(" ", args))
        .addContext("durationMs", durationMs)
        .info("command", "Command executed in {}ms", durationMs);
  }

  /**
   * Logs a database query.
   *
   * @param queryType the type of query (SELECT, INSERT, UPDATE, DELETE)
   * @param table the table name
   * @param durationMs the execution duration
   */
  public static void logQuery(String queryType, String table, long durationMs) {
    withContext("eventType", "db_query")
        .addContext("queryType", queryType)
        .addContext("table", table)
        .addContext("durationMs", durationMs)
        .debug("storage", "DB query {} on {} took {}ms", queryType, table, durationMs);
  }

  /**
   * Logs a slow database query.
   *
   * @param queryType the type of query
   * @param table the table name
   * @param durationMs the execution duration
   */
  public static void logSlowQuery(String queryType, String table, long durationMs) {
    withContext("eventType", "slow_query")
        .addContext("queryType", queryType)
        .addContext("table", table)
        .addContext("durationMs", durationMs)
        .warn("storage", "Slow DB query detected: {} on {} took {}ms", queryType, table, durationMs);
  }

  /**
   * Logs a Redis operation.
   *
   * @param operation the operation type
   * @param key the Redis key (sanitized)
   * @param success whether the operation succeeded
   */
  public static void logRedis(String operation, String key, boolean success) {
    withContext("eventType", "redis_operation")
        .addContext("operation", operation)
        .addContext("key", sanitizeKey(key))
        .addContext("success", success)
        .debug("redis", "Redis {} {} - {}", operation, key, success ? "SUCCESS" : "FAILED");
  }

  /**
   * Logs a player joining the server.
   *
   * @param player the player
   */
  public static void logPlayerJoin(Player player) {
    forPlayerAction(player, "join")
        .addContext("ipAddress", player.getAddress() != null ?
            player.getAddress().getAddress().getHostAddress() : "unknown")
        .info("player", "Player {} joined", player.getName());
  }

  /**
   * Logs a player leaving the server.
   *
   * @param player the player
   */
  public static void logPlayerQuit(Player player) {
    forPlayerAction(player, "quit")
        .info("player", "Player {} quit", player.getName());
  }

  /**
   * Logs a chunk claim operation.
   *
   * @param townId the town claiming
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   * @param world the world name
   */
  public static void logChunkClaim(String townId, int chunkX, int chunkZ, String world) {
    withContext("eventType", "chunk_claim")
        .addContext("townId", townId)
        .addContext("chunkX", chunkX)
        .addContext("chunkZ", chunkZ)
        .addContext("world", world)
        .info("territory", "Chunk claimed at ({}, {}) in {} by {}", chunkX, chunkZ, world, townId);
  }

  // ========== Utility Methods ==========

  /**
   * Gets a logger for a specific component.
   *
   * @param component the component name
   * @return the logger
   */
  private static Logger getLogger(String component) {
    return COMPONENT_LOGGERS.getOrDefault(component.toLowerCase(), ROOT_LOGGER);
  }

  /**
   * Gets the plugin version.
   *
   * @return the version string
   */
  private static String getVersion() {
    try {
      return TownsAndNations.getPlugin().getDescription().getVersion();
    } catch (Exception e) {
      return "unknown";
    }
  }

  /**
   * Gets the server name.
   *
   * @return the server name
   */
  private static String getServerName() {
    return Bukkit.getServer().getName();
  }

  /**
   * Formats a location for logging.
   *
   * @param location the location
   * @return the formatted string
   */
  private static String formatLocation(org.bukkit.Location location) {
    if (location == null) return "unknown";
    return String.format("%s[%.0f,%.0f,%.0f]",
        location.getWorld() != null ? location.getWorld().getName() : "?",
        location.getX(), location.getY(), location.getZ());
  }

  /**
   * Sanitizes a Redis key for logging (removes sensitive data).
   *
   * @param key the Redis key
   * @return the sanitized key
   */
  private static String sanitizeKey(String key) {
    if (key == null) return "null";
    // Remove potential sensitive data patterns
    return key.replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "[UUID]")
        .replaceAll("\\d+", "[NUM]");
  }

  // ========== LogEntry Builder ==========

  /**
   * Builder for structured log entries with context.
   */
  public static final class LogEntry {
    private final Map<String, Object> context;
    private String level = "INFO";
    private String component;

    private LogEntry() {
      this.context = new ConcurrentHashMap<>();
    }

    /**
     * Adds context information to this log entry.
     *
     * @param key the context key
     * @param value the context value
     * @return this builder for chaining
     */
    public LogEntry addContext(String key, Object value) {
      this.context.put(key, value);
      // Also add to MDC for logging patterns
      MDC.put(key, String.valueOf(value));
      return this;
    }

    /**
     * Sets the log level.
     *
     * @param level the log level (TRACE, DEBUG, INFO, WARN, ERROR)
     * @return this builder for chaining
     */
    public LogEntry setLevel(String level) {
      this.level = level.toUpperCase();
      return this;
    }

    /**
     * Sets the component.
     *
     * @param component the component name
     * @return this builder for chaining
     */
    public LogEntry setComponent(String component) {
      this.component = component;
      return this;
    }

    /**
     * Logs the entry at TRACE level.
     *
     * @param message the message
     * @param args the message arguments
     */
    public void trace(String message, Object... args) {
      log("TRACE", message, args);
    }

    /**
     * Logs the entry at DEBUG level.
     *
     * @param message the message
     * @param args the message arguments
     */
    public void debug(String message, Object... args) {
      log("DEBUG", message, args);
    }

    /**
     * Logs the entry at INFO level.
     *
     * @param message the message
     * @param args the message arguments
     */
    public void info(String message, Object... args) {
      log("INFO", message, args);
    }

    /**
     * Logs the entry at WARN level.
     *
     * @param message the message
     * @param args the message arguments
     */
    public void warn(String message, Object... args) {
      log("WARN", message, args);
    }

    /**
     * Logs the entry at ERROR level.
     *
     * @param message the message
     * @param args the message arguments
     */
    public void error(String message, Object... args) {
      log("ERROR", message, args);
    }

    /**
     * Logs the entry at ERROR level with an exception.
     *
     * @param message the message
     * @param throwable the exception
     */
    public void error(String message, Throwable throwable) {
      log("ERROR", message, null, throwable);
    }

    private void log(String level, String message, Object[] args) {
      log(level, message, args, null);
    }

    private void log(String level, String message, Object[] args, Throwable throwable) {
      String comp = component != null ? component : "general";
      Logger logger = getLogger(comp);

      // Build full message with context
      String fullMessage = message;
      if (!context.isEmpty()) {
        fullMessage = message + " " + context;
      }

      // Log at appropriate level
      switch (level.toUpperCase()) {
        case "TRACE" -> logger.trace(fullMessage, args);
        case "DEBUG" -> logger.debug(fullMessage, args);
        case "INFO" -> logger.info(fullMessage, args);
        case "WARN" -> logger.warn(fullMessage, args);
        case "ERROR" -> {
          if (throwable != null) {
            logger.error(fullMessage, throwable);
          } else {
            logger.error(fullMessage, args);
          }
        }
      }

      // Clear MDC after logging
      clearMdc();
    }

    private void clearMdc() {
      for (String key : context.keySet()) {
        MDC.remove(key);
      }
    }
  }
}
