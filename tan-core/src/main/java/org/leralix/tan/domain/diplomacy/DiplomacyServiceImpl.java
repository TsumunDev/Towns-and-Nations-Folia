package org.leralix.tan.domain.diplomacy;

import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.DiplomacyProposal;
import org.leralix.tan.dataclass.RelationData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.diplomacy.DiplomacyComponent;
import org.leralix.tan.dataclass.territory.war.WarComponent;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.events.EventManager;
import org.leralix.tan.events.events.DiplomacyProposalAcceptedInternalEvent;
import org.leralix.tan.events.events.DiplomacyProposalInternalEvent;
import org.leralix.tan.lang.FilledLang;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.stored.NationDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.graphic.TeamUtils;
import org.leralix.tan.utils.gameplay.TerritoryUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Implementation of DiplomacyService.
 * Extracts diplomacy-related logic from TerritoryData following the Strangler Fig Pattern.
 *
 * <p>This service is enabled via the feature flag {@code development.use-new-diplomacy-service}.</p>
 *
 * @see DiplomacyService
 * @since 2.0.0
 */
public class DiplomacyServiceImpl implements DiplomacyService {

    private static final Logger LOGGER = Logger.getLogger(DiplomacyServiceImpl.class.getName());

    private final TownDataStorage townStorage;

    /**
     * Creates a new DiplomacyServiceImpl.
     *
     * @param townStorage The town data storage
     */
    public DiplomacyServiceImpl(TownDataStorage townStorage) {
        this.townStorage = townStorage;
    }

    // ===== Relations =====

    @Override
    public CompletableFuture<RelationData> getRelations(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return new RelationData();
                }
                return territory.getRelations();
            });
    }

    @Override
    public CompletableFuture<TownRelation> getRelationWith(String territoryId, String otherTerritoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return TownRelation.NEUTRAL;
                }
                return territory.getRelationWith(otherTerritoryId);
            });
    }

    @Override
    public CompletableFuture<Void> setRelation(String territoryId, TerritoryData otherTerritory, TownRelation newRelation) {
        return resolveTerritory(territoryId)
            .thenCompose(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return CompletableFuture.completedFuture(null);
                }
                TownRelation oldRelation = territory.getRelationWith(otherTerritory);
                EventManager.getInstance()
                    .callEvent(new DiplomacyProposalAcceptedInternalEvent(otherTerritory, territory, oldRelation, newRelation));
                territory.getRelations().setRelation(newRelation, otherTerritory);
                otherTerritory.getRelations().setRelation(newRelation, territory);
                TeamUtils.updateAllScoreboardColor();
                return CompletableFuture.completedFuture(null);
            });
    }

    // ===== Diplomatic Proposals =====

    @Override
    public CompletableFuture<Collection<DiplomacyProposal>> getDiplomaticProposals(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return Collections.emptyList();
                }
                return territory.getAllDiplomacyProposal();
            });
    }

    @Override
    public CompletableFuture<Map<String, DiplomacyProposal>> getDiplomaticProposalsMap(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return Collections.emptyMap();
                }
                return Collections.emptyMap();
            });
    }

    @Override
    public CompletableFuture<Void> receiveDiplomaticProposal(String territoryId, TerritoryData proposingTerritory, TownRelation wantedRelation) {
        return resolveTerritory(territoryId)
            .thenCompose(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return CompletableFuture.completedFuture(null);
                }
                territory.removeDiplomaticProposal(proposingTerritory);
                addDiplomaticProposalToTown(territory, proposingTerritory, wantedRelation);
                return CompletableFuture.completedFuture(null);
            });
    }

    @Override
    public CompletableFuture<Void> removeDiplomaticProposal(String territoryId, String proposingTerritoryId) {
        return resolveTerritory(territoryId)
            .thenCompose(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return CompletableFuture.completedFuture(null);
                }
                territory.removeDiplomaticProposal(proposingTerritoryId);
                return CompletableFuture.completedFuture(null);
            });
    }

    // ===== Overlord/Vassal =====

    @Override
    public CompletableFuture<Optional<TerritoryData>> getOverlord(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return Optional.empty();
                }
                return territory.getOverlord();
            });
    }

    @Override
    public CompletableFuture<Boolean> hasOverlord(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return false;
                }
                return territory.haveOverlord();
            });
    }

    @Override
    public CompletableFuture<List<TerritoryData>> getVassals(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return Collections.emptyList();
                }
                return territory.getVassals();
            });
    }

    @Override
    public CompletableFuture<List<String>> getVassalIds(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return Collections.emptyList();
                }
                return territory.getVassalsID();
            });
    }

    @Override
    public CompletableFuture<Boolean> isVassal(String territoryId, String otherTerritoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return false;
                }
                return territory.isVassal(otherTerritoryId);
            });
    }

    @Override
    public CompletableFuture<Void> setOverlord(String territoryId, TerritoryData overlord) {
        return resolveTerritory(territoryId)
            .thenCompose(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return CompletableFuture.completedFuture(null);
                }
                territory.setOverlord(overlord);
                return CompletableFuture.completedFuture(null);
            });
    }

    @Override
    public CompletableFuture<Void> removeOverlord(String territoryId) {
        return resolveTerritory(territoryId)
            .thenCompose(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return CompletableFuture.completedFuture(null);
                }
                territory.removeOverlord();
                return CompletableFuture.completedFuture(null);
            });
    }

    // ===== Vassalization Proposals =====

    @Override
    public CompletableFuture<List<String>> getOverlordProposals(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return Collections.emptyList();
                }
                return Collections.emptyList();
            });
    }

    @Override
    public CompletableFuture<Boolean> hasVassalisationProposal(String territoryId, String overlordId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return false;
                }
                TerritoryData overlord = TerritoryUtil.getTerritory(overlordId);
                return overlord != null && territory.containsVassalisationProposal(overlord);
            });
    }

    @Override
    public CompletableFuture<Void> addVassalisationProposal(String territoryId, String overlordId) {
        return resolveTerritory(territoryId)
            .thenCompose(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return CompletableFuture.completedFuture(null);
                }
                TerritoryData overlord = TerritoryUtil.getTerritory(overlordId);
                if (overlord != null) {
                    territory.addVassalisationProposal(overlord);
                }
                return CompletableFuture.completedFuture(null);
            });
    }

    @Override
    public CompletableFuture<Void> removeVassalisationProposal(String territoryId, String overlordId) {
        return resolveTerritory(territoryId)
            .thenCompose(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return CompletableFuture.completedFuture(null);
                }
                TerritoryData overlord = TerritoryUtil.getTerritory(overlordId);
                if (overlord != null) {
                    territory.removeVassalisationProposal(overlord);
                }
                return CompletableFuture.completedFuture(null);
            });
    }

    // ===== War Status =====

    @Override
    public CompletableFuture<Optional<WarComponent>> getWarComponent(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return Optional.empty();
                }
                return Optional.empty();
            });
    }

    @Override
    public CompletableFuture<Boolean> isAtWar(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return false;
                }
                return territory.isAtWar();
            });
    }

    @Override
    public CompletableFuture<Collection<String>> getAttacksInvolvedIds(String territoryId) {
        return resolveTerritory(territoryId)
            .thenApply(territory -> {
                if (territory == null) {
                    LOGGER.warning("DiplomacyService: Territory not found: " + territoryId);
                    return Collections.emptyList();
                }
                return territory.getAttacksInvolvedID();
            });
    }

    @Override
    public boolean isEnabled() {
        TownsAndNations plugin = TownsAndNations.getPlugin();
        if (plugin == null) {
            return false; // Plugin not initialized (e.g., during tests)
        }
        return plugin.getConfig()
            .getBoolean("development.use-new-diplomacy-service", false);
    }

    // ===== Helper Methods =====

    /**
     * Helper method to add a diplomatic proposal to a town.
     * Mirrors the logic in TerritoryData.addDiplomaticProposal.
     */
    private void addDiplomaticProposalToTown(TerritoryData town, TerritoryData proposingTerritory, TownRelation wantedRelation) {
        EventManager.getInstance()
            .callEvent(new DiplomacyProposalInternalEvent(town, proposingTerritory, wantedRelation));
    }

    private CompletableFuture<TerritoryData> resolveTerritory(String territoryId) {
        if (territoryId == null || territoryId.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        if (territoryId.startsWith("N")) {
            return NationDataStorage.getInstance().get(territoryId).thenApply(t -> t);
        }
        if (territoryId.startsWith("R")) {
            return RegionDataStorage.getInstance().get(territoryId).thenApply(t -> t);
        }
        return townStorage.get(territoryId).thenApply(t -> t);
    }
}
