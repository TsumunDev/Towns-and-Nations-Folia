package org.leralix.tan.dataclass.territory.treasury;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TreasuryComponent class.
 * Tests balance management, immutability, and validation.
 */
public class TreasuryComponentTest {

    @Test
    @DisplayName("Default constructor should create component with zero balance")
    void testDefaultConstructor() {
        // Act
        TreasuryComponent treasury = new TreasuryComponent();

        // Assert
        assertEquals(0.0, treasury.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Constructor with balance should store it correctly")
    void testConstructorWithBalance() {
        // Act
        TreasuryComponent treasury = new TreasuryComponent(1000.0);

        // Assert
        assertEquals(1000.0, treasury.getBalance(), 0.001);
    }

    @Test
    @DisplayName("addToBalance with positive amount should return new component with increased balance")
    void testAddToBalancePositive() {
        // Arrange
        TreasuryComponent original = new TreasuryComponent(500.0);

        // Act
        TreasuryComponent modified = original.addToBalance(250.0);

        // Assert
        assertNotSame(original, modified, "Should return new instance");
        assertEquals(500.0, original.getBalance(), 0.001, "Original should be unchanged");
        assertEquals(750.0, modified.getBalance(), 0.001, "Modified should have increased balance");
    }

    @Test
    @DisplayName("addToBalance with negative amount should return new component with decreased balance")
    void testAddToBalanceNegative() {
        // Arrange
        TreasuryComponent original = new TreasuryComponent(500.0);

        // Act
        TreasuryComponent modified = original.addToBalance(-200.0);

        // Assert
        assertEquals(300.0, modified.getBalance(), 0.001);
    }

    @Test
    @DisplayName("withdraw should decrease balance and return new instance")
    void testWithdraw() {
        // Arrange
        TreasuryComponent original = new TreasuryComponent(1000.0);

        // Act
        TreasuryComponent modified = original.withdraw(300.0);

        // Assert
        assertEquals(1000.0, original.getBalance(), 0.001);
        assertEquals(700.0, modified.getBalance(), 0.001);
    }

    @Test
    @DisplayName("withdraw with negative amount should throw exception")
    void testWithdrawNegative() {
        // Arrange
        TreasuryComponent treasury = new TreasuryComponent(1000.0);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            treasury.withdraw(-100.0);
        });
    }

    @Test
    @DisplayName("withdraw more than balance should allow negative balance (overdraft)")
    void testWithdrawOverdraft() {
        // Arrange
        TreasuryComponent original = new TreasuryComponent(100.0);

        // Act
        TreasuryComponent modified = original.withdraw(200.0);

        // Assert
        assertEquals(-100.0, modified.getBalance(), 0.001, "Overdraft should be allowed");
    }

    @Test
    @DisplayName("deposit should increase balance and return new instance")
    void testDeposit() {
        // Arrange
        TreasuryComponent original = new TreasuryComponent(500.0);

        // Act
        TreasuryComponent modified = original.deposit(300.0);

        // Assert
        assertEquals(500.0, original.getBalance(), 0.001);
        assertEquals(800.0, modified.getBalance(), 0.001);
    }

    @Test
    @DisplayName("deposit with negative amount should throw exception")
    void testDepositNegative() {
        // Arrange
        TreasuryComponent treasury = new TreasuryComponent(1000.0);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            treasury.deposit(-100.0);
        });
    }

    @Test
    @DisplayName("hasSufficientFunds should return true when balance >= amount")
    void testHasSufficientFundsTrue() {
        // Arrange
        TreasuryComponent treasury = new TreasuryComponent(1000.0);

        // Act
        boolean result = treasury.hasSufficientFunds(500.0);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("hasSufficientFunds should return false when balance < amount")
    void testHasSufficientFundsFalse() {
        // Arrange
        TreasuryComponent treasury = new TreasuryComponent(1000.0);

        // Act
        boolean result = treasury.hasSufficientFunds(1500.0);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("hasSufficientFunds should return true when balance equals amount")
    void testHasSufficientFundsEqual() {
        // Arrange
        TreasuryComponent treasury = new TreasuryComponent(1000.0);

        // Act
        boolean result = treasury.hasSufficientFunds(1000.0);

        // Assert
        assertTrue(result, "Should have sufficient funds when balance equals amount");
    }

    @Test
    @DisplayName("getDeficit should return 0 when sufficient funds")
    void testGetDeficitSufficient() {
        // Arrange
        TreasuryComponent treasury = new TreasuryComponent(1000.0);

        // Act
        double deficit = treasury.getDeficit(500.0);

        // Assert
        assertEquals(0.0, deficit, 0.001);
    }

    @Test
    @DisplayName("getDeficit should return positive amount when insufficient funds")
    void testGetDeficitInsufficient() {
        // Arrange
        TreasuryComponent treasury = new TreasuryComponent(500.0);

        // Act
        double deficit = treasury.getDeficit(1000.0);

        // Assert
        assertEquals(500.0, deficit, 0.001);
    }

    @Test
    @DisplayName("withBalance should return new instance with specified balance")
    void testWithBalance() {
        // Arrange
        TreasuryComponent original = new TreasuryComponent(500.0);

        // Act
        TreasuryComponent modified = original.withBalance(2000.0);

        // Assert
        assertEquals(500.0, original.getBalance(), 0.001);
        assertEquals(2000.0, modified.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Builder should create component with specified balance")
    void testBuilder() {
        // Act
        TreasuryComponent treasury = TreasuryComponent.builder()
            .balance(5000.0)
            .build();

        // Assert
        assertEquals(5000.0, treasury.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Builder with no values should use default")
    void testBuilderDefault() {
        // Act
        TreasuryComponent treasury = TreasuryComponent.builder().build();

        // Assert
        assertEquals(0.0, treasury.getBalance(), 0.001);
    }

    @Test
    @DisplayName("equals should return true for identical components")
    void testEquals() {
        // Arrange
        TreasuryComponent t1 = new TreasuryComponent(1000.0);
        TreasuryComponent t2 = new TreasuryComponent(1000.0);

        // Assert
        assertEquals(t1, t2);
    }

    @Test
    @DisplayName("equals should return false for different balances")
    void testNotEquals() {
        // Arrange
        TreasuryComponent t1 = new TreasuryComponent(1000.0);
        TreasuryComponent t2 = new TreasuryComponent(2000.0);

        // Assert
        assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void testHashCode() {
        // Arrange
        TreasuryComponent t1 = new TreasuryComponent(1000.0);
        TreasuryComponent t2 = new TreasuryComponent(1000.0);

        // Assert
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    @DisplayName("toString should contain balance")
    void testToString() {
        // Arrange
        TreasuryComponent treasury = new TreasuryComponent(1234.56);

        // Act
        String str = treasury.toString();

        // Assert
        assertTrue(str.contains("1234.56"), "Should contain balance");
    }

    @Test
    @DisplayName("Chained operations should work correctly")
    void testChainedOperations() {
        // Arrange
        TreasuryComponent original = new TreasuryComponent(1000.0);

        // Act
        TreasuryComponent modified = original
            .deposit(500.0)
            .withdraw(200.0)
            .addToBalance(100.0);

        // Assert
        assertEquals(1400.0, modified.getBalance(), 0.001);
        assertEquals(1000.0, original.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Multiple operations can be chained")
    void testMultipleChainedOperations() {
        // Arrange
        TreasuryComponent original = new TreasuryComponent(0.0);

        // Act
        TreasuryComponent result = original
            .deposit(1000.0)
            .withdraw(200.0)
            .withdraw(100.0)
            .deposit(500.0);

        // Assert
        assertEquals(1200.0, result.getBalance(), 0.001);
    }

    @Test
    @DisplayName("getFormattedBalance should return non-null string")
    void testGetFormattedBalance() {
        // Arrange
        TreasuryComponent treasury = new TreasuryComponent(1234.56);

        // Act
        String formatted = treasury.getFormattedBalance();

        // Assert
        assertNotNull(formatted);
        assertTrue(formatted.contains("1234") || formatted.contains("56"));
    }
}
