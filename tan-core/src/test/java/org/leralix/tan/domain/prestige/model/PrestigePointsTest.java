package org.leralix.tan.domain.prestige.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PrestigePoints}.
 * <p>
 * Tests the immutable prestige component including earning, spending,
 * transaction history, and validation.
 */
@DisplayName("PrestigePoints Tests")
class PrestigePointsTest {

    @Test
    @DisplayName("Should create empty prestige component")
    void create_ShouldReturnZeroValues() {
        // Act
        PrestigePoints points = PrestigePoints.create();

        // Assert
        assertEquals(0, points.currentBalance());
        assertEquals(0, points.totalEarned());
        assertEquals(0, points.totalSpent());
        assertTrue(points.transactionHistory().isEmpty());
    }

    @Test
    @DisplayName("Should calculate net balance correctly")
    void getNetBalance_EarnedNotSpent_ReturnsCorrect() {
        // Arrange
        PrestigePoints points = new PrestigePoints(500, 1000, 500, List.of());

        // Act
        long net = points.getNetBalance();

        // Assert
        assertEquals(500, net, "Net balance should equal earned - spent");
    }

    @Test
    @DisplayName("Should add prestige points")
    void add_ValidAmount_IncreasesBalance() {
        // Arrange
        PrestigePoints points = PrestigePoints.create();

        // Act
        PrestigePoints updated = points.add(100, PrestigeSource.QUEST_COMPLETION, "Test quest");

        // Assert
        assertEquals(100, updated.currentBalance());
        assertEquals(100, updated.totalEarned());
        assertEquals(0, updated.totalSpent());
        assertEquals(1, updated.transactionHistory().size());
    }

    @Test
    @DisplayName("Should throw when adding non-positive amount")
    void add_NonPositiveAmount_ThrowsException() {
        // Arrange
        PrestigePoints points = PrestigePoints.create();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> points.add(0, PrestigeSource.QUEST_COMPLETION, "test"));
        assertThrows(IllegalArgumentException.class, () -> points.add(-10, PrestigeSource.QUEST_COMPLETION, "test"));
    }

    @Test
    @DisplayName("Should spend prestige points")
    void spend_SufficientAmount_DecreasesBalance() {
        // Arrange
        PrestigePoints points = new PrestigePoints(500, 500, 0, List.of());

        // Act
        PrestigePoints updated = points.spend(200, "Test upgrade");

        // Assert
        assertEquals(300, updated.currentBalance());
        assertEquals(500, updated.totalEarned());
        assertEquals(200, updated.totalSpent());
        assertEquals(1, updated.transactionHistory().size());
    }

    @Test
    @DisplayName("Should throw when spending non-positive amount")
    void spend_NonPositiveAmount_ThrowsException() {
        // Arrange
        PrestigePoints points = PrestigePoints.create();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> points.spend(0, "test"));
        assertThrows(IllegalArgumentException.class, () -> points.spend(-10, "test"));
    }

    @Test
    @DisplayName("Should throw when spending more than balance")
    void spend_InsufficientBalance_ThrowsException() {
        // Arrange
        PrestigePoints points = new PrestigePoints(50, 50, 0, List.of());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> points.spend(100, "Expensive upgrade")
        );
        assertTrue(ex.getMessage().contains("Insufficient prestige balance"));
    }

    @Test
    @DisplayName("Should check affordability correctly")
    void canAfford_VariousAmounts_ReturnsCorrect() {
        // Arrange
        PrestigePoints points = new PrestigePoints(500, 500, 0, List.of());

        // Act & Assert
        assertTrue(points.canAfford(0), "Should afford zero");
        assertTrue(points.canAfford(500), "Should afford exact balance");
        assertTrue(points.canAfford(499), "Should afford less than balance");
        assertFalse(points.canAfford(501), "Should not afford more than balance");
        assertFalse(points.canAfford(1000), "Should not afford much more than balance");
    }

    @Test
    @DisplayName("Should maintain transaction history")
    void addAnd_ShouldRecordTransactions() {
        // Arrange
        PrestigePoints points = PrestigePoints.create();

        // Act
        PrestigePoints afterEarn = points.add(100, PrestigeSource.QUEST_COMPLETION, "Quest 1");
        PrestigePoints afterSpend = afterEarn.spend(50, "Upgrade 1");

        // Assert
        List<PrestigeTransaction> history = afterSpend.transactionHistory();
        assertEquals(2, history.size());

        // Most recent first
        assertTrue(history.get(0).description().contains("Upgrade 1"));
        assertTrue(history.get(1).description().contains("Quest 1"));
    }

    @Test
    @DisplayName("Should limit transaction history size")
    void add_ExceedsMaxSize_TrimsHistory() {
        // Arrange
        PrestigePoints points = PrestigePoints.create();

        // Act - Add more than MAX_HISTORY_SIZE (100) transactions
        PrestigePoints result = points;
        for (int i = 0; i < 150; i++) {
            result = result.add(10, PrestigeSource.LEVEL_MILESTONE, "Milestone " + i);
        }

        // Assert
        assertEquals(100, result.transactionHistory().size(), "History should be trimmed to max size");
    }

    @Test
    @DisplayName("Should get recent transactions limited")
    void getRecentTransactions_HasHistory_ReturnsLimited() {
        // Arrange
        PrestigePoints points = PrestigePoints.create();
        for (int i = 0; i < 10; i++) {
            points = points.add(10, PrestigeSource.LEVEL_MILESTONE, "Milestone " + i);
        }

        // Act
        List<PrestigeTransaction> recent = points.getRecentTransactions(5);

        // Assert
        assertEquals(5, recent.size(), "Should return limited number of transactions");
    }

    @Test
    @DisplayName("Should get recent transactions when history smaller")
    void getRecentTransactions_HistorySmallerThanCount_ReturnsAll() {
        // Arrange
        PrestigePoints points = PrestigePoints.create();
        points = points.add(10, PrestigeSource.QUEST_COMPLETION, "Quest 1");
        points = points.add(20, PrestigeSource.QUEST_COMPLETION, "Quest 2");

        // Act
        List<PrestigeTransaction> recent = points.getRecentTransactions(10);

        // Assert
        assertEquals(2, recent.size(), "Should return all transactions when history is smaller");
    }

    @Test
    @DisplayName("Should throw on negative balance in constructor")
    void constructor_NegativeBalance_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> new PrestigePoints(-1, 0, 0, List.of()));
    }

    @Test
    @DisplayName("Should throw on negative total earned in constructor")
    void constructor_NegativeTotalEarned_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> new PrestigePoints(0, -1, 0, List.of()));
    }

    @Test
    @DisplayName("Should throw on negative total spent in constructor")
    void constructor_NegativeTotalSpent_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> new PrestigePoints(0, 0, -1, List.of()));
    }

    @Test
    @DisplayName("Should make transaction history unmodifiable")
    void constructor_MakesHistoryUnmodifiable() {
        // Arrange
        PrestigePoints points = PrestigePoints.create();

        // Act & Assert
        assertThrows(UnsupportedOperationException.class,
            () -> points.transactionHistory().add(PrestigeTransaction.earn(1, PrestigeSource.QUEST_COMPLETION, "test")));
    }

    @Test
    @DisplayName("Should handle multiple transactions correctly")
    void addAnd_MultipleTransactions_MaintainsCorrectTotals() {
        // Arrange
        PrestigePoints points = PrestigePoints.create();

        // Act
        points = points.add(100, PrestigeSource.QUEST_COMPLETION, "Quest 1");
        points = points.add(50, PrestigeSource.LEVEL_MILESTONE, "Level up");
        points = points.spend(75, "Upgrade 1");
        points = points.add(200, PrestigeSource.TIER_ASCENSION, "Tier up");

        // Assert
        assertEquals(275, points.currentBalance(), "Balance: 100+50-75+200 = 275");
        assertEquals(350, points.totalEarned(), "Total earned: 100+50+200 = 350");
        assertEquals(75, points.totalSpent(), "Total spent: 75");
        assertEquals(4, points.transactionHistory().size(), "4 transactions recorded");
    }

    @Test
    @DisplayName("Should preserve immutability when adding")
    void add_ReturnsNewInstance_DoesNotModifyOriginal() {
        // Arrange
        PrestigePoints original = PrestigePoints.create();
        long originalBalance = original.currentBalance();

        // Act
        PrestigePoints modified = original.add(100, PrestigeSource.QUEST_COMPLETION, "test");

        // Assert
        assertEquals(originalBalance, original.currentBalance(), "Original should be unchanged");
        assertEquals(100, modified.currentBalance(), "Modified should have new value");
    }

    @Test
    @DisplayName("Should preserve immutability when spending")
    void spend_ReturnsNewInstance_DoesNotModifyOriginal() {
        // Arrange
        PrestigePoints original = new PrestigePoints(500, 500, 0, List.of());
        long originalBalance = original.currentBalance();

        // Act
        PrestigePoints modified = original.spend(200, "test");

        // Assert
        assertEquals(originalBalance, original.currentBalance(), "Original should be unchanged");
        assertEquals(300, modified.currentBalance(), "Modified should have new value");
    }
}
