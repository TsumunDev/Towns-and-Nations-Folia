package org.leralix.tan.gui.cosmetic;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.leralix.lib.utils.config.ConfigUtil;
import org.leralix.tan.TownsAndNations;
public class LayoutManager {
  private static LayoutManager instance;
  private final Map<String, MenuLayout> layouts;
  private LayoutManager() {
    this.layouts = new HashMap<>();
    Plugin plugin = TownsAndNations.getPlugin();
    File menuDir = new File(plugin.getDataFolder(), "menu");
    if (!menuDir.exists()) {
      menuDir.mkdirs();
      plugin.getLogger().info("[TaN-Layout] Created menu directory: " + menuDir.getAbsolutePath());
    }
    File layoutFile = new File(menuDir, "layouts.yml");
    if (!layoutFile.exists()) {
      try {
        plugin.saveResource("menu/layouts.yml", false);
        plugin.getLogger().info("[TaN-Layout] Extracted layouts.yml from plugin resources");
      } catch (Exception e) {
        plugin.getLogger().warning("[TaN-Layout] Could not extract layouts.yml: " + e.getMessage());
        try {
          layoutFile.createNewFile();
          java.io.FileWriter writer = new java.io.FileWriter(layoutFile);
          writer.write("# COCONATION LAYOUTS - Fichier créé automatiquement\n");
          writer.write("# Décommente les sections pour personnaliser les menus\n\n");
          writer.write("main_menu:\n");
          writer.write("  rows: 3\n");
          writer.write("  slots:\n");
          writer.write("    time_icon: 4\n");
          writer.write("    nation: 10\n");
          writer.write("    region: 12\n");
          writer.write("    town: 14\n");
          writer.write("    player: 16\n");
          writer.write("    back: 18\n\n");
          writer.write("town_menu:\n");
          writer.write("  rows: 4\n");
          writer.write("  filler: \"BLUE_STAINED_GLASS_PANE\"\n");
          writer.write("  slots:\n");
          writer.write("    territory_info: 4\n");
          writer.write("    treasury: 10\n");
          writer.write("    members: 11\n");
          writer.write("    land: 12\n");
          writer.write("    browse: 13\n");
          writer.write("    diplomacy: 14\n");
          writer.write("    level: 15\n");
          writer.write("    settings: 16\n");
          writer.write("    building: 19\n");
          writer.write("    attack: 20\n");
          writer.write("    hierarchy: 21\n");
          writer.write("    landmarks: 25\n");
          writer.write("    back: 27\n");
          writer.close();
          plugin.getLogger().info("[TaN-Layout] Created basic layouts.yml file manually");
        } catch (Exception ex) {
          plugin.getLogger().severe("[TaN-Layout] Failed to create layouts.yml: " + ex.getMessage());
        }
      }
    }
    plugin.getLogger().info("[TaN-Layout] Loading layouts from: " + layoutFile.getAbsolutePath());
    if (!layoutFile.exists()) {
      plugin.getLogger().warning("[TaN-Layout] No layouts.yml found after creation attempt!");
      return;
    }
    YamlConfiguration config = YamlConfiguration.loadConfiguration(layoutFile);
    int totalMenus = 0;
    int loadedMenus = 0;
    for (String menuKey : config.getKeys(false)) {
      totalMenus++;
      try {
        ConfigurationSection menuSection = config.getConfigurationSection(menuKey);
        if (menuSection != null) {
          MenuLayout layout = parseLayout(menuSection);
          layouts.put(menuKey, layout);
          loadedMenus++;
          plugin.getLogger().info("[TaN-Layout] ✓ Loaded menu: " + menuKey + " (rows=" + layout.getRows() + ", slots=" + layout.getSlots().size() + ")");
        }
      } catch (Exception e) {
        plugin.getLogger().warning("[TaN-Layout] ✖ Error parsing layout for menu: " + menuKey + " - " + e.getMessage());
      }
    }
    plugin.getLogger().info("[TaN-Layout] ════════════════════════════════════════");
    plugin.getLogger().info("[TaN-Layout] Loaded " + loadedMenus + "/" + totalMenus + " menu layouts.");
    if (loadedMenus == 0) {
      plugin.getLogger().warning("[TaN-Layout] No layouts loaded! Check if all entries are commented out with #");
    }
    plugin.getLogger().info("[TaN-Layout] ════════════════════════════════════════");
  }
  private MenuLayout parseLayout(ConfigurationSection section) {
    int rows = section.getInt("rows", 3);
    String fillerStr = section.getString("filler", "BLACK_STAINED_GLASS_PANE");
    Material filler = parseMaterial(fillerStr);
    String glyph = section.getString("glyph", null);
    if (glyph != null && !glyph.isEmpty()) {
      glyph = processUnicodeEscapes(glyph);
    }
    Map<String, Integer> slots = new HashMap<>();
    ConfigurationSection slotsSection = section.getConfigurationSection("slots");
    if (slotsSection != null) {
      for (String slotKey : slotsSection.getKeys(false)) {
        slots.put(slotKey, slotsSection.getInt(slotKey));
      }
    }
    return new MenuLayout(rows, filler, slots, glyph);
  }
  private String processUnicodeEscapes(String input) {
    if (input == null || !input.contains("\\u")) {
      return input;
    }
    StringBuilder result = new StringBuilder();
    int i = 0;
    while (i < input.length()) {
      if (i < input.length() - 5 && input.charAt(i) == '\\' && input.charAt(i + 1) == 'u') {
        try {
          String hex = input.substring(i + 2, i + 6);
          int codePoint = Integer.parseInt(hex, 16);
          result.append((char) codePoint);
          i += 6;
        } catch (Exception e) {
          result.append(input.charAt(i));
          i++;
        }
      } else {
        result.append(input.charAt(i));
        i++;
      }
    }
    return result.toString();
  }
  private Material parseMaterial(String value) {
    if (value == null) return Material.BLACK_STAINED_GLASS_PANE;
    String materialName = value.replace("minecraft:", "").toUpperCase();
    try {
      return Material.valueOf(materialName);
    } catch (IllegalArgumentException e) {
      return Material.BLACK_STAINED_GLASS_PANE;
    }
  }
  public static LayoutManager getInstance() {
    if (instance == null) {
      instance = new LayoutManager();
    }
    return instance;
  }
  public static void reload() {
    instance = new LayoutManager();
  }
  public int getSlotOrDefault(String menuKey, String slotKey, int defaultSlot) {
    MenuLayout layout = layouts.get(menuKey);
    if (layout == null) {
      return defaultSlot;
    }
    return layout.getSlotOrDefault(slotKey, defaultSlot);
  }
  public int getRowsOrDefault(String menuKey, int defaultRows) {
    MenuLayout layout = layouts.get(menuKey);
    if (layout == null) {
      return defaultRows;
    }
    return layout.getRows();
  }
  public Material getFillerOrDefault(String menuKey, Material defaultFiller) {
    MenuLayout layout = layouts.get(menuKey);
    if (layout == null) {
      return defaultFiller;
    }
    return layout.getFiller();
  }
  public String getGlyphOrDefault(String menuKey) {
    MenuLayout layout = layouts.get(menuKey);
    if (layout == null) {
      return null;
    }
    return layout.getGlyph();
  }
  public boolean hasLayout(String menuKey) {
    return layouts.containsKey(menuKey);
  }
}