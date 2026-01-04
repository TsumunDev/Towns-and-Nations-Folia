package org.leralix.tan.gui.cosmetic;

import java.util.Map;
import org.bukkit.Material;

/**
 * Represents a menu layout configuration.
 * Contains slot positions, number of rows, filler material, and optional Nexo glyph for custom GUI textures.
 */
public class MenuLayout {

  private final int rows;
  private final Material filler;
  private final Map<String, Integer> slots;
  private final String glyph;

  public MenuLayout(int rows, Material filler, Map<String, Integer> slots, String glyph) {
    this.rows = rows;
    this.filler = filler;
    this.slots = slots;
    this.glyph = glyph;
  }

  public int getRows() {
    return rows;
  }

  public Material getFiller() {
    return filler;
  }

  /**
   * Get the Nexo glyph for custom GUI texture (can be null if not configured).
   */
  public String getGlyph() {
    return glyph;
  }

  /**
   * Get the slot for a given key, or the default if not configured.
   */
  public int getSlotOrDefault(String key, int defaultSlot) {
    return slots.getOrDefault(key, defaultSlot);
  }

  /**
   * Check if a slot is configured for the given key.
   */
  public boolean hasSlot(String key) {
    return slots.containsKey(key);
  }

  /**
   * Get all configured slots.
   */
  public Map<String, Integer> getSlots() {
    return slots;
  }
}
