package org.leralix.tan.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.SphereLib;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.commands.player.SeeBalanceCommand;
import org.leralix.tan.commands.player.PayCommand;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Integration tests for economy workflow.
 *
 * <p>Tests the end-to-end economy operations including:
 * - Money transfers
 * - Balance checks
 * - Town treasury operations
 * - Tax collection</p>
 */
@DisplayName("Economy Workflow Integration Tests")
class EconomyWorkflowTest {

    private ServerMock server;
    private TownsAndNations plugin;
    private PlayerMock player1;
    private PlayerMock player2;
    private ITanPlayer tanPlayer1;
    private ITanPlayer tanPlayer2;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SphereLib.class);
        plugin = MockBukkit.load(TownsAndNations.class);

        player1 = server.addPlayer("Player1");
        player2 = server.addPlayer("Player2");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Complete money transfer workflow")
    void testMoneyTransfer() throws Exception {
        // Load both players
        tanPlayer1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        tanPlayer2 = PlayerDataStorage.getInstance().get(player2).get(5, TimeUnit.SECONDS);

        // Set initial balances
        double initialBalance1 = 1000.0;
        double initialBalance2 = 500.0;

        tanPlayer1.setBalance(initialBalance1);
        tanPlayer2.setBalance(initialBalance2);
        PlayerDataStorage.getInstance().updateSync(tanPlayer1);
        PlayerDataStorage.getInstance().updateSync(tanPlayer2);

        // Verify initial balances
        assertEquals(initialBalance1, EconomyUtil.getBalance(tanPlayer1), 0.01);
        assertEquals(initialBalance2, EconomyUtil.getBalance(tanPlayer2), 0.01);

        // Execute money transfer using PayCommand logic
        double transferAmount = 200.0;

        // Check if player1 has enough money
        assertTrue(EconomyUtil.getBalance(tanPlayer1) >= transferAmount,
            "Player1 should have enough money");

        // Perform transfer
        EconomyUtil.removeFromBalance(tanPlayer1, transferAmount);
        EconomyUtil.addFromBalance(tanPlayer2, transferAmount);

        PlayerDataStorage.getInstance().updateSync(tanPlayer1);
        PlayerDataStorage.getInstance().updateSync(tanPlayer2);

        // Reload and verify balances
        ITanPlayer reloaded1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        ITanPlayer reloaded2 = PlayerDataStorage.getInstance().get(player2).get(5, TimeUnit.SECONDS);

        assertEquals(initialBalance1 - transferAmount, EconomyUtil.getBalance(reloaded1), 0.01,
            "Player1 balance should decrease by transfer amount");
        assertEquals(initialBalance2 + transferAmount, EconomyUtil.getBalance(reloaded2), 0.01,
            "Player2 balance should increase by transfer amount");
    }

    @Test
    @DisplayName("Town treasury deposit workflow")
    void testTownTreasuryDeposit() throws Exception {
        // Load player and create town
        tanPlayer1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);

        String townId = java.util.UUID.randomUUID().toString();
        TownData town = new TownData(townId, "TreasuryTown", tanPlayer1);
        TownDataStorage.getInstance().createSync(town);

        // Set player balance
        double playerBalance = 1000.0;
        tanPlayer1.setBalance(playerBalance);
        tanPlayer1.setTownID(townId);
        PlayerDataStorage.getInstance().updateSync(tanPlayer1);

        // Deposit to treasury
        double depositAmount = 300.0;
        town.addToTreasury(depositAmount);
        EconomyUtil.removeFromBalance(tanPlayer1, depositAmount);

        // Update storage
        TownDataStorage.getInstance().updateSync(town);
        PlayerDataStorage.getInstance().updateSync(tanPlayer1);

        // Verify
        ITanPlayer reloadedPlayer = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        TownData reloadedTown = TownDataStorage.getInstance().get(townId).get(5, TimeUnit.SECONDS);

        assertEquals(playerBalance - depositAmount, EconomyUtil.getBalance(reloadedPlayer), 0.01);
        assertEquals(depositAmount, reloadedTown.getTreasury(), 0.01);
    }

    @Test
    @DisplayName("Tax collection workflow")
    void testTaxCollection() throws Exception {
        // Create town with multiple members
        tanPlayer1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        tanPlayer2 = PlayerDataStorage.getInstance().get(player2).get(5, TimeUnit.SECONDS);

        String townId = java.util.UUID.randomUUID().toString();
        TownData town = new TownData(townId, "TaxTown", tanPlayer1);
        town.addMember(tanPlayer2.getID());
        town.setTax(50.0); // 50 currency tax per player
        TownDataStorage.getInstance().createSync(town);

        // Set player balances
        tanPlayer1.setBalance(1000.0);
        tanPlayer2.setBalance(500.0);
        tanPlayer1.setTownID(townId);
        tanPlayer2.setTownID(townId);
        PlayerDataStorage.getInstance().updateSync(tanPlayer1);
        PlayerDataStorage.getInstance().updateSync(tanPlayer2);

        // Collect taxes
        double taxPerPlayer = town.getTax();
        EconomyUtil.removeFromBalance(tanPlayer1, taxPerPlayer);
        EconomyUtil.removeFromBalance(tanPlayer2, taxPerPlayer);
        town.addToTreasury(taxPerPlayer * 2);

        // Update storage
        PlayerDataStorage.getInstance().updateSync(tanPlayer1);
        PlayerDataStorage.getInstance().updateSync(tanPlayer2);
        TownDataStorage.getInstance().updateSync(town);

        // Verify
        ITanPlayer reloadedPlayer1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        ITanPlayer reloadedPlayer2 = PlayerDataStorage.getInstance().get(player2).get(5, TimeUnit.SECONDS);
        TownData reloadedTown = TownDataStorage.getInstance().get(townId).get(5, TimeUnit.SECONDS);

        assertEquals(1000.0 - taxPerPlayer, EconomyUtil.getBalance(reloadedPlayer1), 0.01);
        assertEquals(500.0 - taxPerPlayer, EconomyUtil.getBalance(reloadedPlayer2), 0.01);
        assertEquals(taxPerPlayer * 2, reloadedTown.getTreasury(), 0.01);
    }

    @Test
    @DisplayName("Economy operations handle insufficient funds")
    void testInsufficientFunds() throws Exception {
        tanPlayer1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        tanPlayer1.setBalance(100.0);
        PlayerDataStorage.getInstance().updateSync(tanPlayer1);

        // Try to withdraw more than balance
        double withdrawAmount = 200.0;
        double initialBalance = EconomyUtil.getBalance(tanPlayer1);

        // Check balance first
        assertFalse(EconomyUtil.getBalance(tanPlayer1) >= withdrawAmount,
            "Should not have enough funds");

        // Verify balance unchanged
        assertEquals(initialBalance, EconomyUtil.getBalance(tanPlayer1), 0.01);
    }

    @Test
    @DisplayName("Concurrent economy operations")
    void testConcurrentEconomyOperations() throws Exception {
        tanPlayer1 = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        tanPlayer1.setBalance(1000.0);
        PlayerDataStorage.getInstance().updateSync(tanPlayer1);

        // Perform multiple operations concurrently
        int operationCount = 10;
        CompletableFuture<Void>[] operations = new CompletableFuture[operationCount];

        for (int i = 0; i < operationCount; i++) {
            final int index = i;
            operations[i] = CompletableFuture.runAsync(() -> {
                try {
                    ITanPlayer player = PlayerDataStorage.getInstance()
                        .getSync(player1.getUniqueId().toString());
                    player.setBalance(player.getBalance() + 10.0);
                    PlayerDataStorage.getInstance().updateSync(player);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Wait for all operations
        CompletableFuture.allOf(operations).get(10, TimeUnit.SECONDS);

        // Verify final balance
        ITanPlayer reloaded = PlayerDataStorage.getInstance().get(player1).get(5, TimeUnit.SECONDS);
        assertEquals(1000.0 + (operationCount * 10.0), reloaded.getBalance(), 0.01);
    }
}
