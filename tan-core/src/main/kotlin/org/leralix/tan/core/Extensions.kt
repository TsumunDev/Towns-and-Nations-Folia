@file:JvmName("TanExtensions")
package org.leralix.tan.core

/**
 * Core Kotlin extensions for Towns and Nations.
 * These extensions provide idiomatic Kotlin access to common plugin operations.
 * 
 * Note: Extensions that depend on plugin classes (ITanPlayer, Storage, etc.)
 * will be added in Phase 2 after those classes are migrated or have Kotlin interfaces.
 */

// ============================================================================
// String Extensions
// ============================================================================

/**
 * Translate Minecraft color codes (& -> §).
 */
fun String.translateColorCodes(): String {
    return this.replace("&", "§")
}

/**
 * Strip all color codes from a string.
 */
fun String.stripColorCodes(): String {
    return this.replace(Regex("§[0-9a-fk-or]"), "")
}

/**
 * Check if string is a valid town/region name.
 * Must be 3-24 characters, alphanumeric with spaces and underscores.
 */
fun String.isValidTerritoryName(): Boolean {
    return this.length in 3..24 && this.matches(Regex("^[a-zA-Z0-9_ ]+$"))
}

/**
 * Capitalize the first letter of each word.
 */
fun String.titleCase(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}

// ============================================================================
// Null Safety Extensions
// ============================================================================

/**
 * Execute block only if value is not null, with logging on null.
 */
inline fun <T : Any, R> T?.ifNotNull(block: (T) -> R): R? {
    return this?.let(block)
}

/**
 * Execute block only if value is not null and not blank (for strings).
 */
inline fun <R> String?.ifNotBlank(block: (String) -> R): R? {
    return if (!this.isNullOrBlank()) block(this) else null
}

/**
 * Return this value or throw an exception with a message.
 */
fun <T : Any> T?.orThrow(message: () -> String): T {
    return this ?: throw IllegalStateException(message())
}

// ============================================================================
// Collection Extensions
// ============================================================================

/**
 * Safe get with default value for maps.
 */
fun <K, V> Map<K, V>.getOrCompute(key: K, defaultValue: () -> V): V {
    return this[key] ?: defaultValue()
}

/**
 * Partition a list into chunks of specified size for batch processing.
 */
fun <T> List<T>.batch(size: Int): List<List<T>> {
    return this.chunked(size)
}

/**
 * Find first element or null, with index.
 */
inline fun <T> List<T>.findIndexed(predicate: (index: Int, T) -> Boolean): Pair<Int, T>? {
    forEachIndexed { index, element ->
        if (predicate(index, element)) return index to element
    }
    return null
}

// ============================================================================
// Result Extensions (Error Handling)
// ============================================================================

/**
 * Execute a block and wrap any exception in a Result.
 * Useful for safe operations that might fail.
 * 
 * Usage:
 * ```kotlin
 * val result = safeCall { riskyOperation() }
 * result.onSuccess { println("Success: $it") }
 *       .onFailure { println("Error: ${it.message}") }
 * ```
 */
inline fun <T> safeCall(block: () -> T): Result<T> {
    return runCatching { block() }
}

/**
 * Map a Result's failure to a different exception type.
 */
inline fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> {
    return this.recoverCatching { throw transform(it) }
}

/**
 * Combine two Results into a Pair.
 */
fun <A, B> Result<A>.zip(other: Result<B>): Result<Pair<A, B>> {
    return this.flatMap { a -> other.map { b -> a to b } }
}

/**
 * FlatMap for Result.
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )
}

// ============================================================================
// Number Extensions
// ============================================================================

/**
 * Format a number with thousand separators.
 */
fun Number.formatWithSeparators(): String {
    return String.format("%,d", this.toLong())
}

/**
 * Format a double to a specific number of decimal places.
 */
fun Double.format(decimals: Int = 2): String {
    return String.format("%.${decimals}f", this)
}

/**
 * Clamp a value between min and max.
 */
fun Int.clamp(min: Int, max: Int): Int {
    return maxOf(min, minOf(max, this))
}

/**
 * Clamp a double value between min and max.
 */
fun Double.clamp(min: Double, max: Double): Double {
    return maxOf(min, minOf(max, this))
}
