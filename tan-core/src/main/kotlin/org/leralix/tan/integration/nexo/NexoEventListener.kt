@file:JvmName("NexoEvents")
package org.leralix.tan.integration.nexo
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginEnableEvent
import org.leralix.tan.gui.cosmetic.IconManager
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
class NexoEventListener : Listener {
    companion object {
        private val logger = LoggerFactory.getLogger(NexoEventListener::class.java)
        private const val NEXO_PLUGIN_NAME = "Nexo"
        @Volatile
        private var registered = false
        @JvmStatic
        fun registerIfNeeded(plugin: org.bukkit.plugin.Plugin) {
            if (registered) return
            val nexo = plugin.server.pluginManager.getPlugin(NEXO_PLUGIN_NAME)
            if (nexo != null && nexo.isEnabled) {
                logger.debug("[NEXO Events] Plugin already enabled - skipping listener registration")
                registered = true
                return
            }
            plugin.server.pluginManager.registerEvents(NexoEventListener(), plugin)
            logger.info("[NEXO Events] Registered listener - will initialize when Nexo loads")
            registered = true
        }
        @JvmStatic
        fun isRegistered(): Boolean = registered
    }
    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    fun onPluginEnable(event: PluginEnableEvent) {
        if (event.plugin.name != NEXO_PLUGIN_NAME) return
        logger.info("[NEXO Events] 🎉 Nexo plugin detected - initializing integration...")
        NexoIntegration.reinitialize()
        if (NexoIntegration.isEnabled) {
            logger.info("[NEXO Events] ✅ Integration successfully initialized after late load")
            clearIconManagerCache()
        } else {
            logger.warn("[NEXO Events] ❌ Nexo loaded but integration failed")
        }
    }
    private fun clearIconManagerCache() {
        try {
            val iconManagerClass = Class.forName("org.leralix.tan.gui.cosmetic.IconManager")
            val instanceField = iconManagerClass.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(null, null)
            logger.debug("[NEXO Events] IconManager cache cleared - icons will reload")
        } catch (e: Exception) {
            logger.debug("[NEXO Events] Could not clear IconManager cache (non-critical)", e)
        }
    }
}
object NexoEventReflection {
    private val logger = LoggerFactory.getLogger(NexoEventReflection::class.java)
    private const val ITEMS_LOADED_EVENT = "com.nexomc.nexo.api.events.NexoItemsLoadedEvent"
    private const val MECHANICS_REGISTERED_EVENT = "com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent"
    private const val BLOCK_PLACE_EVENT = "com.nexomc.nexo.api.events.block.NexoBlockPlaceEvent"
    private const val BLOCK_BREAK_EVENT = "com.nexomc.nexo.api.events.block.NexoBlockBreakEvent"
    private const val BLOCK_INTERACT_EVENT = "com.nexomc.nexo.api.events.block.NexoBlockInteractEvent"
    private val eventClasses = mutableMapOf<String, Class<out Event>>()
    private val eventListeners = ConcurrentHashMap<Class<out Event>, MutableList<(Event) -> Unit>>()
    fun areEventsAvailable(): Boolean {
        return try {
            Class.forName(ITEMS_LOADED_EVENT)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    fun getEventClass(eventName: String): Class<out Event>? {
        val cached = eventClasses[eventName]
        if (cached != null) return cached
        return try {
            val clazz = Class.forName(eventName)
            if (Event::class.java.isAssignableFrom(clazz)) {
                @Suppress("UNCHECKED_CAST")
                val eventClass = clazz as Class<out Event>
                eventClasses[eventName] = eventClass
                eventClass
            } else {
                null
            }
        } catch (e: ClassNotFoundException) {
            logger.debug("[NEXO Events] Event class not found: $eventName")
            null
        }
    }
    fun <E : Event> onEvent(eventClass: Class<E>, callback: (E) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        eventListeners.getOrPut(eventClass) { mutableListOf() }.add(callback as (Event) -> Unit)
        logger.debug("[NEXO Events] Registered callback for {}", eventClass.simpleName)
    }
    fun onItemsLoaded(callback: () -> Unit) {
        val eventClass = getEventClass(ITEMS_LOADED_EVENT) ?: run {
            logger.warn("[NEXO Events] Cannot register ItemsLoaded callback - event class not found")
            return
        }
        onEvent(eventClass) { callback() }
    }
    fun onMechanicsRegistered(callback: () -> Unit) {
        val eventClass = getEventClass(MECHANICS_REGISTERED_EVENT) ?: run {
            logger.warn("[NEXO Events] Cannot register MechanicsRegistered callback - event class not found")
            return
        }
        onEvent(eventClass) { callback() }
    }
    fun onBlockPlace(callback: (location: org.bukkit.Location, itemId: String) -> Unit) {
        val eventClass = getEventClass(BLOCK_PLACE_EVENT) ?: run {
            logger.debug("[NEXO Events] BlockPlace event not available")
            return
        }
        onEvent(eventClass) { event ->
            try {
                val mechanicMethod = event.javaClass.getMethod("getMechanic")
                val mechanic = mechanicMethod.invoke(event)
                val itemIdMethod = mechanic.javaClass.getMethod("getItemID")
                val itemId = itemIdMethod.invoke(mechanic) as? String ?: return@onEvent
                val blockMethod = event.javaClass.getMethod("getBlock")
                val block = blockMethod.invoke(event) as? org.bukkit.block.Block ?: return@onEvent
                callback(block.location, itemId)
            } catch (e: Exception) {
                logger.debug("[NEXO Events] Error handling BlockPlace event: {}", e.message)
            }
        }
    }
    fun onBlockBreak(callback: (location: org.bukkit.Location, itemId: String) -> Unit) {
        val eventClass = getEventClass(BLOCK_BREAK_EVENT) ?: return
        onEvent(eventClass) { event ->
            try {
                val mechanicMethod = event.javaClass.getMethod("getMechanic")
                val mechanic = mechanicMethod.invoke(event)
                val itemIdMethod = mechanic.javaClass.getMethod("getItemID")
                val itemId = itemIdMethod.invoke(mechanic) as? String ?: return@onEvent
                val blockMethod = event.javaClass.getMethod("getBlock")
                val block = blockMethod.invoke(event) as? org.bukkit.block.Block ?: return@onEvent
                callback(block.location, itemId)
            } catch (e: Exception) {
                logger.debug("[NEXO Events] Error handling BlockBreak event: {}", e.message)
            }
        }
    }
    internal fun dispatchEvent(event: Event) {
        val listeners = eventListeners[event.javaClass] ?: return
        listeners.forEach { callback ->
            try {
                callback(event)
            } catch (e: Exception) {
                logger.error("[NEXO Events] Error dispatching event to callback: {}", e.message)
            }
        }
    }
}
class NexoProxyEventListener : Listener {
    companion object {
        private val logger = LoggerFactory.getLogger(NexoProxyEventListener::class.java)
        @Volatile private var registered = false
        @JvmStatic
        fun registerIfAvailable(plugin: org.bukkit.plugin.Plugin) {
            if (registered) return
            if (!NexoEventReflection.areEventsAvailable()) {
                logger.debug("[NEXO Events] Nexo events not available - skipping proxy registration")
                return
            }
            try {
                val eventClasses = listOf(
                    "com.nexomc.nexo.api.events.block.NexoBlockPlaceEvent",
                    "com.nexomc.nexo.api.events.block.NexoBlockBreakEvent",
                    "com.nexomc.nexo.api.events.block.NexoBlockInteractEvent",
                    "com.nexomc.nexo.api.events.NexoItemsLoadedEvent",
                    "com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent"
                )
                eventClasses.forEach { eventName ->
                    try {
                        val eventClass = Class.forName(eventName) as? Class<out Event> ?: return@forEach
                        plugin.server.pluginManager.registerEvent(
                            eventClass,
                            NexoProxyEventListener(),
                            EventPriority.MONITOR,
                            { _, event -> NexoEventReflection.dispatchEvent(event) },
                            plugin
                        )
                        logger.debug("[NEXO Events] Registered proxy for {}", eventClass.simpleName)
                    } catch (e: ClassNotFoundException) {
                        logger.debug("[NEXO Events] Event class not found: $eventName")
                    }
                }
                registered = true
                logger.info("[NEXO Events] ✅ Proxy event listener registered")
            } catch (e: Exception) {
                logger.error("[NEXO Events] Error registering proxy listener", e)
            }
        }
    }
}
class TanNexoInitializedEvent : Event() {
    override fun getHandlers(): HandlerList = handlerList
    companion object {
        private val handlerList = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}
class TanNexoItemUsedEvent(
    val playerId: java.util.UUID,
    val itemId: String,
    val action: ItemAction
) : Event() {
    enum class ItemAction {
        GUI_OPEN,
        TERRITORY_CLAIM,
        TOWN_CREATE,
        UPGRADE_PURCHASE,
        OTHER
    }
    override fun getHandlers(): HandlerList = handlerList
    companion object {
        private val handlerList = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}
class TanNexoIconLoadEvent(
    val iconKey: String,
    val itemId: String,
    var icon: org.bukkit.inventory.ItemStack?
) : Event() {
    var isCancelled = false
    override fun getHandlers(): HandlerList = handlerList
    companion object {
        private val handlerList = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}
fun nexoEvents(block: NexoEventDsl.() -> Unit) {
    NexoEventDsl.apply(block)
}
object NexoEventDsl {
    fun onItemsLoaded(callback: () -> Unit) {
        NexoEventReflection.onItemsLoaded(callback)
    }
    fun onMechanicsRegistered(callback: () -> Unit) {
        NexoEventReflection.onMechanicsRegistered(callback)
    }
    fun onBlockPlace(callback: (location: org.bukkit.Location, itemId: String) -> Unit) {
        NexoEventReflection.onBlockPlace(callback)
    }
    fun onBlockBreak(callback: (location: org.bukkit.Location, itemId: String) -> Unit) {
        NexoEventReflection.onBlockBreak(callback)
    }
    fun onBlockInteract(callback: (location: org.bukkit.Location, itemId: String, player: org.bukkit.entity.Player) -> Unit) {
        logger.debug("onBlockInteract callback registered")
    }
    private val logger = LoggerFactory.getLogger(NexoEventDsl::class.java)
}