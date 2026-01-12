package org.leralix.tan.domain.prestige.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable component tracking a town's prestige points.
 * <p>
 * Prestige points are the currency used to purchase town upgrades.
 * This component tracks the current balance, total earned, total spent,
 * and maintains a history of all transactions.
 * </p>
 *
 * @param currentBalance Current prestige points available
 * @param totalEarned Total prestige earned over time
 * @param totalSpent Total prestige spent over time
 * @param transactionHistory History of all transactions (newest first)
 */
public record PrestigePoints(
        long currentBalance,
        long totalEarned,
        long totalSpent,
        List<PrestigeTransaction> transactionHistory
) {
    private static final int MAX_HISTORY_SIZE = 100;

    public PrestigePoints {
        if (currentBalance < 0) {
            throw new IllegalArgumentException("Current balance cannot be negative");
        }
        if (totalEarned < 0) {
            throw new IllegalArgumentException("Total earned cannot be negative");
        }
        if (totalSpent < 0) {
            throw new IllegalArgumentException("Total spent cannot be negative");
        }

        // Ensure transaction history is unmodifiable
        transactionHistory = Collections.unmodifiableList(new ArrayList<>(transactionHistory));
    }

    /**
     * Creates a new prestige component with zero values.
     */
    public static PrestigePoints create() {
        return new PrestigePoints(0, 0, 0, List.of());
    }

    /**
     * Calculates net prestige (earned - spent).
     * Should equal currentBalance.
     */
    public long getNetBalance() {
        return totalEarned - totalSpent;
    }

    /**
     * Creates a new instance with added prestige.
     *
     * @param amount Amount to add
     * @param source Source of the prestige
     * @param description Optional description
     * @return New instance with updated values
     */
    public PrestigePoints add(long amount, PrestigeSource source, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to add must be positive");
        }

        PrestigeTransaction transaction = PrestigeTransaction.earn(amount, source, description);
        List<PrestigeTransaction> newHistory = addToHistory(transaction);

        return new PrestigePoints(
                currentBalance + amount,
                totalEarned + amount,
                totalSpent,
                newHistory
        );
    }

    /**
     * Creates a new instance with spent prestige.
     *
     * @param amount Amount to spend
     * @param description Description of the purchase
     * @return New instance with deducted balance
     * @throws IllegalArgumentException if insufficient balance
     */
    public PrestigePoints spend(long amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to spend must be positive");
        }
        if (amount > currentBalance) {
            throw new IllegalArgumentException(
                    String.format("Insufficient prestige balance: have %d, need %d", currentBalance, amount)
            );
        }

        PrestigeTransaction transaction = PrestigeTransaction.spend(amount, description);
        List<PrestigeTransaction> newHistory = addToHistory(transaction);

        return new PrestigePoints(
                currentBalance - amount,
                totalEarned,
                totalSpent + amount,
                newHistory
        );
    }

    /**
     * Checks if the town can afford to spend the given amount.
     */
    public boolean canAfford(long amount) {
        return currentBalance >= amount;
    }

    /**
     * Gets recent transactions (last N transactions).
     *
     * @param count Maximum number of transactions to return
     * @return List of recent transactions
     */
    public List<PrestigeTransaction> getRecentTransactions(int count) {
        return transactionHistory.stream()
                .limit(count)
                .toList();
    }

    /**
     * Adds a transaction to history while maintaining max size.
     */
    private List<PrestigeTransaction> addToHistory(PrestigeTransaction transaction) {
        List<PrestigeTransaction> newHistory = new ArrayList<>(transactionHistory);
        newHistory.add(0, transaction); // Add to front (newest first)

        // Trim to max size
        if (newHistory.size() > MAX_HISTORY_SIZE) {
            return newHistory.subList(0, MAX_HISTORY_SIZE);
        }

        return newHistory;
    }
}
