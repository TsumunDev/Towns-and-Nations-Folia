package org.leralix.tan.utils.text
import org.leralix.lib.utils.RandomUtil
import org.leralix.tan.utils.constants.Constants
object StringUtil {
    private val HEX_COLOR_REGEX = Regex("^[0-9A-Fa-f]{6}$")
    @JvmStatic
    fun isValidColorCode(colorCode: String): Boolean = HEX_COLOR_REGEX.matches(colorCode)
    @JvmStatic
    fun hexColorToInt(hexColor: String): Int = hexColor.toInt(16)
    @JvmStatic
    fun randomColor(): Int {
        val random = RandomUtil.getRandom()
        val red = random.nextInt(256)
        val green = random.nextInt(256)
        val blue = random.nextInt(256)
        return (red shl 16) or (green shl 8) or blue
    }
    @JvmStatic
    fun getColoredMoney(money: Double): String {
        val formatted = formatMoney(money)
        return when {
            money > 0 -> "§a+$formatted"
            money < 0 -> "§c$formatted"
            else -> "§7$formatted"
        }
    }
    @JvmStatic
    fun formatMoney(amount: Double): String {
        val absAmount = kotlin.math.abs(amount)
        val suffix = when {
            absAmount < 1_000 -> ""
            absAmount < 1_000_000 -> "K"
            absAmount < 1_000_000_000 -> "M"
            absAmount < 1_000_000_000_000L -> "B"
            else -> "T"
        }
        if (suffix.isEmpty()) return handleDigits(amount).toString()
        val divisor = when (suffix) {
            "K" -> 1_000.0
            "M" -> 1_000_000.0
            "B" -> 1_000_000_000.0
            "T" -> 1_000_000_000_000.0
            else -> 1.0
        }
        return "%.1f$suffix".format(amount / divisor)
    }
    @JvmStatic
    fun handleDigits(amount: Double): Double {
        val digitVal = Math.pow(10.0, Constants.getNbDigits().toDouble())
        return Math.round(amount * digitVal) / digitVal
    }
}
fun String.isValidHexColor(): Boolean = StringUtil.isValidColorCode(this)
fun String.toColorInt(): Int = StringUtil.hexColorToInt(this)
fun Double.formatAsMoney(): String = StringUtil.formatMoney(this)
fun Double.formatAsColoredMoney(): String = StringUtil.getColoredMoney(this)
fun Double.roundToDigits(): Double = StringUtil.handleDigits(this)
fun String.colorize(): String = this.replace('&', '§')
fun String.stripColors(): String = this.replace(Regex("§[0-9a-fk-or]"), "")
fun String?.isNullOrBlankOrEmpty(): Boolean = this.isNullOrBlank()
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String =
    if (length <= maxLength) this
    else take(maxLength - ellipsis.length) + ellipsis
fun String.capitalizeFirst(): String =
    if (isEmpty()) this
    else this[0].uppercaseChar() + substring(1).lowercase()
fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { it.capitalizeFirst() }