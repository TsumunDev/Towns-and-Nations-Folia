@file:JvmName("NexoExtensions")
package org.leralix.tan.integration.nexo
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlinx.coroutines.*
import java.util.concurrent.CompletableFuture
fun String.toNexoItem(): ItemStack? {
    return NexoIntegration.getItem(this)
}
fun String.toNexoItemOr(fallback: Material): ItemStack {
    return NexoIntegration.getItemOrDefault(this, fallback)
}
fun String.toNexoItemOr(fallback: ItemStack): ItemStack {
    return NexoIntegration.getItemOrDefault(this, fallback)
}
fun String.isValidNexoItem(): Boolean {
    return NexoIntegration.itemExists(this)
}
suspend fun String.toNexoItemSuspending(): ItemStack? = withContext(Dispatchers.IO) {
    NexoIntegration.getItem(this@toNexoItemSuspending)
}
fun ItemStack?.isNexoItem(): Boolean {
    return this != null && NexoIntegration.getIdFromItem(this) != null
}
fun ItemStack?.isNexoItem(itemId: String): Boolean {
    return this.getNexoId() == itemId
}
fun ItemStack?.getNexoId(): String? {
    return if (this != null) NexoIntegration.getIdFromItem(this) else null
}
fun ItemStack?.isAnyNexoItem(vararg itemIds: String): Boolean {
    val id = this.getNexoId() ?: return false
    return id in itemIds
}
fun ItemStack?.toNexoDebugString(): String {
    if (this == null) return "null"
    val nexoId = this.getNexoId()
    return if (nexoId != null) "NexoItem[$nexoId, type=${this.type}]" else "VanillaItem[${this.type}]"
}
fun Player.giveNexoItem(itemId: String, amount: Int = 1, fallback: Material? = null): Boolean {
    val item = NexoIntegration.getItem(itemId)
    if (item != null) {
        item.amount = amount
        inventory.addItem(item)
        return true
    } else if (fallback != null) {
        val fallbackItem = ItemStack(fallback, amount)
        inventory.addItem(fallbackItem)
        return false
    }
    return false
}
fun Player.hasNexoItem(itemId: String): Boolean {
    return inventory.contents?.any { it != null && it.isNexoItem(itemId) } == true
}
fun Player.countNexoItem(itemId: String): Int {
    return inventory.contents?.filter { it != null && it.isNexoItem(itemId) }?.sumOf { it?.amount ?: 0 } ?: 0
}
fun Player.removeNexoItem(itemId: String, amount: Int = Int.MAX_VALUE): Int {
    var remaining = amount
    val contents = inventory.contents ?: return 0
    for (item in contents) {
        if (remaining <= 0) break
        if (item != null && item.isNexoItem(itemId)) {
            val toRemove = minOf(remaining, item.amount)
            item.amount -= toRemove
            remaining -= toRemove
        }
    }
    for (i in contents.indices) {
        if (contents[i]?.amount == 0) {
            contents[i] = null
        }
    }
    return amount - remaining
}
fun org.bukkit.Location.isNexoBlock(): Boolean {
    return NexoIntegration.isCustomBlock(this)
}
fun org.bukkit.Location.isNexoFurniture(): Boolean {
    return NexoIntegration.isFurniture(this)
}
fun org.bukkit.Location.placeNexoBlock(itemId: String): Boolean {
    return NexoIntegration.placeBlock(itemId, this)
}
fun org.bukkit.Location.placeNexoFurniture(itemId: String, player: Player? = null): Boolean {
    return NexoIntegration.placeFurniture(itemId, this, player)
}
fun org.bukkit.Location.removeNexoBlock(): Boolean {
    return NexoIntegration.removeBlock(this)
}
fun org.bukkit.Location.removeNexoFurniture(): Boolean {
    return NexoIntegration.removeFurniture(this)
}
fun Collection<ItemStack>.filterNexoItems(): List<ItemStack> {
    return filter { it.isNexoItem() }
}
fun Collection<ItemStack>.filterNexoItemsById(itemId: String): List<ItemStack> {
    return filter { it.isNexoItem(itemId) }
}
fun Collection<ItemStack>.getNexoIds(): Set<String> {
    return mapNotNull { it.getNexoId() }.toSet()
}
fun Collection<ItemStack>.countNexoItems(): Int {
    return count { it.isNexoItem() }
}
fun Collection<ItemStack>.containsNexoItems(): Boolean {
    return any { it.isNexoItem() }
}
fun Collection<String>.mapToNexoItems(): List<ItemStack?> {
    return map { NexoIntegration.getItem(it) }
}
fun Collection<String>.mapToNexoItemsOr(fallback: Material): List<ItemStack> {
    return map { NexoIntegration.getItemOrDefault(it, fallback) }
}
fun getItemSafely(itemId: String): Result<ItemStack> {
    return runCatching {
        NexoIntegration.getItem(itemId) ?: throw NoSuchElementException("Nexo item '$itemId' not found")
    }
}
fun placeBlockSafely(itemId: String, location: org.bukkit.Location): Result<Boolean> {
    return runCatching {
        NexoIntegration.placeBlock(itemId, location)
    }
}
fun placeFurnitureSafely(itemId: String, location: org.bukkit.Location, player: Player? = null): Result<Boolean> {
    return runCatching {
        NexoIntegration.placeFurniture(itemId, location, player)
    }
}
fun nexo(block: NexoDsl.() -> Unit) {
    NexoDsl.apply(block)
}
object NexoDsl {
    fun getItem(itemId: String, callback: (ItemStack) -> Unit) {
        NexoIntegration.getItem(itemId)?.let(callback)
    }
    fun getItemOr(itemId: String, fallback: Material, callback: (ItemStack) -> Unit) {
        callback(NexoIntegration.getItemOrDefault(itemId, fallback))
    }
    fun ifExists(itemId: String, callback: () -> Unit) {
        if (NexoIntegration.itemExists(itemId)) {
            callback()
        }
    }
    fun ifNotExists(itemId: String, callback: () -> Unit) {
        if (!NexoIntegration.itemExists(itemId)) {
            callback()
        }
    }
    fun placeBlock(itemId: String, location: org.bukkit.Location, callback: (Boolean) -> Unit = {}) {
        callback(NexoIntegration.placeBlock(itemId, location))
    }
    fun placeFurniture(itemId: String, location: org.bukkit.Location, player: Player? = null, callback: (Boolean) -> Unit = {}) {
        callback(NexoIntegration.placeFurniture(itemId, location, player))
    }
    fun batch(vararg itemIds: String, callback: (List<Pair<String, ItemStack?>>) -> Unit) {
        val results = itemIds.map { it to NexoIntegration.getItem(it) }
        callback(results)
    }
    fun ifEnabled(callback: () -> Unit) {
        if (NexoIntegration.isEnabled) {
            callback()
        }
    }
    fun ifDisabled(callback: () -> Unit) {
        if (!NexoIntegration.isEnabled) {
            callback()
        }
    }
    fun cacheStats(): String {
        return NexoIntegration.getCacheStats()
    }
    fun clearCache() {
        NexoIntegration.clearCache()
    }
    fun reinitialize() {
        NexoIntegration.reinitialize()
    }
    fun getVersion(): String? {
        return NexoIntegration.nexoVersion
    }
    fun isPluginPresent(): Boolean {
        return NexoIntegration.isPluginPresent()
    }
}
fun nexoItemsLoadedFlow(): kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.flow {
    if (NexoIntegration.isEnabled) {
        emit(Unit)
    }
}
fun nexoStatusFlow(): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flow {
    emit(NexoIntegration.isEnabled)
}
object NexoJavaExtensions {
    @JvmStatic
    fun toNexoItemAsync(itemId: String): CompletableFuture<ItemStack?> {
        return CompletableFuture.supplyAsync {
            itemId.toNexoItem()
        }
    }
    @JvmStatic
    fun existsAsync(itemId: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            itemId.isValidNexoItem()
        }
    }
    @JvmStatic
    fun giveAsync(player: Player, itemId: String, amount: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            player.giveNexoItem(itemId, amount)
        }
    }
    @JvmStatic
    fun getBatchAsync(itemIds: List<String>): CompletableFuture<List<ItemStack?>> {
        return CompletableFuture.supplyAsync {
            itemIds.map { NexoIntegration.getItem(it) }
        }
    }
}