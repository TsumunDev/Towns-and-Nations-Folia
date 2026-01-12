package org.leralix.tan.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.leralix.lib.commands.SubCommand;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.performance.PerformanceMonitor;
import org.leralix.tan.performance.PerformanceMonitor.Statistics;
import org.leralix.tan.utils.text.TanChatUtils;

/**
 * Admin command to view performance statistics.
 *
 * <p>Usage: /ccnadmin perfstats [operation]</p>
 *
 * <p>Without arguments: Shows statistics for all tracked operations<br>
 * With argument: Shows detailed statistics for specific operation</p>
 *
 * @since 0.16.0
 */
public class PerformanceStatsCommand extends SubCommand {

  @Override
  public String getName() {
    return "perfstats";
  }

  @Override
  public String getDescription() {
    return "View async operation performance statistics";
  }

  @Override
  public int getArguments() {
    return 0; // 0-1 arguments (optional operation name)
  }

  @Override
  public String getSyntax() {
    return "/ccnadmin perfstats [operation]";
  }

  @Override
  public List<String> getTabCompleteSuggestions(CommandSender sender, String lowerCase, String[] args) {
    List<String> suggestions = new ArrayList<>();

    if (args.length == 2) {
      // Suggest tracked operation names
      Set<String> operations = PerformanceMonitor.getTrackedOperations();
      suggestions.addAll(operations);
      suggestions.add("all");
      suggestions.add("reset");
    }

    return suggestions;
  }

  @Override
  public void perform(CommandSender sender, String[] args) {
    if (args.length == 1) {
      // Show all statistics
      showAllStatistics(sender);
    } else if (args.length == 2) {
      String arg = args[1].toLowerCase();

      if (arg.equals("reset")) {
        if (sender.hasPermission("tan.admin.perfstats.reset")) {
          PerformanceMonitor.reset();
          TanChatUtils.message(sender, "§aPerformance statistics have been reset.");
        } else {
          TanChatUtils.message(sender, "§cYou don't have permission to reset statistics.");
        }
      } else if (arg.equals("all")) {
        showAllStatistics(sender);
      } else {
        // Show specific operation statistics
        showOperationStatistics(sender, args[1]);
      }
    } else {
      TanChatUtils.message(sender, "§cUsage: " + getSyntax());
    }
  }

  /**
   * Displays statistics for all tracked operations.
   *
   * @param sender The command sender
   */
  private void showAllStatistics(CommandSender sender) {
    TanChatUtils.message(sender, "§6=== Async Performance Statistics ===");

    Set<String> operations = PerformanceMonitor.getTrackedOperations();
    if (operations.isEmpty()) {
      TanChatUtils.message(sender, "§eNo operations tracked yet.");
      return;
    }

    for (String operationName : operations) {
      Statistics stats = PerformanceMonitor.getStatistics(operationName);
      if (stats != null) {
        TanChatUtils.message(sender, String.format(
            "§e%s: §f%d calls | §a%.2f%% success | §bAvg: %.2fms | §7Min: %.2fms | §cMax: %.2fms",
            operationName,
            stats.callCount(),
            stats.getSuccessRate(),
            stats.getAverageDurationMs(),
            stats.getMinDurationMs(),
            stats.getMaxDurationMs()
        ));
      }
    }

    TanChatUtils.message(sender, "§6=====================================");
    TanChatUtils.message(sender, "§7Use §e/ccnadmin perfstats <operation> §7for details");
  }

  /**
   * Displays detailed statistics for a specific operation.
   *
   * @param sender The command sender
   * @param operationName The operation name
   */
  private void showOperationStatistics(CommandSender sender, String operationName) {
    Statistics stats = PerformanceMonitor.getStatistics(operationName);

    if (stats == null) {
      TanChatUtils.message(sender, "§cNo statistics found for operation: §e" + operationName);
      return;
    }

    TanChatUtils.message(sender, "§6=== Performance: " + operationName + " ===");
    TanChatUtils.message(sender, String.format("§eTotal Calls: §f%d", stats.callCount()));
    TanChatUtils.message(sender, String.format("§aSuccessful: §f%d (%.2f%%)", stats.successCount(), stats.getSuccessRate()));
    TanChatUtils.message(sender, String.format("§cFailed: §f%d (%.2f%%)", stats.failureCount(), 100 - stats.getSuccessRate()));
    TanChatUtils.message(sender, "");
    TanChatUtils.message(sender, String.format("§bAverage Duration: §f%.2fms", stats.getAverageDurationMs()));
    TanChatUtils.message(sender, String.format("§7Min Duration: §f%.2fms", stats.getMinDurationMs()));
    TanChatUtils.message(sender, String.format("§cMax Duration: §f%.2fms", stats.getMaxDurationMs()));
    TanChatUtils.message(sender, "§6======================================");
  }
}
