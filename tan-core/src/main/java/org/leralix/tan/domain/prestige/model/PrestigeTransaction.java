package org.leralix.tan.domain.prestige.model;

import java.util.Objects;

/**
 * Represents a single prestige transaction in a town's history.
 * <p>
 * Transactions are immutable records of when and how prestige
 * points were earned or spent.
 * </p>
 *
 * @param timestamp Unix timestamp when the transaction occurred
 * @param amount Amount of prestige (positive for earned, negative for spent)
 * @param source Source of the prestige
 * @param type Transaction type (EARN or SPEND)
 * @param description Optional description (e.g., "Completed: Wheat Harvest I")
 */
public record PrestigeTransaction(
        long timestamp,
        long amount,
        PrestigeSource source,
        TransactionType type,
        String description
) {
    public PrestigeTransaction {
        if (timestamp <= 0) {
            throw new IllegalArgumentException("Timestamp must be positive");
        }
        Objects.requireNonNull(source, "Prestige source cannot be null");
        Objects.requireNonNull(type, "Transaction type cannot be null");

        // Validate amount matches type
        if (type == TransactionType.EARN && amount <= 0) {
            throw new IllegalArgumentException("EARN transactions must have positive amount");
        }
        if (type == TransactionType.SPEND && amount >= 0) {
            throw new IllegalArgumentException("SPEND transactions must have negative amount");
        }
    }

    /**
     * Creates a prestige transaction for earning points.
     */
    public static PrestigeTransaction earn(long amount, PrestigeSource source, String description) {
        return new PrestigeTransaction(
                System.currentTimeMillis(),
                amount,
                source,
                TransactionType.EARN,
                description
        );
    }

    /**
     * Creates a prestige transaction for spending points.
     */
    public static PrestigeTransaction spend(long amount, String description) {
        return new PrestigeTransaction(
                System.currentTimeMillis(),
                -amount,
                PrestigeSource.SHOP_PURCHASE,
                TransactionType.SPEND,
                description
        );
    }

    /**
     * Gets the absolute value of the amount (always positive).
     */
    public long getAbsoluteAmount() {
        return Math.abs(amount);
    }
}
