package org.leralix.tan.utils.gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
@Deprecated(since = "0.16.0", forRemoval = true)
public class GuiHelperBridge {
  private GuiHelperBridge() {
    throw new IllegalStateException("Utility class");
  }
  public static GuiItem createBackArrow(Player player, Consumer<Player> openMenuAction) {
    return org.leralix.tan.utils.deprecated.GuiUtil.createBackArrow(player, openMenuAction);
  }
  public static GuiItem getUnnamedItem(Material material) {
    return org.leralix.tan.utils.deprecated.GuiUtil.getUnnamedItem(material);
  }
  public static ItemStack getUnnamedItemStack(Material material) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.displayName(net.kyori.adventure.text.Component.text(""));
      item.setItemMeta(meta);
    }
    return item;
  }
  public static ItemStack getDecorativePane(Material material) {
    return getUnnamedItemStack(material);
  }
  @Deprecated(since = "0.16.0", forRemoval = true)
  public static void createIterator(
      dev.triumphteam.gui.guis.Gui gui,
      List<GuiItem> guItems,
      int page,
      Player player,
      Consumer<Player> backArrowAction,
      Consumer<Player> nextPageAction,
      Consumer<Player> previousPageAction) {
    org.leralix.tan.utils.deprecated.GuiUtil.createIterator(
        gui, guItems, page, player, backArrowAction, nextPageAction, previousPageAction);
  }
  @Deprecated(since = "0.16.0", forRemoval = true)
  public static void createIterator(
      dev.triumphteam.gui.guis.Gui gui,
      List<GuiItem> guItems,
      int page,
      Player player,
      Consumer<Player> backArrowAction,
      Consumer<Player> nextPageAction,
      Consumer<Player> previousPageAction,
      Material decorativeMaterial) {
    org.leralix.tan.utils.deprecated.GuiUtil.createIterator(
        gui,
        guItems,
        page,
        player,
        backArrowAction,
        nextPageAction,
        previousPageAction,
        decorativeMaterial);
  }
  @Deprecated(since = "0.16.0", forRemoval = true)
  public static void createIterator(
      dev.triumphteam.gui.guis.Gui gui,
      List<GuiItem> guItems,
      int page,
      Player player,
      Consumer<Player> backArrowAction,
      Consumer<Player> nextPageAction,
      Consumer<Player> previousPageAction,
      ItemStack decorativeGlassPane) {
    org.leralix.tan.utils.deprecated.GuiUtil.createIterator(
        gui,
        guItems,
        page,
        player,
        backArrowAction,
        nextPageAction,
        previousPageAction,
        decorativeGlassPane);
  }
}