package org.leralix.tan.utils
import org.leralix.tan.utils.constants.Constants
import kotlin.math.pow
import kotlin.math.roundToLong
object NumberUtils {
    private var nbDigits: Int = 2
    @JvmStatic
    fun init() {
        nbDigits = Constants.getNbDigits()
    }
    @JvmStatic
    fun roundWithDigits(value: Double): Double {
        val multiplier = 10.0.pow(nbDigits)
        return (value * multiplier).roundToLong() / multiplier
    }
    @JvmStatic
    fun getDigits(): Int = nbDigits
    @JvmStatic
    fun parseIntOrNull(value: String): Int? = value.toIntOrNull()
    @JvmStatic
    fun parseDoubleOrNull(value: String): Double? = value.toDoubleOrNull()
    @JvmStatic
    fun parseLongOrNull(value: String): Long? = value.toLongOrNull()
    @JvmStatic
    fun clamp(value: Double, min: Double, max: Double): Double =
        value.coerceIn(min, max)
    @JvmStatic
    fun clamp(value: Int, min: Int, max: Int): Int =
        value.coerceIn(min, max)
    @JvmStatic
    fun percentage(current: Double, total: Double): Double =
        if (total == 0.0) 0.0 else (current / total) * 100.0
    @JvmStatic
    fun formatPercentage(current: Double, total: Double): String =
        "%.1f%%".format(percentage(current, total))
}
fun Double.roundToConfiguredDigits(): Double = NumberUtils.roundWithDigits(this)
fun Double.clamp(min: Double, max: Double): Double = coerceIn(min, max)
fun Int.clamp(min: Int, max: Int): Int = coerceIn(min, max)
fun Double.percentageOf(total: Double): Double = NumberUtils.percentage(this, total)
fun Double.asPercentageOf(total: Double): String = NumberUtils.formatPercentage(this, total)
fun Double.isPositive(): Boolean = this > 0
fun Double.isNegative(): Boolean = this < 0
fun Double.isZero(epsilon: Double = 0.0001): Boolean =
    kotlin.math.abs(this) < epsilon
fun Long.formatWithSeparators(): String =
    String.format("%,d", this)
fun Int.formatWithSeparators(): String =
    String.format("%,d", this)