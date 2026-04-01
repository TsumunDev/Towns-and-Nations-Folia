package org.leralix.tan.dataclass.territory.cosmetic;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.utils.gameplay.ItemStackSerializer;
/**
 * Custom icon implementation using base64 serialized ItemStack.
 *
 * <p>Legacy fields (materialTypeName, customModelData) were removed in version 0.15.2.
 * Old icon data will display as BARRIER icon - re-save your custom icons through GUI to update.</p>
 *
 * @since 0.15.1
 */
public class CustomIcon implements ICustomIcon {
  private String base64Item;

  public CustomIcon(ItemStack icon) {
    this.base64Item = ItemStackSerializer.serializeItemStack(icon);
  }

  public ItemStack getIcon() {
    if (base64Item == null) {
      // Legacy data without base64 serialization - return fallback icon
      return new ItemStack(Material.BARRIER);
    }
    return ItemStackSerializer.deserializeItemStack(base64Item);
  }
}
