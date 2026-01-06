package org.leralix.tan.dataclass;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
public record ChunkCoordinates(int x, int z, String worldID) {
  public ChunkCoordinates(Chunk chunk) {
    this(chunk.getX(), chunk.getZ(), chunk.getWorld().getUID().toString());
  }
  public Chunk getChunk() {
    World world = Bukkit.getWorld(UUID.fromString(worldID));
    if (world == null) {
      return null;
    }
    return world.getChunkAt(x, z);
  }
}