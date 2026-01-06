@file:JvmName("NexoUpdateChecker")
package org.leralix.tan.integration.nexo
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
object UpdateCheckerConfig {
    @JvmStatic
    var autoCheckEnabled = true
    @JvmStatic
    var checkIntervalHours = 24
    @JvmStatic
    var notifyAdminsOnJoin = true
    @JvmStatic
    var notifyPermission = "tan.admin"
    const val NEXO_MAVEN_URL = "https://repo.maven.apache.org/maven2/com/nexomc/Nexo/maven-metadata.xml"
    const val NEXO_RELEASES_API = "https://api.github.com/repos/NexoMC/Nexo/releases/latest"
    const val NEXO_DOWNLOAD_URL = "https://github.com/NexoMC/Nexo/releases"
    val COMPATIBILITY_MAP: Map<String, String> = mapOf(
        "1.21.11" to "1.17.0",
        "1.21.9" to "1.17.0",
        "1.21.7" to "1.17.0",
        "1.21.4" to "1.17.0",
        "1.21.3" to "1.17.0",
        "1.21.2" to "1.17.0",
        "1.21.1" to "1.17.0",
        "1.21" to "1.17.0",
        "1.20.4" to "1.17.0",
        "1.20.3" to "1.17.0",
        "1.20.2" to "1.17.0",
        "1.20.1" to "1.17.0",
        "1.20" to "1.17.0",
        "1.19.4" to "1.17.0",
        "1.19.3" to "1.17.0",
        "1.19.2" to "1.17.0",
        "1.19.1" to "1.17.0",
        "1.19" to "1.17.0"
    )
}
data class VersionInfo(
    val dependencyName: String,
    val currentVersion: String?,
    val latestVersion: String?,
    val isUpdateAvailable: Boolean,
    val isCompatible: Boolean,
    val downloadUrl: String,
    val releaseNotes: String? = null,
    val lastChecked: Long = System.currentTimeMillis()
) {
    fun isStale(): Boolean {
        val hoursSinceCheck = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastChecked)
        return hoursSinceCheck >= UpdateCheckerConfig.checkIntervalHours
    }
    fun getLastCheckedFormatted(): String {
        val hours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastChecked)
        return when {
            hours < 1 -> "Just now"
            hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            else -> "${hours / 24} day${if (hours / 24 > 1) "s" else ""} ago"
        }
    }
}
object NexoUpdateChecker {
    private val logger = LoggerFactory.getLogger(NexoUpdateChecker::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val versionCache = ConcurrentHashMap<String, VersionInfo>()
    @Volatile
    private var lastGlobalCheck = 0L
    @JvmStatic
    fun shouldCheck(): Boolean {
        val hoursSinceLastCheck = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastGlobalCheck)
        return hoursSinceLastCheck >= UpdateCheckerConfig.checkIntervalHours
    }
    @JvmStatic
    fun getServerVersion(): String {
        val version = Bukkit.getVersion()
        val match = Regex("(?:Folia|Paper)\\s*([\\d.]+)").find(version)
        return match?.groupValues?.get(1) ?: "Unknown"
    }
    @JvmStatic
    fun getApiVersion(): String {
        return Bukkit.getServer().javaClass.getPackage().implementationVersion ?: "Unknown"
    }
    @JvmStatic
    fun checkNexoCompatibility(nexoVersion: String?): Boolean {
        if (nexoVersion == null) return false
        val serverVersion = getServerVersion()
        logger.debug("[NEXO Update] Checking compatibility: Nexo $nexoVersion on server $serverVersion")
        return try {
            Class.forName("io.papermc.paper.datacomponent.item.ItemAttackSpeed")
            Class.forName("io.papermc.paper.datacomponent.item.ItemAttributeModifiers")
            Class.forName("io.papermc.paper.datacomponent.item.AttackRange")
            true
        } catch (e: ClassNotFoundException) {
            logger.warn("[NEXO Update] Incompatibility detected: Missing Paper class: ${e.message}")
            false
        }
    }
    @JvmStatic
    fun checkForUpdates(): CompletableFuture<VersionInfo?> {
        return CompletableFuture.supplyAsync {
            performNexoCheck()
        }
    }
    private fun performNexoCheck(): VersionInfo? {
        if (!UpdateCheckerConfig.autoCheckEnabled) {
            logger.debug("[NEXO Update] Auto-check disabled")
            return null
        }
        lastGlobalCheck = System.currentTimeMillis()
        logger.info("[NEXO Update] Checking for updates...")
        val currentVersion = NexoIntegration.nexoVersion
        val serverVersion = getServerVersion()
        try {
            val githubInfo = fetchLatestFromGitHub()
            if (githubInfo != null) {
                val (latestVersion, downloadUrl, releaseNotes) = githubInfo
                val isUpdateAvailable = isUpdateAvailable(currentVersion, latestVersion)
                val isCompatible = checkNexoCompatibility(currentVersion)
                val info = VersionInfo(
                    dependencyName = "Nexo",
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    isUpdateAvailable = isUpdateAvailable,
                    isCompatible = isCompatible,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes
                )
                versionCache["nexo"] = info
                logUpdateResult(info)
                return info
            }
            val mavenVersion = fetchLatestFromMaven()
            if (mavenVersion != null) {
                val isUpdateAvailable = isUpdateAvailable(currentVersion, mavenVersion)
                val isCompatible = checkNexoCompatibility(currentVersion)
                val info = VersionInfo(
                    dependencyName = "Nexo",
                    currentVersion = currentVersion,
                    latestVersion = mavenVersion,
                    isUpdateAvailable = isUpdateAvailable,
                    isCompatible = isCompatible,
                    downloadUrl = UpdateCheckerConfig.NEXO_DOWNLOAD_URL
                )
                versionCache["nexo"] = info
                logUpdateResult(info)
                return info
            }
            logger.warn("[NEXO Update] Could not fetch latest version from GitHub or Maven")
            return null
        } catch (e: Exception) {
            logger.error("[NEXO Update] Error checking for updates: ${e.message}", e)
            return null
        }
    }
    private fun fetchLatestFromGitHub(): Triple<String, String, String?>? {
        return try {
            val connection = URL(UpdateCheckerConfig.NEXO_RELEASES_API).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "TAN-UpdateChecker")
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val tagMatch = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(response)
                val urlMatch = Regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"").find(response)
                val bodyMatch = Regex("\"body\"\\s*:\\s*\"([^\"]*)\"").find(response)
                val tag = tagMatch?.groupValues?.get(1)?.removePrefix("v") ?: return null
                val url = urlMatch?.groupValues?.get(1) ?: UpdateCheckerConfig.NEXO_DOWNLOAD_URL
                val notes = bodyMatch?.groupValues?.get(1)?.replace("\\n", "\n")?.take(500)
                Triple(tag, url, notes)
            } else {
                logger.debug("[NEXO Update] GitHub API returned $responseCode")
                null
            }
        } catch (e: Exception) {
            logger.debug("[NEXO Update] GitHub fetch failed: ${e.message}")
            null
        }
    }
    private fun fetchLatestFromMaven(): String? {
        return try {
            val connection = URL(UpdateCheckerConfig.NEXO_MAVEN_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val latestMatch = Regex("<latest>([^<]+)</latest>").find(response)
                latestMatch?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug("[NEXO Update] Maven fetch failed: ${e.message}")
            null
        }
    }
    private fun isUpdateAvailable(current: String?, latest: String): Boolean {
        if (current == null) return true
        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0.until(maxOf(currentParts.size, latestParts.size))) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (c > l) return false
            }
            return false
        } catch (e: Exception) {
            return current != latest
        }
    }
    private fun logUpdateResult(info: VersionInfo) {
        when {
            !info.isCompatible && info.currentVersion != null -> {
                logger.error("┌────────────────────────────────────────────────────────────────┐")
                logger.error("│ ⚠️  NEXO VERSION INCOMPATIBILITY                                    │")
                logger.error("├────────────────────────────────────────────────────────────────┤")
                logger.error("│ Current Nexo: ${info.currentVersion.padEnd(52)}│")
                logger.error("│ Server Version: ${getServerVersion().padEnd(49)}│")
                logger.error("│                                                                      │")
                logger.error("│ Your Nexo version is INCOMPATIBLE with this server version.       │")
                logger.error("│ Update Nexo to resolve this issue.                                  │")
                logger.error("│                                                                      │")
                logger.error("│ Download: ${info.downloadUrl.padEnd(54)}│")
                logger.error("└────────────────────────────────────────────────────────────────┘")
            }
            info.isUpdateAvailable -> {
                logger.warn("┌────────────────────────────────────────────────────────────────┐")
                logger.warn("│ 📦 NEXO UPDATE AVAILABLE                                          │")
                logger.warn("├────────────────────────────────────────────────────────────────┤")
                logger.warn("│ Current Version: ${(info.currentVersion ?: "Not installed").padEnd(48)}│")
                logger.warn("│ Latest Version:  ${(info.latestVersion ?: "Unknown").padEnd(48)}│")
                logger.warn("│                                                                      │")
                logger.warn("│ Download: ${info.downloadUrl.padEnd(54)}│")
                logger.warn("│                                                                      │")
                logger.warn("│ Use /tandebug nexo checkversion for more details                   │")
                logger.warn("└────────────────────────────────────────────────────────────────┘")
            }
            else -> {
                logger.info("[NEXO Update] ✓ Nexo is up to date (${info.currentVersion})")
            }
        }
    }
    @JvmStatic
    fun getVersionInfo(dependencyId: String): VersionInfo? {
        return versionCache[dependencyId]
    }
    @JvmStatic
    fun getNexoInfo(): VersionInfo? {
        var info = versionCache["nexo"]
        if (info == null || info.isStale()) {
            runBlocking {
                val result = checkForUpdates().get()
                info = result
            }
        }
        return info
    }
    @JvmStatic
    fun getStatus(): String {
        val info = versionCache["nexo"]
        return when {
            info == null -> "Not checked yet"
            !info.isCompatible && info.currentVersion != null -> "§c⚠ INCOMPATIBLE (v${info.currentVersion})"
            info.isUpdateAvailable -> "§e⚠ Update available: ${info.currentVersion} → ${info.latestVersion}"
            else -> "§a✓ Up to date (v${info.currentVersion})"
        }
    }
    @JvmStatic
    fun notifySender(sender: CommandSender) {
        val info = versionCache["nexo"] ?: run {
            sender.sendMessage("§7No update information available. Use /tandebug nexo checkversion to check.")
            return
        }
        when {
            !info.isCompatible && info.currentVersion != null -> {
                sender.sendMessage("§c┌──────────────────────────────────────────────────────────┐")
                sender.sendMessage("§c│ ⚠️  NEXO VERSION INCOMPATIBILITY                            │")
                sender.sendMessage("§c├──────────────────────────────────────────────────────────┤")
                sender.sendMessage("§c│ Current Nexo: §f${info.currentVersion.padEnd(43)}§c│")
                sender.sendMessage("§c│ Server Version: §f${getServerVersion().padEnd(40)}§c│")
                sender.sendMessage("§c│                                                            │")
                sender.sendMessage("§c│ Your Nexo version requires classes not available on      │")
                sender.sendMessage("§c│ this server version.                                      │")
                sender.sendMessage("§c│                                                            │")
                sender.sendMessage("§c│ §eDownload: §f" + info.downloadUrl.take(50).padEnd(50) + "§c│")
                sender.sendMessage("§c└──────────────────────────────────────────────────────────┘")
            }
            info.isUpdateAvailable -> {
                sender.sendMessage("§e┌──────────────────────────────────────────────────────────┐")
                sender.sendMessage("§e│ 📦 NEXO UPDATE AVAILABLE                                    │")
                sender.sendMessage("§e├──────────────────────────────────────────────────────────┤")
                sender.sendMessage("§e│ Current Version: §f${(info.currentVersion ?: "Not installed").padEnd(40)}§e│")
                sender.sendMessage("§e│ Latest Version:  §f${(info.latestVersion ?: "Unknown").padEnd(43)}§e│")
                sender.sendMessage("§e│                                                            │")
                sender.sendMessage("§e│ §eDownload: §f" + info.downloadUrl.take(50).padEnd(50) + "§e│")
                sender.sendMessage("§e│                                                            │")
                sender.sendMessage("§e│ Last checked: §7${info.getLastCheckedFormatted().padEnd(43)}§e│")
                sender.sendMessage("§e└──────────────────────────────────────────────────────────┘")
                if (info.releaseNotes != null) {
                    sender.sendMessage("§7Release Notes (first 500 chars):")
                    sender.sendMessage("§f" + info.releaseNotes.take(300) + "...")
                }
            }
            else -> {
                sender.sendMessage("§a✓ Nexo is up to date! (v${info.currentVersion})")
                sender.sendMessage("§7Last checked: ${info.getLastCheckedFormatted()}")
            }
        }
    }
    @JvmStatic
    fun notifyAdminIfUpdateAvailable(player: Player) {
        if (!UpdateCheckerConfig.notifyAdminsOnJoin) return
        if (!player.hasPermission(UpdateCheckerConfig.notifyPermission)) return
        scope.launch {
            delay(3000)
            val info = getNexoInfo() ?: return@launch
            if (info.isUpdateAvailable || (!info.isCompatible && info.currentVersion != null)) {
                player.sendMessage("§7[§6TAN§7] §eNexo update available!")
                player.sendMessage("§7Use §f/tandebug nexo checkversion §7for details")
            }
        }
    }
    @JvmStatic
    fun getDetailedInfo(): List<String> {
        val info = versionCache["nexo"]
        val serverVersion = getServerVersion()
        val apiVersion = getApiVersion()
        val currentVersion = NexoIntegration.nexoVersion
        return buildList {
            add("§6========== NEXO VERSION INFO ==========")
            add("§7Server Version: §f$serverVersion")
            add("§7API Version: §f$apiVersion")
            add("§7───────────────────────────────────────────")
            if (info != null) {
                add("§7Current Version: §f${info.currentVersion ?: "Not installed"}")
                add("§7Latest Version: §f${info.latestVersion ?: "Unknown"}")
                add("§7Update Available: §${if (info.isUpdateAvailable) "eYes" else "aNo"}")
                add("§7Compatible: §${if (info.isCompatible) "aYes" else "cNo"}")
                add("§7───────────────────────────────────────────")
                add("§7Download URL: §f${info.downloadUrl}")
                add("§7Last Checked: §f${info.getLastCheckedFormatted()}")
            } else {
                add("§7Current Version: §f${currentVersion ?: "Not installed"}")
                add("§7Update check not performed yet.")
            }
            add("§7───────────────────────────────────────────")
            val isCompatible = checkNexoCompatibility(currentVersion)
            add("§7Compatibility Check: §${if (isCompatible) "a✓ PASS" else "c✗ FAIL"}")
            if (!isCompatible && currentVersion != null) {
                add("§c  Nexo $currentVersion requires Paper classes not")
                add("§c  available on this server version.")
                add("§c  Please update your server or use an")
                add("§c  older Nexo version.")
            }
            add("§6=======================================")
        }
    }
    @JvmStatic
    fun initialize() {
        if (!UpdateCheckerConfig.autoCheckEnabled) {
            logger.info("[NEXO Update] Auto-update check disabled")
            return
        }
        scope.launch {
            try {
                performNexoCheck()
            } catch (e: Exception) {
                logger.error("[NEXO Update] Initial check failed: ${e.message}", e)
            }
        }
        scope.launch {
            while (isActive) {
                delay(TimeUnit.HOURS.toMillis(UpdateCheckerConfig.checkIntervalHours.toLong()))
                if (UpdateCheckerConfig.autoCheckEnabled) {
                    try {
                        performNexoCheck()
                    } catch (e: Exception) {
                        logger.error("[NEXO Update] Periodic check failed: ${e.message}")
                    }
                }
            }
        }
    }
    @JvmStatic
    fun shutdown() {
        scope.cancel()
        logger.debug("[NEXO Update] Update checker shut down")
    }
}
object DependencyUpdateChecker {
    private val logger = LoggerFactory.getLogger(DependencyUpdateChecker::class.java)
    enum class Dependency(
        val pluginName: String,
        val downloadUrl: String,
        val spigotId: String? = null
    ) {
        PLACEHOLDER_API("PlaceholderAPI", "https://www.spigotmc.org/resources/placeholderapi.6245/", "6245"),
        WORLD_GUARD("WorldGuard", "https://dev.bukkit.org/projects/worldguard", null),
        VAULT("Vault", "https://www.spigotmc.org/resources/vault.34315/", "34315"),
        LUCK_PERMS("LuckPerms", "https://luckperms.net/download", null),
        ESSENTIALS("Essentials", "https://www.spigotmc.org/resources/essentialsx.9089/", "9089"),
        DECOR_HHEADS("DecentHolograms", "https://www.spigotmc.org/resources/decentholograms.96927/", "96927"),
        CITIZENS("Citizens", "https://www.spigotmc.org/resources/citizens.22128/", "22128"),
        MYTHIC_MOBS("MythicMobs", "https://www.spigotmc.org/resources/mythicmobs.5702/", "5702")
    }
    private val dependencyCache = ConcurrentHashMap<Dependency, VersionInfo>()
    @JvmStatic
    fun checkDependency(dependency: Dependency): CompletableFuture<VersionInfo?> {
        return CompletableFuture.supplyAsync {
            performDependencyCheck(dependency)
        }
    }
    @JvmStatic
    fun checkAll(): CompletableFuture<Map<Dependency, VersionInfo?>> {
        return CompletableFuture.supplyAsync {
            Dependency.entries.map { it to performDependencyCheck(it) }.toMap()
        }
    }
    @JvmStatic
    fun getInfo(dependency: Dependency): VersionInfo? {
        return dependencyCache[dependency]
    }
    private fun performDependencyCheck(dependency: Dependency): VersionInfo? {
        val plugin = Bukkit.getPluginManager().getPlugin(dependency.pluginName)
        val currentVersion = plugin?.description?.version
        val latestVersion = null
        val isUpdateAvailable = false
        val info = VersionInfo(
            dependencyName = dependency.pluginName,
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            isUpdateAvailable = isUpdateAvailable,
            isCompatible = plugin?.isEnabled == true,
            downloadUrl = dependency.downloadUrl
        )
        dependencyCache[dependency] = info
        return info
    }
}