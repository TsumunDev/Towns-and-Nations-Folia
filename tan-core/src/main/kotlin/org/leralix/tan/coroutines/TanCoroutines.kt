package org.leralix.tan.coroutines
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
object TanCoroutines {
    private val logger = LoggerFactory.getLogger(TanCoroutines::class.java)
    private val isInitialized = AtomicBoolean(false)
    private lateinit var supervisorJob: CompletableJob
    lateinit var scope: CoroutineScope
        private set
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
    @JvmStatic
    fun shutdown() {
        if (isInitialized.compareAndSet(true, false)) {
            logger.info("Shutting down TanCoroutines...")
            supervisorJob.cancel()
            logger.info("TanCoroutines shutdown complete (jobs cancelled)")
        }
    }
    @JvmStatic
    fun isReady(): Boolean = isInitialized.get()
    fun launch(
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        ensureInitialized()
        return scope.launch(context, start, block)
    }
    fun <T> async(
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        ensureInitialized()
        return scope.async(context, start, block)
    }
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
suspend fun <T> CompletableFuture<T>.awaitTan(): T = this.await()
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