package org.leralix.tan.domain.prestige.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PrestigeTransaction}.
 * <p>
 * Tests the transaction record including factory methods,
 * validation, and utility methods.
 */
@DisplayName("PrestigeTransaction Tests")
class PrestigeTransactionTest {

    @Test
    @DisplayName("Should create earn transaction correctly")
    void earn_ValidAmount_CreatesTransaction() {
        // Act
        PrestigeTransaction transaction = PrestigeTransaction.earn(100, PrestigeSource.QUEST_COMPLETION, "Test quest");

        // Assert
        assertEquals(100, transaction.amount());
        assertEquals(PrestigeSource.QUEST_COMPLETION, transaction.source());
        assertEquals(TransactionType.EARN, transaction.type());
        assertEquals("Test quest", transaction.description());
        assertTrue(transaction.timestamp() > 0, "Timestamp should be positive");
    }

    @Test
    @DisplayName("Should create spend transaction correctly")
    void spend_ValidAmount_CreatesTransaction() {
        // Act
        PrestigeTransaction transaction = PrestigeTransaction.spend(50, "Test upgrade");

        // Assert
        assertEquals(-50, transaction.amount(), "Spend transactions have negative amount");
        assertEquals(PrestigeSource.SHOP_PURCHASE, transaction.source());
        assertEquals(TransactionType.SPEND, transaction.type());
        assertEquals("Test upgrade", transaction.description());
    }

    @Test
    @DisplayName("Should throw on zero timestamp")
    void constructor_ZeroTimestamp_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> new PrestigeTransaction(0, 100, PrestigeSource.QUEST_COMPLETION, TransactionType.EARN, "test"));
    }

    @Test
    @DisplayName("Should throw on negative timestamp")
    void constructor_NegativeTimestamp_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> new PrestigeTransaction(-1, 100, PrestigeSource.QUEST_COMPLETION, TransactionType.EARN, "test"));
    }

    @Test
    @DisplayName("Should throw on null source")
    void constructor_NullSource_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
            () -> new PrestigeTransaction(System.currentTimeMillis(), 100, null, TransactionType.EARN, "test"));
    }

    @Test
    @DisplayName("Should throw on null type")
    void constructor_NullType_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
            () -> new PrestigeTransaction(System.currentTimeMillis(), 100, PrestigeSource.QUEST_COMPLETION, null, "test"));
    }

    @Test
    @DisplayName("Should throw on zero or negative amount for EARN")
    void constructor_ZeroAmountForEarn_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> new PrestigeTransaction(System.currentTimeMillis(), 0, PrestigeSource.QUEST_COMPLETION, TransactionType.EARN, "test"));
        assertThrows(IllegalArgumentException.class,
            () -> new PrestigeTransaction(System.currentTimeMillis(), -10, PrestigeSource.QUEST_COMPLETION, TransactionType.EARN, "test"));
    }

    @Test
    @DisplayName("Should throw on zero or positive amount for SPEND")
    void constructor_PositiveAmountForSpend_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> new PrestigeTransaction(System.currentTimeMillis(), 0, PrestigeSource.SHOP_PURCHASE, TransactionType.SPEND, "test"));
        assertThrows(IllegalArgumentException.class,
            () -> new PrestigeTransaction(System.currentTimeMillis(), 10, PrestigeSource.SHOP_PURCHASE, TransactionType.SPEND, "test"));
    }

    @Test
    @DisplayName("Should get absolute amount for earn transaction")
    void getAbsoluteAmount_EarnTransaction_ReturnsPositive() {
        // Arrange
        PrestigeTransaction transaction = PrestigeTransaction.earn(100, PrestigeSource.QUEST_COMPLETION, "test");

        // Act
        long absolute = transaction.getAbsoluteAmount();

        // Assert
        assertEquals(100, absolute);
    }

    @Test
    @DisplayName("Should get absolute amount for spend transaction")
    void getAbsoluteAmount_SpendTransaction_ReturnsPositive() {
        // Arrange
        PrestigeTransaction transaction = PrestigeTransaction.spend(50, "test");

        // Act
        long absolute = transaction.getAbsoluteAmount();

        // Assert
        assertEquals(50, absolute);
    }

    @Test
    @DisplayName("Should handle null description")
    void earn_NullDescription_HandlesGracefully() {
        // Act
        PrestigeTransaction transaction = PrestigeTransaction.earn(100, PrestigeSource.QUEST_COMPLETION, null);

        // Assert
        assertNull(transaction.description());
    }

    @Test
    @DisplayName("Should handle empty description")
    void earn_EmptyDescription_HandlesGracefully() {
        // Act
        PrestigeTransaction transaction = PrestigeTransaction.earn(100, PrestigeSource.QUEST_COMPLETION, "");

        // Assert
        assertEquals("", transaction.description());
    }

    @Test
    @DisplayName("Should create transaction with all sources")
    void earn_AllSources_CreatesValidTransactions() {
        // Act & Assert
        assertNotNull(PrestigeTransaction.earn(1, PrestigeSource.QUEST_COMPLETION, "test"));
        assertNotNull(PrestigeTransaction.earn(1, PrestigeSource.LEVEL_MILESTONE, "test"));
        assertNotNull(PrestigeTransaction.earn(1, PrestigeSource.TIER_ASCENSION, "test"));
        assertNotNull(PrestigeTransaction.earn(1, PrestigeSource.ADMIN_COMMAND, "test"));
        assertNotNull(PrestigeTransaction.earn(1, PrestigeSource.WAR_VICTORY, "test"));
        assertNotNull(PrestigeTransaction.earn(1, PrestigeSource.LANDMARK_CAPTURE, "test"));
    }

    @Test
    @DisplayName("Should record creation time accurately")
    void earn_Timestamp_IsRecent() {
        // Arrange
        long before = System.currentTimeMillis();

        // Act
        PrestigeTransaction transaction = PrestigeTransaction.earn(100, PrestigeSource.QUEST_COMPLETION, "test");

        // Assert
        long after = System.currentTimeMillis();
        assertTrue(transaction.timestamp() >= before && transaction.timestamp() <= after,
            "Timestamp should be between before and after");
    }

    @Test
    @DisplayName("Should equal identical transactions")
    void equals_SameTransaction_ReturnsTrue() {
        // Arrange
        long timestamp = System.currentTimeMillis();
        PrestigeTransaction t1 = new PrestigeTransaction(timestamp, 100, PrestigeSource.QUEST_COMPLETION, TransactionType.EARN, "test");
        PrestigeTransaction t2 = new PrestigeTransaction(timestamp, 100, PrestigeSource.QUEST_COMPLETION, TransactionType.EARN, "test");

        // Act & Assert
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    @DisplayName("Should not equal different transactions")
    void equals_DifferentTransaction_ReturnsFalse() {
        // Arrange
        PrestigeTransaction t1 = PrestigeTransaction.earn(100, PrestigeSource.QUEST_COMPLETION, "test");
        PrestigeTransaction t2 = PrestigeTransaction.earn(200, PrestigeSource.QUEST_COMPLETION, "test");

        // Act & Assert
        assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("Should format toString correctly")
    void toString_ContainsKeyFields() {
        // Arrange
        PrestigeTransaction transaction = PrestigeTransaction.earn(100, PrestigeSource.QUEST_COMPLETION, "Test Quest");

        // Act
        String str = transaction.toString();

        // Assert
        assertNotNull(str);
        assertTrue(str.contains("100") || str.contains("amount") || str.contains("QUEST_COMPLETION"));
    }
}
