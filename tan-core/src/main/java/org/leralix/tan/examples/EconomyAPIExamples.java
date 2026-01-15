package org.leralix.tan.examples;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.economy.AbstractTanEcon;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.economy.TanEconomyExternal;
import org.leralix.tan.economy.TanEconomyStandalone;
import org.leralix.tan.service.AsyncEconomyService;
import org.leralix.tan.storage.stored.PlayerDataStorage;

import net.milkbowl.vault.economy.Economy;

/**
 * Code examples demonstrating usage of Economy APIs.
 *
 * <p>This class provides practical examples for common economy operations including
 * balance checking, money transfers, transaction management, and Vault integration.</p>
 *
 * <h2>Table of Contents:</h2>
 * <ul>
 *   <li>{@link #checkBalanceExample} - Checking player balance</li>
 *   <li>{@link #transferMoneyExample} - Transferring money between players</li>
 *   <li>{@link #customTransactionExample} - Creating custom transactions</li>
 *   <li>{@link #vaultIntegrationExample} - Integrating with Vault economy</li>
 *   <li>{@link #externalEconomyExample} - Using external economy plugins</li>
 *   <li>{@link #asyncEconomyExample} - Async economy operations</li>
 *   <li>{@link #townTreasuryExample} - Managing town treasury</li>
 * </ul>
 *
 * @since 0.16.0
 */
public class EconomyAPIExamples {

    /**
     * Example: Checking player balance.
     *
     * <p>This example demonstrates how to check a player's balance using the EconomyUtil facade.
     * The balance is retrieved synchronously for simple cases.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link EconomyUtil#getBalance(Player)} for synchronous balance checks</li>
     *   <li>Use {@link EconomyUtil#getBalance(OfflinePlayer)} for offline players</li>
     *   <li>Use {@link EconomyUtil#getBalance(ITanPlayer)} for ITanPlayer objects</li>
     *   <li>Balances are stored as double precision floating point numbers</li>
     * </ul>
     *
     * @param player The player to check balance for
     * @return The player's current balance
     */
    public static double checkBalanceExample(Player player) {
        // Method 1: Check balance using Player object
        double balance1 = EconomyUtil.getBalance(player);

        // Method 2: Check balance using ITanPlayer
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance()
            .getSync(player.getUniqueId().toString());
        double balance2 = EconomyUtil.getBalance(tanPlayer);

        // Both methods return the same value
        return balance1;
    }

    /**
     * Example: Transferring money between players.
     *
     * <p>This example shows how to safely transfer money from one player to another
     * with balance validation and atomic operations.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Always check balance before withdrawing using {@link AbstractTanEcon#has}</li>
     *   <li>Use {@link EconomyUtil#removeFromBalance} to deduct money</li>
     *   <li>Use {@link EconomyUtil#addFromBalance} to add money</li>
     *   <li>Transactions should be wrapped in validation logic</li>
     * </ul>
     *
     * @param fromPlayer The player sending money
     * @param toPlayer The player receiving money
     * @param amount The amount to transfer
     * @return true if transfer succeeded, false otherwise
     */
    public static boolean transferMoneyExample(Player fromPlayer, Player toPlayer, double amount) {
        // Get ITanPlayer objects
        ITanPlayer fromTanPlayer = PlayerDataStorage.getInstance()
            .getSync(fromPlayer.getUniqueId().toString());
        ITanPlayer toTanPlayer = PlayerDataStorage.getInstance()
            .getSync(toPlayer.getUniqueId().toString());

        // Check if sender has sufficient balance
        if (EconomyUtil.getBalance(fromTanPlayer) < amount) {
            fromPlayer.sendMessage("Insufficient funds");
            return false;
        }

        // Perform transfer
        EconomyUtil.removeFromBalance(fromTanPlayer, amount);
        EconomyUtil.addFromBalance(toTanPlayer, amount);

        // Notify players
        fromPlayer.sendMessage("Sent " + amount + " to " + toPlayer.getName());
        toPlayer.sendMessage("Received " + amount + " from " + fromPlayer.getName());

        return true;
    }

    /**
     * Example: Creating a custom transaction.
     *
     * <p>This example demonstrates how to create a custom transaction with validation,
     * logging, and rollback capabilities. Custom transactions are useful for shops,
     * payments, and other economy interactions.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Validate all conditions before modifying balances</li>
     *   <li>Store original balances for potential rollback</li>
     *   <li>Use {@link EconomyUtil#formatMoney(double)} for display</li>
     *   <li>Implement proper error handling and user feedback</li>
     * </ul>
     *
     * @param player The player making the purchase
     * @param cost The cost of the transaction
     * @param description Description of the transaction
     * @return true if transaction succeeded, false otherwise
     */
    public static boolean customTransactionExample(Player player, double cost, String description) {
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance()
            .getSync(player.getUniqueId().toString());

        // Validate balance
        double currentBalance = EconomyUtil.getBalance(tanPlayer);
        if (currentBalance < cost) {
            String formattedCost = EconomyUtil.formatMoney(cost);
            String formattedBalance = EconomyUtil.formatMoney(currentBalance);
            player.sendMessage("Insufficient funds. Need: " + formattedCost +
                               ", Have: " + formattedBalance);
            return false;
        }

        // Store original balance for potential rollback
        double originalBalance = currentBalance;

        try {
            // Perform transaction
            EconomyUtil.removeFromBalance(tanPlayer, cost);

            // Verify transaction succeeded
            double newBalance = EconomyUtil.getBalance(tanPlayer);
            if (Math.abs(newBalance - (originalBalance - cost)) > 0.01) {
                throw new IllegalStateException("Transaction verification failed");
            }

            // Success notification
            player.sendMessage("Transaction successful: " + description);
            player.sendMessage("Paid: " + EconomyUtil.formatMoney(cost));
            player.sendMessage("Remaining balance: " + EconomyUtil.formatMoney(newBalance));

            return true;

        } catch (Exception e) {
            // Rollback on error
            tanPlayer.addToBalance(originalBalance - EconomyUtil.getBalance(tanPlayer));
            player.sendMessage("Transaction failed. Funds refunded.");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Example: Integrating with Vault economy.
     *
     * <p>This example demonstrates how to use TAN's economy with the Vault API.
     * TAN provides a Vault-compatible implementation via {@link TanEconomyVault}.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>TAN economy implements Vault's {@link Economy} interface</li>
     *   <li>Use standard Vault API methods for compatibility</li>
     *   <li>TAN's Vault implementation uses blocking getSync() calls internally</li>
     *   <li>Avoid calling Vault methods on Folia region threads</li>
     * </ul>
     *
     * <p><b>Thread Safety Warning:</b> Vault's API is synchronous by definition.
     * TAN's implementation uses blocking {@code getSync()} calls to comply with Vault's contract.
     * Avoid calling on region threads in Folia.</p>
     *
     * @param vaultEconomy The Vault Economy instance (injected by TAN)
     * @param player The player to perform economy operations on
     */
    public static void vaultIntegrationExample(Economy vaultEconomy, Player player) {
        // Check balance using Vault API
        double balance = vaultEconomy.getBalance(player);
        player.sendMessage("Your balance: " + vaultEconomy.format(balance));

        // Withdraw money using Vault API
        double amount = 100.0;
        if (vaultEconomy.has(player, amount)) {
            var response = vaultEconomy.withdrawPlayer(player, amount);
            if (response.type == net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS) {
                player.sendMessage("Withdrew: " + vaultEconomy.format(amount));
            }
        }

        // Deposit money using Vault API
        var depositResponse = vaultEconomy.depositPlayer(player, 50.0);
        if (depositResponse.type == net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS) {
            player.sendMessage("Deposited: " + vaultEconomy.format(50.0));
        }
    }

    /**
     * Example: Using external economy plugins.
     *
     * <p>This example shows how to integrate with external economy plugins like
     * EssentialsX, CMI, or other Vault-compatible economy plugins.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link TanEconomyExternal} to wrap external Vault economy</li>
     *   <li>TAN will delegate all economy operations to the external plugin</li>
     *   <li>Currency names and formats are inherited from external plugin</li>
     *   <li>Use {@link EconomyUtil#isStandalone()} to check economy type</li>
     * </ul>
     *
     * @param externalEconomy The external Vault economy instance
     * @param player The player to perform operations on
     */
    public static void externalEconomyExample(Economy externalEconomy, Player player) {
        // Check if TAN is using standalone or external economy
        if (EconomyUtil.isStandalone()) {
            player.sendMessage("Using TAN standalone economy");
        } else {
            player.sendMessage("Using external economy plugin");

            // Wrap external economy
            TanEconomyExternal tanExternal = new TanEconomyExternal(externalEconomy);

            // Get ITanPlayer for operations
            ITanPlayer tanPlayer = PlayerDataStorage.getInstance()
                .getSync(player.getUniqueId().toString());

            // Use TAN economy API (delegates to external)
            double balance = tanExternal.getBalance(tanPlayer);
            player.sendMessage("Balance: " + tanExternal.formatMoney(balance));
        }
    }

    /**
     * Example: Async economy operations.
     *
     * <p>This example demonstrates the recommended pattern for performing economy
     * operations asynchronously to avoid blocking threads in Folia.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link AsyncEconomyService} for non-blocking operations</li>
     *   <li>Avoid {@code getSync()} methods in async pipelines</li>
     *   <li>Use {@link CompletableFuture#thenCompose} for chaining operations</li>
     *   <li>Handle errors with {@link CompletableFuture#exceptionally}</li>
     * </ul>
     *
     * @param playerId The UUID of the player to perform operations on
     * @return CompletableFuture that completes when operations finish
     */
    public static CompletableFuture<Void> asyncEconomyExample(String playerId) {
        return AsyncEconomyService.getBalance(playerId)
            .thenCompose(balance -> {
                // Check if player can afford purchase
                double cost = 1000.0;
                if (balance < cost) {
                    throw new IllegalStateException("Insufficient funds");
                }

                // Withdraw money asynchronously
                return AsyncEconomyService.withdraw(playerId, cost);
            })
            .thenAccept(newBalance -> {
                // Transaction completed
                System.out.println("Purchase successful. New balance: " + newBalance);
            })
            .exceptionally(throwable -> {
                // Handle errors
                System.err.println("Transaction failed: " + throwable.getMessage());
                return null;
            });
    }

    /**
     * Example: Managing town treasury.
     *
     * <p>This example demonstrates how to interact with town/region economy
     * using the territory's built-in treasury system.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Towns have their own treasury managed by {@code TreasuryComponent}</li>
     *   <li>Use {@code town.addToBalance()} to deposit to town treasury</li>
     *   <li>Use {@code town.removeFromBalance()} to withdraw from town treasury</li>
     *   <li>Use {@code town.getBalance()} to check town treasury balance</li>
     *   <li>Taxes are collected automatically from town members</li>
     * </ul>
     *
     * @param townId The ID of the town to manage treasury for
     * @param amount The amount to deposit
     * @return CompletableFuture that completes when deposit finishes
     */
    public static CompletableFuture<Void> townTreasuryExample(String townId, double amount) {
        return org.leralix.tan.storage.stored.TownDataStorage.getInstance()
            .get(townId)
            .thenAccept(town -> {
                if (town == null) {
                    throw new IllegalArgumentException("Town not found");
                }

                // Check current treasury balance
                double currentBalance = town.getBalance();
                System.out.println("Current treasury balance: " + currentBalance);

                // Deposit to town treasury
                town.addToBalance(amount);

                // Verify deposit
                double newBalance = town.getBalance();
                System.out.println("New treasury balance: " + newBalance);

                // Save to storage
                org.leralix.tan.storage.stored.TownDataStorage.getInstance()
                    .putSync(town.getID(), town);
            });
    }

    /**
     * Example: Formatting money for display.
     *
     * <p>This example shows how to format money amounts with currency symbols
     * for display to players.</p>
     *
     * @param amount The amount to format
     * @return Formatted string with currency symbol
     */
    public static String formatMoneyExample(double amount) {
        // Format using EconomyUtil (uses configured currency)
        String formatted = EconomyUtil.formatMoney(amount);

        // Example outputs:
        // - Standalone: "1000.00$" (or configured currency)
        // - External: "1000.00 Coins" (external plugin currency)

        return formatted;
    }

    /**
     * Example: Thread-safe economy operations.
     *
     * <p>This example demonstrates how to perform economy operations safely
     * in multi-threaded environments.</p>
     *
     * <h3>Thread Safety Guidelines:</h3>
     * <ul>
     *   <li>Always use async methods in Folia region threads</li>
     *   <li>Never call {@code getSync()} in async pipelines</li>
     *   <li>Use {@code supplyAsync()} for CPU-intensive calculations</li>
     *   <li>Economy operations are atomic at the database level</li>
     * </ul>
     *
     * @param player The player to perform operations on
     */
    public static void threadSafeEconomyExample(Player player) {
        // GOOD: Async pattern (recommended for Folia)
        String playerId = player.getUniqueId().toString();

        AsyncEconomyService.getBalance(playerId)
            .thenAccept(balance -> {
                player.sendMessage("Your balance: " + balance);
            });

        // AVOID: Synchronous pattern in Folia (blocks region thread)
        // ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(playerId);
        // double balance = EconomyUtil.getBalance(tanPlayer); // BAD!

        // For standalone tasks, use supplyAsync
        CompletableFuture.supplyAsync(() -> {
            // CPU-intensive calculation (e.g., interest calculation)
            return EconomyUtil.getBalance(player) * 1.05; // 5% interest
        }).thenAccept(interest -> {
            player.sendMessage("Interest earned: " + interest);
        });
    }
}
