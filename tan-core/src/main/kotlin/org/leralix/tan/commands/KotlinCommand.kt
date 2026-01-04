package org.leralix.tan.commands

import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.leralix.lib.commands.SubCommand
import org.leralix.tan.TownsAndNations
import org.leralix.tan.coroutines.FoliaDispatchers
import org.leralix.tan.coroutines.TanCoroutines
import org.leralix.tan.dataclass.ITanPlayer
import org.leralix.tan.dataclass.territory.TownData
import org.leralix.tan.lang.Lang
import org.leralix.tan.service.PlayerDataService
import org.leralix.tan.service.TerritoryService
import org.leralix.tan.utils.text.TanChatUtils
import org.slf4j.LoggerFactory

/**
 * Base class for Kotlin async commands.
 * Provides coroutine-based command execution with automatic error handling.
 * 
 * Usage:
 * ```kotlin
 * class MyCommand : KotlinCommand(
 *     name = "mycommand",
 *     description = "Does something",
 *     arguments = 1,
 *     syntax = "/tan mycommand <arg>"
 * ) {
 *     override suspend fun executeAsync(sender: CommandSender, args: Array<String>) {
 *         val player = sender.asPlayer() ?: return
 *         val tanPlayer = player.tanPlayer() ?: return
 *         
 *         // Async operations here
 *         val town = TerritoryService.getTown(tanPlayer.townID)
 *         player.sendMessage("Your town: ${town?.name}")
 *     }
 * }
 * ```
 */
abstract class KotlinCommand(
    private val commandName: String,
    private val commandDescription: String,
    private val argumentCount: Int,
    private val commandSyntax: String
) : SubCommand() {
    
    companion object {
        private val logger = LoggerFactory.getLogger(KotlinCommand::class.java)
    }
    
    override fun getName(): String = commandName
    override fun getDescription(): String = commandDescription
    override fun getArguments(): Int = argumentCount
    override fun getSyntax(): String = commandSyntax
    
    /**
     * Override this to provide tab completions.
     * Default returns player suggestions.
     */
    override fun getTabCompleteSuggestions(
        player: CommandSender,
        currentMessage: String,
        args: Array<String>
    ): List<String> = emptyList()
    
    /**
     * Entry point from Java - launches coroutine for async execution.
     */
    final override fun perform(commandSender: CommandSender, args: Array<String>) {
        TanCoroutines.launch {
            try {
                executeAsync(commandSender, args)
            } catch (e: CommandException) {
                // Expected exceptions - just show message to user
                commandSender.sendMessage(e.message)
            } catch (e: Exception) {
                logger.error("Error executing command $commandName", e)
                commandSender.sendMessage("§cAn error occurred: ${e.message}")
            }
        }
    }
    
    /**
     * Implement this for async command logic.
     * Can use suspend functions freely.
     */
    abstract suspend fun executeAsync(sender: CommandSender, args: Array<String>)
    
    // ============ Helper Extensions ============
    
    /**
     * Get sender as Player, or send error message and return null.
     */
    protected fun CommandSender.asPlayer(): Player? {
        return (this as? Player) ?: run {
            sendMessage("§cThis command can only be used by players.")
            null
        }
    }
    
    /**
     * Get ITanPlayer for this player.
     */
    protected suspend fun Player.tanPlayer(): ITanPlayer? {
        return PlayerDataService.getPlayer(this) ?: run {
            sendMessage("§cPlayer data not found.")
            null
        }
    }
    
    /**
     * Get ITanPlayer for a sender (must be Player).
     */
    protected suspend fun CommandSender.tanPlayer(): ITanPlayer? {
        return asPlayer()?.tanPlayer()
    }
    
    /**
     * Validate argument count, throwing exception if invalid.
     */
    protected fun requireArgs(args: Array<String>, required: Int) {
        if (args.size < required) {
            throw CommandException("§cUsage: $commandSyntax")
        }
    }
    
    /**
     * Parse a double argument with error handling.
     */
    protected fun parseDouble(value: String, argName: String = "value"): Double {
        return value.toDoubleOrNull()
            ?: throw CommandException("§c'$value' is not a valid number for $argName")
    }
    
    /**
     * Parse an integer argument with error handling.
     */
    protected fun parseInt(value: String, argName: String = "value"): Int {
        return value.toIntOrNull()
            ?: throw CommandException("§c'$value' is not a valid integer for $argName")
    }
    
    /**
     * Find an online player by name.
     */
    protected fun findOnlinePlayer(name: String): Player? {
        return Bukkit.getPlayer(name)
    }
    
    /**
     * Find an offline player by name.
     */
    @Suppress("DEPRECATION")
    protected fun findOfflinePlayer(name: String): OfflinePlayer {
        return Bukkit.getOfflinePlayer(name)
    }
    
    /**
     * Get ITanPlayer for an offline player.
     */
    protected suspend fun OfflinePlayer.tanPlayer(): ITanPlayer? {
        return PlayerDataService.getPlayer(uniqueId)
    }
    
    /**
     * Require player to be in a town, throwing exception if not.
     */
    protected suspend fun ITanPlayer.requireTown(): TownData {
        val townId = this.townId
        if (townId.isNullOrEmpty()) {
            throw CommandException("§cYou must be in a town to use this command")
        }
        return TerritoryService.getTown(townId)
            ?: throw CommandException("§cTown data not found.")
    }
    
    /**
     * Send a success message to the sender.
     */
    protected fun CommandSender.success(message: String) {
        if (this is Player) {
            TanChatUtils.message(this, "§a$message")
        } else {
            sendMessage("§a$message")
        }
    }
    
    /**
     * Send an error message to the sender.
     */
    protected fun CommandSender.error(message: String) {
        if (this is Player) {
            TanChatUtils.message(this, "§c$message")
        } else {
            sendMessage("§c$message")
        }
    }
    
    /**
     * Send an info message to the sender.
     */
    protected fun CommandSender.info(message: String) {
        if (this is Player) {
            TanChatUtils.message(this, "§7$message")
        } else {
            sendMessage("§7$message")
        }
    }
    
    /**
     * Run a block on the main thread (for chunk/entity operations).
     */
    protected suspend fun <T> onMainThread(player: Player, block: () -> T): T {
        return withContext(FoliaDispatchers.forEntity(TownsAndNations.getPlugin(), player)) {
            block()
        }
    }
    
    // ============ Tab Complete Helpers ============
    
    /**
     * Get online player names for tab completion.
     */
    protected fun onlinePlayerNames(): List<String> {
        return Bukkit.getOnlinePlayers().map { it.name }
    }
    
    /**
     * Filter suggestions by current input.
     */
    protected fun List<String>.filterByInput(input: String): List<String> {
        val lower = input.lowercase()
        return filter { it.lowercase().startsWith(lower) }
    }
}

/**
 * Exception for command errors that should be shown to the user.
 */
class CommandException(override val message: String) : Exception(message)
