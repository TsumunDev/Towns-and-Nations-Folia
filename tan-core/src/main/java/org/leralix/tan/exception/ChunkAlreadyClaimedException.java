package org.leralix.tan.exception;

/**
 * Exception thrown when attempting to claim a chunk that is already claimed.
 *
 * @since 0.15.0
 */
public class ChunkAlreadyClaimedException extends TerritoryException {

  private static final long serialVersionUID = 1L;

  private final int chunkX;
  private final int chunkZ;
  private final String currentOwner;

  /**
   * Creates an exception for an already claimed chunk.
   *
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   * @param currentOwner the name of the current owner (town/player)
   */
  public ChunkAlreadyClaimedException(int chunkX, int chunkZ, String currentOwner) {
    super("TERR_001", String.format("Chunk at (%d, %d) is already claimed by %s",
        chunkX, chunkZ, currentOwner), null);
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
    this.currentOwner = currentOwner;

    addContext("chunkX", chunkX);
    addContext("chunkZ", chunkZ);
    addContext("currentOwner", currentOwner);
  }

  public int getChunkX() {
    return chunkX;
  }

  public int getChunkZ() {
    return chunkZ;
  }

  public String getCurrentOwner() {
    return currentOwner;
  }

  @Override
  public String getUserMessage() {
    return String.format("This chunk is already claimed by %s", currentOwner);
  }
}
