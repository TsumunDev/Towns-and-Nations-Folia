@file:JvmName("TanExtensions")
package org.leralix.tan.core
fun String.translateColorCodes(): String {
    return this.replace("&", "§")
}
fun String.stripColorCodes(): String {
    return this.replace(Regex("§[0-9a-fk-or]"), "")
}
fun String.isValidTerritoryName(): Boolean {
    return this.length in 3..24 && this.matches(Regex("^[a-zA-Z0-9_ ]+$"))
}
fun String.titleCase(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}
inline fun <T : Any, R> T?.ifNotNull(block: (T) -> R): R? {
    return this?.let(block)
}
inline fun <R> String?.ifNotBlank(block: (String) -> R): R? {
    return if (!this.isNullOrBlank()) block(this) else null
}
fun <T : Any> T?.orThrow(message: () -> String): T {
    return this ?: throw IllegalStateException(message())
}
fun <K, V> Map<K, V>.getOrCompute(key: K, defaultValue: () -> V): V {
    return this[key] ?: defaultValue()
}
fun <T> List<T>.batch(size: Int): List<List<T>> {
    return this.chunked(size)
}
inline fun <T> List<T>.findIndexed(predicate: (index: Int, T) -> Boolean): Pair<Int, T>? {
    forEachIndexed { index, element ->
        if (predicate(index, element)) return index to element
    }
    return null
}
inline fun <T> safeCall(block: () -> T): Result<T> {
    return runCatching { block() }
}
inline fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> {
    return this.recoverCatching { throw transform(it) }
}
fun <A, B> Result<A>.zip(other: Result<B>): Result<Pair<A, B>> {
    return this.flatMap { a -> other.map { b -> a to b } }
}
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )
}
fun Number.formatWithSeparators(): String {
    return String.format("%,d", this.toLong())
}
fun Double.format(decimals: Int = 2): String {
    return String.format("%.${decimals}f", this)
}
fun Int.clamp(min: Int, max: Int): Int {
    return maxOf(min, minOf(max, this))
}
fun Double.clamp(min: Double, max: Double): Double {
    return maxOf(min, minOf(max, this))
}