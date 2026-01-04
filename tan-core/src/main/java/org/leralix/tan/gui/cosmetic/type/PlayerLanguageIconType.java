package org.leralix.tan.gui.cosmetic.type;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Icon type that displays the player's language flag.
 * 
 * NOTE: This icon type is problematic because getItemStack() is called synchronously
 * but we need async player data loading. The proper fix is to refactor IconType
 * to accept ITanPlayer or LangType directly.
 * 
 * Current workaround: Use English as fallback since player lang should be loaded in cache
 * by the time GUI renders (all GUI open() methods load player data async first).
 */
public class PlayerLanguageIconType extends IconType {
  @Override
  protected ItemStack getItemStack(Player player) {
<<<<<<< Updated upstream
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    return tanPlayer.getLang().getIcon(tanPlayer.getLang());
=======
    // ⚠️ TEMPORARY FIX: Return English flag as placeholder
    // Proper fix requires refactoring IconType.getItemStack() signature
    // to accept pre-loaded ITanPlayer or LangType parameter
    return org.leralix.tan.lang.LangType.ENGLISH.getIcon(org.leralix.tan.lang.LangType.ENGLISH);
>>>>>>> Stashed changes
  }
}


