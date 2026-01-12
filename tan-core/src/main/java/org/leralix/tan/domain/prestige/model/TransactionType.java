package org.leralix.tan.domain.prestige.model;

/**
 * Type of prestige transaction.
 */
public enum TransactionType {
    /**
     * Prestige points were earned (positive transaction).
     */
    EARN,

    /**
     * Prestige points were spent (negative transaction).
     */
    SPEND
}
