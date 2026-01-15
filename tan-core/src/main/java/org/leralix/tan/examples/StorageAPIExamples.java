package org.leralix.tan.examples;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Code examples demonstrating usage of Storage and Data Access APIs.
 *
 * <p>This class provides practical examples for querying and modifying plugin data,
 * including async patterns, caching behavior, and batch operations.</p>
 *
 * <h2>Table of Contents:</h2>
 * <ul>
 *   <li>{@link #loadPlayerDataAsyncExample} - Loading player data asynchronously</li>
 *   <li>{@link #queryTownByNameExample} - Querying towns by name</li>
 *   <li>{@link #batchQueryExample} - Batching queries for performance</li>
 *   <li>{@link #cacheBehaviorExample} - Understanding cache behavior</li>
 *   <li>{@link #townDataQueriesExample} - Common town queries</li>
 *   <li>{@link #regionDataQueriesExample} - Common region queries</li>
 *   <li>{@link #asyncPatternExample} - Recommended async patterns</li>
 * </ul>
 *
 * @since 0.16.0
 */
public class StorageAPIExamples {

    /**
     * Example: Loading player data asynchronously.
     *
     * <p>This example demonstrates the recommended pattern for loading player data
     * using CompletableFuture to avoid blocking threads in Folia.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@code get()} for async loading (non-blocking)</li>
     *   <li>Use {@code getSync()} only when absolutely necessary (blocking)</li>
     *   <li>Always handle null cases (player may not exist)</li>
     *   <li>Chain operations with {@code thenCompose()} for sequential async steps</li>
     * </ul>
     *
     * @param playerId The UUID of the player to load
     * @return CompletableFuture that completes with the player data
     */
    public static CompletableFuture<ITanPlayer> loadPlayerDataAsyncExample(String playerId) {
        // GOOD: Async pattern (recommended for Folia)
        return PlayerDataStorage.getInstance().get(playerId)
            .thenApply(player -> {
                if (player == null) {
                    // Player doesn't exist, create new
                    player = PlayerDataStorage.getInstance().getSync(playerId);
                }
                return player;
            });
    }

    /**
     * Example: Querying towns by name.
     *
     * <p>This example shows how to search for towns by name. Since storage classes
     * don't provide name-based queries directly, we need to iterate through all towns.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@code getAll()} to get all territories of a type</li>
     *   <li>Name comparison should be case-insensitive</li>
     *   <li>Consider caching name-to-ID mappings for performance</li>
     *   <li>Use async patterns to avoid blocking</li>
     * </ul>
     *
     * @param townName The name of the town to find
     * @return CompletableFuture with the found town, or null if not found
     */
    public static CompletableFuture<TownData> queryTownByNameExample(String townName) {
        return TownDataStorage.getInstance().getAll()
            .thenApply(towns -> {
                return towns.stream()
                    .filter(town -> town.getName().equalsIgnoreCase(townName))
                    .findFirst()
                    .orElse(null);
            });
    }

    /**
     * Example: Batching queries for performance.
     *
     * <p>This example demonstrates how to batch multiple queries together to
     * improve performance by reducing database round-trips.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@code getAll()} to load all data at once</li>
     *   <li>Filter in-memory rather than making multiple queries</li>
     *   <li>Cache is automatically populated by {@code getAll()}</li>
     *   <li>Significantly faster than individual queries</li>
     * </ul>
     *
     * @param playerIds Collection of player IDs to load
     * @return CompletableFuture with list of loaded players
     */
    public static CompletableFuture<List<ITanPlayer>> batchQueryExample(Collection<String> playerIds) {
        // Load all players at once (better than individual queries)
        return PlayerDataStorage.getInstance().getAll()
            .thenApply(allPlayers -> {
                // Filter to only the players we need
                return allPlayers.stream()
                    .filter(player -> playerIds.contains(player.getID()))
                    .collect(Collectors.toList());
            });
    }

    /**
     * Example: Understanding cache behavior.
     *
     * <p>This example demonstrates how the storage cache works and when to bypass it.</p>
     *
     * <h3>Cache Behavior:</h3>
     * <ul>
     *   <li>First {@code get()} loads from database and caches the result</li>
     *   <li>Subsequent {@code get()} calls return cached data (fast)</li>
     *   <li>Cache is invalidated when data is modified via {@code put()}</li>
     *   <li>Use {@code getAll()} to warm up cache for multiple items</li>
     * </ul>
     *
     * @param playerId The player ID to query
     */
    public static void cacheBehaviorExample(String playerId) {
        // First access: Loads from database (slow)
        ITanPlayer player1 = PlayerDataStorage.getInstance().getSync(playerId);

        // Second access: Returns cached data (fast)
        ITanPlayer player2 = PlayerDataStorage.getInstance().getSync(playerId);

        // Modify data - invalidates cache
        player1.addToBalance(100.0);
        PlayerDataStorage.getInstance().putSync(playerId, player1);

        // Next access: Reloads from database (cache invalidated)
        ITanPlayer player3 = PlayerDataStorage.getInstance().getSync(playerId);
    }

    /**
     * Example: Common town data queries.
     *
     * <p>This example shows common patterns for querying town data including
     * finding towns by various criteria.</p>
     *
     * @param townName The town name to search for
     * @return CompletableFuture with the found town
     */
    public static CompletableFuture<TownData> townDataQueriesExample(String townName) {
        TownDataStorage storage = TownDataStorage.getInstance();

        // Method 1: Get by ID (if you have it)
        return storage.get("some-town-id")
            .thenCompose(town -> {
                if (town != null) {
                    return CompletableFuture.completedFuture(town);
                }

                // Method 2: Search by name
                return storage.getAll().thenApply(towns -> {
                    return towns.stream()
                        .filter(t -> t.getName().equalsIgnoreCase(townName))
                        .findFirst()
                        .orElse(null);
                });
            });
    }

    /**
     * Example: Common region data queries.
     *
     * <p>This example demonstrates querying region data and finding regions
     * by various criteria like vassal towns.</p>
     *
     * @param regionName The region name to search for
     * @return CompletableFuture with the found region
     */
    public static CompletableFuture<RegionData> regionDataQueriesExample(String regionName) {
        RegionDataStorage storage = RegionDataStorage.getInstance();

        // Get all regions and filter by name
        return storage.getAll()
            .thenApply(regions -> {
                return regions.stream()
                    .filter(region -> region.getName().equalsIgnoreCase(regionName))
                    .findFirst()
                    .orElse(null);
            });
    }

    /**
     * Example: Finding region by vassal town.
     *
     * <p>This example shows how to find which region a town belongs to
     * by checking vassal relationships.</p>
     *
     * @param townId The ID of the town to find the overlord for
     * @return CompletableFuture with the overlord region, or null if independent
     */
    public static CompletableFuture<RegionData> findRegionByVassalExample(String townId) {
        return TownDataStorage.getInstance().get(townId)
            .thenCompose(town -> {
                if (town == null || !town.haveOverlord()) {
                    return CompletableFuture.completedFuture(null);
                }

                // Get the overlord region
                String overlordId = town.getOverlordID();
                return RegionDataStorage.getInstance().get(overlordId);
            });
    }

    /**
     * Example: Recommended async pattern for storage operations.
     *
     * <p>This example demonstrates the recommended pattern for chaining multiple
     * storage operations asynchronously.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@code thenCompose()} for dependent async operations</li>
     *   <li>Use {@code thenApply()} for transformations</li>
     *   <li>Use {@code thenAccept()} for final operations</li>
     *   <li>Use {@code exceptionally()} for error handling</li>
     *   <li>Avoid {@code getSync()} in async pipelines</li>
     * </ul>
     *
     * @param playerId The player ID to query
     * @param townId The town ID to check membership for
     * @return CompletableFuture that completes when all operations finish
     */
    public static CompletableFuture<Boolean> asyncPatternExample(String playerId, String townId) {
        // Chain multiple async operations
        return PlayerDataStorage.getInstance().get(playerId)
            .thenCompose(player -> {
                if (player == null) {
                    return CompletableFuture.completedFuture(false);
                }

                // Player exists, now check town
                return TownDataStorage.getInstance().get(townId)
                    .thenApply(town -> {
                        if (town == null) {
                            return false;
                        }

                        // Check if player is in town
                        return town.isPlayerInTown(player);
                    });
            })
            .exceptionally(throwable -> {
                // Handle errors
                System.err.println("Error checking town membership: " + throwable.getMessage());
                return false;
            });
    }

    /**
     * Example: Creating new data with validation.
     *
     * <p>This example shows how to create new towns/regions with proper validation
     * and error handling.</p>
     *
     * @param player The player creating the town
     * @param townName The name for the new town
     * @return CompletableFuture that completes with the created town
     */
    public static CompletableFuture<TownData> createTownWithValidationExample(Player player, String townName) {
        // Get player data
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance()
            .getSync(player.getUniqueId().toString());

        // Check if player already has a town
        if (tanPlayer.hasTown()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Player already has a town"));
        }

        // Check if town name already exists
        return TownDataStorage.getInstance().getAll()
            .thenCompose(towns -> {
                boolean nameExists = towns.stream()
                    .anyMatch(town -> town.getName().equalsIgnoreCase(townName));

                if (nameExists) {
                    return CompletableFuture.failedFuture(
                        new IllegalStateException("Town name already exists"));
                }

                // Create the town
                return TownDataStorage.getInstance().newTown(townName, tanPlayer);
            })
            .thenApply(town -> {
                // Town created successfully
                player.sendMessage("Town " + townName + " created!");
                return town;
            })
            .exceptionally(throwable -> {
                // Handle errors
                player.sendMessage("Failed to create town: " + throwable.getMessage());
                return null;
            });
    }

    /**
     * Example: Deleting data with cleanup.
     *
     * <p>This example demonstrates how to delete towns/regions with proper
     * cleanup of related data.</p>
     *
     * @param townId The ID of the town to delete
     * @return CompletableFuture that completes when deletion finishes
     */
    public static CompletableFuture<Void> deleteTownWithCleanupExample(String townId) {
        return TownDataStorage.getInstance().get(townId)
            .thenAccept(town -> {
                if (town == null) {
                    throw new IllegalArgumentException("Town not found");
                }

                // Remove all members from the town
                for (ITanPlayer member : town.getITanPlayerList()) {
                    town.removePlayer(member);
                }

                // Unclaim all chunks
                town.getClaimedChunks().forEach(chunk -> {
                    town.unclaim(chunk.getLocation());
                });

                // Delete the town
                TownDataStorage.getInstance().delete(town);
            });
    }

    /**
     * Example: Loading and updating data safely.
     *
     * <p>This example shows the load-modify-save pattern that ensures
     * data consistency and proper cache invalidation.</p>
     *
     * @param playerId The player ID to update
     * @param amountToAdd The amount to add to balance
     * @return CompletableFuture that completes when update finishes
     */
    public static CompletableFuture<Void> loadModifySaveExample(String playerId, double amountToAdd) {
        return PlayerDataStorage.getInstance().get(playerId)
            .thenAccept(player -> {
                if (player == null) {
                    throw new IllegalArgumentException("Player not found");
                }

                // Modify data
                player.addToBalance(amountToAdd);

                // Save changes (invalidates cache)
                PlayerDataStorage.getInstance().putSync(playerId, player);

                System.out.println("Updated balance: " + player.getBalance());
            });
    }

    /**
     * Example: Querying with pagination.
     *
     * <p>This example demonstrates how to implement pagination for large datasets.</p>
     *
     * @param pageNumber The page number (1-indexed)
     * @param pageSize The number of items per page
     * @return CompletableFuture with the paginated list of towns
     */
    public static CompletableFuture<List<TownData>> paginatedQueryExample(int pageNumber, int pageSize) {
        return TownDataStorage.getInstance().getAll()
            .thenApply(towns -> {
                // Calculate pagination
                int startIndex = (pageNumber - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, towns.size());

                // Validate page number
                if (startIndex >= towns.size()) {
                    return List.of(); // Page out of range
                }

                // Extract page from list
                return towns.stream()
                    .skip(startIndex)
                    .limit(pageSize)
                    .collect(Collectors.toList());
            });
    }
}
