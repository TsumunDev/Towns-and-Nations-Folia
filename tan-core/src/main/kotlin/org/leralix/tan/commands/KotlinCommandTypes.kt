package org.leralix.tan.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.leralix.tan.dataclass.ITanPlayer

/**
 * Base class for player-only Kotlin commands.
 * Automatically validates that the sender is a Player and loads TanPlayer data.
 * 
 * Usage:
 * ```kotlin
 * class MyPlayerCommand : KotlinPlayerCommand(
 *     name = "mycommand",
 *     description = "Player command",
 *     arguments = 0,
 *     syntax = "/tan mycommand"
 * ) {
 *     override suspend fun executePlayer(player: Player, tanPlayer: ITanPlayer, args: Array<String>) {
 *         player.sendMessage("Hello ${tanPlayer.name}!")
 *     }
 * }
 * ```
 */
abstract class KotlinPlayerCommand(
    name: String,
    description: String,
    arguments: Int,
    syntax: String
) : KotlinCommand(name, description, arguments, syntax) {
    
    final override suspend fun executeAsync(sender: CommandSender, args: Array<String>) {
        val player = sender.asPlayer() ?: return
        val tanPlayer = player.tanPlayer() ?: return
        
        executePlayer(player, tanPlayer, args)
    }
    
    /**
     * Implement this for player command logic.
     * Player and TanPlayer are guaranteed to be valid.
     */
    abstract suspend fun executePlayer(player: Player, tanPlayer: ITanPlayer, args: Array<String>)
}

/**
 * Base class for town member commands.
 * Automatically validates player is in a town.
 * 
 * Usage:
 * ```kotlin
 * class TownInfoCommand : KotlinTownCommand(
 *     name = "info",
 *     description = "Show town info",
 *     arguments = 0,
 *     syntax = "/tan info"
 * ) {
 *     override suspend fun executeTown(
 *         player: Player, 
 *         tanPlayer: ITanPlayer, 
 *         town: TownData,
 *         args: Array<String>
 *     ) {
 *         player.sendMessage("Town: ${town.name}")
 *         player.sendMessage("Balance: ${town.balance}")
 *     }
 * }
 * ```
 */
abstract class KotlinTownCommand(
    name: String,
    description: String,
    arguments: Int,
    syntax: String
) : KotlinPlayerCommand(name, description, arguments, syntax) {
    
    final override suspend fun executePlayer(
        player: Player, 
        tanPlayer: ITanPlayer, 
        args: Array<String>
    ) {
        val town = tanPlayer.requireTown()
        executeTown(player, tanPlayer, town, args)
    }
    
    /**
     * Implement this for town command logic.
     * Player is guaranteed to be in a town.
     */
    abstract suspend fun executeTown(
        player: Player,
        tanPlayer: ITanPlayer,
        town: org.leralix.tan.dataclass.territory.TownData,
        args: Array<String>
    )
}

/**
 * Base class for admin commands.
 * Can be executed by console or player with admin permission.
 * 
 * Usage:
 * ```kotlin
 * class ReloadCommand : KotlinAdminCommand(
 *     name = "reload",
 *     description = "Reload config",
 *     arguments = 0,
 *     syntax = "/tanadmin reload",
 *     permission = "tan.admin.reload"
 * ) {
 *     override suspend fun executeAdmin(sender: CommandSender, args: Array<String>) {
 *         // Reload logic
 *         sender.success("Config reloaded!")
 *     }
 * }
 * ```
 */
abstract class KotlinAdminCommand(
    name: String,
    description: String,
    arguments: Int,
    syntax: String,
    private val permission: String = "tan.admin"
) : KotlinCommand(name, description, arguments, syntax) {
    
    final override suspend fun executeAsync(sender: CommandSender, args: Array<String>) {
        // Check permission
        if (!sender.hasPermission(permission)) {
            throw CommandException("§cYou don't have permission to use this command.")
        }
        
        executeAdmin(sender, args)
    }
    
    /**
     * Implement this for admin command logic.
     */
    abstract suspend fun executeAdmin(sender: CommandSender, args: Array<String>)
}
