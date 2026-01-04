package org.leralix.tan.utils

import org.leralix.lib.utils.RandomUtil
import org.leralix.tan.utils.constants.Constants

/**
 * Kotlin string utilities with idiomatic extensions.
 * Replaces org.leralix.tan.utils.text.StringUtil
 */
object StringUtils {
    
    private val HEX_COLOR_REGEX = Regex("^[0-9A-Fa-f]{6}$")
    
    /**
     * Check if a string is a valid hex color code (ex: 00FF00 for green)
     */
    @JvmStatic
    fun isValidColorCode(colorCode: String): Boolean = HEX_COLOR_REGEX.matches(colorCode)
    
    /**
     * Convert a hex color code to an integer
     */
    @JvmStatic
    fun hexColorToInt(hexColor: String): Int = hexColor.toInt(16)
    
    /**
     * Generate a random RGB color as integer
     */
    @JvmStatic
    fun randomColor(): Int {
        val random = RandomUtil.getRandom()
        val red = random.nextInt(256)
        val green = random.nextInt(256)
        val blue = random.nextInt(256)
        return (red shl 16) or (green shl 8) or blue
    }
    
    /**
     * Format money with color based on sign
     */
    @JvmStatic
    fun getColoredMoney(money: Double): String {
        val formatted = formatMoney(money)
        return when {
            money > 0 -> "§a+$formatted"
            money < 0 -> "§c$formatted"
            else -> "§7$formatted"
        }
    }
    
    /**
     * Format money with K/M/B/T suffixes
     */
    @JvmStatic
    fun formatMoney(amount: Double): String = when {
        amount < 1_000 -> handleDigits(amount).toString()
        amount < 1_000_000 -> "%.1fK".format(amount / 1_000)
        amount < 1_000_000_000 -> "%.1fM".format(amount / 1_000_000)
        amount < 1_000_000_000_000L -> "%.1fB".format(amount / 1_000_000_000)
        else -> "%.1fT".format(amount / 1_000_000_000_000L)
    }
    
    /**
     * Round to configured number of digits
     */
    @JvmStatic
    fun handleDigits(amount: Double): Double {
        val digitVal = Math.pow(10.0, Constants.getNbDigits().toDouble())
        return Math.round(amount * digitVal) / digitVal
    }
}

// Extension functions for idiomatic Kotlin usage

/**
 * Check if this string is a valid hex color code
 */
fun String.isValidHexColor(): Boolean = StringUtils.isValidColorCode(this)

/**
 * Convert this hex string to color integer
 */
fun String.toColorInt(): Int = StringUtils.hexColorToInt(this)

/**
 * Format this number as money with K/M/B/T suffixes
 */
fun Double.formatAsMoney(): String = StringUtils.formatMoney(this)

/**
 * Format this number as colored money (+green, -red)
 */
fun Double.formatAsColoredMoney(): String = StringUtils.getColoredMoney(this)

/**
 * Round to configured decimal places
 */
fun Double.roundToDigits(): Double = StringUtils.handleDigits(this)

/**
 * Colorize string with Minecraft color codes
 */
fun String.colorize(): String = this.replace('&', '§')

/**
 * Strip Minecraft color codes from string
 */
fun String.stripColors(): String = this.replace(Regex("§[0-9a-fk-or]"), "")

/**
 * Check if string is null or blank
 */
fun String?.isNullOrBlankOrEmpty(): Boolean = this.isNullOrBlank()

/**
 * Truncate string to max length with ellipsis
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String = 
    if (length <= maxLength) this
    else take(maxLength - ellipsis.length) + ellipsis

/**
 * Capitalize first letter only
 */
fun String.capitalizeFirst(): String = 
    if (isEmpty()) this
    else this[0].uppercaseChar() + substring(1).lowercase()

/**
 * Convert to title case (each word capitalized)
 */
fun String.toTitleCase(): String = 
    split(" ").joinToString(" ") { it.capitalizeFirst() }
