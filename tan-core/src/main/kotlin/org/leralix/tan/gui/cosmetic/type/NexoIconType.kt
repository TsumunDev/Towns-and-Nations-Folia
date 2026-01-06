package org.leralix.tan.gui.cosmetic.type
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.leralix.tan.integration.nexo.NexoIntegration
import org.slf4j.LoggerFactory
class NexoIconType(
    val nexoItemId: String,
    val fallbackMaterial: Material = Material.BARRIER
) : IconType() {
    companion object {
        private val logger = LoggerFactory.getLogger(NexoIconType::class.java)
        @JvmStatic
        fun parse(iconString: String): NexoIconType? {
            if (!iconString.startsWith("nexo:", ignoreCase = true)) {
                return null
            }
            val parts = iconString.split(":", limit = 3)
            if (parts.size < 2) {
                logger.warn("[NexoIconType] Invalid format (missing item ID): $iconString")
                return null
            }
            val itemId = parts[1].trim()
            if (itemId.isEmpty()) {
                logger.warn("[NexoIconType] Invalid format (empty item ID): $iconString")
                return null
            }
            val fallback = if (parts.size >= 3) {
                val fallbackName = parts[2].trim()
                try {
                    Material.valueOf(fallbackName)
                } catch (e: IllegalArgumentException) {
                    logger.warn("[NexoIconType] Invalid fallback material '$fallbackName' - using BARRIER")
                    Material.BARRIER
                }
            } else {
                Material.BARRIER
            }
            return NexoIconType(itemId, fallback)
        }
    }
    override fun getItemStack(player: Player?): ItemStack {
        val startTime = System.currentTimeMillis()
        val item = NexoIntegration.getItemOrDefault(nexoItemId, fallbackMaterial)
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed > 50) {
            logger.warn("[NexoIconType] Slow load for '{}' ({}ms) - consider pre-fetching", nexoItemId, elapsed)
        }
        val meta = item.itemMeta
        val hasCmd = meta?.hasCustomModelData() == true
        val cmd = if (hasCmd) meta?.customModelData else null
        logger.info("[NexoIconType] Loading '{}': type={}, hasCmd={}, cmd={}, isFallback={}",
            nexoItemId, item.type, hasCmd, cmd, item.type == fallbackMaterial)
        val cloned = item.clone()
        val clonedMeta = cloned.itemMeta
        val clonedHasCmd = clonedMeta?.hasCustomModelData() == true
        val clonedCmd = if (clonedHasCmd) clonedMeta?.customModelData else null
        logger.info("[NexoIconType] After clone '{}': hasCmd={}, cmd={}", nexoItemId, clonedHasCmd, clonedCmd)
        return cloned
    }
    fun isUsingFallback(): Boolean {
        if (!NexoIntegration.isEnabled) return true
        return !NexoIntegration.itemExists(nexoItemId)
    }
    override fun toString(): String {
        return "NexoIconType(itemId='$nexoItemId', fallback=$fallbackMaterial, usingFallback=${isUsingFallback()})"
    }
}