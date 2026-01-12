package org.leralix.tan.domain.quest.rewards;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.domain.quest.model.QuestReward;

/**
 * Quest reward that grants physical items to the player.
 *
 * @param itemStack The item stack to grant
 */
public record ItemQuestReward(ItemStack itemStack) implements QuestReward {

    public ItemQuestReward {
        if (itemStack == null) {
            throw new IllegalArgumentException("ItemStack cannot be null");
        }
    }

    @Override
    public RewardType getType() {
        return RewardType.ITEM;
    }

    @Override
    public String getDescription() {
        String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
                ? itemStack.getItemMeta().getDisplayName()
                : itemStack.getType().name().replace("_", " ").toLowerCase();

        return String.format("%dx %s", itemStack.getAmount(), itemName);
    }

    @Override
    public void grant(Player player) {
        // Give item to player's inventory
        player.getInventory().addItem(itemStack.clone());

        // Drop items if inventory is full
        // (Bukkit's addItem() does this automatically)
    }
}
