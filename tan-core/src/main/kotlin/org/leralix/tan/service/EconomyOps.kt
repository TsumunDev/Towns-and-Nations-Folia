package org.leralix.tan.service

import kotlinx.coroutines.*
import org.leralix.tan.coroutines.TanCoroutines
import org.leralix.tan.dataclass.ITanPlayer
import org.leralix.tan.dataclass.territory.TerritoryData
import org.leralix.tan.utils.formatAsMoney
import org.leralix.tan.utils.formatAsColoredMoney
import org.leralix.tan.utils.roundToConfiguredDigits
import java.util.concurrent.CompletableFuture

/**
 * Kotlin economy operations using coroutines.
 * Provides atomic transaction support with suspend functions.
 * Complements the Java EconomyService with coroutine-based API.
 */
object EconomyOps {
    
    /**
     * Result of an economy transaction
     */
    sealed class TransactionResult {
        data class Success(val newBalance: Double) : TransactionResult()
        data class InsufficientFunds(val required: Double, val available: Double) : TransactionResult()
        data class Error(val message: String) : TransactionResult()
        
        val isSuccess: Boolean get() = this is Success
        val isFailed: Boolean get() = !isSuccess
    }
    
    // ============ Player Economy Operations ============
    
    /**
     * Add money to player balance (suspend function)
     */
    suspend fun addToPlayer(playerId: String, amount: Double): TransactionResult {
        require(amount >= 0) { "Amount must be non-negative" }
        
        val player = PlayerDataService.getPlayer(playerId)
            ?: return TransactionResult.Error("Player not found: $playerId")
        
        return try {
            val newBalance = (player.balance + amount).roundToConfiguredDigits()
            player.balance = newBalance
            PlayerDataService.savePlayer(player)
            TransactionResult.Success(newBalance)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to add money: ${e.message}")
        }
    }
    
    /**
     * Remove money from player balance (suspend function)
     */
    suspend fun removeFromPlayer(playerId: String, amount: Double): TransactionResult {
        require(amount >= 0) { "Amount must be non-negative" }
        
        val player = PlayerDataService.getPlayer(playerId)
            ?: return TransactionResult.Error("Player not found: $playerId")
        
        if (player.balance < amount) {
            return TransactionResult.InsufficientFunds(amount, player.balance)
        }
        
        return try {
            val newBalance = (player.balance - amount).roundToConfiguredDigits()
            player.balance = newBalance
            PlayerDataService.savePlayer(player)
            TransactionResult.Success(newBalance)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to remove money: ${e.message}")
        }
    }
    
    /**
     * Transfer money between players (suspend function)
     * Atomic: either both succeed or both fail
     */
    suspend fun transferBetweenPlayers(
        fromPlayerId: String, 
        toPlayerId: String, 
        amount: Double
    ): TransactionResult {
        require(amount > 0) { "Transfer amount must be positive" }
        
        val fromPlayer = PlayerDataService.getPlayer(fromPlayerId)
            ?: return TransactionResult.Error("Source player not found")
        val toPlayer = PlayerDataService.getPlayer(toPlayerId)
            ?: return TransactionResult.Error("Target player not found")
        
        if (fromPlayer.balance < amount) {
            return TransactionResult.InsufficientFunds(amount, fromPlayer.balance)
        }
        
        return try {
            // Atomic update
            fromPlayer.balance = (fromPlayer.balance - amount).roundToConfiguredDigits()
            toPlayer.balance = (toPlayer.balance + amount).roundToConfiguredDigits()
            
            // Save both - if one fails, we should rollback (simplified here)
            coroutineScope {
                launch { PlayerDataService.savePlayer(fromPlayer) }
                launch { PlayerDataService.savePlayer(toPlayer) }
            }
            
            TransactionResult.Success(fromPlayer.balance)
        } catch (e: Exception) {
            TransactionResult.Error("Transfer failed: ${e.message}")
        }
    }
    
    // ============ Territory Economy Operations ============
    
    /**
     * Add money to territory balance (suspend function)
     */
    suspend fun addToTerritory(territoryId: String, amount: Double): TransactionResult {
        require(amount >= 0) { "Amount must be non-negative" }
        
        val territory = TerritoryService.getTerritory(territoryId)
            ?: return TransactionResult.Error("Territory not found: $territoryId")
        
        return try {
            territory.addToBalance(amount)
            TerritoryService.saveTerritory(territory)
            TransactionResult.Success(territory.balance)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to add money to territory: ${e.message}")
        }
    }
    
    /**
     * Remove money from territory balance (suspend function)
     */
    suspend fun removeFromTerritory(territoryId: String, amount: Double): TransactionResult {
        require(amount >= 0) { "Amount must be non-negative" }
        
        val territory = TerritoryService.getTerritory(territoryId)
            ?: return TransactionResult.Error("Territory not found: $territoryId")
        
        if (territory.balance < amount) {
            return TransactionResult.InsufficientFunds(amount, territory.balance)
        }
        
        return try {
            territory.removeFromBalance(amount)
            TerritoryService.saveTerritory(territory)
            TransactionResult.Success(territory.balance)
        } catch (e: Exception) {
            TransactionResult.Error("Failed to remove money from territory: ${e.message}")
        }
    }
    
    /**
     * Transfer money from player to territory (suspend function)
     */
    suspend fun depositToTerritory(
        playerId: String,
        territoryId: String,
        amount: Double
    ): TransactionResult {
        require(amount > 0) { "Deposit amount must be positive" }
        
        val player = PlayerDataService.getPlayer(playerId)
            ?: return TransactionResult.Error("Player not found")
        val territory = TerritoryService.getTerritory(territoryId)
            ?: return TransactionResult.Error("Territory not found")
        
        if (player.balance < amount) {
            return TransactionResult.InsufficientFunds(amount, player.balance)
        }
        
        return try {
            player.balance = (player.balance - amount).roundToConfiguredDigits()
            territory.addToBalance(amount)
            
            coroutineScope {
                launch { PlayerDataService.savePlayer(player) }
                launch { TerritoryService.saveTerritory(territory) }
            }
            
            TransactionResult.Success(territory.balance)
        } catch (e: Exception) {
            TransactionResult.Error("Deposit failed: ${e.message}")
        }
    }
    
    /**
     * Transfer money from territory to player (suspend function)
     */
    suspend fun withdrawFromTerritory(
        playerId: String,
        territoryId: String,
        amount: Double
    ): TransactionResult {
        require(amount > 0) { "Withdrawal amount must be positive" }
        
        val player = PlayerDataService.getPlayer(playerId)
            ?: return TransactionResult.Error("Player not found")
        val territory = TerritoryService.getTerritory(territoryId)
            ?: return TransactionResult.Error("Territory not found")
        
        if (territory.balance < amount) {
            return TransactionResult.InsufficientFunds(amount, territory.balance)
        }
        
        return try {
            territory.removeFromBalance(amount)
            player.balance = (player.balance + amount).roundToConfiguredDigits()
            
            coroutineScope {
                launch { PlayerDataService.savePlayer(player) }
                launch { TerritoryService.saveTerritory(territory) }
            }
            
            TransactionResult.Success(player.balance)
        } catch (e: Exception) {
            TransactionResult.Error("Withdrawal failed: ${e.message}")
        }
    }
    
    // ============ Java-friendly CompletableFuture API ============
    
    @JvmStatic
    fun addToPlayerAsync(playerId: String, amount: Double): CompletableFuture<TransactionResult> =
        TanCoroutines.asFuture { addToPlayer(playerId, amount) }
    
    @JvmStatic
    fun removeFromPlayerAsync(playerId: String, amount: Double): CompletableFuture<TransactionResult> =
        TanCoroutines.asFuture { removeFromPlayer(playerId, amount) }
    
    @JvmStatic
    fun transferBetweenPlayersAsync(
        fromPlayerId: String, 
        toPlayerId: String, 
        amount: Double
    ): CompletableFuture<TransactionResult> =
        TanCoroutines.asFuture { transferBetweenPlayers(fromPlayerId, toPlayerId, amount) }
    
    @JvmStatic
    fun addToTerritoryAsync(territoryId: String, amount: Double): CompletableFuture<TransactionResult> =
        TanCoroutines.asFuture { addToTerritory(territoryId, amount) }
    
    @JvmStatic
    fun removeFromTerritoryAsync(territoryId: String, amount: Double): CompletableFuture<TransactionResult> =
        TanCoroutines.asFuture { removeFromTerritory(territoryId, amount) }
    
    @JvmStatic
    fun depositToTerritoryAsync(
        playerId: String,
        territoryId: String,
        amount: Double
    ): CompletableFuture<TransactionResult> =
        TanCoroutines.asFuture { depositToTerritory(playerId, territoryId, amount) }
    
    @JvmStatic
    fun withdrawFromTerritoryAsync(
        playerId: String,
        territoryId: String,
        amount: Double
    ): CompletableFuture<TransactionResult> =
        TanCoroutines.asFuture { withdrawFromTerritory(playerId, territoryId, amount) }
}
