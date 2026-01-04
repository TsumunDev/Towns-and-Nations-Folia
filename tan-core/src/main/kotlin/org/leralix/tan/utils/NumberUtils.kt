package org.leralix.tan.utils

import org.leralix.tan.utils.constants.Constants
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Kotlin number utilities with extensions.
 * Replaces org.leralix.tan.utils.text.NumberUtil
 */
object NumberUtils {
    
    private var nbDigits: Int = 2
    
    /**
     * Initialize with configured digit count
     */
    @JvmStatic
    fun init() {
        nbDigits = Constants.getNbDigits()
    }
    
    /**
     * Round a value to configured decimal places
     */
    @JvmStatic
    fun roundWithDigits(value: Double): Double {
        val multiplier = 10.0.pow(nbDigits)
        return (value * multiplier).roundToLong() / multiplier
    }
    
    /**
     * Get the configured number of digits
     */
    @JvmStatic
    fun getDigits(): Int = nbDigits
    
    /**
     * Parse integer safely, returning null on failure
     */
    @JvmStatic
    fun parseIntOrNull(value: String): Int? = value.toIntOrNull()
    
    /**
     * Parse double safely, returning null on failure
     */
    @JvmStatic
    fun parseDoubleOrNull(value: String): Double? = value.toDoubleOrNull()
    
    /**
     * Parse long safely, returning null on failure
     */
    @JvmStatic
    fun parseLongOrNull(value: String): Long? = value.toLongOrNull()
    
    /**
     * Clamp a value between min and max
     */
    @JvmStatic
    fun clamp(value: Double, min: Double, max: Double): Double = 
        value.coerceIn(min, max)
    
    /**
     * Clamp an integer between min and max
     */
    @JvmStatic
    fun clamp(value: Int, min: Int, max: Int): Int = 
        value.coerceIn(min, max)
    
    /**
     * Calculate percentage
     */
    @JvmStatic
    fun percentage(current: Double, total: Double): Double = 
        if (total == 0.0) 0.0 else (current / total) * 100.0
    
    /**
     * Format percentage with % suffix
     */
    @JvmStatic
    fun formatPercentage(current: Double, total: Double): String = 
        "%.1f%%".format(percentage(current, total))
}

// Extension functions

/**
 * Round this double to configured decimal places
 */
fun Double.roundToConfiguredDigits(): Double = NumberUtils.roundWithDigits(this)

/**
 * Clamp this value between min and max
 */
fun Double.clamp(min: Double, max: Double): Double = coerceIn(min, max)

/**
 * Clamp this integer between min and max
 */
fun Int.clamp(min: Int, max: Int): Int = coerceIn(min, max)

/**
 * Calculate what percentage this value is of total
 */
fun Double.percentageOf(total: Double): Double = NumberUtils.percentage(this, total)

/**
 * Format as percentage string
 */
fun Double.asPercentageOf(total: Double): String = NumberUtils.formatPercentage(this, total)

/**
 * Check if this number is positive
 */
fun Double.isPositive(): Boolean = this > 0

/**
 * Check if this number is negative
 */
fun Double.isNegative(): Boolean = this < 0

/**
 * Check if this number is zero (with epsilon tolerance)
 */
fun Double.isZero(epsilon: Double = 0.0001): Boolean = 
    kotlin.math.abs(this) < epsilon

/**
 * Format with thousands separators
 */
fun Long.formatWithSeparators(): String = 
    String.format("%,d", this)

/**
 * Format with thousands separators
 */
fun Int.formatWithSeparators(): String = 
    String.format("%,d", this)
