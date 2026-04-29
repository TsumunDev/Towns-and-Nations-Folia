package org.leralix.tan.domain.territory;

import java.util.*;
import org.leralix.tan.dataclass.RankData;

/**
 * Stateless rank operations for a territory.
 * Operates on the ranks map provided by TerritoryData (not its own copy).
 */
public class TerritoryRankService {

    private final Map<Integer, RankData> ranks;

    public TerritoryRankService(Map<Integer, RankData> ranks) {
        this.ranks = ranks;
    }

    public Map<Integer, RankData> getRanks() {
        if (ranks == null) {
            return new HashMap<>();
        }
        return ranks;
    }

    public Collection<RankData> getAllRanks() {
        return getRanks().values();
    }

    public Collection<RankData> getAllRanksSorted() {
        return getRanks().values().stream()
            .sorted(Comparator.comparingInt(p -> -p.getLevel()))
            .toList();
    }

    public RankData getRank(int rankID) {
        return getRanks().get(rankID);
    }

    public int getNumberOfRank() {
        return getRanks().size();
    }

    public boolean isRankNameUsed(String name) {
        for (RankData rank : getAllRanks()) {
            if (rank.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public RankData registerNewRank(String rankName) {
        int nextRankId = 0;
        for (RankData rank : getAllRanks()) {
            if (rank.getID() >= nextRankId) nextRankId = rank.getID() + 1;
        }
        RankData newRank = new RankData(nextRankId, rankName);
        getRanks().put(nextRankId, newRank);
        return newRank;
    }

    public void removeRank(int key) {
        getRanks().remove(key);
    }
}
