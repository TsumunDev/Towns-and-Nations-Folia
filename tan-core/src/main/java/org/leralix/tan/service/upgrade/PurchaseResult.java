package org.leralix.tan.service.upgrade;

import org.leralix.tan.domain.upgrade.model.TownUpgrade;

import java.util.Optional;

/**
 * Result of an upgrade purchase attempt.
 *
 * @param success Whether the purchase was successful
 * @param message User-friendly message describing the result
 * @param upgrade The purchased upgrade (empty if failed)
 */
public record PurchaseResult(
        boolean success,
        String message,
        Optional<TownUpgrade> upgrade
) {

    /**
     * Creates a successful purchase result.
     */
    public static PurchaseResult success(TownUpgrade upgrade) {
        return new PurchaseResult(
                true,
                "§aUpgrade purchased successfully!",
                Optional.of(upgrade)
        );
    }

    /**
     * Creates a successful purchase result with custom message.
     */
    public static PurchaseResult success(TownUpgrade upgrade, String message) {
        return new PurchaseResult(
                true,
                message,
                Optional.of(upgrade)
        );
    }

    /**
     * Creates a failed purchase result.
     */
    public static PurchaseResult failure(String reason) {
        return new PurchaseResult(
                false,
                "§c" + reason,
                Optional.empty()
        );
    }

    /**
     * Creates a failed result due to insufficient prestige.
     */
    public static PurchaseResult insufficientFunds(int required, int have) {
        return failure(
                String.format("Insufficient prestige points! Need %d, have %d.", required, have)
        );
    }

    /**
     * Creates a failed result due to unmet requirements.
     */
    public static PurchaseResult requirementsNotMet() {
        return failure("You do not meet the requirements for this upgrade.");
    }
}
