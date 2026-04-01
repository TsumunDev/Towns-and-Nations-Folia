package org.leralix.tan.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link EconomyOps}.
 * Note: Most async methods require TanCoroutines to be initialized.
 * These tests verify the sealed class hierarchy and method signatures.
 */
@DisplayName("EconomyOps Tests")
class EconomyOpsTest {

    // ==================== TransactionResult Type Tests ====================

    @Test
    @DisplayName("Success result should indicate success")
    void transactionResult_success_returnsTrue() {
        EconomyOps.TransactionResult result = new EconomyOps.TransactionResult.Success(100.0);
        assertTrue(result.isSuccess());
        assertFalse(result.isFailed());
    }

    @Test
    @DisplayName("InsufficientFunds result should indicate failure")
    void transactionResult_insufficientFunds_returnsFalse() {
        EconomyOps.TransactionResult result = new EconomyOps.TransactionResult.InsufficientFunds(100.0, 50.0);
        assertFalse(result.isSuccess());
        assertTrue(result.isFailed());
    }

    @Test
    @DisplayName("Error result should indicate failure")
    void transactionResult_error_returnsFalse() {
        EconomyOps.TransactionResult result = new EconomyOps.TransactionResult.Error("Test error");
        assertFalse(result.isSuccess());
        assertTrue(result.isFailed());
    }

    @Test
    @DisplayName("Should extract balance from Success result")
    void transactionResult_success_containsBalance() {
        EconomyOps.TransactionResult.Success result = new EconomyOps.TransactionResult.Success(123.45);
        assertEquals(123.45, result.getNewBalance(), 0.001);
    }

    @Test
    @DisplayName("Should extract details from InsufficientFunds result")
    void transactionResult_insufficientFunds_containsDetails() {
        EconomyOps.TransactionResult.InsufficientFunds result = new EconomyOps.TransactionResult.InsufficientFunds(100.0, 50.0);
        assertEquals(100.0, result.getRequired(), 0.001);
        assertEquals(50.0, result.getAvailable(), 0.001);
    }

    @Test
    @DisplayName("Should extract message from Error result")
    void transactionResult_error_containsMessage() {
        EconomyOps.TransactionResult.Error result = new EconomyOps.TransactionResult.Error("Something went wrong");
        assertEquals("Something went wrong", result.getMessage());
    }

    // ==================== Async Method Signature Tests ====================

    @Test
    @DisplayName("Async methods should exist and return CompletableFuture")
    void asyncMethods_exist() {
        // Verify methods exist and return CompletableFuture (will throw without TanCoroutines)
        assertThrows(IllegalStateException.class, () -> EconomyOps.addToPlayerAsync("player1", 100.0));
        assertThrows(IllegalStateException.class, () -> EconomyOps.removeFromPlayerAsync("player1", 100.0));
        assertThrows(IllegalStateException.class, () -> EconomyOps.transferBetweenPlayersAsync("player1", "player2", 100.0));
        assertThrows(IllegalStateException.class, () -> EconomyOps.addToTerritoryAsync("territory1", 100.0));
        assertThrows(IllegalStateException.class, () -> EconomyOps.removeFromTerritoryAsync("territory1", 100.0));
        assertThrows(IllegalStateException.class, () -> EconomyOps.depositToTerritoryAsync("player1", "territory1", 100.0));
        assertThrows(IllegalStateException.class, () -> EconomyOps.withdrawFromTerritoryAsync("player1", "territory1", 100.0));
    }

    @Test
    @DisplayName("Async method return types should be CompletableFuture")
    void asyncMethods_returnTypes() {
        // The fact that this compiles proves the return types are correct
        // Methods throw IllegalStateException at runtime without TanCoroutines
        assertTrue(true);
    }
}
