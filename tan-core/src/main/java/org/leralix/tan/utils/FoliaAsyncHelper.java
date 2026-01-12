package org.leralix.tan.utils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Async helper utilities for Folia regionalized threading.
 *
 * <p>This utility provides patterns for performing asynchronous operations
 * and returning to the appropriate region thread for continued execution.</p>
 *
 * <h3>Primary Use Cases:</h3>
 * <ul>
 *   <li>Database/storage I/O operations (offload from region threads)</li>
 *   <li>External API calls (HTTP, Redis, etc.)</li>
 *   <li>Expensive computations that should not block region ticks</li>
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 * <ul>
 *   <li>{@code supplyAsync()} executes on {@link org.bukkit.scheduler.BukkitScheduler#runTaskAsynchronously}</li>
 *   <li>{@code thenRunOnRegion()} schedules back on the region thread</li>
 *   <li>{@code thenRunOnEntity()} schedules back on the entity's region thread</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 *
 * <h4>Example 1: Simple Storage Query</h4>
 * <pre>{@code
 * // Load player data asynchronously, then use it on region thread
 * FoliaAsyncHelper.supplyAsync(() -> PlayerDataStorage.getInstance().get(playerId))
 *     .thenRunOnRegion(plugin, player.getLocation(), playerData -> {
 *         // Safe to access Bukkit APIs here
 *         player.sendMessage("Balance: " + playerData.getBalance());
 *     });
 * }</pre>
 *
 * <h4>Example 2: Chained Async Operations</h4>
 * <pre>{@code
 * // Load town, then load all members, then update GUI
 * FoliaAsyncHelper.supplyAsync(() -> TownDataStorage.getInstance().get(townId))
 *     .thenComposeAsync(town -> loadAllMembers(town))
 *     .thenRunOnRegion(plugin, player.getLocation(), members -> {
 *         new TownMembersMenu(player, members).open();
 *     });
 * }</pre>
 *
 * <h4>Example 3: Error Handling</h4>
 * <pre>{@code
 * FoliaAsyncHelper.supplyAsync(() -> expensiveOperation())
 *     .thenRunOnRegion(plugin, location, result -> {
 *         handleSuccess(result);
 *     })
 *     .exceptionally(throwable -> {
 *         logger.error("Operation failed", throwable);
 *         return null;
 *     });
 * }</pre>
 *
 * @since 0.16.0
 * @see FoliaScheduler for region-thread scheduling
 */
public class FoliaAsyncHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(FoliaAsyncHelper.class);

  private FoliaAsyncHelper() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Executes a {@link Supplier} asynchronously off the region thread.
   *
   * <p>Use this for I/O operations (database, network, file system) or expensive
   * computations that should not block the region tick.</p>
   *
   * <p><b>Warning:</b> Do not access Bukkit/Minecraft APIs in the supplier.
   * They must be accessed in {@code thenRunOnRegion()} or {@code thenRunOnEntity()}.</p>
   *
   * @param supplier The operation to execute asynchronously
   * @param <T> The result type
   * @return A CompletableFuture that will complete with the result
   *
   * @deprecated Prefer {@link #supplyAsync(Plugin, Supplier)} for better error handling
   */
  @Deprecated
  public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier);
  }

  /**
   * Executes a {@link Supplier} asynchronously with proper plugin context.
   *
   * <p>This variant uses the async scheduler and includes error logging.</p>
   *
   * @param plugin The plugin instance
   * @param supplier The operation to execute asynchronously
   * @param <T> The result type
   * @return A CompletableFuture that will complete with the result
   */
  public static <T> CompletableFuture<T> supplyAsync(Plugin plugin, Supplier<T> supplier) {
    CompletableFuture<T> future = new CompletableFuture<>();

    FoliaScheduler.runTaskAsynchronously(plugin, () -> {
      try {
        T result = supplier.get();
        future.complete(result);
      } catch (Throwable t) {
        LOGGER.error("Async operation failed", t);
        future.completeExceptionally(t);
      }
    });

    return future;
  }

  /**
   * Executes a {@link Runnable} asynchronously off the region thread.
   *
   * <p>Use this for I/O operations where no result is needed.</p>
   *
   * @param plugin The plugin instance
   * @param runnable The operation to execute asynchronously
   * @return A CompletableFuture that completes when the runnable finishes
   */
  public static CompletableFuture<Void> runAsync(Plugin plugin, Runnable runnable) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    FoliaScheduler.runTaskAsynchronously(plugin, () -> {
      try {
        runnable.run();
        future.complete(null);
      } catch (Throwable t) {
        LOGGER.error("Async operation failed", t);
        future.completeExceptionally(t);
      }
    });

    return future;
  }

  /**
   * Schedules a {@link Runnable} to run on a region thread.
   *
   * <p>Use this for region-thread operations that don't require a value parameter.</p>
   *
   * @param plugin The plugin instance
   * @param location The location to determine the region
   * @param runnable The runnable to execute on the region thread
   * @return A CompletableFuture that completes after the runnable finishes
   */
  public static CompletableFuture<Void> thenRunOnRegion(
      Plugin plugin,
      Location location,
      Runnable runnable) {

    CompletableFuture<Void> future = new CompletableFuture<>();

    FoliaScheduler.runTaskAtLocation(plugin, location, () -> {
      try {
        runnable.run();
        future.complete(null);
      } catch (Throwable t) {
        LOGGER.error("Region thread operation failed", t);
        future.completeExceptionally(t);
      }
    });

    return future;
  }

  /**
   * Schedules a {@link java.util.function.Consumer} to run on a region thread with a value.
   *
   * <p>This is the continuation step after {@code supplyAsync()}. Use this to safely
   * access Bukkit/Minecraft APIs with the result of an async computation.</p>
   *
   * @param plugin The plugin instance
   * @param location The location to determine the region
   * @param value The value to pass to the consumer
   * @param consumer The consumer to run on the region thread
   * @param <T> The value type
   * @return A CompletableFuture that completes after the consumer runs
   */
  public static <T> CompletableFuture<Void> thenRunOnRegion(
      Plugin plugin,
      Location location,
      T value,
      java.util.function.Consumer<T> consumer) {

    CompletableFuture<Void> future = new CompletableFuture<>();

    FoliaScheduler.runTaskAtLocation(plugin, location, () -> {
      try {
        consumer.accept(value);
        future.complete(null);
      } catch (Throwable t) {
        LOGGER.error("Region thread operation failed", t);
        future.completeExceptionally(t);
      }
    });

    return future;
  }

  /**
   * Schedules a {@link java.util.function.Consumer} to run on a player's entity region.
   *
   * <p>Use this when the operation is tied to a specific player (e.g., GUI updates,
   * sending messages, inventory modifications).</p>
   *
   * @param plugin The plugin instance
   * @param player The player whose region to use
   * @param value The value to pass to the consumer
   * @param consumer The consumer to run on the entity thread
   * @param <T> The value type
   * @return A CompletableFuture that completes after the consumer runs
   */
  public static <T> CompletableFuture<Void> thenRunOnEntity(
      Plugin plugin,
      Player player,
      T value,
      java.util.function.Consumer<T> consumer) {

    CompletableFuture<Void> future = new CompletableFuture<>();

    FoliaScheduler.runEntityTask(plugin, player, () -> {
      try {
        consumer.accept(value);
        future.complete(null);
      } catch (Throwable t) {
        LOGGER.error("Entity thread operation failed for player: " + player.getName(), t);
        future.completeExceptionally(t);
      }
    });

    return future;
  }

  /**
   * Chains an async transformation operation.
   *
   * <p>Use this to perform additional async operations after the first one completes.</p>
   *
   * @param future The source future
   * @param function The transformation function (returns a new CompletableFuture)
   * @param <T> The source type
   * @param <U> The result type
   * @return A CompletableFuture with the transformed result
   */
  public static <T, U> CompletableFuture<U> thenComposeAsync(
      CompletableFuture<T> future,
      Function<T, CompletableFuture<U>> function) {

    return future.thenCompose(function);
  }

  /**
   * Chains a synchronous transformation operation.
   *
   * <p>Use this to transform the result without additional async operations.</p>
   *
   * @param future The source future
   * @param function The transformation function
   * @param <T> The source type
   * @param <U> The result type
   * @return A CompletableFuture with the transformed result
   */
  public static <T, U> CompletableFuture<U> thenApply(
      CompletableFuture<T> future,
      Function<T, U> function) {

    return future.thenApply(function);
  }

  /**
   * Combines two async operations and runs a consumer when both complete.
   *
   * <p>Useful for loading two independent data sources in parallel.</p>
   *
   * @param plugin The plugin instance
   * @param location The location for the final consumer
   * @param future1 The first future
   * @param future2 The second future
   * @param consumer The consumer that receives both results
   * @param <T> The first result type
   * @param <U> The second result type
   * @return A CompletableFuture that completes when both operations finish
   */
  public static <T, U> CompletableFuture<Void> thenRunOnRegionWhenBoth(
      Plugin plugin,
      Location location,
      CompletableFuture<T> future1,
      CompletableFuture<U> future2,
      java.util.function.BiConsumer<T, U> consumer) {

    return future1.thenCombine(future2, (t, u) -> {
      CompletableFuture<Void> resultFuture = new CompletableFuture<>();

      FoliaScheduler.runTaskAtLocation(plugin, location, () -> {
        try {
          consumer.accept(t, u);
          resultFuture.complete(null);
        } catch (Throwable ex) {
          LOGGER.error("Combined operation failed", ex);
          resultFuture.completeExceptionally(ex);
        }
      });

      return resultFuture;
    }).thenCompose(future -> future);
  }
}
