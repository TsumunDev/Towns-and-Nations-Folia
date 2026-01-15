package org.leralix.tan.profiling;

import dev.triumphteam.gui.guis.Gui;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.FoliaScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Benchmark utility for measuring GUI performance.
 *
 * <p>This class provides methods to measure and record the time it takes to perform
 * various GUI operations, helping identify performance bottlenecks.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * GUIBenchmark.startBenchmark("MainMenu.open");
 * MainMenu.open(player);
 * GUIBenchmark.endBenchmark("MainMenu.open");
 * }</pre>
 */
public class GUIBenchmark {

    private static final Logger logger = LoggerFactory.getLogger(GUIBenchmark.class);

    private static final Map<String, BenchmarkResult> results = new LinkedHashMap<>();
    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();
    private static final ThreadLocal<String> currentOperation = new ThreadLocal<>();

    private GUIBenchmark() {}

    /**
     * Starts a benchmark for the given operation.
     *
     * @param operationName the name of the operation being benchmarked
     */
    public static void startBenchmark(String operationName) {
        startTime.set(System.nanoTime());
        currentOperation.set(operationName);
        logger.debug("[Benchmark] Starting: {}", operationName);
    }

    /**
     * Ends the current benchmark and records the result.
     */
    public static void endBenchmark() {
        Long start = startTime.get();
        String operation = currentOperation.get();

        if (start == null || operation == null) {
            logger.warn("[Benchmark] endBenchmark called without startBenchmark");
            return;
        }

        long end = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(end - start);

        recordResult(operation, durationMs);

        startTime.remove();
        currentOperation.remove();
    }

    /**
     * Records a benchmark result.
     *
     * @param operation the operation name
     * @param durationMs the duration in milliseconds
     */
    public static void recordResult(String operation, long durationMs) {
        results.computeIfAbsent(operation, k -> new BenchmarkResult(operation))
               .addSample(durationMs);

        logger.debug("[Benchmark] {} completed in {}ms", operation, durationMs);

        if (durationMs > 500) {
            logger.warn("[Benchmark] SLOW OPERATION: {} took {}ms", operation, durationMs);
        }
    }

    /**
     * Gets all benchmark results.
     *
     * @return a map of operation names to benchmark results
     */
    public static Map<String, BenchmarkResult> getResults() {
        return new LinkedHashMap<>(results);
    }

    /**
     * Gets the benchmark result for a specific operation.
     *
     * @param operation the operation name
     * @return the benchmark result, or null if not found
     */
    public static BenchmarkResult getResult(String operation) {
        return results.get(operation);
    }

    /**
     * Clears all benchmark results.
     */
    public static void clearResults() {
        results.clear();
        logger.info("[Benchmark] All results cleared");
    }

    /**
     * Prints a summary of all benchmark results to the console.
     */
    public static void printSummary() {
        if (results.isEmpty()) {
            logger.info("[Benchmark] No results to display");
            return;
        }

        logger.info("=".repeat(80));
        logger.info("GUI PERFORMANCE BENCHMARK SUMMARY");
        logger.info("=".repeat(80));

        results.values().forEach(result -> {
            logger.info(String.format(
                "%-50s | Avg: %4dms | Min: %4dms | Max: %4dms | Samples: %4d",
                result.getOperation(),
                result.getAverageMs(),
                result.getMinMs(),
                result.getMaxMs(),
                result.getSampleCount()
            ));
        });

        logger.info("=".repeat(80));
    }

    /**
     * Gets the top N slowest operations.
     *
     * @param n the number of results to return
     * @return a list of benchmark results sorted by average time (descending)
     */
    public static List<BenchmarkResult> getTopSlowest(int n) {
        return results.values().stream()
            .sorted((a, b) -> Long.compare(b.getAverageMs(), a.getAverageMs()))
            .limit(n)
            .toList();
    }

    /**
     * Represents a single benchmark result with statistics.
     */
    public static class BenchmarkResult {
        private final String operation;
        private final List<Long> samples = new ArrayList<>();
        private long minMs = Long.MAX_VALUE;
        private long maxMs = 0;
        private long totalMs = 0;

        public BenchmarkResult(String operation) {
            this.operation = operation;
        }

        void addSample(long durationMs) {
            samples.add(durationMs);
            totalMs += durationMs;
            minMs = Math.min(minMs, durationMs);
            maxMs = Math.max(maxMs, durationMs);
        }

        public String getOperation() {
            return operation;
        }

        public long getAverageMs() {
            return samples.isEmpty() ? 0 : totalMs / samples.size();
        }

        public long getMinMs() {
            return samples.isEmpty() ? 0 : minMs;
        }

        public long getMaxMs() {
            return samples.isEmpty() ? 0 : maxMs;
        }

        public int getSampleCount() {
            return samples.size();
        }

        public long getPercentile(int percentile) {
            if (samples.isEmpty()) {
                return 0;
            }

            List<Long> sorted = new ArrayList<>(samples);
            sorted.sort(Long::compareTo);

            int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
        }

        public long getP95() {
            return getPercentile(95);
        }

        public long getP99() {
            return getPercentile(99);
        }
    }

    /**
     * Benchmarks a GUI opening operation with async tracking.
     *
     * @param playerName the name of the player opening the GUI
     * @param guiName the name of the GUI being opened
     * @param guiOpener a function that opens the GUI
     * @return a CompletableFuture that completes when the GUI is opened
     */
    public static CompletableFuture<Void> benchmarkGUIOpen(
            String playerName,
            String guiName,
            Consumer<Player> guiOpener) {

        String operation = guiName + ".open";
        startBenchmark(operation);

        return CompletableFuture.runAsync(() -> {
            try {
                // Find the player
                Player player = TownsAndNations.getPlugin().getServer()
                    .getPlayer(playerName);

                if (player == null) {
                    logger.warn("[Benchmark] Player not found: {}", playerName);
                    return;
                }

                // Open the GUI on the player's region thread
                FoliaScheduler.runEntityTask(
                    TownsAndNations.getPlugin(),
                    player,
                    () -> {
                        try {
                            guiOpener.accept(player);
                            endBenchmark();
                        } catch (Exception e) {
                            logger.error("[Benchmark] Error opening GUI", e);
                            endBenchmark();
                        }
                    }
                );
            } catch (Exception e) {
                logger.error("[Benchmark] Error in benchmarkGUIOpen", e);
                endBenchmark();
            }
        });
    }

    /**
     * Benchmarks an async data loading operation.
     *
     * @param <T> the type of data being loaded
     * @param operationName the name of the operation
     * @param future the CompletableFuture to benchmark
     * @return a CompletableFuture that completes when the operation is done
     */
    public static <T> CompletableFuture<T> benchmarkAsync(
            String operationName,
            CompletableFuture<T> future) {

        startBenchmark(operationName);

        return future.whenComplete((result, throwable) -> {
            endBenchmark();
            if (throwable != null) {
                logger.error("[Benchmark] Error in async operation: {}", operationName, throwable);
            }
        });
    }

    /**
     * Benchmarks a database query operation.
     *
     * @param <T> the type of data being queried
     * @param operationName the name of the query
     * @param query the query operation to benchmark
     * @return the result of the query
     */
    public static <T> T benchmarkQuery(String operationName, java.util.function.Supplier<T> query) {
        startBenchmark(operationName);
        try {
            T result = query.get();
            endBenchmark();
            return result;
        } catch (Exception e) {
            endBenchmark();
            logger.error("[Benchmark] Error in query: {}", operationName, e);
            throw e;
        }
    }

    /**
     * Benchmarks a GUI item creation operation.
     *
     * @param itemName the name of the item being created
     * @param itemCreator the function that creates the item
     * @return the created ItemStack
     */
    public static ItemStack benchmarkItemCreation(
            String itemName,
            java.util.function.Supplier<ItemStack> itemCreator) {

        startBenchmark("GUIItem.create." + itemName);
        try {
            ItemStack item = itemCreator.get();
            endBenchmark();
            return item;
        } catch (Exception e) {
            endBenchmark();
            logger.error("[Benchmark] Error creating item: {}", itemName, e);
            throw e;
        }
    }

    /**
     * Generates a performance report as a formatted string.
     *
     * @return the performance report
     */
    public static String generateReport() {
        StringBuilder report = new StringBuilder();

        report.append("\n");
        report.append("=".repeat(80)).append("\n");
        report.append("GUI PERFORMANCE BENCHMARK REPORT\n");
        report.append("Generated: ").append(new java.util.Date()).append("\n");
        report.append("=".repeat(80)).append("\n\n");

        if (results.isEmpty()) {
            report.append("No benchmark data available.\n");
            return report.toString();
        }

        // Summary statistics
        report.append("SUMMARY\n");
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("%-50s | %8s | %8s | %8s | %8s | %8s\n",
            "Operation", "Avg", "P95", "P99", "Min", "Max"));
        report.append("-".repeat(80)).append("\n");

        results.values().forEach(result -> {
            report.append(String.format("%-50s | %6dms | %6dms | %6dms | %6dms | %6dms\n",
                result.getOperation(),
                result.getAverageMs(),
                result.getP95(),
                result.getP99(),
                result.getMinMs(),
                result.getMaxMs()));
        });

        report.append("\n");

        // Top 5 slowest operations
        report.append("TOP 5 SLOWEST OPERATIONS\n");
        report.append("-".repeat(80)).append("\n");

        List<BenchmarkResult> slowest = getTopSlowest(5);
        for (int i = 0; i < slowest.size(); i++) {
            BenchmarkResult result = slowest.get(i);
            report.append(String.format("%d. %s\n", i + 1, result.getOperation()));
            report.append(String.format("   Average: %dms, P95: %dms, P99: %dms, Samples: %d\n\n",
                result.getAverageMs(),
                result.getP95(),
                result.getP99(),
                result.getSampleCount()));
        }

        report.append("=".repeat(80)).append("\n");

        return report.toString();
    }

    /**
     * Saves the benchmark report to a file.
     *
     * @param filePath the path to save the report
     */
    public static void saveReport(String filePath) {
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get(filePath),
                generateReport().getBytes()
            );
            logger.info("[Benchmark] Report saved to: {}", filePath);
        } catch (Exception e) {
            logger.error("[Benchmark] Failed to save report", e);
        }
    }
}
