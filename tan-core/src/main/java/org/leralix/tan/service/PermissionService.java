package org.leralix.tan.service;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.enums.permissions.ChunkPermissionType;
import org.leralix.tan.storage.SudoPlayerStorage;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.utils.constants.EnabledPermissions;

/**
 * Permission service with optimized caching for high-frequency checks.
 * Uses PermissionCache to avoid async database calls in hot paths.
 */
public class PermissionService {

  private final PermissionCache permissionCache = PermissionCache.getInstance();

  /**
   * Async permission check with cache fallback.
   * For most hot-path event handlers, use checkPermissionSync() instead.
   */
  public CompletableFuture<Boolean> canPlayerDoAction(
      Location location, Player player, ChunkPermissionType permissionType) {
    if (EnabledPermissions.getInstance().isPermissionDisabled(permissionType)) {
      return CompletableFuture.completedFuture(true);
    }
    if (SudoPlayerStorage.isSudoPlayer(player)) return CompletableFuture.completedFuture(true);

    // Check cache first
    Boolean cached = permissionCache.getCached(player, location.getChunk(), permissionType);
    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    // Cache miss - compute asynchronously
    ClaimedChunk2 claimedChunk = NewClaimedChunkStorage.getInstance().get(location.getChunk());
    return PlayerDataStorage.getInstance()
        .get(player)
        .thenApply(
            tanPlayer -> {
              boolean isAtWar = tanPlayer.isAtWarWith(claimedChunk.getOwner());
              boolean result = isAtWar || claimedChunk.canPlayerDo(player, permissionType, location);

              // Cache the result for future checks
              permissionCache.put(player, location.getChunk(), permissionType, result);

              return result;
            });
  }

  /**
   * Synchronous permission check for hot-path event handlers.
   * Returns cached result or computes synchronously (only for cache misses).
   * Use this in high-frequency event handlers like BlockBreakEvent, BlockPlaceEvent, etc.
   *
   * @return true if player can perform action, false otherwise
   */
  public boolean canPlayerDoActionSync(
      Location location, Player player, ChunkPermissionType permissionType) {
    if (EnabledPermissions.getInstance().isPermissionDisabled(permissionType)) {
      return true;
    }
    if (SudoPlayerStorage.isSudoPlayer(player)) return true;

    // Check cache first (non-blocking)
    Boolean cached = permissionCache.getCached(player, location.getChunk(), permissionType);
    if (cached != null) {
      return cached;
    }

    // Cache miss - need to compute (potentially blocking, but cache miss rate should be low)
    ClaimedChunk2 claimedChunk = NewClaimedChunkStorage.getInstance().get(location.getChunk());
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());

    if (tanPlayer == null) {
      // Player not found - default to allowing action to avoid false positives
      return true;
    }

    boolean isAtWar = tanPlayer.isAtWarWith(claimedChunk.getOwner());
    boolean result = isAtWar || claimedChunk.canPlayerDo(player, permissionType, location);

    // Cache the result
    permissionCache.put(player, location.getChunk(), permissionType, result);

    return result;
  }

  public boolean canPvpHappen(Player player1, Player player2) {
    if (!NewClaimedChunkStorage.getInstance()
        .get(player2.getLocation().getChunk())
        .canPVPHappen()) {
      return false;
    }
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player1);
    ITanPlayer tanPlayer2 = PlayerDataStorage.getInstance().getSync(player2);
    TownRelation relation = tanPlayer.getRelationWithPlayerSync(tanPlayer2);
    return Constants.getRelationConstants(relation).canPvP();
  }

  /**
   * Invalidate cached permissions for a player.
   * Call this when player joins/leaves a town or relations change.
   */
  public void invalidatePlayerCache(java.util.UUID playerUuid) {
    permissionCache.invalidatePlayer(playerUuid);
  }

  /**
   * Invalidate cached permissions for a chunk.
   * Call this when chunk ownership changes.
   */
  public void invalidateChunkCache(int chunkX, int chunkZ, String worldUuid) {
    permissionCache.invalidateChunk(chunkX, chunkZ, worldUuid);
  }
}