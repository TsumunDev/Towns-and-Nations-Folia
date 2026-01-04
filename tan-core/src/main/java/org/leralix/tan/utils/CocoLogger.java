package org.leralix.tan.utils;

public class CocoLogger {

  public static final String RESET = "\u001B[0m";
  public static final String BLACK = "\u001B[30m";
  public static final String RED = "\u001B[31m";
  public static final String GREEN = "\u001B[32m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String PURPLE = "\u001B[35m";
  public static final String CYAN = "\u001B[36m";
  public static final String WHITE = "\u001B[37m";

  public static final String BRIGHT_BLACK = "\u001B[90m";
  public static final String BRIGHT_RED = "\u001B[91m";
  public static final String BRIGHT_GREEN = "\u001B[92m";
  public static final String BRIGHT_YELLOW = "\u001B[93m";
  public static final String BRIGHT_BLUE = "\u001B[94m";
  public static final String BRIGHT_PURPLE = "\u001B[95m";
  public static final String BRIGHT_CYAN = "\u001B[96m";
  public static final String BRIGHT_WHITE = "\u001B[97m";

  public static final String BOLD = "\u001B[1m";
  public static final String UNDERLINE = "\u001B[4m";
  public static final String REVERSED = "\u001B[7m";

  public static final String CHECK = "✓";
  public static final String CROSS = "✖";
  public static final String WARNING = "⚠";
  public static final String INFO = "ℹ";
  public static final String ARROW = "→";
  public static final String STAR = "★";
  public static final String GEAR = "⚙";
  public static final String DATABASE = "⛁";
  public static final String NETWORK = "⇄";
  public static final String ROCKET = "🚀";
  public static final String HOURGLASS = "⏱";

  public static void printBanner() {
    String banner =
        "\n"
            + BRIGHT_YELLOW
            + BOLD
            + "    ╔══════════════════════════════════════════════════╗\n"
            + "    ║  "
            + YELLOW
            + "▀▀ █▀█ █▀▀ █▀█ █▄ █ ▄▀█ ▀█▀ █ █▀█ █▄ █"
            + BRIGHT_YELLOW
            + "  ║\n"
            + "    ║  "
            + YELLOW
            + "▄▄ █▄█ █▄▄ █▄█ █ ▀█ █▀█  █  █ █▄█ █ ▀█"
            + BRIGHT_YELLOW
            + "  ║\n"
            + "    ╚══════════════════════════════════════════════════╝"
            + RESET
            + "\n\n"
            + "    "
            + BRIGHT_YELLOW
            + "★ "
            + YELLOW
            + "Système de Gestion Territoriale Multi-Serveurs"
            + RESET
            + "\n"
            + "    "
            + BRIGHT_YELLOW
            + "⚡ "
            + BRIGHT_WHITE
            + "Folia & Paper"
            + BRIGHT_YELLOW
            + " │ "
            + BRIGHT_GREEN
            + "800+ Joueurs"
            + RESET
            + "\n\n";

    System.out.println(banner);
  }

  public static String success(String message) {
    return BRIGHT_YELLOW + "  ✓ " + RESET + BRIGHT_WHITE + message + RESET;
  }

  public static String info(String message) {
    return BRIGHT_YELLOW + "  ℹ " + RESET + YELLOW + message + RESET;
  }

  public static String warning(String message) {
    return BRIGHT_YELLOW + "  ⚠ " + RESET + YELLOW + message + RESET;
  }

  public static String error(String message) {
    return BRIGHT_RED + "  ✖ " + RESET + BRIGHT_RED + message + RESET;
  }

  public static String loading(String module) {
    return BRIGHT_YELLOW + "  ⚙ " + RESET + YELLOW + module + RESET;
  }

  public static String database(String message) {
    return BRIGHT_YELLOW + "  ⛁ " + RESET + YELLOW + message + RESET;
  }

  public static String network(String message) {
    return BRIGHT_YELLOW + "  ⇄ " + RESET + YELLOW + message + RESET;
  }

  public static String performance(String message) {
    return BRIGHT_YELLOW + "  🚀 " + RESET + BRIGHT_WHITE + message + RESET;
  }

  public static String boxed(String message, String color) {
    String line = BRIGHT_YELLOW + "─".repeat(55) + RESET;
    String content =
        "\n"
            + line
            + "\n    "
            + BRIGHT_YELLOW
            + "★ "
            + RESET
            + BRIGHT_WHITE
            + BOLD
            + message
            + RESET
            + "\n"
            + line;

    return content;
  }

  public static String syncLog(String serverName, String status, long timeMs, String details) {
    String statusColor;
    String statusText;

    switch (status.toUpperCase()) {
      case "EN_COURS":
        statusColor = BRIGHT_YELLOW;
        statusText = "En cours";
        break;
      case "REUSSI":
        statusColor = BRIGHT_GREEN;
        statusText = "Réussi";
        break;
      case "ECHEC":
        statusColor = BRIGHT_RED;
        statusText = "Échec";
        break;
      default:
        statusColor = BRIGHT_WHITE;
        statusText = status;
    }

    String timeStr = (timeMs > 0) ? formatTime(timeMs) : "";

    return String.format(
        "%s[%s] %s%s %s| %s",
        statusColor,
        serverName,
        statusText,
        RESET,
        timeStr.isEmpty() ? "" : "(" + timeStr + ") ",
        details);
  }

  public static void section(String title) {
    String titleLine =
        "\n    "
            + BRIGHT_YELLOW
            + "◧ "
            + RESET
            + BOLD
            + BRIGHT_WHITE
            + title
            + RESET
            + "\n";

    System.out.println(titleLine);
  }

  public static String progressBar(int current, int total, int barLength) {
    int filled = (int) ((double) current / total * barLength);
    int empty = barLength - filled;

    String filledBar = BRIGHT_GREEN + "█".repeat(Math.max(0, filled)) + RESET;
    String emptyBar = BRIGHT_BLACK + "░".repeat(Math.max(0, empty)) + RESET;
    String percentage = String.format("%3d%%", (int) ((double) current / total * 100));

    return String.format(
        "%s[%s%s%s] %s%s %s(%d/%d)",
        BRIGHT_WHITE, RESET, filledBar, emptyBar, BRIGHT_CYAN, percentage, RESET, current, total);
  }

  public static String formatTime(long ms) {
    String color;
    if (ms < 50) {
      color = BRIGHT_GREEN;
    } else if (ms < 200) {
      color = BRIGHT_YELLOW;
    } else {
      color = BRIGHT_RED;
    }

    return color + ms + "ms" + RESET;
  }

  public static String prefix(String message) {
    return BRIGHT_CYAN
        + "["
        + BRIGHT_YELLOW
        + BOLD
        + "CocoNation"
        + RESET
        + BRIGHT_CYAN
        + "]"
        + RESET
        + " "
        + message;
  }

  public static String stripColors(String message) {
    return message.replaceAll("\u001B\\[[;\\d]*m", "");
  }
}
