package org.leralix.tan.service.economy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.service.EconomyOps;
import org.leralix.tan.service.PlayerDataService;
import org.leralix.tan.testutils.AbstractPluginTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for realistic economy scenarios.
 * <p>
 * These tests simulate real-world economy operations that occur in game:
 * <ul>
 *   <li>Tax collection from multiple players</li>
 *   <li>Bank deposits and withdrawals</li>
 *   <li>Payment processing</li>
 *   <li>Economy rollback scenarios</li>
 *   <li>Multi-step transactions</li>
 * </ul>
 */
@DisplayName("Economy Scenario Tests")
class EconomyScenarioTest extends AbstractPluginTest {

    // ==================== Tax Collection Scenarios ====================

    @Test
    @DisplayName("Tax collection from multiple players should be accurate")
    void taxCollection_multiplePlayers_accurateTotal() throws Exception {
        // Arrange
        int playerCount = 20;
        double taxPerPlayer = 50.0;
        double expectedTotalTax = playerCount * taxPerPlayer;

        List<String> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            String playerId = "tax_player_" + i;
            createTestPlayer(playerId);
            EconomyOps.addToPlayerAsync(playerId, 500.0).get();
            players.add(playerId);
        }

        String treasuryPlayer = "treasury";
        createTestPlayer(treasuryPlayer);

        // Act - Collect tax from all players
        double totalCollected = 0.0;
        int successfulCollections = 0;

        for (String player : players) {
            EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
                player, treasuryPlayer, taxPerPlayer).get();

            if (result.isSuccess()) {
                successfulCollections++;
                totalCollected += taxPerPlayer;
            }
        }

        // Assert
        assertEquals(playerCount, successfulCollections,
            "All tax collections should succeed");

        double treasuryBalance = getPlayerBalanceSync(treasuryPlayer);
        assertEquals(expectedTotalTax, treasuryBalance, 0.01,
            "Treasury should contain exact total of collected taxes");
    }

    @Test
    @DisplayName("Tax collection should handle insufficient funds gracefully")
    void taxCollection_insufficientFunds_gracefulHandling() throws Exception {
        // Arrange
        List<String> richPlayers = List.of("rich_1", "rich_2", "rich_3");
        List<String> poorPlayers = List.of("poor_1", "poor_2", "poor_3");
        double taxAmount = 100.0;

        for (String player : richPlayers) {
            createTestPlayer(player);
            EconomyOps.addToPlayerAsync(player, 1000.0).get();
        }

        for (String player : poorPlayers) {
            createTestPlayer(player);
            EconomyOps.addToPlayerAsync(player, 50.0).get(); // Not enough for tax
        }

        String treasury = "tax_treasury";
        createTestPlayer(treasury);

        // Act - Try to collect from all
        int successCount = 0;
        int failCount = 0;

        for (String player : richPlayers) {
            EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
                player, treasury, taxAmount).get();
            if (result.isSuccess()) successCount++;
            else failCount++;
        }

        for (String player : poorPlayers) {
            EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
                player, treasury, taxAmount).get();
            if (result.isSuccess()) successCount++;
            else {
                failCount++;
                assertTrue(result instanceof EconomyOps.TransactionResult.InsufficientFunds,
                    "Poor players should get InsufficientFunds result");
            }
        }

        // Assert
        assertEquals(3, successCount, "All rich players should pay tax");
        assertEquals(3, failCount, "All poor players should fail tax payment");

        double treasuryBalance = getPlayerBalanceSync(treasury);
        assertEquals(300.0, treasuryBalance, 0.01,
            "Treasury should only contain tax from rich players");
    }

    @Test
    @DisplayName("Percentage tax calculation should be accurate")
    void percentageTax_accurateCalculation() throws Exception {
        // Arrange
        String player = "percent_tax_player";
        createTestPlayer(player);
        double balance = 1000.0;
        double taxRate = 0.10; // 10%
        EconomyOps.addToPlayerAsync(player, balance).get();

        String treasury = "percent_treasury";
        createTestPlayer(treasury);

        // Act - Calculate and collect percentage tax
        double taxAmount = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(balance * taxRate);
        EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
            player, treasury, taxAmount).get();

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(100.0, taxAmount, 0.001);
        assertEquals(900.0, getPlayerBalanceSync(player), 0.01);
        assertEquals(100.0, getPlayerBalanceSync(treasury), 0.01);
    }

    // ==================== Bank Operations ====================

    @Test
    @DisplayName("Multiple deposits should accumulate correctly")
    void multipleDeposits_correctAccumulation() throws Exception {
        // Arrange
        String player = "depositor_player";
        String bank = "bank";
        createTestPlayer(player);
        createTestPlayer(bank);
        EconomyOps.addToPlayerAsync(player, 1000.0).get();

        // Act - Make multiple deposits
        double[] deposits = {100.0, 50.0, 25.0, 10.0, 5.0};
        double totalDeposited = 0.0;

        for (double deposit : deposits) {
            EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
                player, bank, deposit).get();
            assertTrue(result.isSuccess());
            totalDeposited += deposit;
        }

        // Assert
        assertEquals(totalDeposited, getPlayerBalanceSync(bank), 0.01);
        assertEquals(1000.0 - totalDeposited, getPlayerBalanceSync(player), 0.01);
    }

    @Test
    @DisplayName("Withdrawal should respect available balance")
    void withdrawal_respectsBalance() throws Exception {
        // Arrange
        String player = "withdrawer_player";
        String bank = "withdrawal_bank";
        createTestPlayer(player);
        createTestPlayer(bank);
        EconomyOps.addToPlayerAsync(player, 100.0).get();
        EconomyOps.addToPlayerAsync(bank, 500.0).get();

        // Act - Withdraw various amounts
        EconomyOps.TransactionResult result1 = EconomyOps.transferBetweenPlayersAsync(
            bank, player, 200.0).get();
        EconomyOps.TransactionResult result2 = EconomyOps.transferBetweenPlayersAsync(
            bank, player, 400.0).get(); // Should fail - only 300 left

        // Assert
        assertTrue(result1.isSuccess());
        assertFalse(result2.isSuccess());
        assertTrue(result2 instanceof EconomyOps.TransactionResult.InsufficientFunds);

        assertEquals(300.0, getPlayerBalanceSync(player), 0.01);
        assertEquals(300.0, getPlayerBalanceSync(bank), 0.01);
    }

    // ==================== Payment Processing ====================

    @Test
    @DisplayName("Payment with insufficient funds should fail atomically")
    void paymentInsufficientFunds_atomicFailure() throws Exception {
        // Arrange
        String payer = "payer";
        String payee = "payee";
        createTestPlayer(payer);
        createTestPlayer(payee);

        EconomyOps.addToPlayerAsync(payer, 50.0).get();
        EconomyOps.addToPlayerAsync(payee, 100.0).get();

        double payeeOriginalBalance = getPlayerBalanceSync(payee);

        // Act - Try to pay more than available
        EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
            payer, payee, 100.0).get();

        // Assert - Payee balance should not change
        assertFalse(result.isSuccess());
        assertTrue(result instanceof EconomyOps.TransactionResult.InsufficientFunds);

        assertEquals(payeeOriginalBalance, getPlayerBalanceSync(payee), 0.001,
            "Payee balance should not change on failed payment");
        assertEquals(50.0, getPlayerBalanceSync(payer), 0.001,
            "Payer balance should not change on failed payment");
    }

    @Test
    @DisplayName("Bulk payment processing should handle failures gracefully")
    void bulkPayment_failuresHandledGracefully() throws Exception {
        // Arrange
        String employer = "employer";
        createTestPlayer(employer);
        EconomyOps.addToPlayerAsync(employer, 500.0).get();

        List<String> employees = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String employee = "employee_" + i;
            createTestPlayer(employee);
            employees.add(employee);
        }

        double salary = 100.0;

        // Act - Try to pay all employees (should fail halfway)
        int successfulPayments = 0;
        int failedPayments = 0;

        for (String employee : employees) {
            EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
                employer, employee, salary).get();

            if (result.isSuccess()) {
                successfulPayments++;
            } else {
                failedPayments++;
                assertTrue(result instanceof EconomyOps.TransactionResult.InsufficientFunds);
            }
        }

        // Assert
        assertEquals(5, successfulPayments, "Should pay 5 employees");
        assertEquals(5, failedPayments, "Should fail for 5 employees");
        assertEquals(0.0, getPlayerBalanceSync(employer), 0.01,
            "Employer should be out of money");
    }

    // ==================== Refund Scenarios ====================

    @Test
    @DisplayName("Refund should restore original balance correctly")
    void refund_restoresBalance() throws Exception {
        // Arrange
        String customer = "customer";
        String merchant = "merchant";
        createTestPlayer(customer);
        createTestPlayer(merchant);

        EconomyOps.addToPlayerAsync(customer, 500.0).get();
        double originalBalance = getPlayerBalanceSync(customer);

        // Act - Purchase and refund
        double purchaseAmount = 100.0;

        // Purchase
        EconomyOps.TransactionResult purchaseResult = EconomyOps.transferBetweenPlayersAsync(
            customer, merchant, purchaseAmount).get();
        assertTrue(purchaseResult.isSuccess());

        double afterPurchaseBalance = getPlayerBalanceSync(customer);

        // Refund
        EconomyOps.TransactionResult refundResult = EconomyOps.transferBetweenPlayersAsync(
            merchant, customer, purchaseAmount).get();
        assertTrue(refundResult.isSuccess());

        // Assert
        assertEquals(originalBalance, getPlayerBalanceSync(customer), 0.001,
            "Customer balance should be restored after refund");
        assertEquals(afterPurchaseBalance + purchaseAmount, getPlayerBalanceSync(customer), 0.001);
    }

    // ==================== Multi-Step Transactions ====================

    @Test
    @DisplayName("Three-party transaction should complete atomically or fail entirely")
    void threePartyTransaction_atomic() throws Exception {
        // Arrange
        String buyer = "buyer";
        String seller = "seller";
        String taxCollector = "tax_collector";

        createTestPlayer(buyer);
        createTestPlayer(seller);
        createTestPlayer(taxCollector);

        EconomyOps.addToPlayerAsync(buyer, 1000.0).get();

        double saleAmount = 500.0;
        double taxAmount = 50.0;

        // Act - Simulate sale with tax
        // 1. Buyer pays seller
        EconomyOps.TransactionResult saleResult = EconomyOps.transferBetweenPlayersAsync(
            buyer, seller, saleAmount).get();
        assertTrue(saleResult.isSuccess());

        // 2. Seller pays tax
        EconomyOps.TransactionResult taxResult = EconomyOps.transferBetweenPlayersAsync(
            seller, taxCollector, taxAmount).get();
        assertTrue(taxResult.isSuccess());

        // Assert
        assertEquals(450.0, getPlayerBalanceSync(buyer), 0.01,
            "Buyer should have paid sale amount");
        assertEquals(450.0, getPlayerBalanceSync(seller), 0.01,
            "Seller should have received sale amount and paid tax");
        assertEquals(50.0, getPlayerBalanceSync(taxCollector), 0.01,
            "Tax collector should have received tax");

        // Total preserved
        double total = getPlayerBalanceSync(buyer) +
                      getPlayerBalanceSync(seller) +
                      getPlayerBalanceSync(taxCollector);
        assertEquals(950.0, total, 0.01,
            "Total money should be preserved (1000 - 50 tax)");
    }

    // ==================== Edge Case Scenarios ====================

    @Test
    @DisplayName("Self-transfer should not cause issues")
    void selfTransfer_noEffect() throws Exception {
        // Arrange
        String player = "self_transfer_player";
        createTestPlayer(player);
        EconomyOps.addToPlayerAsync(player, 500.0).get();
        double originalBalance = getPlayerBalanceSync(player);

        // Act - Try to transfer to self
        EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
            player, player, 100.0).get();

        // Assert - Behavior depends on implementation
        // Either it succeeds with no effect, or fails
        if (result.isSuccess()) {
            assertEquals(originalBalance, getPlayerBalanceSync(player), 0.001,
                "Self-transfer should not change balance");
        } else {
            assertEquals(originalBalance, getPlayerBalanceSync(player), 0.001,
                "Self-transfer should not change balance");
        }
    }

    @Test
    @DisplayName("Minimum amount transfer should work correctly")
    void minimumAmountTransfer_correct() throws Exception {
        // Arrange
        String player1 = "min_sender";
        String player2 = "min_receiver";
        createTestPlayer(player1);
        createTestPlayer(player2);
        EconomyOps.addToPlayerAsync(player1, 1000.0).get();

        // Act - Transfer minimum meaningful amount
        double minAmount = 0.01;
        EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
            player1, player2, minAmount).get();

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(999.99, getPlayerBalanceSync(player1), 0.001);
        assertEquals(0.01, getPlayerBalanceSync(player2), 0.001);
    }

    @Test
    @DisplayName("Maximum safe transfer should work correctly")
    void maximumSafeTransfer_correct() throws Exception {
        // Arrange
        String player1 = "max_sender";
        String player2 = "max_receiver";
        createTestPlayer(player1);
        createTestPlayer(player2);

        double largeAmount = Double.MAX_VALUE / 4;
        EconomyOps.addToPlayerAsync(player1, largeAmount).get();

        // Act - Transfer large amount
        EconomyOps.TransactionResult result = EconomyOps.transferBetweenPlayersAsync(
            player1, player2, largeAmount / 2).get();

        // Assert - Should handle gracefully
        assertNotNull(result);
        assertFalse(Double.isInfinite(getPlayerBalanceSync(player1)));
        assertFalse(Double.isInfinite(getPlayerBalanceSync(player2)));
    }

    // ==================== Economy State Verification ====================

    @Test
    @DisplayName("Total money in economy should be conserved")
    void totalMoneyConserved_acrossOperations() throws Exception {
        // Arrange
        List<String> players = new ArrayList<>();
        double totalInitial = 0.0;
        int playerCount = 10;

        for (int i = 0; i < playerCount; i++) {
            String player = "conserv_player_" + i;
            createTestPlayer(player);
            double initial = 1000.0;
            EconomyOps.addToPlayerAsync(player, initial).get();
            totalInitial += initial;
            players.add(player);
        }

        // Act - Perform many transactions
        for (int i = 0; i < 100; i++) {
            String from = players.get(i % playerCount);
            String to = players.get((i + 1) % playerCount);
            double amount = 10.0 + (i % 5) * 10.0;
            EconomyOps.transferBetweenPlayersAsync(from, to, amount).get();
        }

        // Assert
        double totalFinal = 0.0;
        for (String player : players) {
            totalFinal += getPlayerBalanceSync(player);
        }

        assertEquals(totalInitial, totalFinal, 0.01,
            "Total money should be conserved across all transactions");
    }

    // ==================== Helper Methods ====================

    private double getPlayerBalanceSync(String playerId) {
        var player = PlayerDataService.getPlayer(playerId);
        return player != null ? player.getBalance() : 0.0;
    }
}
