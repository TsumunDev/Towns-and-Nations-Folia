package org.leralix.tan.utils.commands;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.leralix.lib.data.SoundEnum;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.text.TanChatUtils;
import org.leralix.tan.validation.InputValidator;
import org.leralix.tan.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized exception and validation handling for command execution.
 * <p>
 * This class provides utilities for:
 * <ul>
 *   <li>Parsing command arguments (int, double, player)</li>
 *   <li>Validating argument counts and ranges</li>
 *   <li>Handling validation errors with user feedback</li>
 *   <li>Structured logging of command execution</li>
 * </ul>
 * </p>
 * <p>
 * <b>Thread Safety:</b><br>
 * All methods in this class are thread-safe and can be used concurrently.
 * </p>
 *
 * @since 0.15.0
 */
public class CommandExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(CommandExceptionHandler.class);
  private CommandExceptionHandler() {
    throw new IllegalStateException("Utility class");
  }
  public static Optional<Integer> parseInt(CommandSender sender, String value, String paramName) {
    try {
      return Optional.of(Integer.parseInt(value));
    } catch (NumberFormatException e) {
      TanChatUtils.message(sender, Lang.SYNTAX_ERROR_AMOUNT, SoundEnum.NOT_ALLOWED);
      logger.debug(
          "Failed to parse integer parameter '{}' from value: {} (Command sender: {})",
          paramName,
          value,
          sender.getName());
      return Optional.empty();
    }
  }
  public static Optional<Double> parseDouble(CommandSender sender, String value, String paramName) {
    try {
      return Optional.of(Double.parseDouble(value));
    } catch (NumberFormatException e) {
      TanChatUtils.message(sender, Lang.SYNTAX_ERROR_AMOUNT, SoundEnum.NOT_ALLOWED);
      logger.debug(
          "Failed to parse double parameter '{}' from value: {} (Command sender: {})",
          paramName,
          value,
          sender.getName());
      return Optional.empty();
    }
  }
  public static Optional<OfflinePlayer> findPlayer(CommandSender sender, String playerName) {
    try {
      OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(playerName);
      if (offlinePlayer.getName() == null && !offlinePlayer.hasPlayedBefore()) {
        TanChatUtils.message(sender, Lang.PLAYER_NOT_FOUND, SoundEnum.NOT_ALLOWED);
        logger.debug("Player not found: {} (Command sender: {})", playerName, sender.getName());
        return Optional.empty();
      }
      return Optional.of(offlinePlayer);
    } catch (Exception e) {
      TanChatUtils.message(sender, Lang.PLAYER_NOT_FOUND, SoundEnum.NOT_ALLOWED);
      logger.error(
          "Exception while looking up player: {} (Command sender: {})",
          playerName,
          sender.getName(),
          e);
      return Optional.empty();
    }
  }
  public static Optional<ITanPlayer> getTanPlayer(
      CommandSender sender, OfflinePlayer offlinePlayer) {
    try {
      ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(offlinePlayer);
      if (tanPlayer == null) {
        TanChatUtils.message(sender, Lang.PLAYER_NOT_FOUND, SoundEnum.NOT_ALLOWED);
        logger.warn(
            "TAN player data not found for: {} (Command sender: {})",
            offlinePlayer.getName(),
            sender.getName());
        return Optional.empty();
      }
      return Optional.of(tanPlayer);
    } catch (Exception e) {
      TanChatUtils.message(sender, Lang.PLAYER_NOT_FOUND, SoundEnum.NOT_ALLOWED);
      logger.error(
          "Exception while retrieving TAN player data for: {} (Command sender: {})",
          offlinePlayer.getName(),
          sender.getName(),
          e);
      return Optional.empty();
    }
  }
  public static boolean safeExecute(
      CommandSender sender, Supplier<Boolean> operation, Lang errorMessage) {
    try {
      return operation.get();
    } catch (Exception e) {
      if (errorMessage != null) {
        TanChatUtils.message(sender, errorMessage, SoundEnum.NOT_ALLOWED);
      } else {
        TanChatUtils.message(sender, Lang.SYNTAX_ERROR, SoundEnum.NOT_ALLOWED);
      }
      logger.error("Exception during command execution (Command sender: {})", sender.getName(), e);
      return false;
    }
  }
  public static boolean validateArgCount(
      CommandSender sender, String[] args, int expected, String syntax) {
    if (args.length < expected) {
      TanChatUtils.message(sender, Lang.NOT_ENOUGH_ARGS_ERROR, SoundEnum.NOT_ALLOWED);
      TanChatUtils.message(sender, Lang.CORRECT_SYNTAX_INFO.get(syntax));
      return false;
    } else if (args.length > expected) {
      TanChatUtils.message(sender, Lang.TOO_MANY_ARGS_ERROR, SoundEnum.NOT_ALLOWED);
      TanChatUtils.message(sender, Lang.CORRECT_SYNTAX_INFO.get(syntax));
      return false;
    }
    return true;
  }
  public static boolean validateMinArgCount(
      CommandSender sender, String[] args, int minimum, String syntax) {
    if (args.length < minimum) {
      TanChatUtils.message(sender, Lang.NOT_ENOUGH_ARGS_ERROR, SoundEnum.NOT_ALLOWED);
      TanChatUtils.message(sender, Lang.CORRECT_SYNTAX_INFO.get(syntax));
      return false;
    }
    return true;
  }
  public static boolean validateMaxArgCount(
      CommandSender sender, String[] args, int maximum, String syntax) {
    if (args.length > maximum) {
      TanChatUtils.message(sender, Lang.TOO_MANY_ARGS_ERROR, SoundEnum.NOT_ALLOWED);
      TanChatUtils.message(sender, Lang.CORRECT_SYNTAX_INFO.get(syntax));
      return false;
    }
    return true;
  }
  public static boolean validateArgCountRange(
      CommandSender sender, String[] args, int min, int max, String syntax) {
    if (args.length < min) {
      TanChatUtils.message(sender, Lang.NOT_ENOUGH_ARGS_ERROR, SoundEnum.NOT_ALLOWED);
      TanChatUtils.message(sender, Lang.CORRECT_SYNTAX_INFO.get(syntax));
      return false;
    } else if (args.length > max) {
      TanChatUtils.message(sender, Lang.TOO_MANY_ARGS_ERROR, SoundEnum.NOT_ALLOWED);
      TanChatUtils.message(sender, Lang.CORRECT_SYNTAX_INFO.get(syntax));
      return false;
    }
    return true;
  }
  public static Optional<Player> requirePlayer(CommandSender sender) {
    if (sender instanceof Player player) {
      return Optional.of(player);
    }
    sender.sendMessage("This command can only be executed by a player.");
    return Optional.empty();
  }
  public static void logCommandExecution(CommandSender sender, String commandName, String[] args) {
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Command executed: /{} {} (Sender: {})",
          commandName,
          String.join(" ", args),
          sender.getName());
    }
  }

  // ========== New validation methods using InputValidator ==========

  /**
   * Validates an integer parameter using InputValidator.
   *
   * @param sender the command sender
   * @param value the string value to parse
   * @param paramName the parameter name for error messages
   * @param min the minimum allowed value
   * @param max the maximum allowed value
   * @return the parsed integer, or empty if validation fails
   */
  public static Optional<Integer> parseInt(
      CommandSender sender, String value, String paramName, int min, int max) {
    try {
      int parsed = Integer.parseInt(value);
      int validated = InputValidator.validateIntRange(parsed, min, max);
      return Optional.of(validated);
    } catch (NumberFormatException e) {
      TanChatUtils.message(sender, Lang.SYNTAX_ERROR_AMOUNT, SoundEnum.NOT_ALLOWED);
      logger.debug(
          "Failed to parse integer parameter '{}' from value: {} (Command sender: {})",
          paramName,
          value,
          sender.getName());
      return Optional.empty();
    } catch (ValidationException e) {
      TanChatUtils.message(sender, Lang.SYNTAX_ERROR, SoundEnum.NOT_ALLOWED);
      logger.debug("Validation failed for '{}': {}", paramName, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Validates a money amount using InputValidator.
   *
   * @param sender the command sender
   * @param value the string value to parse
   * @param paramName the parameter name for error messages
   * @return the parsed amount, or empty if validation fails
   */
  public static Optional<Double> parseMoneyAmount(CommandSender sender, String value, String paramName) {
    try {
      double parsed = Double.parseDouble(value);
      double validated = InputValidator.validateMoneyAmount(parsed);
      return Optional.of(validated);
    } catch (NumberFormatException e) {
      TanChatUtils.message(sender, Lang.SYNTAX_ERROR_AMOUNT, SoundEnum.NOT_ALLOWED);
      logger.debug(
          "Failed to parse amount parameter '{}' from value: {} (Command sender: {})",
          paramName,
          value,
          sender.getName());
      return Optional.empty();
    } catch (ValidationException e) {
      TanChatUtils.message(sender, Lang.SYNTAX_ERROR, SoundEnum.NOT_ALLOWED);
      logger.debug("Validation failed for '{}': {}", paramName, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Validates a town name using InputValidator.
   *
   * @param sender the command sender
   * @param name the town name to validate
   * @return the validated name, or empty if validation fails
   */
  public static Optional<String> validateTownName(CommandSender sender, String name) {
    try {
      String validated = InputValidator.validateTownName(name);
      return Optional.of(validated);
    } catch (ValidationException e) {
      TanChatUtils.message(sender, Lang.SYNTAX_ERROR, SoundEnum.NOT_ALLOWED);
      logger.debug("Invalid town name '{}': {}", InputValidator.sanitizeForLog(name), e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Handles a ValidationException with proper user feedback and logging.
   *
   * @param sender the command sender
   * @param e the validation exception
   * @return true if the exception was handled, false otherwise
   */
  public static boolean handleValidationException(CommandSender sender, ValidationException e) {
    if (e.isPermissionError()) {
      TanChatUtils.message(sender, Lang.PLAYER_ACTION_NO_PERMISSION, SoundEnum.NOT_ALLOWED);
    } else if (e.isFormatError()) {
      TanChatUtils.message(sender, Lang.SYNTAX_ERROR, SoundEnum.NOT_ALLOWED);
    } else {
      TanChatUtils.message(sender, Lang.SYNTAX_ERROR, SoundEnum.NOT_ALLOWED);
    }

    logger.info("Command validation failed for {}: {}", sender.getName(), e.getLogMessage());
    return true;
  }

  /**
   * Executes a command operation with comprehensive exception handling.
   *
   * @param sender the command sender
   * @param operation the operation to execute
   * @param errorMessage the error message to show on failure
   * @return true if the operation succeeded, false otherwise
   */
  public static boolean executeWithValidation(
      CommandSender sender, Supplier<Boolean> operation, Lang errorMessage) {
    try {
      return operation.get();
    } catch (ValidationException e) {
      handleValidationException(sender, e);
      return false;
    } catch (Exception e) {
      if (errorMessage != null) {
        TanChatUtils.message(sender, errorMessage, SoundEnum.NOT_ALLOWED);
      } else {
        TanChatUtils.message(sender, Lang.SYNTAX_ERROR, SoundEnum.NOT_ALLOWED);
      }
      logger.error("Exception during command execution (Command sender: {})", sender.getName(), e);
      return false;
    }
  }
}