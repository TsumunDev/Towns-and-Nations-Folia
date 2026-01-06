package org.leralix.tan.utils.gameplay
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.leralix.tan.gui.service.requirements.model.ItemScope
fun Player.hasEnoughItem(itemScope: ItemScope, amount: Int): Boolean {
    return getItemsNumberInInventory(itemScope) >= amount
}
fun Player.getItemsNumberInInventory(itemScope: ItemScope): Int {
    var total = 0
    for (item in inventory.contents) {
        if (item == null || item.type == Material.AIR || item.amount <= 0) continue
        if (itemScope.isInScope(item)) {
            total += item.amount
        }
    }
    return total
}
fun Player.removeItemsFromInventory(itemScope: ItemScope, amountToRemove: Int): Int {
    if (amountToRemove <= 0) return 0
    var amountRemoved = 0
    for (item in inventory.contents) {
        if (item == null || item.type == Material.AIR || item.amount <= 0) continue
        if (itemScope.isInScope(item)) {
            val itemAmount = item.amount
            val remainingToRemove = amountToRemove - amountRemoved
            val toRemove = minOf(itemAmount, remainingToRemove)
            item.amount = itemAmount - toRemove
            amountRemoved += toRemove
            if (amountRemoved >= amountToRemove) {
                break
            }
        }
    }
    return amountRemoved
}
object InventoryUtil {
    @JvmStatic
    fun playerEnoughItem(player: Player?, itemScope: ItemScope?, amount: Int): Boolean {
        return player != null && itemScope != null && player.hasEnoughItem(itemScope, amount)
    }
    @JvmStatic
    fun getItemsNumberInInventory(player: Player?, itemScope: ItemScope?): Int {
        if (player == null || itemScope == null) return 0
        return player.getItemsNumberInInventory(itemScope)
    }
    @JvmStatic
    fun removeItemsFromInventory(player: Player?, itemScope: ItemScope?, amountToRemove: Int) {
        if (player != null && itemScope != null) {
            player.removeItemsFromInventory(itemScope, amountToRemove)
        }
    }
}