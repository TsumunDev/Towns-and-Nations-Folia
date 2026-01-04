package org.leralix.tan.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Central coroutine scope manager for Towns and Nations.
 * Provides a supervised scope for all async operations in the plugin.
 * 
 * Usage:
 * ```kotlin
 * TanCoroutines.launch {
 *     val player = PlayerDataStorage.getInstance().get(uuid).awaitTan()
 *     // Process player data
 * }
 * ```
 */
object TanCoroutines {
    
    private val logger = LoggerFactory.getLogger(TanCoroutines::class.java)
    
    private val isInitialized = AtomicBoolean(false)
    
    private lateinit var supervisorJob: CompletableJob
    
    /**
     * Main coroutine scope for plugin operations.
     * Uses IO dispatcher for database/network operations.
     */
    lateinit var scope: CoroutineScope
        private set
    
    /**
     * Initialize the coroutine scope. Call this in plugin onEnable().
     */
    @JvmStatic
    fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            supervisorJob = SupervisorJob()
            scope = CoroutineScope(
                supervisorJob + 
                Dispatchers.IO + 
                CoroutineName("TaN-Coroutines") +
                CoroutineExceptionHandler { context, throwable ->
                    logger.error("Uncaught exception in coroutine [${context[CoroutineName]}]: ${throwable.message}", throwable)
                }
            )
            logger.info("TanCoroutines initialized successfully")
        }
    }
    
    /**
     * Shutdown the coroutine scope gracefully. Call this in plugin onDisable().
     */
    @JvmStatic
    fun shutdown() {
        if (isInitialized.compareAndSet(true, false)) {
            runBlocking {
                logger.info("Shutting down TanCoroutines...")
                supervisorJob.cancelAndJoin()
                logger.info("TanCoroutines shutdown complete")
            }
        }
    }
    
    /**
     * Check if coroutines are initialized.
     */
    @JvmStatic
    fun isReady(): Boolean = isInitialized.get()
    
    /**
     * Launch a new coroutine in the TaN scope.
     */
    fun launch(
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        ensureInitialized()
        return scope.launch(context, start, block)
    }
    
    /**
     * Launch a new async coroutine in the TaN scope.
     */
    fun <T> async(
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        ensureInitialized()
        return scope.async(context, start, block)
    }
    
    /**
     * Convert a suspend function to a CompletableFuture for Java interop.
     * Static method accessible from Java via TanCoroutines.asFuture(() -> ...)
     */
    @JvmStatic
    fun <T> asFuture(
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> T
    ): CompletableFuture<T> {
        ensureInitialized()
        return scope.asFuture(context, block)
    }
    
    private fun ensureInitialized() {
        if (!isInitialized.get()) {
            throw IllegalStateException("TanCoroutines not initialized. Call TanCoroutines.initialize() in plugin onEnable()")
        }
    }
}

/**
 * Extension function to await a CompletableFuture in a suspend context.
 * This is the bridge between Java's CompletableFuture and Kotlin coroutines.
 * 
 * Usage:
 * ```kotlin
 * suspend fun getPlayer(uuid: UUID): ITanPlayer {
 *     return PlayerDataStorage.getInstance().get(uuid).awaitTan()
 * }
 * ```
 */
suspend fun <T> CompletableFuture<T>.awaitTan(): T = this.await()

/**
 * Extension function to convert a suspend function result to CompletableFuture.
 * Useful for Java interop when Java code needs to call Kotlin suspend functions.
 * 
 * Usage from Java:
 * ```java
 * CompletableFuture<TownData> future = TownServiceKt.getTownAsync(townId);
 * ```
 */
fun <T> CoroutineScope.asFuture(
    context: CoroutineContext = Dispatchers.IO,
    block: suspend CoroutineScope.() -> T
): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    launch(context) {
        try {
            future.complete(block())
        } catch (e: CancellationException) {
            future.cancel(true)
        } catch (e: Throwable) {
            future.completeExceptionally(e)
        }
    }
    return future
}
