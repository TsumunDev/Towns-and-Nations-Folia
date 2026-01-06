package org.leralix.tan.utils.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.ChatColor
import org.bukkit.inventory.meta.ItemMeta
private val SECTION_SERIALIZER = LegacyComponentSerializer.legacySection()
fun String.fromLegacy(): Component = SECTION_SERIALIZER.deserialize(this)
fun List<String>.fromLegacy(): List<Component> = map { it.fromLegacy() }
fun Component.toLegacy(): String = SECTION_SERIALIZER.serialize(this)
fun ItemMeta.setDisplayNameLegacy(legacyDisplayName: String) {
    displayName(legacyDisplayName.fromLegacy())
}
fun ItemMeta.setLoreLegacy(legacyLore: List<String>) {
    lore(legacyLore.fromLegacy())
}
@Suppress("DEPRECATION")
fun TextColor.toLegacyChatColor(): ChatColor {
    if (this is NamedTextColor) {
        return when (this) {
            NamedTextColor.BLACK -> ChatColor.BLACK
            NamedTextColor.DARK_BLUE -> ChatColor.DARK_BLUE
            NamedTextColor.DARK_GREEN -> ChatColor.DARK_GREEN
            NamedTextColor.DARK_AQUA -> ChatColor.DARK_AQUA
            NamedTextColor.DARK_RED -> ChatColor.DARK_RED
            NamedTextColor.DARK_PURPLE -> ChatColor.DARK_PURPLE
            NamedTextColor.GOLD -> ChatColor.GOLD
            NamedTextColor.GRAY -> ChatColor.GRAY
            NamedTextColor.DARK_GRAY -> ChatColor.DARK_GRAY
            NamedTextColor.BLUE -> ChatColor.BLUE
            NamedTextColor.GREEN -> ChatColor.GREEN
            NamedTextColor.AQUA -> ChatColor.AQUA
            NamedTextColor.RED -> ChatColor.RED
            NamedTextColor.LIGHT_PURPLE -> ChatColor.LIGHT_PURPLE
            NamedTextColor.YELLOW -> ChatColor.YELLOW
            NamedTextColor.WHITE -> ChatColor.WHITE
            else -> ChatColor.WHITE
        }
    }
    return ChatColor.WHITE
}
object ComponentUtil {
    @JvmStatic
    fun fromLegacy(legacyText: String?): Component {
        return legacyText?.fromLegacy() ?: Component.empty()
    }
    @JvmStatic
    fun fromLegacy(legacyTexts: List<String>?): List<Component> {
        return legacyTexts?.fromLegacy() ?: emptyList()
    }
    @JvmStatic
    fun toLegacy(component: Component?): String {
        return component?.toLegacy() ?: ""
    }
    @JvmStatic
    fun setDisplayName(meta: ItemMeta?, legacyDisplayName: String?) {
        if (meta != null && legacyDisplayName != null) {
            meta.setDisplayNameLegacy(legacyDisplayName)
        }
    }
    @JvmStatic
    fun setLore(meta: ItemMeta?, legacyLore: List<String>?) {
        if (meta != null && legacyLore != null) {
            meta.setLoreLegacy(legacyLore)
        }
    }
    @JvmStatic
    @Suppress("DEPRECATION")
    fun toLegacyChatColor(textColor: TextColor?): ChatColor {
        return textColor?.toLegacyChatColor() ?: ChatColor.WHITE
    }
}