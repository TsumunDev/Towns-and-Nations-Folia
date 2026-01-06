package org.leralix.tan.coroutines
import kotlinx.coroutines.*
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import kotlin.coroutines.CoroutineContext
object FoliaDispatchers {
    @JvmStatic
    fun global(plugin: Plugin): CoroutineDispatcher {
        return object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                try {
                    val foliaSchedulerClass = Class.forName("org.leralix.tan.utils.FoliaScheduler")
                    val runTaskMethod = foliaSchedulerClass.getMethod("runTask", Plugin::class.java, Runnable::class.java)
                    runTaskMethod.invoke(null, plugin, block)
                } catch (e: Exception) {
                    block.run()
                }
            }
        }
    }
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
fun CoroutineScope.launchOnFolia(
    plugin: Plugin,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(FoliaDispatchers.global(plugin), start, block)
fun CoroutineScope.launchOnFolia(
    plugin: Plugin,
    location: Location,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(FoliaDispatchers.forLocation(plugin, location), start, block)
fun CoroutineScope.launchOnFolia(
    plugin: Plugin,
    entity: Entity,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(FoliaDispatchers.forEntity(plugin, entity), start, block)
suspend fun <T> withFoliaContext(plugin: Plugin, block: suspend CoroutineScope.() -> T): T {
    return withContext(FoliaDispatchers.global(plugin), block)
}
suspend fun <T> withFoliaContext(plugin: Plugin, location: Location, block: suspend CoroutineScope.() -> T): T {
    return withContext(FoliaDispatchers.forLocation(plugin, location), block)
}
suspend fun <T> withFoliaContext(plugin: Plugin, entity: Entity, block: suspend CoroutineScope.() -> T): T {
    return withContext(FoliaDispatchers.forEntity(plugin, entity), block)
}