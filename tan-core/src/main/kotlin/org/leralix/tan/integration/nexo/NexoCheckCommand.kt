@file:JvmName("NexoCommands")
package org.leralix.tan.integration.nexo
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.leralix.lib.commands.SubCommand
import kotlinx.coroutines.*
import java.util.concurrent.CompletableFuture
class NexoCheckCommand : SubCommand() {
    override fun getName(): String = "nexoch"
    override fun getDescription(): String = "Check Nexo integration status"
    override fun getArguments(): Int = 0
    override fun getSyntax(): String = "/tandebug nexoch"
    override fun getTabCompleteSuggestions(sender: CommandSender, lowerCase: String, args: Array<out String>): List<String> {
        return emptyList()
    }
    override fun perform(sender: CommandSender, args: Array<out String>) {
        sender.sendMessage("§6========== NEXO INTEGRATION STATUS ==========")
        val nexoPlugin = Bukkit.getPluginManager().getPlugin("Nexo")
        when {
            nexoPlugin == null -> {
                sender.sendMessage("§c✗ Nexo Plugin: NOT INSTALLED")
                sender.sendMessage("§7  Install Nexo plugin to use nexo: icons")
                sender.sendMessage("§7  Download: https://nexomc.com/")
                return
            }
            !nexoPlugin.isEnabled -> {
                sender.sendMessage("§c✗ Nexo Plugin: INSTALLED BUT DISABLED")
                sender.sendMessage("§7  Check your server logs for startup errors")
                return
            }
            else -> {
                sender.sendMessage("§a✓ Nexo Plugin: v${nexoPlugin.description.version}")
            }
        }
        val enabled = NexoIntegration.isEnabled
        val incompatible = NexoIntegration.isVersionIncompatible
        sender.sendMessage("§7─────────────────────────────────────────────")
        sender.sendMessage("§7Integration: ${if (enabled) "§a✓ ENABLED" else if (incompatible) "§c⚠ INCOMPATIBLE" else "§c✗ DISABLED"}")
        sender.sendMessage("§7Initialized: ${if (NexoIntegration.isInitialized) "§aYes" else "§cNo"}")
        sender.sendMessage("§7Version: §f${NexoIntegration.nexoVersion ?: "Unknown"}")
        if (incompatible) {
            sender.sendMessage("§c─────────────────────────────────────────────")
            sender.sendMessage("§c⚠ NEXO VERSION INCOMPATIBILITY!")
            sender.sendMessage("§cYour Nexo version is incompatible with this server version.")
            sender.sendMessage("§cUpdate Nexo: §ehttps://github.com/NexoMC/Nexo/releases")
            sender.sendMessage("§cIcons will use fallback materials (BARRIER)")
            sender.sendMessage("§7─────────────────────────────────────────────")
            return
        }
        if (!enabled) {
            sender.sendMessage("§c─────────────────────────────────────────────")
            sender.sendMessage("§c⚠ Integration failed - check console for errors")
            return
        }
        sender.sendMessage("§7─────────────────────────────────────────────")
        sender.sendMessage("§7Cache Statistics:")
        sender.sendMessage("§7  ${NexoIntegration.getCacheStats()}")
        sender.sendMessage("§7─────────────────────────────────────────────")
        sender.sendMessage("§7Testing item fetch...")
        val testItems = listOf("bouton_fleche_gauche", "bouton_fleche_droite")
        for (itemId in testItems) {
            val exists = NexoIntegration.itemExists(itemId)
            val status = if (exists) "§a✓" else "§c✗"
            sender.sendMessage("§7  $status '$itemId': ${if (exists) "Found" else "Not found"}")
        }
        sender.sendMessage("§6===============================================")
    }
}
class NexoDebugCommand : SubCommand() {
    override fun getName(): String = "nexo"
    override fun getDescription(): String = "Advanced Nexo debug commands"
    override fun getArguments(): Int = -1
    override fun getSyntax(): String = "/tandebug nexo <get|list|give|clear|reload|info|checkversion> [args...]"
    override fun getTabCompleteSuggestions(sender: CommandSender, lowerCase: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("get", "list", "give", "clear", "reload", "info", "test", "events", "checkversion")
            2 -> when (args[0].lowercase()) {
                "get", "info" -> getCommonItemIds()
                "give" -> Bukkit.getOnlinePlayers().map { it.name }
                "list" -> listOf("cached", "all", "notfound")
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "give" -> getCommonItemIds()
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    override fun perform(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            showHelp(sender)
            return
        }
        when (args[0].lowercase()) {
            "get" -> handleGet(sender, args)
            "list" -> handleList(sender, args)
            "give" -> handleGive(sender, args)
            "clear" -> handleClear(sender)
            "reload" -> handleReload(sender)
            "info" -> handleInfo(sender, args)
            "test" -> handleTest(sender, args)
            "events" -> handleEvents(sender)
            "checkversion" -> handleCheckVersion(sender)
            else -> {
                sender.sendMessage("§cUnknown subcommand: ${args[0]}")
                showHelp(sender)
            }
        }
    }
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§6========== NEXO DEBUG COMMANDS ==========")
        sender.sendMessage("§a/tandebug nexo get <item>§7 - Fetch and show an item")
        sender.sendMessage("§a/tandebug nexo list [cached|all|notfound]§7 - List items")
        sender.sendMessage("§a/tandebug nexo give <player> <item> [amount]§7 - Give item to player")
        sender.sendMessage("§a/tandebug nexo info <item>§7 - Show detailed item info")
        sender.sendMessage("§a/tandebug nexo checkversion§7 - Check for Nexo updates")
        sender.sendMessage("§a/tandebug nexo test [item]§7 - Test item fetch with timing")
        sender.sendMessage("§a/tandebug nexo events§7 - Show event listener status")
        sender.sendMessage("§a/tandebug nexo clear§7 - Clear all caches")
        sender.sendMessage("§a/tandebug nexo reload§7 - Reinitialize integration")
        sender.sendMessage("§6==========================================")
    }
    private fun handleGet(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /tandebug nexo get <item_id>")
            return
        }
        val itemId = args[1]
        if (sender is Player) {
            val item = NexoIntegration.getItem(itemId)
            if (item != null) {
                sender.inventory.addItem(item)
                sender.sendMessage("§aGave you: $itemId (${item.type})")
            } else {
                sender.sendMessage("§cItem '$itemId' not found")
            }
        } else {
            sender.sendMessage("§cThis command can only be used by players")
        }
    }
    private fun handleList(sender: CommandSender, args: Array<out String>) {
        val filter = args.getOrNull(1)?.lowercase() ?: "cached"
        sender.sendMessage("§6========== NEXO ITEM LIST ==========")
        sender.sendMessage("§7Filter: §f$filter")
        when (filter) {
            "cached" -> {
                sender.sendMessage("§7Showing cached items...")
                sender.sendMessage("§7Cached items: ${NexoIntegration.getCacheStats()}")
            }
            "notfound" -> {
                sender.sendMessage("§7Items that weren't found (cached as missing):")
                sender.sendMessage("§7Count: (not exposed in API)")
            }
            "all" -> {
                sender.sendMessage("§7Total statistics: ${NexoIntegration.getCacheStats()}")
            }
            else -> {
                sender.sendMessage("§cUnknown filter: $filter")
            }
        }
        sender.sendMessage("§6====================================")
    }
    private fun handleGive(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§cUsage: /tandebug nexo give <player> <item_id> [amount]")
            return
        }
        val targetPlayer = Bukkit.getPlayer(args[1])
        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer not found: ${args[1]}")
            return
        }
        val itemId = args[2]
        val amount = args.getOrNull(3)?.toIntOrNull() ?: 1
        val success = targetPlayer.giveNexoItem(itemId, amount)
        if (success) {
            sender.sendMessage("§aGave ${amount}x '$itemId' to ${targetPlayer.name}")
        } else {
            sender.sendMessage("§cFailed to give '$itemId' to ${targetPlayer.name} (item not found)")
        }
    }
    private fun handleClear(sender: CommandSender) {
        NexoIntegration.clearCache()
        sender.sendMessage("§a✓ Cleared all Nexo caches")
    }
    private fun handleReload(sender: CommandSender) {
        sender.sendMessage("§7Reinitializing Nexo integration...")
        NexoIntegration.reinitialize()
        if (NexoIntegration.isEnabled) {
            sender.sendMessage("§a✓ Integration reinitialized successfully")
        } else {
            sender.sendMessage("§c✗ Reinitialization failed")
        }
        sender.sendMessage("§7Status: ${NexoIntegration.getCacheStats()}")
    }
    private fun handleInfo(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /tandebug nexo info <item_id>")
            return
        }
        val itemId = args[1]
        sender.sendMessage("§6========== NEXO ITEM INFO ==========")
        sender.sendMessage("§7Item ID: §f$itemId")
        sender.sendMessage("§7Exists: §${if (NexoIntegration.itemExists(itemId)) "aYes" else "cNo"}")
        val item = NexoIntegration.getItem(itemId)
        if (item != null) {
            sender.sendMessage("§7────────────────────────────────")
            sender.sendMessage("§7Type: §f${item.type}")
            sender.sendMessage("§7Amount: §f${item.amount}")
            sender.sendMessage("§7Display Name: §f${item.itemMeta?.displayName ?: "None"}")
            sender.sendMessage("§7Lore: §f${item.itemMeta?.lore()?.joinToString("\n") ?: "None"}")
            sender.sendMessage("§7Custom Model Data: §f${item.itemMeta?.hasCustomModelData()}")
            sender.sendMessage("§7Enchantments: §f${item.enchantments.keys.joinToString()}")
        } else {
            sender.sendMessage("§c✗ Item not found or Nexo not enabled")
        }
        sender.sendMessage("§6====================================")
    }
    private fun handleTest(sender: CommandSender, args: Array<out String>) {
        val testItems = if (args.size >= 2) {
            listOf(args[1])
        } else {
            listOf("bouton_fleche_gauche", "bouton_fleche_droite", "custom_crown")
        }
        sender.sendMessage("§6========== NEXO FETCH TEST ==========")
        for (itemId in testItems) {
            val start = System.nanoTime()
            val item = NexoIntegration.getItem(itemId)
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            val status = if (item != null) "§a✓" else "§c✗"
            val type = item?.type?.name ?: "N/A"
            sender.sendMessage(String.format("§7%s %s: %s (%.2fms)", status, itemId, type, elapsed))
        }
        sender.sendMessage("§7Cache Stats: ${NexoIntegration.getCacheStats()}")
        sender.sendMessage("§6====================================")
    }
    private fun handleEvents(sender: CommandSender) {
        sender.sendMessage("§6========== NEXO EVENTS ==========")
        sender.sendMessage("§7Plugin Listener: ${if (NexoEventListener.isRegistered()) "§aRegistered" else "§cNot Registered"}")
        sender.sendMessage("§7Events Available: ${if (NexoEventReflection.areEventsAvailable()) "§aYes" else "§cNo"}")
        sender.sendMessage("§7Integration Status: ${if (NexoIntegration.isEnabled) "§aEnabled" else "§cDisabled"}")
        sender.sendMessage("§6=================================")
    }
    private fun handleCheckVersion(sender: CommandSender) {
        sender.sendMessage("§7Checking for Nexo updates...")
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val info = NexoUpdateChecker.checkForUpdates().get()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (info != null) {
                        NexoUpdateChecker.getDetailedInfo().forEach { line ->
                            sender.sendMessage(line)
                        }
                    } else {
                        sender.sendMessage("§cFailed to fetch version information.")
                        sender.sendMessage("§7Check your internet connection or try again later.")
                    }
                }
            } catch (e: Exception) {
                sender.sendMessage("§cError checking for updates: ${e.message}")
            }
        }
    }
    private fun getCommonItemIds(): List<String> {
        return listOf(
            "bouton_fleche_gauche",
            "bouton_fleche_droite",
            "custom_crown",
            "gold_coin_stack",
            "enchanted_sword"
        )
    }
}
class NexoConsoleCommand : org.bukkit.command.Command("nexo") {
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§6Nexo Integration Commands:")
            sender.sendMessage("§a/nexo status§7 - Show integration status")
            sender.sendMessage("§a/nexo get <item>§7 - Test fetch an item")
            sender.sendMessage("§a/nexo list§7 - List cached items")
            sender.sendMessage("§a/nexo reload§7 - Reinitialize integration")
            sender.sendMessage("§a/nexo clear§7 - Clear caches")
            return true
        }
        when (args[0].lowercase()) {
            "status" -> showStatus(sender)
            "get" -> handleGet(sender, args)
            "list" -> showList(sender)
            "reload" -> {
                sender.sendMessage("§7Reinitializing...")
                NexoIntegration.reinitialize()
                sender.sendMessage("§${if (NexoIntegration.isEnabled) "a" else "c"}Done. Status: ${NexoIntegration.getCacheStats()}")
            }
            "clear" -> {
                NexoIntegration.clearCache()
                sender.sendMessage("§aCaches cleared.")
            }
            else -> {
                sender.sendMessage("§cUnknown command: ${args[0]}")
                return false
            }
        }
        return true
    }
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("status", "get", "list", "reload", "clear")
            2 -> if (args[0] == "get") listOf("bouton_fleche_gauche", "custom_crown") else emptyList()
            else -> emptyList()
        }
    }
    private fun showStatus(sender: CommandSender) {
        val nexoPlugin = Bukkit.getPluginManager().getPlugin("Nexo")
        sender.sendMessage("§6========== NEXO STATUS ==========")
        sender.sendMessage("§7Plugin: §${if (nexoPlugin != null) "aInstalled v${nexoPlugin.description.version}" else "cNot Installed"}")
        sender.sendMessage("§7Integration: §${if (NexoIntegration.isEnabled) "aEnabled" else "cDisabled"}")
        sender.sendMessage("§7${NexoIntegration.getCacheStats()}")
        sender.sendMessage("§6================================")
    }
    private fun handleGet(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /nexo get <item_id>")
            return
        }
        val item = NexoIntegration.getItem(args[1])
        sender.sendMessage("§7Item '${args[1]}': §${if (item != null) "aFound (${item.type})" else "cNot Found"}")
    }
    private fun showList(sender: CommandSender) {
        sender.sendMessage("§7Cache: ${NexoIntegration.getCacheStats()}")
    }
}
private fun getNotFoundCount(): Int {
    return 0
}