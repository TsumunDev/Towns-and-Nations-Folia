package org.leralix.tan.utils.gui;
import dev.triumphteam.gui.guis.Gui;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.gui.circuitbreaker.GuiCircuitBreaker;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.FoliaScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class AsyncGuiHelper {
  private static final Logger logger = LoggerFactory.getLogger(AsyncGuiHelper.class);
  private static final GuiCircuitBreaker guiCircuitBreaker =
      new GuiCircuitBreaker(5, 60000);
  private AsyncGuiHelper() {
    throw new IllegalStateException("Utility class");
  }
  public static <T> void loadAsync(
      Player player, Supplier<T> asyncLoader, Consumer<T> mainThreadConsumer) {
    FoliaScheduler.runTaskAsynchronously(
        TownsAndNations.getPlugin(),
        () -> {
          try {
            T data = asyncLoader.get();
            FoliaScheduler.runTask(
                TownsAndNations.getPlugin(),
                () -> {
                  if (player.isOnline()) {
                    try {
                      mainThreadConsumer.accept(data);
                    } catch (Exception e) {
                      logger.error(
                          "Error updating GUI for player {} on main thread", player.getName(), e);
                    }
                  }
                });
          } catch (Exception e) {
            logger.error("Error loading GUI data for player {}", player.getName(), e);
          }
        });
  }
  public static <T> void loadAsyncWithFallback(
      Player player, Supplier<T> asyncLoader, Consumer<T> mainThreadConsumer, T fallbackValue) {
    FoliaScheduler.runTaskAsynchronously(
        TownsAndNations.getPlugin(),
        () -> {
          T data;
          try {
            data = asyncLoader.get();
          } catch (Exception e) {
            logger.error(
                "Error loading GUI data for player {}, using fallback", player.getName(), e);
            data = fallbackValue;
          }
          final T finalData = data;
          FoliaScheduler.runTask(
              TownsAndNations.getPlugin(),
              () -> {
                if (player.isOnline()) {
                  try {
                    mainThreadConsumer.accept(finalData);
                  } catch (Exception e) {
                    logger.error(
                        "Error updating GUI for player {} on main thread", player.getName(), e);
                  }
                }
              });
        });
  }
  public static void prefetchPlayerData(Player player, Consumer<ITanPlayer> guiCreator) {
    FoliaScheduler.runTaskAsynchronously(
        TownsAndNations.getPlugin(),
        () -> {
          try {
            ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
            FoliaScheduler.runTask(
                TownsAndNations.getPlugin(),
                () -> {
                  if (player.isOnline() && tanPlayer != null) {
                    try {
                      guiCreator.accept(tanPlayer);
                    } catch (Exception e) {
                      logger.error(
                          "Error creating GUI for player {} with prefetched data",
                          player.getName(),
                          e);
                    }
                  }
                });
          } catch (Exception e) {
            logger.error("Error prefetching player data for {}", player.getName(), e);
          }
        });
  }
  public static <T> CompletableFuture<T> loadAsyncFuture(Player player, Supplier<T> asyncLoader) {
    CompletableFuture<T> future = new CompletableFuture<>();
    FoliaScheduler.runTaskAsynchronously(
        TownsAndNations.getPlugin(),
        () -> {
          try {
            T data = asyncLoader.get();
            future.complete(data);
          } catch (Exception e) {
            logger.error("Error loading GUI data for player {}", player.getName(), e);
            future.completeExceptionally(e);
          }
        });
    return future;
  }
  public static void refreshGui(Gui gui, Player player, Runnable updateAction) {
    FoliaScheduler.runTask(
        TownsAndNations.getPlugin(),
        () -> {
          if (player.isOnline()) {
            try {
              updateAction.run();
              gui.update();
            } catch (Exception e) {
              logger.error("Error refreshing GUI for player {}", player.getName(), e);
            }
          }
        });
  }
  public static boolean shouldReload(long lastLoadTime, long cacheDurationMillis) {
    return (System.currentTimeMillis() - lastLoadTime) > cacheDurationMillis;
  }
  public static final long CACHE_DURATION_STANDARD = 30_000L;
  public static final long CACHE_DURATION_SHORT = 5_000L;
  public static final long CACHE_DURATION_LONG = 300_000L;
  public static void executeWithCircuitBreaker(
      Player player, Runnable guiOperation, Runnable onSuccess, Consumer<Throwable> onFailure) {
    guiCircuitBreaker.execute(
        guiOperation,
        ex -> {
          logger.debug("GUI operation succeeded for player {}", player.getName());
          onSuccess.run();
        },
        ex -> {
          logger.warn("GUI operation failed for player {}: {}", player.getName(), ex.getMessage());
          onFailure.accept(ex);
        });
  }
  public static String getCircuitBreakerState() {
    return guiCircuitBreaker.getState();
  }

  // ===== NEW PREFETCHING AND PAGINATION METHODS (Story 6.2) =====

  /**
   * Prefetched data for main menu.
   */
  public static class MainMenuPrefetch {
    private ITanPlayer player;
    private TownData town;
    private RegionData region;

    public ITanPlayer getPlayer() { return player; }
    public void setPlayer(ITanPlayer player) { this.player = player; }

    public TownData getTown() { return town; }
    public void setTown(TownData town) { this.town = town; }

    public RegionData getRegion() { return region; }
    public void setRegion(RegionData region) { this.region = region; }

    public boolean hasTown() { return town != null; }
    public boolean hasRegion() { return region != null; }
  }

  /**
   * Prefetches all data needed for the main menu in parallel.
   *
   * <p>This loads player data, town data, and region data concurrently,
   * significantly reducing total load time compared to sequential loading.</p>
   *
   * @param player the player opening the menu
   * @return a CompletableFuture containing the prefetched data
   */
  public static CompletableFuture<MainMenuPrefetch> prefetchMainMenuData(Player player) {
    logger.debug("[AsyncGuiHelper] Prefetching main menu data for: {}", player.getName());

    long startTime = System.nanoTime();

    return PlayerDataStorage.getInstance().get(player)
        .thenCompose(tanPlayer -> {
          // Load town and region in parallel
          CompletableFuture<TownData> townFuture = tanPlayer.hasTown()
              ? tanPlayer.getTown()
              : CompletableFuture.completedFuture(null);

          CompletableFuture<RegionData> regionFuture = tanPlayer.hasRegion()
              ? tanPlayer.getRegion()
              : CompletableFuture.completedFuture(null);

          return CompletableFuture.allOf(townFuture, regionFuture)
              .thenApply(v -> {
                MainMenuPrefetch prefetch = new MainMenuPrefetch();
                prefetch.setPlayer(tanPlayer);
                prefetch.setTown(townFuture.join());
                prefetch.setRegion(regionFuture.join());

                long duration = (System.nanoTime() - startTime) / 1_000_000;
                logger.debug("[AsyncGuiHelper] Main menu prefetch completed in {}ms for: {}",
                    duration, player.getName());

                return prefetch;
              });
        });
  }

  /**
   * Loads town members with pagination support.
   *
   * <p>This method batches member loading to avoid N+1 query problems.
   * Only loads the specified page of members.</p>
   *
   * @param town the town to load members for
   * @param offset the offset to start from
   * @param limit the maximum number of members to load
   * @return a CompletableFuture containing the list of members
   */
  public static CompletableFuture<java.util.List<ITanPlayer>> loadTownMembersPaginated(
      TownData town, int offset, int limit) {

    java.util.List<java.util.UUID> memberIds = new java.util.ArrayList<>(town.getMemberIDs());

    // Apply pagination
    int fromIndex = Math.min(offset, memberIds.size());
    int toIndex = Math.min(offset + limit, memberIds.size());
    java.util.List<java.util.UUID> paginatedIds = memberIds.subList(fromIndex, toIndex);

    if (paginatedIds.isEmpty()) {
      return CompletableFuture.completedFuture(java.util.Collections.emptyList());
    }

    // Batch load all members in parallel
    java.util.List<CompletableFuture<ITanPlayer>> futures = new java.util.ArrayList<>();
    for (java.util.UUID memberId : paginatedIds) {
      CompletableFuture<ITanPlayer> future = PlayerDataStorage.getInstance()
          .get(memberId.toString());
      futures.add(future);
    }

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> {
          java.util.List<ITanPlayer> members = new java.util.ArrayList<>();
          for (CompletableFuture<ITanPlayer> future : futures) {
            ITanPlayer member = future.join();
            if (member != null) {
              members.add(member);
            }
          }
          return members;
        });
  }
}