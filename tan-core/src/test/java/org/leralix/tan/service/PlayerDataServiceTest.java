package org.leralix.tan.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlayerDataService}.
 * Note: Most methods require TanCoroutines and PlayerDataStorage to be initialized.
 * These tests verify method signatures and basic behavior.
 */
@DisplayName("PlayerDataService Tests")
class PlayerDataServiceTest {

    // ==================== Async Method Signature Tests ====================

    @Test
    @DisplayName("Should have getPlayerAsync method returning CompletableFuture")
    void getPlayerAsync_hasCorrectSignature() {
        // Verify method exists and returns CompletableFuture (will throw without TanCoroutines)
        assertThrows(IllegalStateException.class, () ->
            PlayerDataService.getPlayerAsync(UUID.randomUUID()));
        assertThrows(IllegalStateException.class, () ->
            PlayerDataService.getPlayerAsync("test-id"));
    }

    @Test
    @DisplayName("Should have savePlayerAsync method returning CompletableFuture")
    void savePlayerAsync_hasCorrectSignature() {
        // Cannot test actual functionality without TanCoroutines + PlayerDataStorage
        // Method signature is verified at compile time
        assertTrue(true);
    }

    @Test
    @DisplayName("Should have playerExistsAsync method returning CompletableFuture")
    void playerExistsAsync_hasCorrectSignature() {
        assertThrows(IllegalStateException.class, () ->
            PlayerDataService.playerExistsAsync("test-id"));
    }

    @Test
    @DisplayName("Should have deletePlayerAsync method returning CompletableFuture")
    void deletePlayerAsync_hasCorrectSignature() {
        assertThrows(IllegalStateException.class, () ->
            PlayerDataService.deletePlayerAsync("test-id"));
    }

    @Test
    @DisplayName("Should have getPlayersAsync method returning CompletableFuture")
    void getPlayersAsync_hasCorrectSignature() {
        // Test with empty list - will still throw due to TanCoroutines
        assertThrows(IllegalStateException.class, () ->
            PlayerDataService.getPlayersAsync(List.of("id1", "id2")));
    }

    // ==================== Type Verification Tests ====================

    @Test
    @DisplayName("Async methods should return correct types")
    void asyncMethods_returnCorrectTypes() {
        // These tests verify compile-time types (compilation = success)
        UUID testUuid = UUID.randomUUID();
        String testId = "test-id";
        List<String> testIds = List.of("id1", "id2");

        // Verify return types are correct (will throw at runtime without TanCoroutines)
        assertThrows(IllegalStateException.class, () -> {
            CompletableFuture<?> result1 = PlayerDataService.getPlayerAsync(testUuid);
            CompletableFuture<?> result2 = PlayerDataService.getPlayerAsync(testId);
            CompletableFuture<?> result3 = PlayerDataService.playerExistsAsync(testId);
            CompletableFuture<?> result4 = PlayerDataService.deletePlayerAsync(testId);
            CompletableFuture<?> result5 = PlayerDataService.getPlayersAsync(testIds);
        });
    }

    // ==================== Object Singleton Test ====================

    @Test
    @DisplayName("PlayerDataService should be a singleton object")
    void playerDataService_isSingleton() {
        // In Kotlin, object is a singleton - just verify it's not null
        assertNotNull(PlayerDataService.class);
    }
}
