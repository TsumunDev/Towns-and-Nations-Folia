package org.leralix.tan.coroutines

import kotlinx.coroutines.*
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import kotlin.coroutines.CoroutineContext

/**
 * Folia-compatible dispatcher factory.
 * Creates dispatchers that route execution through FoliaScheduler for proper region-aware scheduling.
 * 
 * Note: On Folia, there is no single "main thread" - each region has its own thread.
 * 
 * Usage:
 * ```kotlin
 * withContext(FoliaDispatchers.global(plugin)) {
 *     // Runs on Folia's global scheduler
 * }
 * 
 * withContext(FoliaDispatchers.forLocation(plugin, location)) {
 *     // Runs on the thread owning this location's region
 * }
 * ```
 */
object FoliaDispatchers {
    
    /**
     * Create a dispatcher for Folia's global region scheduler.
     * Suitable for operations that don't interact with specific chunks/entities.
     */
    @JvmStatic
    fun global(plugin: Plugin): CoroutineDispatcher {
        return object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                // Use reflection to call FoliaScheduler to avoid compile-time dependency
                try {
                    val foliaSchedulerClass = Class.forName("org.leralix.tan.utils.FoliaScheduler")
                    val runTaskMethod = foliaSchedulerClass.getMethod("runTask", Plugin::class.java, Runnable::class.java)
                    runTaskMethod.invoke(null, plugin, block)
                } catch (e: Exception) {
                    // Fallback: run directly (for testing or non-Folia environments)
                    block.run()
                }
            }
        }
    }
    
    /**
     * Create a dispatcher for a specific location's region.
     * Use this for chunk-related operations.
     */
    @JvmStatic
    fun forLocation(plugin: Plugin, location: Location): CoroutineDispatcher {
        return object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                try {
                    val foliaSchedulerClass = Class.forName("org.leralix.tan.utils.FoliaScheduler")
                    val runTaskMethod = foliaSchedulerClass.getMethod(
                        "runTaskAtLocation", 
                        Plugin::class.java, 
                        Location::class.java, 
                        Runnable::class.java
                    )
                    runTaskMethod.invoke(null, plugin, location, block)
                } catch (e: Exception) {
                    block.run()
                }
            }
        }
    }
    
    /**
     * Create a dispatcher for a specific entity's region.
     * Use this for entity-related operations.
     */
    @JvmStatic
    fun forEntity(plugin: Plugin, entity: Entity): CoroutineDispatcher {
        return object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                try {
                    val foliaSchedulerClass = Class.forName("org.leralix.tan.utils.FoliaScheduler")
                    val runTaskMethod = foliaSchedulerClass.getMethod(
                        "runEntityTask", 
                        Plugin::class.java, 
                        Entity::class.java, 
                        Runnable::class.java
                    )
                    runTaskMethod.invoke(null, plugin, entity, block)
                } catch (e: Exception) {
                    block.run()
                }
            }
        }
    }
}

/**
 * Extension to launch a coroutine on Folia's global scheduler.
 */
fun CoroutineScope.launchOnFolia(
    plugin: Plugin,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(FoliaDispatchers.global(plugin), start, block)

/**
 * Extension to launch a coroutine on a location's region thread.
 */
fun CoroutineScope.launchOnFolia(
    plugin: Plugin,
    location: Location,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(FoliaDispatchers.forLocation(plugin, location), start, block)

/**
 * Extension to launch a coroutine on an entity's region thread.
 */
fun CoroutineScope.launchOnFolia(
    plugin: Plugin,
    entity: Entity,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(FoliaDispatchers.forEntity(plugin, entity), start, block)

/**
 * Switch to Folia's global scheduler within a coroutine.
 */
suspend fun <T> withFoliaContext(plugin: Plugin, block: suspend CoroutineScope.() -> T): T {
    return withContext(FoliaDispatchers.global(plugin), block)
}

/**
 * Switch to a location's region thread within a coroutine.
 */
suspend fun <T> withFoliaContext(plugin: Plugin, location: Location, block: suspend CoroutineScope.() -> T): T {
    return withContext(FoliaDispatchers.forLocation(plugin, location), block)
}

/**
 * Switch to an entity's region thread within a coroutine.
 */
suspend fun <T> withFoliaContext(plugin: Plugin, entity: Entity, block: suspend CoroutineScope.() -> T): T {
    return withContext(FoliaDispatchers.forEntity(plugin, entity), block)
}
