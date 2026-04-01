package org.leralix.tan.domain.member;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.RankData;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing members within a town.
 * Extracts member-related logic from TownData following the Strangler Fig Pattern.
 *
 * <p>This service handles:</p>
 * <ul>
 *   <li>Player management (add, remove, get)</li>
 *   <li>Player join requests</li>
 *   <li>Leader/ownership management</li>
 *   <li>Rank management</li>
 * </ul>
 *
 * @since 2.0.0
 */
public interface MemberService {

    // ===== Player Management =====

    /**
     * Adds a player to the town.
     *
     * @param townId The town ID
     * @param playerId The player to add
     * @return CompletableFuture that completes when the player is added
     */
    CompletableFuture<Void> addPlayer(String townId, ITanPlayer playerId);

    /**
     * Removes a player from the town by ID.
     *
     * @param townId The town ID
     * @param playerId The player ID to remove
     * @return CompletableFuture that completes when the player is removed
     */
    CompletableFuture<Void> removePlayer(String townId, String playerId);

    /**
     * Removes a player from the town.
     *
     * @param townId The town ID
     * @param player The player to remove
     * @return CompletableFuture that completes when the player is removed
     */
    CompletableFuture<Void> removePlayer(String townId, ITanPlayer player);

    /**
     * Gets the list of player IDs in the town.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the collection of player IDs
     */
    CompletableFuture<Collection<String>> getPlayerIDs(String townId);

    /**
     * Gets the list of players in the town.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the collection of players
     */
    CompletableFuture<Collection<ITanPlayer>> getPlayers(String townId);

    /**
     * Gets the current player count in the town.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the player count
     */
    CompletableFuture<Integer> getPlayerCount(String townId);

    /**
     * Checks if a player is a member of the town.
     *
     * @param townId The town ID
     * @param playerId The player ID to check
     * @return CompletableFuture containing true if the player is a member
     */
    CompletableFuture<Boolean> isMember(String townId, String playerId);

    // ===== Join Requests =====

    /**
     * Adds a player join request to the town.
     *
     * @param townId The town ID
     * @param player The player requesting to join
     * @return CompletableFuture that completes when the request is added
     */
    CompletableFuture<Void> addJoinRequest(String townId, Player player);

    /**
     * Adds a player join request to the town by UUID.
     *
     * @param townId The town ID
     * @param playerUuid The player UUID requesting to join
     * @return CompletableFuture that completes when the request is added
     */
    CompletableFuture<Void> addJoinRequest(String townId, String playerUuid);

    /**
     * Removes a player join request from the town.
     *
     * @param townId The town ID
     * @param player The player whose request to remove
     * @return CompletableFuture that completes when the request is removed
     */
    CompletableFuture<Void> removeJoinRequest(String townId, Player player);

    /**
     * Removes a player join request from the town by UUID.
     *
     * @param townId The town ID
     * @param playerUuid The player UUID whose request to remove
     * @return CompletableFuture that completes when the request is removed
     */
    CompletableFuture<Void> removeJoinRequest(String townId, String playerUuid);

    /**
     * Checks if a player has already requested to join the town.
     *
     * @param townId The town ID
     * @param playerUuid The player UUID to check
     * @return CompletableFuture containing true if the player has requested
     */
    CompletableFuture<Boolean> hasJoinRequest(String townId, String playerUuid);

    /**
     * Gets the set of player join requests for the town.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the set of player UUIDs
     */
    CompletableFuture<Set<String>> getJoinRequests(String townId);

    // ===== Leader/Ownership =====

    /**
     * Gets the town's leader/mayor.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the leader player, or empty if none
     */
    CompletableFuture<ITanPlayer> getLeader(String townId);

    /**
     * Gets the leader's player ID.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the leader ID, or empty if none
     */
    CompletableFuture<String> getLeaderId(String townId);

    /**
     * Sets a new leader for the town.
     *
     * @param townId The town ID
     * @param leaderId The new leader's ID
     * @return CompletableFuture that completes when the leader is set
     */
    CompletableFuture<Void> setLeader(String townId, String leaderId);

    /**
     * Checks if a player is the leader of the town.
     *
     * @param townId The town ID
     * @param playerId The player ID to check
     * @return CompletableFuture containing true if the player is the leader
     */
    CompletableFuture<Boolean> isLeader(String townId, String playerId);

    /**
     * Checks if the town has no leader.
     *
     * @param townId The town ID
     * @return CompletableFuture containing true if the town has no leader
     */
    CompletableFuture<Boolean> hasNoLeader(String townId);

    // ===== Ranks =====

    /**
     * Gets a player's rank within the town.
     *
     * @param townId The town ID
     * @param player The player to get the rank for
     * @return CompletableFuture containing the rank data
     */
    CompletableFuture<RankData> getRank(String townId, ITanPlayer player);

    /**
     * Gets the town's default rank.
     *
     * @param townId The town ID
     * @return CompletableFuture containing the default rank data
     */
    CompletableFuture<RankData> getDefaultRank(String townId);

    // ===== Kick =====

    /**
     * Kicks a player from the town.
     *
     * @param townId The town ID
     * @param kickedPlayer The player to kick
     * @return CompletableFuture that completes when the player is kicked
     */
    CompletableFuture<Void> kickPlayer(String townId, OfflinePlayer kickedPlayer);

    // ===== Feature Flag =====

    /**
     * Checks if this service is enabled via feature flag.
     *
     * @return true if the service is enabled
     */
    boolean isEnabled();
}
