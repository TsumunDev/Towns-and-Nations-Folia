package org.leralix.tan.commands.admin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.gui.user.MainMenu;
import org.leralix.tan.gui.user.territory.TownMenu;
import org.leralix.tan.gui.user.property.PlayerPropertiesMenu;
import org.leralix.tan.gui.user.territory.RegionMenu;
import org.leralix.tan.profiling.GUIBenchmark;
import org.leralix.tan.profiling.PerformanceProfiler;
import org.leralix.tan.utils.text.TanChatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command for profiling GUI performance.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>/ccnadmin profile start - Start profiling</li>
 *   <li>/ccnadmin profile stop - Stop and print results</li>
 *   <li>/ccnadmin profile clear - Clear profiling data</li>
 *   <li>/ccnadmin profile report - Generate detailed report</li>
 *   <li>/ccnadmin profile benchmark <gui> - Benchmark a specific GUI</li>
 * </ul>
 */
public class PerformanceProfileCommand {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceProfileCommand.class);
    private static boolean profiling = false;

    public static void register() {
        new CommandAPICommand("profile")
            .withPermission("tan.admin")
            .withSubcommand(new Subcommand("start")
                .executes((sender, args) -> {
                    startProfiling(sender);
                }))
            .withSubcommand(new Subcommand("stop")
                .executes((sender, args) -> {
                    stopProfiling(sender);
                }))
            .withSubcommand(new Subcommand("clear")
                .executes((sender, args) -> {
                    clearProfiling(sender);
                }))
            .withSubcommand(new Subcommand("report")
                .executes((sender, args) -> {
                    generateReport(sender);
                }))
            .withSubcommand(new Subcommand("benchmark")
                .withArguments(org.leralix.tan.commandlib.CustomArguments.GUIArgument.argument("gui",
                    "The GUI to benchmark (MainMenu, TownMenu, RegionMenu, PropertyMenu)"))
                .executesPlayer((player, args) -> {
                    String guiType = (String) args[0];
                    benchmarkGUI(player, guiType);
                }))
            .register();

        logger.info("PerformanceProfileCommand registered");
    }

    private static void startProfiling(CommandSender sender) {
        if (profiling) {
            TanChatUtils.sendMessage(sender, "§cProfiling is already running!");
            return;
        }

        profiling = true;
        PerformanceProfiler.setEnabled(true);
        GUIBenchmark.clearResults();

        TanChatUtils.sendMessage(sender, "§aProfiling started. Perform GUI operations now.");
        TanChatUtils.sendMessage(sender, "§eUse /ccnadmin profile stop to view results.");

        logger.info("[Profile] Profiling session started");
    }

    private static void stopProfiling(CommandSender sender) {
        if (!profiling) {
            TanChatUtils.sendMessage(sender, "§cNo profiling session in progress!");
            return;
        }

        profiling = false;
        PerformanceProfiler.setEnabled(false);

        TanChatUtils.sendMessage(sender, "§aProfiling stopped.");
        TanChatUtils.sendMessage(sender, "§eGenerating reports...");

        logger.info("[Profile] Profiling session stopped");

        GUIBenchmark.printSummary();
        PerformanceProfiler.report();

        TanChatUtils.sendMessage(sender, "§aReports generated. Check console for details.");
    }

    private static void clearProfiling(CommandSender sender) {
        GUIBenchmark.clearResults();
        PerformanceProfiler.clear();

        TanChatUtils.sendMessage(sender, "§aAll profiling data cleared.");

        logger.info("[Profile] Profiling data cleared");
    }

    private static void generateReport(CommandSender sender) {
        String reportPath = "logs/gui_performance_report.txt";
        GUIBenchmark.saveReport(reportPath);

        TanChatUtils.sendMessage(sender, "§aPerformance report saved to: §e" + reportPath);
        TanChatUtils.sendMessage(sender, "§eCheck console for quick summary.");

        GUIBenchmark.printSummary();

        logger.info("[Profile] Report generated: {}", reportPath);
    }

    private static void benchmarkGUI(Player player, String guiType) {
        if (!player.isOp()) {
            TanChatUtils.sendMessage(player, "§cOnly ops can use this command!");
            return;
        }

        TanChatUtils.sendMessage(player, "§eBenchmarking " + guiType + "...");

        switch (guiType.toLowerCase()) {
            case "mainmenu" -> GUIBenchmark.benchmarkGUIOpen(
                player.getName(),
                "MainMenu",
                MainMenu::open
            );

            case "townmenu" -> {
                TownsAndNations.getPlugin().getServer().getScheduler()
                    .runTaskAsynchronously(TownsAndNations.getPlugin(), () -> {
                        // Get player's town
                        org.leralix.tan.storage.stored.PlayerDataStorage.getInstance()
                            .get(player)
                            .thenAccept(tanPlayer -> {
                                if (!tanPlayer.hasTown()) {
                                    TanChatUtils.sendMessage(player, "§cYou must be in a town to benchmark TownMenu!");
                                    return;
                                }

                                tanPlayer.getTown().thenAccept(townData -> {
                                    GUIBenchmark.benchmarkGUIOpen(
                                        player.getName(),
                                        "TownMenu",
                                        p -> TownMenu.open(p, townData)
                                    );
                                });
                            });
                    });
            }

            case "regionmenu" -> {
                TownsAndNations.getPlugin().getServer().getScheduler()
                    .runTaskAsynchronously(TownsAndNations.getPlugin(), () -> {
                        org.leralix.tan.storage.stored.PlayerDataStorage.getInstance()
                            .get(player)
                            .thenAccept(tanPlayer -> {
                                if (!tanPlayer.hasRegion()) {
                                    TanChatUtils.sendMessage(player, "§cYou must be in a region to benchmark RegionMenu!");
                                    return;
                                }

                                tanPlayer.getRegion().thenAccept(regionData -> {
                                    GUIBenchmark.benchmarkGUIOpen(
                                        player.getName(),
                                        "RegionMenu",
                                        p -> RegionMenu.open(p, regionData)
                                    );
                                });
                            });
                    });
            }

            case "propertymenu" -> GUIBenchmark.benchmarkGUIOpen(
                player.getName(),
                "PropertyMenu",
                PlayerPropertiesMenu::open
            );

            default -> {
                TanChatUtils.sendMessage(player, "§cUnknown GUI type: " + guiType);
                TanChatUtils.sendMessage(player, "§eAvailable: MainMenu, TownMenu, RegionMenu, PropertyMenu");
            }
        }
    }
}
