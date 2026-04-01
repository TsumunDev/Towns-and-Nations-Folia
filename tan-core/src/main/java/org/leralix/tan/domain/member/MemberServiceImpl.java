package org.leralix.tan.domain.member;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.RankData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of MemberService.
 * Extracts member-related logic from TownData following the Strangler Fig Pattern.
 *
 * <p>This service is enabled via the feature flag {@code development.use-new-member-service}.</p>
 *
 * @see MemberService
 * @since 2.0.0
 */
public class MemberServiceImpl implements MemberService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemberServiceImpl.class);

    private final TownDataStorage townStorage;

    /**
     * Creates a new MemberServiceImpl.
     *
     * @param townStorage The town data storage
     */
    public MemberServiceImpl(TownDataStorage townStorage) {
        this.townStorage = townStorage;
    }

    // ===== Player Management =====

    @Override
    public CompletableFuture<Void> addPlayer(String townId, ITanPlayer player) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.addPlayer(player);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public CompletableFuture<Void> removePlayer(String townId, String playerId) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.removePlayer(playerId);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public CompletableFuture<Void> removePlayer(String townId, ITanPlayer player) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.removePlayer(player);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public CompletableFuture<Collection<String>> getPlayerIDs(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return Collections.emptyList();
                }
                return town.getPlayerIDList();
            });
    }

    @Override
    public CompletableFuture<Collection<ITanPlayer>> getPlayers(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return Collections.emptyList();
                }
                return town.getITanPlayerList();
            });
    }

    @Override
    public CompletableFuture<Integer> getPlayerCount(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return 0;
                }
                return town.getITanPlayerList().size();
            });
    }

    @Override
    public CompletableFuture<Boolean> isMember(String townId, String playerId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return false;
                }
                return town.getPlayerIDList().contains(playerId);
            });
    }

    // ===== Join Requests =====

    @Override
    public CompletableFuture<Void> addJoinRequest(String townId, Player player) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.addPlayerJoinRequest(player);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public CompletableFuture<Void> addJoinRequest(String townId, String playerUuid) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.addPlayerJoinRequest(playerUuid);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public CompletableFuture<Void> removeJoinRequest(String townId, Player player) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.removePlayerJoinRequest(player);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public CompletableFuture<Void> removeJoinRequest(String townId, String playerUuid) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.removePlayerJoinRequest(playerUuid);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public CompletableFuture<Boolean> hasJoinRequest(String townId, String playerUuid) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return false;
                }
                return town.isPlayerAlreadyRequested(playerUuid);
            });
    }

    @Override
    public CompletableFuture<Set<String>> getJoinRequests(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return Collections.emptySet();
                }
                return town.getPlayerJoinRequestSet();
            });
    }

    // ===== Leader/Ownership =====

    @Override
    public CompletableFuture<ITanPlayer> getLeader(String townId) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                return town.getLeaderDataAsync();
            });
    }

    @Override
    public CompletableFuture<String> getLeaderId(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return null;
                }
                return town.getLeaderID();
            });
    }

    @Override
    public CompletableFuture<Void> setLeader(String townId, String leaderId) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                town.setLeaderID(leaderId);
                return townStorage.updateAsync(town);
            });
    }

    @Override
    public CompletableFuture<Boolean> isLeader(String townId, String playerId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return false;
                }
                return town.isLeader(playerId);
            });
    }

    @Override
    public CompletableFuture<Boolean> hasNoLeader(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return true;
                }
                return town.haveNoLeader();
            });
    }

    // ===== Ranks =====

    @Override
    public CompletableFuture<RankData> getRank(String townId, ITanPlayer player) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return null;
                }
                return town.getRank(player);
            });
    }

    @Override
    public CompletableFuture<RankData> getDefaultRank(String townId) {
        return townStorage.get(townId)
            .thenApply(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return null;
                }
                return town.getTownDefaultRank();
            });
    }

    // ===== Kick =====

    @Override
    public CompletableFuture<Void> kickPlayer(String townId, OfflinePlayer kickedPlayer) {
        return townStorage.get(townId)
            .thenCompose(town -> {
                if (town == null) {
                    LOGGER.warn("MemberService: Town not found: " + townId);
                    return CompletableFuture.completedFuture(null);
                }
                return town.kickPlayerAsync(kickedPlayer);
            });
    }

    // ===== Feature Flag =====

    @Override
    public boolean isEnabled() {
        TownsAndNations plugin = TownsAndNations.getPlugin();
        if (plugin == null) {
            return false; // Plugin not initialized (e.g., during tests)
        }
        return plugin.getConfig()
            .getBoolean("development.use-new-member-service", false);
    }
}
