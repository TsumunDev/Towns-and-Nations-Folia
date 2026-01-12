package org.leralix.tan.performance;

import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.leralix.tan.TownsAndNations;

/**
 * Performance monitoring utility for tracking async operation metrics.
 *
 * <p>This class provides thread-safe performance monitoring for async operations
 * including execution time, success/failure rates, and operation counts.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Monitor an operation
 * long startTime = PerformanceMonitor.startOperation("playerDataLoad");
 * try {
 *     // ... perform operation ...
 *     PerformanceMonitor.recordSuccess("playerDataLoad", startTime);
 * } catch (Exception e) {
 *     PerformanceMonitor.recordFailure("playerDataLoad", startTime);
 * }
 * }</pre>
 *
 * @since 0.16.0
 */
public class PerformanceMonitor {

  private static final ConcurrentHashMap<String, OperationMetrics> metrics = new ConcurrentHashMap<>();

  /** Thread-unsafe metrics class - use per-operation key */
  private static class OperationMetrics {
    private final LongAdder callCount = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final LongAdder totalDurationNanos = new LongAdder();
    private final AtomicLong minDurationNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxDurationNanos = new AtomicLong(0);

    void record(long durationNanos, boolean success) {
      callCount.increment();
      totalDurationNanos.add(durationNanos);

      // Update min/max
      long currentMin = minDurationNanos.get();
      while (durationNanos < currentMin && !minDurationNanos.compareAndSet(currentMin, durationNanos)) {
        currentMin = minDurationNanos.get();
      }

      long currentMax = maxDurationNanos.get();
      while (durationNanos > currentMax && !maxDurationNanos.compareAndSet(currentMax, durationNanos)) {
        currentMax = maxDurationNanos.get();
      }

      if (success) {
        successCount.increment();
      } else {
        failureCount.increment();
      }
    }

    Statistics getStatistics() {
      long calls = callCount.sum();
      long successes = successCount.sum();
      long failures = failureCount.sum();
      long totalNanos = totalDurationNanos.sum();

      return new Statistics(
          calls,
          successes,
          failures,
          calls > 0 ? totalNanos / calls : 0,
          minDurationNanos.get(),
          maxDurationNanos.get()
      );
    }
  }

  /**
   * Statistics snapshot for an operation.
   *
   * @param callCount Total number of calls
   * @param successCount Number of successful calls
   * @param failureCount Number of failed calls
   * @param averageDurationNanos Average duration in nanoseconds
   * @param minDurationNanos Minimum duration in nanoseconds
   * @param maxDurationNanos Maximum duration in nanoseconds
   */
  public record Statistics(
      long callCount,
      long successCount,
      long failureCount,
      long averageDurationNanos,
      long minDurationNanos,
      long maxDurationNanos
  ) {
    /** @return Success rate as percentage (0-100) */
    public double getSuccessRate() {
      return callCount > 0 ? (successCount * 100.0 / callCount) : 0;
    }

    /** @return Average duration in milliseconds */
    public double getAverageDurationMs() {
      return averageDurationNanos / 1_000_000.0;
    }

    /** @return Minimum duration in milliseconds */
    public double getMinDurationMs() {
      return minDurationNanos / 1_000_000.0;
    }

    /** @return Maximum duration in milliseconds */
    public double getMaxDurationMs() {
      return maxDurationNanos / 1_000_000.0;
    }
  }

  /**
   * Records the start of an operation.
   *
   * @param operationName Unique name for the operation type
   * @return Start timestamp in nanoseconds
   */
  public static long startOperation(String operationName) {
    return System.nanoTime();
  }

  /**
   * Records a successful operation completion.
   *
   * @param operationName Unique name for the operation type
   * @param startTimeNanos Start timestamp from {@link #startOperation}
   */
  public static void recordSuccess(String operationName, long startTimeNanos) {
    long duration = System.nanoTime() - startTimeNanos;
    record(operationName, duration, true);
  }

  /**
   * Records a failed operation.
   *
   * @param operationName Unique name for the operation type
   * @param startTimeNanos Start timestamp from {@link #startOperation}
   */
  public static void recordFailure(String operationName, long startTimeNanos) {
    long duration = System.nanoTime() - startTimeNanos;
    record(operationName, duration, false);
  }

  /**
   * Records an operation with explicit duration.
   *
   * @param operationName Unique name for the operation type
   * @param durationNanos Operation duration in nanoseconds
   * @param success Whether the operation succeeded
   */
  public static void record(String operationName, long durationNanos, boolean success) {
    OperationMetrics metrics = PerformanceMonitor.metrics.computeIfAbsent(
        operationName,
        k -> new OperationMetrics()
    );
    metrics.record(durationNanos, success);
  }

  /**
   * Gets statistics for a specific operation.
   *
   * @param operationName Unique name for the operation type
   * @return Statistics snapshot, or null if operation not tracked
   */
  public static Statistics getStatistics(String operationName) {
    OperationMetrics metrics = PerformanceMonitor.metrics.get(operationName);
    return metrics != null ? metrics.getStatistics() : null;
  }

  /**
   * Logs current performance statistics to console.
   *
   * <p>This is useful for debugging and monitoring during development.</p>
   */
  public static void logStatistics() {
    TownsAndNations plugin = TownsAndNations.getPlugin();
    if (plugin == null) return;

    plugin.getLogger().info("=== Performance Statistics ===");

    metrics.forEach((operationName, metricsObj) -> {
      Statistics stats = metricsObj.getStatistics();
      plugin.getLogger().info(String.format(
          "%s: %d calls | %.2f%% success | Avg: %.2fms | Min: %.2fms | Max: %.2fms",
          operationName,
          stats.callCount(),
          stats.getSuccessRate(),
          stats.getAverageDurationMs(),
          stats.getMinDurationMs(),
          stats.getMaxDurationMs()
      ));
    });

    plugin.getLogger().info("===============================");
  }

  /**
   * Resets all statistics.
   *
   * <p>Useful for running fresh benchmarks or clearing accumulated data.</p>
   */
  public static void reset() {
    metrics.clear();
  }

  /**
   * Gets all tracked operation names.
   *
   * @return Set of operation names
   */
  public static java.util.Set<String> getTrackedOperations() {
    return metrics.keySet();
  }
}
