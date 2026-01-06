package org.leralix.tan.utils.text
import org.leralix.tan.utils.NumberUtils
@Deprecated(
    message = "Use org.leralix.tan.utils.NumberUtils instead",
    ReplaceWith("NumberUtils")
)
object NumberUtil {
    @JvmStatic
    fun init() = NumberUtils.init()
    @JvmStatic
    fun roundWithDigits(value: Double): Double = NumberUtils.roundWithDigits(value)
    @JvmStatic
    fun getDigits(): Int = NumberUtils.getDigits()
    @JvmStatic
    fun parseIntOrNull(value: String): Int? = NumberUtils.parseIntOrNull(value)
    @JvmStatic
    fun parseDoubleOrNull(value: String): Double? = NumberUtils.parseDoubleOrNull(value)
    @JvmStatic
    fun parseLongOrNull(value: String): Long? = NumberUtils.parseLongOrNull(value)
    @JvmStatic
    fun clamp(value: Double, min: Double, max: Double): Double = NumberUtils.clamp(value, min, max)
    @JvmStatic
    fun clamp(value: Int, min: Int, max: Int): Int = NumberUtils.clamp(value, min, max)
    @JvmStatic
    fun percentage(current: Double, total: Double): Double = NumberUtils.percentage(current, total)
    @JvmStatic
    fun formatPercentage(current: Double, total: Double): String = NumberUtils.formatPercentage(current, total)
}