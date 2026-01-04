package org.leralix.tan.commands

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.leralix.tan.dataclass.ITanPlayer
import org.leralix.tan.dataclass.territory.TownData
import org.leralix.tan.lang.Lang
import org.leralix.tan.service.PlayerDataService
import org.leralix.tan.service.TerritoryService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Command validation utilities for Kotlin commands.
 */
object CommandValidation {
    
    /**
     * Require that args has at least the specified count.
     */
    fun requireArgs(args: Array<String>, count: Int, syntax: String) {
        if (args.size < count) {
            throw CommandException("§cUsage: $syntax")
        }
    }
    
    /**
     * Parse a double, throwing CommandException on failure.
     */
    fun parseDouble(value: String, name: String = "value"): Double {
        return value.toDoubleOrNull()
            ?: throw CommandException("§c'$value' is not a valid number for $name")
    }
    
    /**
     * Parse an int, throwing CommandException on failure.
     */
    fun parseInt(value: String, name: String = "value"): Int {
        return value.toIntOrNull()
            ?: throw CommandException("§c'$value' is not a valid integer for $name")
    }
    
    /**
     * Parse a positive double.
     */
    fun parsePositiveDouble(value: String, name: String = "amount"): Double {
        val parsed = parseDouble(value, name)
        if (parsed <= 0) {
            throw CommandException("§c$name must be positive")
        }
        return parsed
    }
    
    /**
     * Parse a non-negative double.
     */
    fun parseNonNegativeDouble(value: String, name: String = "amount"): Double {
        val parsed = parseDouble(value, name)
        if (parsed < 0) {
            throw CommandException("§c$name cannot be negative")
        }
        return parsed
    }
    
    /**
     * Find an online player by name.
     */
    fun findOnlinePlayer(name: String): Player {
        return Bukkit.getPlayer(name)
            ?: throw CommandException("§cPlayer '$name' is not online")
    }
    
    /**
     * Find an online player, or null if not found.
     */
    fun findOnlinePlayerOrNull(name: String): Player? = Bukkit.getPlayer(name)
    
    /**
     * Find an offline player by name.
     */
    @Suppress("DEPRECATION")
    fun findOfflinePlayer(name: String): OfflinePlayer = Bukkit.getOfflinePlayer(name)
    
    /**
     * Get TanPlayer for an offline player.
     */
    suspend fun getTanPlayer(offlinePlayer: OfflinePlayer): ITanPlayer {
        return PlayerDataService.getPlayer(offlinePlayer.uniqueId)
            ?: throw CommandException("§cPlayer has never joined the server")
    }
    
    /**
     * Get TanPlayer by name.
     */
    suspend fun getTanPlayerByName(name: String): ITanPlayer {
        val offline = findOfflinePlayer(name)
        return getTanPlayer(offline)
    }
    
    /**
     * Get town by ID.
     */
    suspend fun getTown(townId: String): TownData {
        return TerritoryService.getTown(townId)
            ?: throw CommandException("§cTown not found")
    }
    
    /**
     * Get town by name (searches all towns).
     */
    suspend fun getTownByName(name: String): TownData {
        return TerritoryService.getAllTowns()
            .find { it.name.equals(name, ignoreCase = true) }
            ?: throw CommandException("§cTown '$name' not found")
    }
    
    /**
     * Require sender to have permission.
     */
    fun requirePermission(sender: CommandSender, permission: String) {
        if (!sender.hasPermission(permission)) {
            throw CommandException("§cYou don't have permission to do that")
        }
    }
    
    /**
     * Require sender to be a player.
     */
    fun requirePlayer(sender: CommandSender): Player {
        return sender as? Player
            ?: throw CommandException("§cThis command can only be used by players")
    }
}

/**
 * Command cooldown manager.
 * Prevents command spam with per-player cooldowns.
 */
object CommandCooldowns {
    
    private data class CooldownKey(val playerId: UUID, val commandName: String)
    private val cooldowns = ConcurrentHashMap<CooldownKey, Long>()
    
    /**
     * Check if player has an active cooldown for a command.
     * Throws CommandException if on cooldown.
     */
    fun checkCooldown(player: Player, commandName: String) {
        val key = CooldownKey(player.uniqueId, commandName)
        val expiry = cooldowns[key] ?: return
        
        val remaining = expiry - System.currentTimeMillis()
        if (remaining > 0) {
            val seconds = (remaining / 1000) + 1
            throw CommandException("§cPlease wait ${seconds}s before using this command again")
        }
        
        // Cooldown expired, remove it
        cooldowns.remove(key)
    }
    
    /**
     * Set a cooldown for a player on a command.
     */
    fun setCooldown(player: Player, commandName: String, seconds: Int) {
        val key = CooldownKey(player.uniqueId, commandName)
        cooldowns[key] = System.currentTimeMillis() + (seconds * 1000L)
    }
    
    /**
     * Clear a player's cooldown for a command.
     */
    fun clearCooldown(player: Player, commandName: String) {
        val key = CooldownKey(player.uniqueId, commandName)
        cooldowns.remove(key)
    }
    
    /**
     * Clear all cooldowns for a player.
     */
    fun clearAllCooldowns(player: Player) {
        cooldowns.keys.removeIf { it.playerId == player.uniqueId }
    }
    
    /**
     * Get remaining cooldown time in seconds, or 0 if no cooldown.
     */
    fun getRemainingCooldown(player: Player, commandName: String): Int {
        val key = CooldownKey(player.uniqueId, commandName)
        val expiry = cooldowns[key] ?: return 0
        
        val remaining = expiry - System.currentTimeMillis()
        return if (remaining > 0) ((remaining / 1000) + 1).toInt() else 0
    }
    
    /**
     * Clean up expired cooldowns (call periodically).
     */
    fun cleanup() {
        val now = System.currentTimeMillis()
        cooldowns.entries.removeIf { it.value < now }
    }
}

/**
 * Extension to check cooldown inline.
 */
fun Player.checkCooldown(commandName: String) = CommandCooldowns.checkCooldown(this, commandName)

/**
 * Extension to set cooldown inline.
 */
fun Player.setCooldown(commandName: String, seconds: Int) = CommandCooldowns.setCooldown(this, commandName, seconds)
