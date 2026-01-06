package org.leralix.tan.gui.cosmetic;
import java.util.Map;
import org.bukkit.Material;
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
  public String getGlyph() {
    return glyph;
  }
  public int getSlotOrDefault(String key, int defaultSlot) {
    return slots.getOrDefault(key, defaultSlot);
  }
  public boolean hasSlot(String key) {
    return slots.containsKey(key);
  }
  public Map<String, Integer> getSlots() {
    return slots;
  }
}