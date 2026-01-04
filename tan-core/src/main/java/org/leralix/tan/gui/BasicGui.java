package org.leralix.tan.gui;

import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.gui.cosmetic.IconManager;
import org.leralix.tan.gui.cosmetic.LayoutManager;
import org.leralix.tan.lang.LangType;

public abstract class BasicGui {

  protected final Gui gui;
  protected final Player player;
  protected final ITanPlayer tanPlayer;
  protected final LangType langType;
  protected final IconManager iconManager;

  /**
   * Non-blocking constructor that accepts pre-fetched player data Use this constructor when player
   * data is already available
   */
  protected BasicGui(Player player, ITanPlayer tanPlayer, String title, int rows) {
    this.gui = Gui.gui().title(Component.text(title)).type(GuiType.CHEST).rows(rows).create();
    this.player = player;
    this.tanPlayer = tanPlayer;
    this.langType = tanPlayer.getLang();
    this.iconManager = IconManager.getInstance();

    gui.setDefaultClickAction(
        event -> {
          if (event.getClickedInventory().getType() != InventoryType.PLAYER) {
            event.setCancelled(true);
          }
        });
    gui.setDragAction(inventoryDragEvent -> inventoryDragEvent.setCancelled(true));
  }

  /**
   * Constructor that uses LayoutManager to get rows from config and applies Nexo glyph if configured
   */
  protected BasicGui(Player player, ITanPlayer tanPlayer, String title, String menuKey, int defaultRows) {
    this(player, tanPlayer, applyGlyphToTitle(title, menuKey), LayoutManager.getInstance().getRowsOrDefault(menuKey, defaultRows));
  }

  /**
   * Apply Nexo glyph to menu title if configured in layouts.yml.
   * This allows custom GUI textures using Nexo glyphs + negative spaces.
   */
  private static String applyGlyphToTitle(String title, String menuKey) {
    if (menuKey == null) {
      return title;
    }
    
    String glyph = LayoutManager.getInstance().getGlyphOrDefault(menuKey);
    if (glyph != null && !glyph.isEmpty()) {
      return glyph + title;
    }
    
    return title;
  }

  public abstract void open();
}
