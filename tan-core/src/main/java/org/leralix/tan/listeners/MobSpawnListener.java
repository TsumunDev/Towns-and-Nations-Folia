package org.leralix.tan.listeners;
import org.bukkit.Chunk;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;

/**
 * Listener for mob spawn events in claimed chunks.
 * <p>
 * Uses cache-first lookups via {@link NewClaimedChunkStorage#get(Chunk)} which
 * returns WildernessChunk on cache miss — a safe default that allows spawning.
 * This avoids blocking Folia region threads with database calls.
 */
public class MobSpawnListener implements Listener {
  @EventHandler
  public void entitySpawn(EntitySpawnEvent e) {
    Chunk currentChunk = e.getEntity().getLocation().getChunk();
    // Cache-first lookup: returns WildernessChunk on cache miss (safe default)
    ClaimedChunk2 claimedChunk2 = NewClaimedChunkStorage.getInstance().get(currentChunk);
    // Wilderness chunks don't restrict spawning, so skip the unclaimed check
    if (claimedChunk2 instanceof org.leralix.tan.dataclass.chunk.WildernessChunk) {
      return;
    }
    EntityType entityType = e.getEntity().getType();
    if (!claimedChunk2.canEntitySpawn(entityType)) {
      e.setCancelled(true);
    }
  }
}