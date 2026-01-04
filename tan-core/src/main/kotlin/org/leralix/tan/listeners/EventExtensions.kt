package org.leralix.tan.listeners

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerEvent
import org.leralix.tan.dataclass.ITanPlayer
import org.leralix.tan.service.PlayerDataService

/**
 * Extension utilities for Bukkit events.
 * Provides coroutine-friendly helpers for common event operations.
 */

/**
 * Cancels the event and optionally sends a message to the player.
 */
fun <T> T.cancelWithMessage(
    player: Player,
    message: String
) where T : Event, T : Cancellable {
    (this as Cancellable).isCancelled = true
    player.sendMessage(message)
}

/**
 * Cancels the event silently.
 */
fun <T : Cancellable> T.cancel() {
    isCancelled = true
}

/**
 * Check if event is cancelled and execute block if not.
 */
inline fun <T : Cancellable> T.ifNotCancelled(block: T.() -> Unit) {
    if (!isCancelled) {
        block()
    }
}

/**
 * Get player from PlayerEvent with null-safety.
 */
val PlayerEvent.safePlayer: Player
    get() = player

/**
 * Extension to get TanPlayer from a PlayerEvent asynchronously.
 * Returns null if player data cannot be found.
 */
suspend fun PlayerEvent.tanPlayer(): ITanPlayer? =
    PlayerDataService.getPlayer(player)

/**
 * Marks an event to be handled with specific priority.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AsyncHandler(
    val value: String = "Handles event asynchronously"
)
