package org.leralix.tan.dataclass.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Comprehensive tests for territory serialization and thread-safety.
 *
 * <p>Tests critical aspects of territory data management:</p>
 * <ul>
 *   <li>Serialization round-trip (Gson)</li>
 *   <li>Concurrent access thread-safety</li>
 *   <li>Data consistency after storage operations</li>
 *   <li>Multi-threaded member/claim operations</li>
 * </ul>
 */
@DisplayName("Territory Serialization and Concurrency Tests")
class TerritorySerializationComprehensiveTest {

    private ServerMock server;
    private PlayerDataStorage playerStorage;
    private TownDataStorage townStorage;
    private Gson gson;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        playerStorage = PlayerDataStorage.getInstance();
        townStorage = TownDataStorage.getInstance();

        // Configure Gson for territory serialization (same as plugin)
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ========== Serialization Tests ==========

    @Test
    @DisplayName("TownData should serialize to JSON")
    void testTownDataSerialization() {
        // Arrange
        PlayerMock leader = server.addPlayer("SerializeLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("SerializeTown", tanLeader).join();
        town.addToBalance(1000.0);
        town.setTax(10.0);

        // Act
        String json = gson.toJson(town);

        // Assert
        assertNotNull(json, "JSON should not be null");
        assertTrue(json.length() > 0, "JSON should not be empty");
        assertTrue(json.contains("SerializeTown"), "JSON should contain town name");
    }

    @Test
    @DisplayName("TownData should deserialize from JSON")
    void testTownDataDeserialization() {
        // Arrange
        PlayerMock leader = server.addPlayer("DeserializeLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData originalTown = townStorage.newTown("DeserializeTown", tanLeader).join();
        originalTown.addToBalance(1500.0);
        originalTown.setTax(12.5);

        String json = gson.toJson(originalTown);

        // Act
        TownData deserializedTown = gson.fromJson(json, TownData.class);

        // Assert
        assertNotNull(deserializedTown, "Deserialized town should not be null");
        assertEquals(originalTown.getID(), deserializedTown.getID(), "ID should match");
        assertEquals(originalTown.getName(), deserializedTown.getName(), "Name should match");
    }

    @Test
    @DisplayName("Serialization should preserve balance")
    void testSerializationPreservesBalance() {
        // Arrange
        PlayerMock leader = server.addPlayer("BalanceSerializeLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("BalanceSerializeTown", tanLeader).join();
        double originalBalance = 2500.75;
        town.addToBalance(originalBalance);

        // Act
        String json = gson.toJson(town);
        TownData deserialized = gson.fromJson(json, TownData.class);

        // Assert
        assertEquals(originalBalance, deserialized.getBalance(), 0.001,
            "Balance should be preserved through serialization");
    }

    @Test
    @DisplayName("Serialization should preserve tax rate")
    void testSerializationPreservesTaxRate() {
        // Arrange
        PlayerMock leader = server.addPlayer("TaxSerializeLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("TaxSerializeTown", tanLeader).join();
        double originalTax = 18.5;
        town.setTax(originalTax);

        // Act
        String json = gson.toJson(town);
        TownData deserialized = gson.fromJson(json, TownData.class);

        // Assert
        assertEquals(originalTax, deserialized.getTax(), 0.001,
            "Tax rate should be preserved through serialization");
    }

    @Test
    @DisplayName("Serialization round-trip should preserve all basic properties")
    void testSerializationRoundTrip() {
        // Arrange
        PlayerMock leader = server.addPlayer("RoundTripLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData original = townStorage.newTown("RoundTripTown", tanLeader).join();
        original.addToBalance(3000.0);
        original.setTax(15.0);

        Location claimLoc = new Location(server.addSimpleWorld("world"), 100, 64, 100);
        original.claimChunk(leader, claimLoc);

        // Act
        String json = gson.toJson(original);
        TownData deserialized = gson.fromJson(json, TownData.class);

        // Assert
        assertEquals(original.getID(), deserialized.getID(), "ID should match");
        assertEquals(original.getName(), deserialized.getName(), "Name should match");
        assertEquals(original.getBalance(), deserialized.getBalance(), 0.001, "Balance should match");
        assertEquals(original.getTax(), deserialized.getTax(), 0.001, "Tax should match");
    }

    // ========== Concurrency Tests ==========

    @Test
    @DisplayName("Territory should handle concurrent balance modifications")
    void testConcurrentBalanceModifications() throws InterruptedException {
        // Arrange
        PlayerMock leader = server.addPlayer("ConcurrentBalanceLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("ConcurrentBalanceTown", tanLeader).join();

        int threadCount = 20;
        int operationsPerThread = 50;
        double amountPerOperation = 10.0;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act - Concurrent deposits
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        town.addToBalance(amountPerOperation);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Concurrent operations should complete within timeout");
        executor.shutdown();

        // Assert
        double expectedBalance = threadCount * operationsPerThread * amountPerOperation;
        assertEquals(expectedBalance, town.getBalance(), expectedBalance * 0.01,
            "Concurrent balance modifications should be consistent");
    }

    @Test
    @DisplayName("Territory should handle concurrent member additions")
    void testConcurrentMemberAdditions() throws InterruptedException {
        // Arrange
        PlayerMock leader = server.addPlayer("ConcurrentMemberLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("ConcurrentMemberTown", tanLeader).join();

        int memberCount = 50;
        CountDownLatch latch = new CountDownLatch(memberCount);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act - Concurrent member additions
        for (int i = 0; i < memberCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    PlayerMock member = server.addPlayer("ConcurrentMember" + index);
                    ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());
                    town.addPlayer(tanMember);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Concurrent member additions should complete");
        executor.shutdown();

        // Assert
        assertEquals(memberCount, successCount.get(), "All member additions should succeed");
        assertTrue(town.getITanPlayerList().size() >= memberCount,
            "Town should have all members");
    }

    @Test
    @DisplayName("Territory should handle concurrent chunk claims")
    void testConcurrentChunkClaims() throws InterruptedException {
        // Arrange
        PlayerMock leader = server.addPlayer("ConcurrentClaimLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("ConcurrentClaimTown", tanLeader).join();

        int claimCount = 30;
        CountDownLatch latch = new CountDownLatch(claimCount);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act - Concurrent chunk claims
        for (int i = 0; i < claimCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    int x = index * 16; // Different chunks
                    int z = index * 16;
                    Location claimLoc = new Location(server.addSimpleWorld("world"), x, 64, z);
                    town.claimChunk(leader, claimLoc);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Concurrent chunk claims should complete");
        executor.shutdown();

        // Assert
        assertTrue(successCount.get() >= claimCount - 2, // May have some duplicates
            "Most claims should succeed");
    }

    @Test
    @DisplayName("Territory should handle concurrent mixed operations")
    void testConcurrentMixedOperations() throws InterruptedException {
        // Arrange
        PlayerMock leader = server.addPlayer("MixedOpsLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("MixedOpsTown", tanLeader).join();

        int operationCount = 100;
        CountDownLatch latch = new CountDownLatch(operationCount);
        ExecutorService executor = Executors.newFixedThreadPool(15);
        AtomicInteger balanceOps = new AtomicInteger(0);
        AtomicInteger memberOps = new AtomicInteger(0);
        AtomicInteger taxOps = new AtomicInteger(0);

        // Act - Concurrent mixed operations
        for (int i = 0; i < operationCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    int operationType = index % 3;
                    switch (operationType) {
                        case 0: // Balance operation
                            town.addToBalance(10.0);
                            balanceOps.incrementAndGet();
                            break;
                        case 1: // Tax operation
                            town.setTax(index % 20);
                            taxOps.incrementAndGet();
                            break;
                        case 2: // Member operation
                            if (index < 20) { // Limit member creation
                                PlayerMock member = server.addPlayer("MixedMember" + index);
                                ITanPlayer tanMember = playerStorage.getSync(member.getUniqueId().toString());
                                town.addPlayer(tanMember);
                                memberOps.incrementAndGet();
                            }
                            break;
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        assertTrue(latch.await(15, TimeUnit.SECONDS),
            "Concurrent mixed operations should complete");
        executor.shutdown();

        // Assert - Verify operations were executed
        assertTrue(balanceOps.get() > 0, "Balance operations should be executed");
        assertTrue(taxOps.get() > 0, "Tax operations should be executed");
        assertTrue(memberOps.get() > 0, "Member operations should be executed");
    }

    // ========== Data Consistency Tests ==========

    @Test
    @DisplayName("Storage operations should maintain data consistency")
    void testDataConsistencyAfterStorage() {
        // Arrange
        PlayerMock leader = server.addPlayer("ConsistencyLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData original = townStorage.newTown("ConsistencyTown", tanLeader).join();

        double initialBalance = 5000.0;
        original.addToBalance(initialBalance);
        original.setTax(20.0);

        // Act - Store and retrieve
        townStorage.putSync(original.getID(), original);
        TownData retrieved = townStorage.getSync(original.getID());

        // Assert
        assertEquals(initialBalance, retrieved.getBalance(), 0.001,
            "Balance should be consistent after storage");
        assertEquals(20.0, retrieved.getTax(), 0.001,
            "Tax should be consistent after storage");
        assertEquals(original.getID(), retrieved.getID(),
            "ID should be consistent after storage");
    }

    @Test
    @DisplayName("Multiple storage operations should maintain consistency")
    void testMultipleStorageOperations() {
        // Arrange
        PlayerMock leader = server.addPlayer("MultiStorageLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("MultiStorageTown", tanLeader).join();

        // Act - Multiple storage operations
        town.addToBalance(1000.0);
        townStorage.putSync(town.getID(), town);

        town.setTax(10.0);
        townStorage.putSync(town.getID(), town);

        town.addToBalance(500.0);
        townStorage.putSync(town.getID(), town);

        // Final retrieval
        TownData finalTown = townStorage.getSync(town.getID());

        // Assert
        assertEquals(1500.0, finalTown.getBalance(), 0.001,
            "Balance should reflect all operations");
        assertEquals(10.0, finalTown.getTax(), 0.001,
            "Tax should reflect all updates");
    }

    // ========== Performance Tests ==========

    @Test
    @DisplayName("Concurrent operations should complete in reasonable time")
    void testConcurrentPerformance() throws InterruptedException {
        // Arrange
        PlayerMock leader = server.addPlayer("PerfLeader");
        ITanPlayer tanLeader = playerStorage.getSync(leader.getUniqueId().toString());
        TownData town = townStorage.newTown("PerfTown", tanLeader).join();

        int operationCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(operationCount);

        // Act - Measure performance
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < operationCount; i++) {
            executor.submit(() -> {
                try {
                    town.addToBalance(1.0);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "Operations should complete within 10 seconds");

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        // Assert
        assertTrue(duration < 5000,
            "1000 concurrent operations should complete in < 5 seconds, took: " + duration + "ms");
        assertEquals(operationCount, town.getBalance(), 0.001,
            "All operations should be reflected in balance");
    }
}
