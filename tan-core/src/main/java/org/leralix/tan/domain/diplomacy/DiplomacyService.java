package org.leralix.tan.domain.diplomacy;

import org.leralix.tan.dataclass.DiplomacyProposal;
import org.leralix.tan.dataclass.RelationData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.war.WarComponent;
import org.leralix.tan.enums.TownRelation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing diplomatic relations between territories.
 *
 * <p>This service extracts diplomacy-related logic from TerritoryData following the
 * Strangler Fig Pattern. It provides async operations for diplomatic management,
 * suitable for Folia's region-based threading.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Relation management (alliances, wars, neutral, etc.)</li>
 *   <li>Diplomatic proposals (sent/received)</li>
 *   <li>Overlord/vassal relationships</li>
 *   <li>Vassalization proposals</li>
 *   <li>War status tracking</li>
 * </ul>
 *
 * @see DiplomacyServiceImpl
 * @see DiplomacyHolder
 * @since 2.0.0
 */
public interface DiplomacyService {

    // ===== Relations =====

    /**
     * Gets the relation data for a territory.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing the relation data (empty if territory not found)
     */
    CompletableFuture<RelationData> getRelations(String territoryId);

    /**
     * Gets the relation with another territory.
     *
     * @param territoryId The territory ID
     * @param otherTerritoryId The other territory ID
     * @return CompletableFuture containing the relation (NEUTRAL if either not found)
     */
    CompletableFuture<TownRelation> getRelationWith(String territoryId, String otherTerritoryId);

    /**
     * Sets the relation with another territory (bidirectional).
     *
     * @param territoryId The territory ID
     * @param otherTerritory The other territory
     * @param newRelation The new relation to set
     * @return CompletableFuture that completes when the relation is updated
     */
    CompletableFuture<Void> setRelation(String territoryId, TerritoryData otherTerritory, TownRelation newRelation);

    // ===== Diplomatic Proposals =====

    /**
     * Gets all diplomatic proposals for a territory.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing the proposals collection
     */
    CompletableFuture<Collection<DiplomacyProposal>> getDiplomaticProposals(String territoryId);

    /**
     * Gets the diplomatic proposals map for a territory.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing the proposals map (empty if territory not found)
     */
    CompletableFuture<Map<String, DiplomacyProposal>> getDiplomaticProposalsMap(String territoryId);

    /**
     * Adds a diplomatic proposal from another territory.
     *
     * @param territoryId The territory ID
     * @param proposingTerritory The proposing territory
     * @param wantedRelation The desired relation
     * @return CompletableFuture that completes when the proposal is added
     */
    CompletableFuture<Void> receiveDiplomaticProposal(String territoryId, TerritoryData proposingTerritory, TownRelation wantedRelation);

    /**
     * Removes a diplomatic proposal.
     *
     * @param territoryId The territory ID
     * @param proposingTerritoryId The proposing territory ID
     * @return CompletableFuture that completes when the proposal is removed
     */
    CompletableFuture<Void> removeDiplomaticProposal(String territoryId, String proposingTerritoryId);

    // ===== Overlord/Vassal =====

    /**
     * Gets the overlord of a territory.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing the overlord (empty if no overlord)
     */
    CompletableFuture<Optional<TerritoryData>> getOverlord(String territoryId);

    /**
     * Checks if a territory has an overlord.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing true if the territory has an overlord
     */
    CompletableFuture<Boolean> hasOverlord(String territoryId);

    /**
     * Gets the vassals of a territory.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing the vassal list
     */
    CompletableFuture<List<TerritoryData>> getVassals(String territoryId);

    /**
     * Gets the vassal IDs of a territory.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing the vassal ID list
     */
    CompletableFuture<List<String>> getVassalIds(String territoryId);

    /**
     * Checks if another territory is a vassal.
     *
     * @param territoryId The territory ID
     * @param otherTerritoryId The other territory ID
     * @return CompletableFuture containing true if the other is a vassal
     */
    CompletableFuture<Boolean> isVassal(String territoryId, String otherTerritoryId);

    /**
     * Sets an overlord for a territory.
     *
     * @param territoryId The territory ID
     * @param overlord The overlord territory
     * @return CompletableFuture that completes when the overlord is set
     */
    CompletableFuture<Void> setOverlord(String territoryId, TerritoryData overlord);

    /**
     * Removes the overlord from a territory.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture that completes when the overlord is removed
     */
    CompletableFuture<Void> removeOverlord(String territoryId);

    // ===== Vassalization Proposals =====

    /**
     * Gets the overlord proposals for a territory.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing the proposal ID list
     */
    CompletableFuture<List<String>> getOverlordProposals(String territoryId);

    /**
     * Checks if the territory has a vassalization proposal from an overlord.
     *
     * @param territoryId The territory ID
     * @param overlordId The overlord ID
     * @return CompletableFuture containing true if a proposal exists
     */
    CompletableFuture<Boolean> hasVassalisationProposal(String territoryId, String overlordId);

    /**
     * Adds a vassalization proposal.
     *
     * @param territoryId The territory ID
     * @param overlordId The overlord territory ID
     * @return CompletableFuture that completes when the proposal is added
     */
    CompletableFuture<Void> addVassalisationProposal(String territoryId, String overlordId);

    /**
     * Removes a vassalization proposal.
     *
     * @param territoryId The territory ID
     * @param overlordId The overlord territory ID
     * @return CompletableFuture that completes when the proposal is removed
     */
    CompletableFuture<Void> removeVassalisationProposal(String territoryId, String overlordId);

    // ===== War Status =====

    /**
     * Gets the war component for a territory.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing the war component (empty if territory not found)
     */
    CompletableFuture<Optional<WarComponent>> getWarComponent(String territoryId);

    /**
     * Checks if a territory is at war.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing true if at war
     */
    CompletableFuture<Boolean> isAtWar(String territoryId);

    /**
     * Gets the IDs of attacks the territory is involved in.
     *
     * @param territoryId The territory ID
     * @return CompletableFuture containing the attack ID list
     */
    CompletableFuture<Collection<String>> getAttacksInvolvedIds(String territoryId);

    /**
     * Checks if this service is enabled via feature flag.
     *
     * @return true if the new diplomacy service is enabled
     */
    boolean isEnabled();
}
