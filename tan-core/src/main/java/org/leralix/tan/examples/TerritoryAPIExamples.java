package org.leralix.tan.examples;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.rank.RankData;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

/**
 * Code examples demonstrating usage of Territory APIs.
 *
 * <p>This class provides practical examples for common operations with towns and regions,
 * including creation, member management, chunk claiming, tax collection, and diplomacy.</p>
 *
 * <h2>Table of Contents:</h2>
 * <ul>
 *   <li>{@link #createTownExample} - Creating a new town</li>
 *   <li>{@link #addMemberExample} - Adding members to a town</li>
 *   <li>{@link #claimChunkExample} - Claiming chunks for a town</li>
 *   <li>{@link #collectTaxesExample} - Collecting taxes from members</li>
 *   <li>{@link #manageRanksExample} - Creating and managing custom ranks</li>
 *   <li>{@link #createRegionExample} - Creating a region (nation)</li>
 *   <li>{@link #vassalManagementExample} - Managing vassal towns</li>
 *   <li>{@link #diplomacyExample} - Managing diplomatic relations</li>
 * </ul>
 *
 * @since 0.16.0
 */
public class TerritoryAPIExamples {

    /**
     * Example: Creating a new town.
     *
     * <p>This example demonstrates how to create a new town with a player as the leader.
     * Town creation automatically initializes all components (treasury, taxes, diplomacy, etc.)</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link TownDataStorage#newTown} for asynchronous creation</li>
     *   <li>The creator becomes the town leader automatically</li>
     *   <li>New towns start with 0 balance and default tax rate</li>
     *   <li>A default rank is created automatically</li>
     * </ul>
     *
     * @param player The player who will become the town leader
     * @param townName The name for the new town
     * @return CompletableFuture that completes with the created TownData
     */
    public static CompletableFuture<TownData> createTownExample(Player player, String townName) {
        // Get the player's ITanPlayer representation
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());

        // Create the town asynchronously
        return TownDataStorage.getInstance().newTown(townName, tanPlayer)
            .thenApply(town -> {
                // Town created successfully
                // You can now perform additional setup
                town.setTax(10.0); // Set tax rate to 10%
                return town;
            });
    }

    /**
     * Example: Adding members to a town.
     *
     * <p>This example shows how to add players to a town and manage their membership.
     * New members are assigned the default rank automatically.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link TownData#addPlayer} to add members</li>
     *   <li>New members get the default rank (see {@link TownData#getDefaultRankID})</li>
     *   <li>The player's town membership is updated automatically</li>
     *   <li>Use {@link TownData#isPlayerInTown} to check membership</li>
     * </ul>
     *
     * @param town The town to add members to
     * @param newMember The player to add as a member
     */
    public static void addMemberExample(TownData town, Player newMember) {
        // Get the player's ITanPlayer representation
        ITanPlayer tanMember = PlayerDataStorage.getInstance().getSync(newMember.getUniqueId().toString());

        // Add the player to the town
        town.addPlayer(tanMember);

        // Verify the player was added
        if (town.isPlayerInTown(tanMember)) {
            // Member added successfully
            newMember.sendMessage("You have joined " + town.getName());
        }
    }

    /**
     * Example: Claiming chunks for a town.
     *
     * <p>This example demonstrates how to claim land for a town.
     * Chunk claims extend the town's territory and enable town-specific features.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link TownData#claimChunk} to claim territory</li>
     *   <li>Each claim has a cost (money) that is deducted from town balance</li>
     *   <li>Claims are bounded by chunk coordinates (16x16 block areas)</li>
     *   <li>Use {@link TownData#hasClaimedChunk} to check ownership</li>
     * </ul>
     *
     * @param town The town claiming the chunk
     * @param claimLocation The location within the chunk to claim
     */
    public static void claimChunkExample(TownData town, Location claimLocation) {
        // Check if town can afford the claim
        double claimCost = 100.0; // Example cost
        if (town.getBalance() < claimCost) {
            throw new IllegalStateException("Insufficient funds to claim chunk");
        }

        // Claim the chunk
        town.claimChunk(claimLocation);

        // Verify the claim
        if (town.hasClaimedChunk(claimLocation)) {
            // Chunk claimed successfully
            town.removeFromBalance(claimCost);
        }
    }

    /**
     * Example: Collecting taxes from town members.
     *
     * <p>This example shows how to collect taxes from all members of a town.
     * Tax collection can be asynchronous and processes all members in parallel.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link TownData#collectTax} to collect taxes from all members</li>
     *   <li>Tax amount is determined by {@link TownData#getTax()} rate</li>
     *   <li>Tax collection is asynchronous for performance</li>
     *   <li>Members with insufficient funds are skipped gracefully</li>
     * </ul>
     *
     * @param town The town collecting taxes
     * @return CompletableFuture that completes when tax collection finishes
     */
    public static CompletableFuture<Void> collectTaxesExample(TownData town) {
        // Collect taxes from all members asynchronously
        return town.collectTax()
            .thenAccept(result -> {
                // Tax collection completed
                double totalCollected = town.getBalance(); // Simplified example
                System.out.println("Collected " + totalCollected + " in taxes");
            });
    }

    /**
     * Example: Creating and managing custom ranks.
     *
     * <p>This example demonstrates how to create custom ranks and assign them to players.
     * Ranks define permissions and hierarchy within a town.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link TownData#registerNewRank} to create custom ranks</li>
     *   <li>Use {@link TownData#setRank} to assign a rank to a player</li>
     *   <li>Use {@link TownData#getPlayerRank} to get a player's current rank</li>
     *   <li>Ranks can have custom permissions (see {@link RankData})</li>
     * </ul>
     *
     * @param town The town to create ranks in
     * @param player The player to assign the custom rank to
     */
    public static void manageRanksExample(TownData town, Player player) {
        // Create a custom rank
        RankData vipRank = town.registerNewRank("VIP");
        vipRank.setLevel(2); // Higher level than default (1)

        // Assign the rank to a player
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player.getUniqueId().toString());
        town.setRank(tanPlayer, vipRank);

        // Verify the rank assignment
        RankData playerRank = town.getPlayerRank(tanPlayer);
        if (playerRank.getID().equals(vipRank.getID())) {
            player.sendMessage("You have been promoted to VIP!");
        }
    }

    /**
     * Example: Creating a region (nation).
     *
     * <p>This example shows how to create a region/nation and add towns as vassals.
     * Regions can collect taxes from vassal towns and manage inter-town diplomacy.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link RegionDataStorage#newRegion} to create a region</li>
     *   <li>Use {@link RegionData#addVassal} to add towns as vassals</li>
     *   <li>Vassal towns pay taxes to their overlord region</li>
     *   <li>Use {@link RegionData#setCapital} to designate a capital town</li>
     * </ul>
     *
     * @param leader The player who will become the region leader
     * @param regionName The name for the new region
     * @param capitalTown The town to designate as the capital
     * @return CompletableFuture that completes with the created RegionData
     */
    public static CompletableFuture<RegionData> createRegionExample(
            Player leader,
            String regionName,
            TownData capitalTown) {

        // Get the leader's ITanPlayer representation
        ITanPlayer tanLeader = PlayerDataStorage.getInstance().getSync(leader.getUniqueId().toString());

        // Create the region asynchronously
        return RegionDataStorage.getInstance().newRegion(regionName, tanLeader)
            .thenApply(region -> {
                // Add the capital town as a vassal
                region.addVassal(capitalTown);

                // Set it as the capital
                region.setCapital(capitalTown.getID());

                return region;
            });
    }

    /**
     * Example: Managing vassal towns.
     *
     * <p>This example demonstrates how to manage vassal relationships between regions and towns.
     * Vassal towns can be added or removed, and the region can collect taxes from them.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link RegionData#addVassal} to add a vassal town</li>
     *   <li>Use {@link RegionData#removeVassal} to remove a vassal</li>
     *   <li>Use {@link RegionData#getVassals} to list all vassals</li>
     *   <li>Use {@link RegionData#isVassal} to check vassal status</li>
     *   <li>Vassal relationships are bidirectional (town has overlord, region has vassal)</li>
     * </ul>
     *
     * @param region The region managing vassals
     * @param town The town to add as a vassal
     */
    public static void vassalManagementExample(RegionData region, TownData town) {
        // Add town as a vassal
        region.addVassal(town);

        // Verify the vassal relationship
        if (region.isVassal(town)) {
            // Vassal added successfully
            // The town now recognizes the region as its overlord
            if (town.haveOverlord() && town.getOverlordID().equals(region.getID())) {
                System.out.println(town.getName() + " is now a vassal of " + region.getName());
            }
        }

        // Get all vassals
        Collection<TownData> vassals = region.getVassals();
        System.out.println("Region has " + vassals.size() + " vassals");

        // Remove a vassal (if needed)
        // region.removeVassal(town.getID());
    }

    /**
     * Example: Managing diplomatic relations.
     *
     * <p>This example shows how to establish and manage diplomatic relations between territories.
     * Relations affect trade, taxes, and PvP behavior between territories.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link TownData#setRelation} to set diplomatic relations</li>
     *   <li>Use {@link TownData#getRelation} to check relations with another territory</li>
     *   <li>Available relations: NEUTRAL, ALLY, ENEMY, OVERLORD, VASSAL</li>
     *   <li>Relations are bidirectional (setting affects both territories)</li>
     * </ul>
     *
     * @param town1 The first territory
     * @param town2 The second territory
     */
    public static void diplomacyExample(TownData town1, TownData town2) {
        // Establish an alliance
        town1.setRelation(town2.getID(), TownRelation.ALLY);

        // Check the relation
        TownRelation relation = town1.getRelation(town2.getID());
        if (relation == TownRelation.ALLY) {
            System.out.println(town1.getName() + " and " + town2.getName() + " are now allies");
        }

        // Change to enemy relation
        town1.setRelation(town2.getID(), TownRelation.ENEMY);

        // Check from the other territory's perspective
        TownRelation fromPerspective = town2.getRelation(town1.getID());
        System.out.println("Relation from " + town2.getName() + ": " + fromPerspective);
    }

    /**
     * Example: Async territory access pattern.
     *
     * <p>This example demonstrates the recommended pattern for accessing territory data asynchronously.
     * Always use async methods to avoid blocking the main thread in Folia.</p>
     *
     * <h3>Key Points:</h3>
     * <ul>
     *   <li>Use {@link TownDataStorage#get} for async town lookup</li>
     *   <li>Chain operations with {@link CompletableFuture#thenCompose}</li>
     *   <li>Use {@link CompletableFuture#thenAccept} for final operations</li>
     *   <li>Avoid blocking operations (getSync) in async pipelines</li>
     * </ul>
     *
     * @param townId The ID of the town to look up
     * @param player The player to send results to
     */
    public static void asyncAccessExample(String townId, Player player) {
        // Async town lookup
        TownDataStorage.getInstance().get(townId)
            .thenAccept(town -> {
                if (town == null) {
                    player.sendMessage("Town not found");
                    return;
                }

                // Access town data safely
                String townName = town.getName();
                double balance = town.getBalance();
                int memberCount = town.getITanPlayerList().size();

                player.sendMessage("Town: " + townName);
                player.sendMessage("Balance: " + balance);
                player.sendMessage("Members: " + memberCount);
            })
            .exceptionally(throwable -> {
                // Handle errors
                player.sendMessage("Error loading town data");
                throwable.printStackTrace();
                return null;
            });
    }
}
