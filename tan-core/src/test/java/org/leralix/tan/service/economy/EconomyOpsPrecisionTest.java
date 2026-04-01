package org.leralix.tan.service.economy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.leralix.tan.service.EconomyOps;
import org.leralix.tan.service.PlayerDataService;
import org.leralix.tan.testutils.AbstractPluginTest;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Precision and decimal accuracy tests for economy operations.
 * <p>
 * Financial calculations require exact precision to avoid:
 * <ul>
 *   <li>Money creation/destruction through rounding errors</li>
 *   <li>Accumulated floating-point errors</li>
 *   <li>Unexpected behavior with edge case values</li>
 * </ul>
 * <p>
 * These tests verify:
 * <ul>
 *   <li>Correct rounding to configured decimal places</li>
 *   <li>Handling of very small and very large values</li>
 *   <li>Precision preservation across multiple operations</li>
 *   <li>Edge cases: zero, infinity, NaN, negative values</li>
 * </ul>
 */
@DisplayName("EconomyOps Precision Tests")
class EconomyOpsPrecisionTest extends AbstractPluginTest {

    private int originalDigits;

    @BeforeEach
    void setUp() {
        super.setUpPlugin();
        originalDigits = org.leralix.tan.utils.NumberUtils.getDigits();
        setTestDigits(2); // Default to 2 decimal places
    }

    @AfterEach
    void tearDown() {
        // Restore original digits
        setTestDigits(originalDigits);
        super.tearDownPlugin();
    }

    private void setTestDigits(int digits) {
        try {
            Field field = org.leralix.tan.utils.NumberUtils.class.getDeclaredField("nbDigits");
            field.setAccessible(true);
            field.set(null, digits);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set test digits", e);
        }
    }

    // ==================== Basic Precision Tests ====================

    @Test
    @DisplayName("Addition should round to configured digits")
    void addToPlayer_withFractionalAmount_roundsCorrectly() throws Exception {
        // Arrange
        String playerId = "precision_add_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 100.0);

        // Act
        EconomyOps.TransactionResult result = addToPlayerSync(playerId, 10.555);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(110.56, getPlayerBalanceSync(playerId), 0.001,
            "Should round to 2 decimal places");
    }

    @Test
    @DisplayName("Subtraction should round to configured digits")
    void removeFromPlayer_withFractionalAmount_roundsCorrectly() throws Exception {
        // Arrange
        String playerId = "precision_remove_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 100.00);

        // Act
        EconomyOps.TransactionResult result = removeFromPlayerSync(playerId, 10.555);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(89.44, getPlayerBalanceSync(playerId), 0.001,
            "Should round to 2 decimal places");
    }

    @Test
    @DisplayName("Transfer should round both sender and receiver balances")
    void transferBetweenPlayers_roundsBothBalances() throws Exception {
        // Arrange
        String player1 = "precision_sender";
        String player2 = "precision_receiver";
        createTestPlayer(player1);
        createTestPlayer(player2);
        addToPlayerSync(player1, 100.0);
        addToPlayerSync(player2, 50.0);

        // Act
        EconomyOps.TransactionResult result = transferSync(player1, player2, 10.555);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(89.44, getPlayerBalanceSync(player1), 0.001);
        assertEquals(60.56, getPlayerBalanceSync(player2), 0.001);
    }

    // ==================== Accumulated Precision Tests ====================

    @Test
    @DisplayName("Multiple small additions should not accumulate error")
    void multipleSmallAdditions_noAccumulatedError() throws Exception {
        // Arrange
        String playerId = "accumulation_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 0.0);

        // Act - Add 0.01 one hundred times
        for (int i = 0; i < 100; i++) {
            addToPlayerSync(playerId, 0.01);
        }

        // Assert
        double balance = getPlayerBalanceSync(playerId);
        assertEquals(1.0, balance, 0.001,
            "100 * 0.01 should equal 1.0 without accumulated error");
    }

    @Test
    @DisplayName("Multiple fractional operations should maintain precision")
    void repeatedFractionalOperations_maintainPrecision() throws Exception {
        // Arrange
        String playerId = "repeat_precision_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 100.0);

        // Act - Add and subtract fractional amounts repeatedly
        for (int i = 0; i < 50; i++) {
            addToPlayerSync(playerId, 0.33);
            removeFromPlayerSync(playerId, 0.33);
        }

        // Assert
        double balance = getPlayerBalanceSync(playerId);
        assertEquals(100.0, balance, 0.01,
            "Repeated add/remove of same amount should return to original");
    }

    @Test
    @DisplayName("Precision test with 3 decimal places")
    void operationsWith3DecimalPlaces_correctRounding() throws Exception {
        // Arrange
        setTestDigits(3);
        String playerId = "precision_3_digits";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 100.0);

        // Act
        addToPlayerSync(playerId, 10.5555);

        // Assert
        double balance = getPlayerBalanceSync(playerId);
        assertEquals(110.556, balance, 0.0001,
            "Should round to 3 decimal places");
    }

    @Test
    @DisplayName("Precision test with 0 decimal places (integers only)")
    void operationsWith0DecimalPlaces_roundsToInteger() throws Exception {
        // Arrange
        setTestDigits(0);
        String playerId = "precision_0_digits";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 100.0);

        // Act
        addToPlayerSync(playerId, 10.99);

        // Assert
        double balance = getPlayerBalanceSync(playerId);
        assertEquals(111.0, balance, 0.001,
            "Should round to nearest integer");
    }

    // ==================== Edge Case Tests ====================

    @ParameterizedTest
    @CsvSource({
        "0.001, 0.0",
        "0.004, 0.0",
        "0.005, 0.01",
        "0.009, 0.01",
        "-0.001, 0.0",
        "-0.005, -0.01",
        "-0.009, -0.01"
    })
    @DisplayName("Should handle rounding edge cases correctly")
    void roundingEdgeCases_correctBehavior(String input, String expected) throws Exception {
        // Arrange
        String playerId = "edge_case_player";
        createTestPlayer(playerId);

        // Act
        EconomyOps.TransactionResult result = addToPlayerSync(playerId, Double.parseDouble(input));

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(Double.parseDouble(expected), getPlayerBalanceSync(playerId), 0.0001);
    }

    @Test
    @DisplayName("Should handle zero value operations")
    void zeroValueOperations_handledCorrectly() throws Exception {
        // Arrange
        String playerId = "zero_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 100.0);

        // Act
        EconomyOps.TransactionResult result = addToPlayerSync(playerId, 0.0);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(100.0, getPlayerBalanceSync(playerId), 0.001);
    }

    @Test
    @DisplayName("Should handle very small amounts without precision loss")
    void verySmallAmounts_noPrecisionLoss() throws Exception {
        // Arrange
        String playerId = "small_amount_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 1000.0);

        // Act
        double totalAdded = 0.0;
        for (int i = 0; i < 1000; i++) {
            double amount = 0.001;
            addToPlayerSync(playerId, amount);
            totalAdded += amount;
        }

        // Assert
        double balance = getPlayerBalanceSync(playerId);
        double expected = 1000.0 + Math.round(totalAdded * 100.0) / 100.0;
        assertEquals(expected, balance, 0.01,
            "Should handle very small amounts correctly");
    }

    @Test
    @DisplayName("Should handle very large amounts")
    void veryLargeAmounts_handledCorrectly() throws Exception {
        // Arrange
        String playerId = "large_amount_player";
        createTestPlayer(playerId);

        // Act
        double largeAmount = 10_000_000.0;
        EconomyOps.TransactionResult result = addToPlayerSync(playerId, largeAmount);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(largeAmount, getPlayerBalanceSync(playerId), 0.001);
    }

    @Test
    @DisplayName("Should handle max double value without overflow")
    void maxDoubleValue_noOverflow() throws Exception {
        // Arrange
        String playerId = "max_double_player";
        createTestPlayer(playerId);

        // Act - This should either succeed or fail gracefully
        double largeAmount = Double.MAX_VALUE / 2;
        EconomyOps.TransactionResult result = addToPlayerSync(playerId, largeAmount);

        // Assert - Just verify it doesn't crash
        assertNotNull(result);
        assertFalse(Double.isInfinite(getPlayerBalanceSync(playerId)),
            "Balance should not become infinite");
    }

    // ==================== Special Value Tests ====================

    @Test
    @DisplayName("Should reject NaN values")
    void nanValue_rejected() throws Exception {
        // Arrange
        String playerId = "nan_player";
        createTestPlayer(playerId);

        // Act & Assert
        assertThrows(Exception.class, () -> addToPlayerSync(playerId, Double.NaN),
            "NaN should be rejected");
    }

    @Test
    @DisplayName("Should reject positive infinity")
    void positiveInfinity_rejected() throws Exception {
        // Arrange
        String playerId = "inf_player";
        createTestPlayer(playerId);

        // Act & Assert
        assertThrows(Exception.class, () -> addToPlayerSync(playerId, Double.POSITIVE_INFINITY),
            "Positive infinity should be rejected");
    }

    @Test
    @DisplayName("Should reject negative infinity")
    void negativeInfinity_rejected() throws Exception {
        // Arrange
        String playerId = "neg_inf_player";
        createTestPlayer(playerId);

        // Act & Assert
        assertThrows(Exception.class, () -> addToPlayerSync(playerId, Double.NEGATIVE_INFINITY),
            "Negative infinity should be rejected");
    }

    @Test
    @DisplayName("Should handle negative amounts in subtraction")
    void negativeAmountInSubtraction_handledCorrectly() throws Exception {
        // Arrange
        String playerId = "negative_subtract_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 100.0);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> removeFromPlayerSync(playerId, -10.0),
            "Negative amount should be rejected in subtraction");
    }

    @Test
    @DisplayName("Should handle negative amounts in addition")
    void negativeAmountInAddition_rejected() throws Exception {
        // Arrange
        String playerId = "negative_add_player";
        createTestPlayer(playerId);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> addToPlayerSync(playerId, -10.0),
            "Negative amount should be rejected in addition");
    }

    // ==================== Transfer Precision Tests ====================

    @Test
    @DisplayName("Transfer with fractional amount should preserve total balance")
    void transferFractionalAmount_preservesTotal() throws Exception {
        // Arrange
        String player1 = "fraction_transfer_1";
        String player2 = "fraction_transfer_2";
        createTestPlayer(player1);
        createTestPlayer(player2);
        addToPlayerSync(player1, 100.0);
        addToPlayerSync(player2, 50.0);
        double totalBalance = 150.0;

        // Act
        transferSync(player1, player2, 33.333);

        // Assert
        double newTotal = getPlayerBalanceSync(player1) + getPlayerBalanceSync(player2);
        assertEquals(totalBalance, newTotal, 0.01,
            "Total balance should be preserved with fractional transfer");
    }

    @Test
    @DisplayName("Multiple fractional transfers should not leak money")
    void multipleFractionalTransfers_noLeak() throws Exception {
        // Arrange
        String player1 = "leak_test_1";
        String player2 = "leak_test_2";
        createTestPlayer(player1);
        createTestPlayer(player2);
        addToPlayerSync(player1, 100.0);
        addToPlayerSync(player2, 100.0);
        double totalBalance = 200.0;

        // Act - Multiple transfers with fractional amounts
        for (int i = 0; i < 10; i++) {
            transferSync(player1, player2, 1.11);
            transferSync(player2, player1, 0.99);
        }

        // Assert
        double newTotal = getPlayerBalanceSync(player1) + getPlayerBalanceSync(player2);
        assertEquals(totalBalance, newTotal, 0.01,
            "No money should be leaked through rounding in multiple transfers");
    }

    // ==================== Boundary Value Tests ====================

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.01, 0.001, 0.0001, 0.00001})
    @DisplayName("Should handle minimum positive values")
    void minimumPositiveValues_handledCorrectly(double amount) throws Exception {
        // Arrange
        String playerId = "min_positive_player";
        createTestPlayer(playerId);

        // Act
        EconomyOps.TransactionResult result = addToPlayerSync(playerId, amount);

        // Assert
        assertTrue(result.isSuccess());
        double balance = getPlayerBalanceSync(playerId);
        assertTrue(balance >= 0.0, "Balance should be non-negative");
    }

    @Test
    @DisplayName("Should handle underflow to zero correctly")
    void underflowToZero_handledCorrectly() throws Exception {
        // Arrange
        String playerId = "underflow_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 0.001);

        // Act - With 2 decimal places, this should round to 0.00
        double balance = getPlayerBalanceSync(playerId);

        // Assert
        assertEquals(0.0, balance, 0.001,
            "Very small amounts should round to zero");
    }

    @Test
    @DisplayName("Should handle exact boundary values")
    void exactBoundaryValues_roundedCorrectly() throws Exception {
        // Arrange
        String playerId = "boundary_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 0.0);

        // Act - Add amounts exactly at rounding boundary
        addToPlayerSync(playerId, 1.004); // Should round to 1.00
        addToPlayerSync(playerId, 1.005); // Should round to 1.01

        // Assert
        double balance = getPlayerBalanceSync(playerId);
        assertEquals(2.01, balance, 0.001,
            "Boundary values should round correctly (banker's rounding)");
    }

    // ==================== Consistency Tests ====================

    @Test
    @DisplayName("Rounding should be consistent across operations")
    void roundingConsistent_acrossOperations() throws Exception {
        // Arrange
        String playerId = "consistent_player";
        createTestPlayer(playerId);

        // Act - Add same fractional amount multiple times
        double amount = 1.555;
        for (int i = 0; i < 10; i++) {
            addToPlayerSync(playerId, amount);
        }

        // Assert - Each should round to 1.56
        double balance = getPlayerBalanceSync(playerId);
        assertEquals(15.6, balance, 0.001,
            "Each operation should round consistently");
    }

    @Test
    @DisplayName("Precision should not depend on operation order")
    void precisionIndependentOfOrder_sameResult() throws Exception {
        // Arrange
        String player1 = "order_test_1";
        String player2 = "order_test_2";
        createTestPlayer(player1);
        createTestPlayer(player2);

        // Act - Different order of operations
        addToPlayerSync(player1, 100.0);
        addToPlayerSync(player1, 50.55);
        removeFromPlayerSync(player1, 25.25);

        addToPlayerSync(player2, 50.55);
        addToPlayerSync(player2, 100.0);
        removeFromPlayerSync(player2, 25.25);

        // Assert - Both should have same balance
        double balance1 = getPlayerBalanceSync(player1);
        double balance2 = getPlayerBalanceSync(player2);
        assertEquals(balance1, balance2, 0.001,
            "Operation order should not affect final balance");
    }

    // ==================== Overflow/Underflow Tests ====================

    @Test
    @DisplayName("Should detect potential overflow in large addition")
    void largeAddition_detectsOverflow() throws Exception {
        // Arrange
        String playerId = "overflow_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, Double.MAX_VALUE / 2);

        // Act
        EconomyOps.TransactionResult result = addToPlayerSync(playerId, Double.MAX_VALUE / 2);

        // Assert - Should handle gracefully
        assertNotNull(result);
        double balance = getPlayerBalanceSync(playerId);
        assertFalse(Double.isInfinite(balance), "Balance should not overflow to infinity");
    }

    @Test
    @DisplayName("Should handle subtraction that rounds to zero")
    void subtractionRoundingToZero_handledCorrectly() throws Exception {
        // Arrange
        String playerId = "round_to_zero_player";
        createTestPlayer(playerId);
        addToPlayerSync(playerId, 0.004); // Rounds to 0.00

        // Act
        EconomyOps.TransactionResult result = removeFromPlayerSync(playerId, 0.003);

        // Assert
        double balance = getPlayerBalanceSync(playerId);
        assertTrue(balance >= 0.0, "Balance should remain non-negative");
    }

    // ==================== Helper Methods ====================

    private EconomyOps.TransactionResult addToPlayerSync(String playerId, double amount) throws Exception {
        return EconomyOps.addToPlayerAsync(playerId, amount).get();
    }

    private EconomyOps.TransactionResult removeFromPlayerSync(String playerId, double amount) throws Exception {
        return EconomyOps.removeFromPlayerAsync(playerId, amount).get();
    }

    private EconomyOps.TransactionResult transferSync(String from, String to, double amount) throws Exception {
        return EconomyOps.transferBetweenPlayersAsync(from, to, amount).get();
    }

    private double getPlayerBalanceSync(String playerId) {
        var player = PlayerDataService.getPlayer(playerId);
        return player != null ? player.getBalance() : 0.0;
    }
}
