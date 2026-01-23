package org.leralix.tan.integration.nexo

import org.bukkit.Material
import org.leralix.tan.TownsAndNations
import org.slf4j.LoggerFactory

/**
 * Configuration reader for Nexo integration.
 * 
 * This object provides config-based class paths and method names,
 * allowing server admins to fix compatibility issues without code changes.
 * 
 * Usage in config.yml:
 * ```yaml
 * nexo:
 *   enabled: true
 *   auto-detect: true
 *   fallback-material: "BARRIER"
 *   class-paths:
 *     items: "com.nexomc.nexo.api.NexoItems"
 *   method-names:
 *     item-from-id: "itemFromId"
 * ```
 */
object NexoConfig {
    private val logger = LoggerFactory.getLogger(NexoConfig::class.java)
    
    private val config get() = TownsAndNations.getPlugin().config
    
    /**
     * Check if Nexo integration is enabled in config.
     */
    @JvmStatic
    fun isEnabled(): Boolean {
        return config.getBoolean("nexo.enabled", true)
    }
    
    /**
     * Check if auto-detection is enabled (recommended).
     */
    @JvmStatic
    fun isAutoDetectEnabled(): Boolean {
        return config.getBoolean("nexo.auto-detect", true)
    }
    
    /**
     * Get class path for a Nexo API class.
     * 
     * @param type One of: "items", "blocks", "furniture"
     * @return Fully qualified class name
     */
    @JvmStatic
    fun getClassPath(type: String): String {
        val fromConfig = config.getString("nexo.class-paths.$type")
        if (fromConfig != null) {
            logger.debug("[NEXO Config] Using custom class path for '$type': $fromConfig")
            return fromConfig
        }
        
        // Fallback to defaults
        val default = when (type) {
            "items" -> "com.nexomc.nexo.api.NexoItems"
            "blocks" -> "com.nexomc.nexo.api.NexoBlocks"
            "furniture" -> "com.nexomc.nexo.api.NexoFurniture"
            else -> {
                logger.warn("[NEXO Config] Unknown class type: $type")
                return ""
            }
        }
        
        logger.debug("[NEXO Config] Using default class path for '$type': $default")
        return default
    }
    
    /**
     * Get method name for a Nexo API method.
     * 
     * @param method One of: "item-from-id", "item-exists", "id-from-item", etc.
     * @return Method name
     */
    @JvmStatic
    fun getMethodName(method: String): String {
        val fromConfig = config.getString("nexo.method-names.$method")
        if (fromConfig != null) {
            logger.debug("[NEXO Config] Using custom method name for '$method': $fromConfig")
            return fromConfig
        }
        
        // Fallback to defaults
        val default = when (method) {
            "item-from-id" -> "itemFromId"
            "item-exists" -> "exists"
            "id-from-item" -> "idFromItem"
            "place-block" -> "place"
            "remove-block" -> "remove"
            "is-custom-block" -> "isCustomBlock"
            "place-furniture" -> "place"
            "remove-furniture" -> "remove"
            "is-furniture" -> "isFurniture"
            else -> {
                logger.warn("[NEXO Config] Unknown method: $method")
                return ""
            }
        }
        
        logger.debug("[NEXO Config] Using default method name for '$method': $default")
        return default
    }
    
    /**
     * Get fallback material when Nexo is unavailable.
     */
    @JvmStatic
    fun getFallbackMaterial(): Material {
        val materialName = config.getString("nexo.fallback-material", "BARRIER")
        return try {
            Material.valueOf((materialName ?: "BARRIER").uppercase())
        } catch (e: IllegalArgumentException) {
            logger.warn("[NEXO Config] Invalid fallback material '$materialName', using BARRIER")
            Material.BARRIER
        }
    }
    
    /**
     * Get cache expiration time in milliseconds.
     */
    @JvmStatic
    fun getCacheExpireAfterWrite(): Long {
        return config.getLong("nexo.cache.expire-after-write", 600000) // 10 minutes default
    }
    
    /**
     * Get maximum cache size.
     */
    @JvmStatic
    fun getCacheMaximumSize(): Long {
        return config.getLong("nexo.cache.maximum-size", 1000)
    }
    
    /**
     * Check if config has custom values (not using defaults).
     */
    @JvmStatic
    fun hasCustomConfig(): Boolean {
        return config.contains("nexo.class-paths") || config.contains("nexo.method-names")
    }
    
    /**
     * Log current configuration.
     */
    @JvmStatic
    fun logConfig() {
        logger.info("[NEXO Config] Configuration:")
        logger.info("[NEXO Config]   Enabled: {}", isEnabled())
        logger.info("[NEXO Config]   Auto-detect: {}", isAutoDetectEnabled())
        logger.info("[NEXO Config]   Fallback: {}", getFallbackMaterial())
        logger.info("[NEXO Config]   Custom config: {}", hasCustomConfig())
        
        if (hasCustomConfig()) {
            logger.info("[NEXO Config]   Using custom class paths or method names from config.yml")
        } else {
            logger.info("[NEXO Config]   Using default class paths (auto-detect)")
        }
    }
}
