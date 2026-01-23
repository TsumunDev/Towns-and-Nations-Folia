package org.leralix.tan.integration.nexo
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.leralix.tan.cache.SimpleCache
import org.leralix.tan.TownsAndNations
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
object NexoIntegration {
    private val logger = LoggerFactory.getLogger(NexoIntegration::class.java)
    private const val NEXO_PLUGIN_NAME = "Nexo"
    private const val NEXO_ITEMS_CLASS = "com.nexomc.nexo.api.NexoItems"
    private const val NEXO_BLOCKS_CLASS = "com.nexomc.nexo.api.NexoBlocks"
    private const val NEXO_FURNITURE_CLASS = "com.nexomc.nexo.api.NexoFurniture"
    private const val NEXO_MECHANICS_REGISTERED_EVENT = "com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent"
    private const val NEXO_ITEMS_LOADED_EVENT = "com.nexomc.nexo.api.events.NexoItemsLoadedEvent"
    private val PAPER_12111_CLASSES = listOf(
        "io.papermc.paper.datacomponent.item.AttackRange",
        "io.papermc.paper.datacomponent.item.ItemArmorTrim",
        "io.papermc.paper.datacomponent.item.ItemEnchantments"
    )
    private var paperVersion: String? = null
    private var _isModernPaper = false
    private var requiredPaperVersion: String? = null
    private var missingModernClasses = listOf<String>()
    @Volatile
    private var _initialized = false
    @Volatile
    private var _enabled = false
    @Volatile
    private var _nexoVersion: String? = null
    @Volatile
    private var _versionIncompatibilityDetected = false
    private var _versionErrorLogged = false
    private val initLock = Any()
    val isInitialized: Boolean get() = _initialized
    val isEnabled: Boolean get() = _initialized && _enabled
    val nexoVersion: String? get() = _nexoVersion
    val isVersionIncompatible: Boolean get() = _versionIncompatibilityDetected
    private var nexoItemsClass: Class<*>? = null
    private var nexoBlocksClass: Class<*>? = null
    private var nexoFurnitureClass: Class<*>? = null
    private var itemFromIdMethod: Method? = null
    private var itemExistsMethod: Method? = null
    private var idFromItemMethod: Method? = null
    private var placeBlockMethod: Method? = null
    private var isCustomBlockMethod: Method? = null
    private var removeBlockMethod: Method? = null
    private var placeFurnitureMethod: Method? = null
    private var isFurnitureMethod: Method? = null
    private var removeFurnitureMethod: Method? = null
    private val itemCache: SimpleCache<String, ItemStack> = SimpleCache(
        name = "NexoItems",
        cache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats()
            .build()
    )
    private val notFoundCache = mutableSetOf<String>()
    @JvmStatic
    fun initialize(): Boolean {
        if (_initialized) return _enabled
        synchronized(initLock) {
            if (_initialized) return _enabled
            logger.info("[NEXO] 🔍 Initializing Nexo integration...")
            detectPaperVersion()
            val nexoPlugin = Bukkit.getPluginManager().getPlugin(NEXO_PLUGIN_NAME)
            if (nexoPlugin == null) {
                logger.warn("[NEXO] Plugin not installed - nexo: icons will use fallback materials")
                _initialized = true
                _enabled = false
                return false
            }
            if (!nexoPlugin.isEnabled) {
                logger.warn("[NEXO] Plugin installed but not enabled - will retry on load")
                _initialized = true
                _enabled = false
                return false
            }
            _nexoVersion = nexoPlugin.description.version
            logger.info("[NEXO] Plugin v{} detected - loading API...", _nexoVersion)
            val nexoVersionNumber = parseVersion(_nexoVersion)
            val requiresModernPaper = isNexoVersion17OrHigher(nexoVersionNumber)
            if (requiresModernPaper && missingModernClasses.isNotEmpty()) {
                val missing = missingModernClasses.joinToString(", ")
                logger.warn("┌─────────────────────────────────────────────────────────────┐")
                logger.warn("│ ⚠️  NEXO INTEGRATION DISABLED - VERSION INCOMPATIBLE          │")
                logger.warn("│                                                             │")
                logger.warn("│ Nexo {} requires Paper 1.21.11+ but your server              │", _nexoVersion)
                logger.warn("│ is running an older version.                                │")
                logger.warn("│                                                             │")
                logger.warn("│ Missing: {}                                                 │", missing)
                logger.warn("│                                                             │")
                logger.warn("│ TO FIX:                                                     │")
                logger.warn("│ 1. Update server to Paper 1.21.11+ (recommended)            │")
                logger.warn("│ 2. OR downgrade Nexo to 1.16.x (works on older Paper)       │")
                logger.warn("│    https://github.com/NexoMC/Nexo/releases/tag/v1.16.1      │")
                logger.warn("│                                                             │")
                logger.warn("│ nexo: icons will use fallback materials.                   │")
                logger.warn("└─────────────────────────────────────────────────────────────┘")
                _versionIncompatibilityDetected = true
                _versionErrorLogged = true
                _initialized = true
                _enabled = false
                return false
            }
            if (requiresModernPaper) {
                logger.info("[NEXO] Nexo {} detected - requires Paper 1.21.11+ ✓", _nexoVersion)
            } else {
                logger.info("[NEXO] Nexo {} detected - compatible with current Paper version", _nexoVersion)
            }
            return try {
                loadNexoClasses()
                loadNexoMethods()
                _enabled = true
                _initialized = true
                logger.info("[NEXO] ✅ Integration enabled - nexo: prefix available!")
                true
            } catch (e: ClassNotFoundException) {
                logger.error("[NEXO] ❌ API class not found - version may be incompatible: {}", e.message)
                _enabled = false
                _initialized = true
                false
            } catch (e: NoSuchMethodException) {
                logger.error("[NEXO] ❌ Required method not found - API may have changed: {}", e.message)
                _enabled = false
                _initialized = true
                false
            } catch (e: Exception) {
                logger.error("[NEXO] ❌ Unexpected initialization error", e)
                _enabled = false
                _initialized = true
                false
            }
        }
    }
    @JvmStatic
    fun reinitialize() {
        synchronized(initLock) {
            logger.info("[NEXO] 🔄 Re-initializing after late plugin load...")
            _initialized = false
            _enabled = false
            notFoundCache.clear()
            itemCache.invalidateAll()
            initialize()
        }
    }
    private fun detectPaperVersion() {
        val startTime = System.currentTimeMillis()
        val serverVersion = Bukkit.getVersion()
        val bukkitVersion = Bukkit.getBukkitVersion()
        paperVersion = bukkitVersion
        val versionNumber = try {
            val versionMatch = Regex("(\\d+)\\.(\\d+)\\.?(\\d+)?").find(bukkitVersion)
            if (versionMatch != null) {
                val major = versionMatch.groupValues[1].toInt()
                val minor = versionMatch.groupValues[2].toInt()
                val patch = if (versionMatch.groupValues[3].isNotEmpty()) versionMatch.groupValues[3].toInt() else 0
                Triple(major, minor, patch)
            } else {
                Triple(1, 20, 0)
            }
        } catch (e: Exception) {
            logger.debug("[NEXO] Could not parse version from '$bukkitVersion': {}", e.message)
            Triple(1, 20, 0)
        }
        val (major, minor, patch) = versionNumber
        _isModernPaper = when {
            major > 1 -> true
            major == 1 && minor > 21 -> true
            major == 1 && minor == 21 && patch >= 11 -> true
            else -> false
        }
        val availableClasses = mutableListOf<String>()
        val missingClasses = mutableListOf<String>()
        for (className in PAPER_12111_CLASSES) {
            try {
                Class.forName(className)
                availableClasses.add(className.substringAfterLast('.'))
            } catch (e: ClassNotFoundException) {
                missingClasses.add(className.substringAfterLast('.'))
            }
        }
        missingModernClasses = missingClasses
        if (_isModernPaper && missingClasses.isNotEmpty()) {
            logger.debug("[NEXO] Paper $bukkitVersion detected but some data component classes are missing")
        } else if (!_isModernPaper && availableClasses.isNotEmpty()) {
            logger.debug("[NEXO] Paper $bukkitVersion may have backported data component classes")
        }
        val elapsed = System.currentTimeMillis() - startTime
        logger.info("[NEXO] 📋 Version Detection:")
        logger.info("[NEXO]    Server: {}", serverVersion.take(50))
        logger.info("[NEXO]    Bukkit API: {}", bukkitVersion)
        logger.info("[NEXO]    Modern Paper (1.21.11+): {}", _isModernPaper)
        logger.info("[NEXO]    Data component classes: {}/{}", availableClasses.size, PAPER_12111_CLASSES.size)
        if (missingClasses.isNotEmpty()) {
            logger.debug("[NEXO]    Missing classes: {}", missingClasses.joinToString(", "))
        }
        logger.debug("[NEXO] Version detection completed in ${elapsed}ms")
    }
    @JvmStatic
    fun hasModernPaperClass(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    @JvmStatic
    fun getPaperVersion(): String = paperVersion ?: "Unknown"
    private fun parseVersion(versionString: String?): Triple<Int, Int, Int> {
        if (versionString == null) return Triple(1, 0, 0)
        val cleanVersion = versionString.removePrefix("v").trim()
        val versionMatch = Regex("(\\d+)\\.(\\d+)\\.?(\\d+)?").find(cleanVersion)
        if (versionMatch != null) {
            val major = versionMatch.groupValues[1].toInt()
            val minor = versionMatch.groupValues[2].toInt()
            val patch = if (versionMatch.groupValues[3].isNotEmpty()) versionMatch.groupValues[3].toInt() else 0
            return Triple(major, minor, patch)
        }
        return Triple(1, 0, 0)
    }
    private fun isNexoVersion17OrHigher(version: Triple<Int, Int, Int>): Boolean {
        val (major, minor, patch) = version
        return major > 1 || (major == 1 && minor >= 17)
    }
    @JvmStatic
    fun isModernPaper(): Boolean = _isModernPaper && missingModernClasses.isEmpty()
    private fun loadNexoClasses() {
        nexoItemsClass = try {
            Class.forName(NEXO_ITEMS_CLASS)
        } catch (e: ClassNotFoundException) {
            Class.forName("com.nexomc.nexo.NexoItems")
        }
        nexoBlocksClass = try {
            Class.forName(NEXO_BLOCKS_CLASS)
        } catch (e: ClassNotFoundException) {
            Class.forName("com.nexomc.nexo.NexoBlocks")
        }
        nexoFurnitureClass = try {
            Class.forName(NEXO_FURNITURE_CLASS)
        } catch (e: ClassNotFoundException) {
            Class.forName("com.nexomc.nexo.NexoFurniture")
        }
        logger.debug("[NEXO] Loaded classes: Items={}, Blocks={}, Furniture={}",
            nexoItemsClass?.simpleName, nexoBlocksClass?.simpleName, nexoFurnitureClass?.simpleName)
    }
    private fun loadNexoMethods() {
        val itemsClass = nexoItemsClass ?: throw ClassNotFoundException("NexoItems class not loaded")
        itemFromIdMethod = itemsClass.getDeclaredMethod("itemFromId", String::class.java)
        itemExistsMethod = itemsClass.getDeclaredMethod("exists", String::class.java)
        idFromItemMethod = itemsClass.getDeclaredMethod("idFromItem", ItemStack::class.java)
        nexoBlocksClass?.let { blocksClass ->
            try {
                isCustomBlockMethod = blocksClass.getDeclaredMethod("isCustomBlock", org.bukkit.Location::class.java)
                placeBlockMethod = blocksClass.getDeclaredMethod("place", String::class.java, org.bukkit.Location::class.java)
                removeBlockMethod = blocksClass.getDeclaredMethod("remove", org.bukkit.Location::class.java)
            } catch (e: NoSuchMethodException) {
                logger.debug("[NEXO] Block methods not available - NexoBlocks not fully supported")
            }
        }
        nexoFurnitureClass?.let { furnitureClass ->
            try {
                isFurnitureMethod = furnitureClass.getDeclaredMethod("isFurniture", org.bukkit.Location::class.java)
                placeFurnitureMethod = furnitureClass.getDeclaredMethod("place", String::class.java, org.bukkit.Location::class.java, org.bukkit.entity.Player::class.java)
                removeFurnitureMethod = furnitureClass.getDeclaredMethod("remove", org.bukkit.Location::class.java)
            } catch (e: NoSuchMethodException) {
                logger.debug("[NEXO] Furniture methods not available - NexoFurniture not fully supported")
            }
        }
        logger.debug("[NEXO] Methods loaded: itemFromId={}, exists={}, idFromItem={}",
            itemFromIdMethod != null, itemExistsMethod != null, idFromItemMethod != null)
    }
    @JvmStatic
    fun getItem(itemId: String): ItemStack? {
        if (!isEnabled) {
            logger.warn("[NEXO] getItem('{}') called but integration is disabled!", itemId)
            return null
        }
        if (itemId in notFoundCache) {
            logger.info("[NEXO] Item '{}' in NOT_FOUND_CACHE - returning null", itemId)
            return null
        }
        itemCache.get(itemId)?.let {
            logger.info("[NEXO] Item '{}' found in ITEM_CACHE - returning cached item", itemId)
            return it
        }
        logger.info("[NEXO] Item '{}' not in cache - fetching from Nexo API...", itemId)
        return fetchItemFromNexo(itemId)
    }
    @JvmStatic
    fun getItemOrDefault(itemId: String, fallback: ItemStack): ItemStack {
        return getItem(itemId) ?: fallback
    }
    @JvmStatic
    fun getItemOrDefault(itemId: String, fallbackMaterial: Material): ItemStack {
        return getItem(itemId) ?: ItemStack(fallbackMaterial)
    }
    @JvmStatic
    fun itemExists(itemId: String): Boolean {
        if (!isEnabled || itemExistsMethod == null) return false
        if (itemId in notFoundCache) return false
        if (itemCache.get(itemId) != null) return true
        return try {
            val result = itemExistsMethod!!.invoke(null, itemId) as? Boolean ?: false
            if (!result) notFoundCache.add(itemId)
            result
        } catch (e: Exception) {
            logger.debug("[NEXO] Error checking item existence for '{}': {}", itemId, e.message)
            false
        }
    }
    @JvmStatic
    fun getIdFromItem(itemStack: ItemStack): String? {
        if (!isEnabled || idFromItemMethod == null) return null
        return try {
            idFromItemMethod!!.invoke(null, itemStack) as? String
        } catch (e: Exception) {
            logger.trace("[NEXO] Error getting ID from item: {}", e.message)
            null
        }
    }
    private fun fetchItemFromNexo(itemId: String): ItemStack? {
        if (itemFromIdMethod == null) return null
        return try {
            logger.info("[NEXO] Fetching item '{}' from Nexo API...", itemId)
            val itemBuilder = itemFromIdMethod!!.invoke(null, itemId)
                ?: run {
                    logger.warn("[NEXO] ❌ Item '{}' NOT FOUND in Nexo (itemFromId returned null)", itemId)
                    notFoundCache.add(itemId)
                    return null
                }
            logger.info("[NEXO] Got ItemBuilder for '{}', builder type: {}", itemId, itemBuilder.javaClass.simpleName)
            logger.info("[NEXO] About to try method 'get()'...")
            var item: ItemStack? = null
            item = tryGetItem(itemBuilder, "get")
            if (item == null) {
                item = tryGetItem(itemBuilder, "getItem")
            }
            if (item == null) {
                item = tryGetItem(itemBuilder, "asItem")
            }
            if (item == null) {
                item = tryGetItem(itemBuilder, "build")
            }
            if (item == null) {
                item = tryGetItem(itemBuilder, "toBukkitItem")
            }
            item ?: run {
                logger.warn("[NEXO] ❌ Item '{}' COULD NOT BE BUILT - all methods failed (get, getItem, asItem, build, toBukkitItem)", itemId)
                notFoundCache.add(itemId)
                return null
            }
            if (item.type.isAir) {
                logger.warn("[NEXO] ❌ Item '{}' is AIR - treating as not found", itemId)
                notFoundCache.add(itemId)
                return null
            }
            val meta = item.itemMeta
            val hasCmd = meta?.hasCustomModelData() == true
            val cmd = if (hasCmd) meta?.customModelData else null
            logger.info("[NEXO] ✅ Loaded item '{}' as {} | CustomModelData: {} (has: {})", itemId, item.type, cmd, hasCmd)
            itemCache.put(itemId, item)
            item
        } catch (e: java.lang.NoClassDefFoundError) {
            logger.warn("[NEXO] Item '{}' skipped - NoClassDefFoundError: {}", itemId, e.message)
            notFoundCache.add(itemId)
            null
        } catch (e: Exception) {
            logger.warn("[NEXO] ❌ EXCEPTION fetching item '{}': {} - {}", itemId, e.javaClass.simpleName, e.message)
            e.printStackTrace()
            notFoundCache.add(itemId)
            null
        }
    }
    private fun tryGetItem(builder: Any, methodName: String): ItemStack? {
        return try {
            val method = builder.javaClass.getMethod(methodName)
            val result = method.invoke(builder)
            logger.info("[NEXO] Method '$methodName' returned: {} (type: {})",
                result?.javaClass?.simpleName ?: "null", result?.javaClass?.name ?: "N/A")
            val itemStack = result as? ItemStack
            if (itemStack != null) {
                logger.info("[NEXO] ✓ Method '$methodName' succeeded - got ItemStack with type {}", itemStack.type)
            } else {
                logger.warn("[NEXO] Method '$methodName' returned non-ItemStack: {}", result?.javaClass?.simpleName)
            }
            itemStack
        } catch (e: NoSuchMethodException) {
            logger.info("[NEXO] Method '$methodName' not found on builder")
            null
        } catch (e: Exception) {
            logger.warn("[NEXO] Method '$methodName' failed: {} - {}", e.javaClass.simpleName, e.message)
            null
        }
    }
    @JvmStatic
    fun placeBlock(itemId: String, location: org.bukkit.Location): Boolean {
        if (!isEnabled || placeBlockMethod == null) return false
        return try {
            placeBlockMethod!!.invoke(null, itemId, location) as? Boolean ?: false
        } catch (e: Exception) {
            logger.debug("[NEXO] Error placing block '{}': {}", itemId, e.message)
            false
        }
    }
    @JvmStatic
    fun isCustomBlock(location: org.bukkit.Location): Boolean {
        if (!isEnabled || isCustomBlockMethod == null) return false
        return try {
            isCustomBlockMethod!!.invoke(null, location) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
    @JvmStatic
    fun removeBlock(location: org.bukkit.Location): Boolean {
        if (!isEnabled || removeBlockMethod == null) return false
        return try {
            removeBlockMethod!!.invoke(null, location) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
    @JvmStatic
    fun placeFurniture(itemId: String, location: org.bukkit.Location, player: org.bukkit.entity.Player?): Boolean {
        if (!isEnabled || placeFurnitureMethod == null) return false
        return try {
            placeFurnitureMethod!!.invoke(null, itemId, location, player) as? Boolean ?: false
        } catch (e: Exception) {
            logger.debug("[NEXO] Error placing furniture '{}': {}", itemId, e.message)
            false
        }
    }
    @JvmStatic
    fun isFurniture(location: org.bukkit.Location): Boolean {
        if (!isEnabled || isFurnitureMethod == null) return false
        return try {
            isFurnitureMethod!!.invoke(null, location) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
    @JvmStatic
    fun removeFurniture(location: org.bukkit.Location): Boolean {
        if (!isEnabled || removeFurnitureMethod == null) return false
        return try {
            removeFurnitureMethod!!.invoke(null, location) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
    @JvmStatic
    fun clearCache() {
        itemCache.invalidateAll()
        notFoundCache.clear()
        logger.info("[NEXO] Cache cleared")
    }
    @JvmStatic
    fun getCacheStats(): String {
        val cacheSize = itemCache.size()
        val notFoundSize = notFoundCache.size
        val status = when {
            _versionIncompatibilityDetected -> "INCOMPATIBLE"
            !_enabled -> "DISABLED"
            else -> "ENABLED"
        }
        return "NexoIntegration[cached=$cacheSize, notFound=$notFoundSize, status=$status, version=$_nexoVersion]"
    }
    @JvmStatic
    fun isPluginPresent(): Boolean {
        val plugin = Bukkit.getPluginManager().getPlugin(NEXO_PLUGIN_NAME)
        return plugin != null && plugin.isEnabled
    }
    @JvmStatic
    fun getItemAsync(itemId: String): CompletableFuture<ItemStack?> {
        return CompletableFuture.supplyAsync {
            getItem(itemId)
        }
    }
}
suspend fun getItemSuspending(itemId: String): ItemStack? = withContext(Dispatchers.IO) {
    NexoIntegration.getItem(itemId)
}