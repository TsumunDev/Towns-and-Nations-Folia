package org.leralix.tan.domain.territory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.leralix.lib.utils.RandomUtil;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.RankData;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.dataclass.newhistory.ChunkPaymentHistory;
import org.leralix.tan.dataclass.newhistory.SalaryPaymentHistory;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.economy.SalaryPaymentLine;
import org.leralix.tan.dataclass.territory.economy.ChunkUpkeepLine;
import org.leralix.tan.dataclass.territory.economy.Budget;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.utils.territory.ChunkUtil;

/**
 * Manages periodic territory tasks: salary payments, chunk upkeep, and tax collection.
 */
public class TerritoryTaskService {

    public void addCommonTaxes(TerritoryData territory, Budget budget) {
        budget.addProfitLine(new SalaryPaymentLine(territory));
        budget.addProfitLine(new ChunkUpkeepLine(territory));
    }

    public CompletableFuture<Void> paySalariesAsync(TerritoryData territory) {
        List<CompletableFuture<Void>> rankFutures = new ArrayList<>();
        for (RankData rank : territory.getAllRanks()) {
            int rankSalary = rank.getSalary();
            List<String> playerIdList = rank.getPlayersID();
            double costOfSalary = (double) playerIdList.size() * rankSalary;
            if (rankSalary == 0 || costOfSalary > territory.getBalance()) {
                continue;
            }
            territory.removeFromBalance(costOfSalary);
            for (String playerId : playerIdList) {
                CompletableFuture<Void> paymentFuture = PlayerDataStorage.getInstance()
                    .get(playerId)
                    .thenCompose(tanPlayer -> {
                        return org.leralix.tan.service.AsyncEconomyService.deposit(
                                tanPlayer.getOfflinePlayer(), rankSalary)
                            .thenRun(() -> {
                                TownsAndNations.getPlugin()
                                    .getDatabaseHandler()
                                    .addTransactionHistory(
                                        new SalaryPaymentHistory(territory, String.valueOf(rank.getID()), costOfSalary));
                            });
                    });
                rankFutures.add(paymentFuture);
            }
        }
        return CompletableFuture.allOf(rankFutures.toArray(new CompletableFuture[0]));
    }

    public void payChunkUpkeep(TerritoryData territory) {
        double upkeepCost = Constants.getUpkeepCost(territory);
        int numberClaimedChunk = territory.getNumberOfClaimedChunk();
        double totalUpkeep = numberClaimedChunk * upkeepCost;
        if (totalUpkeep > territory.getBalance()) {
            deletePortionOfChunk(territory);
            TownsAndNations.getPlugin()
                .getDatabaseHandler()
                .addTransactionHistory(new ChunkPaymentHistory(territory, -1));
        } else {
            territory.removeFromBalance(totalUpkeep);
            TownsAndNations.getPlugin()
                .getDatabaseHandler()
                .addTransactionHistory(new ChunkPaymentHistory(territory, totalUpkeep));
        }
    }

    private void deletePortionOfChunk(TerritoryData territory) {
        int minNbOfUnclaimedChunk = Constants.getMinimumNumberOfChunksUnclaimed();
        int nbOfUnclaimedChunk = 0;
        double percentageOfChunkToKeep = Constants.getPercentageOfChunksUnclaimed();
        List<ClaimedChunk2> borderChunks = ChunkUtil.getBorderChunks(territory);
        for (ClaimedChunk2 claimedChunk2 : borderChunks) {
            if (RandomUtil.getRandom().nextDouble() < percentageOfChunkToKeep) {
                NewClaimedChunkStorage.getInstance().unclaimChunkAndUpdate(claimedChunk2);
                nbOfUnclaimedChunk++;
            }
        }
        if (nbOfUnclaimedChunk < minNbOfUnclaimedChunk) {
            for (ClaimedChunk2 claimedChunk2 : borderChunks) {
                NewClaimedChunkStorage.getInstance().unclaimChunkAndUpdate(claimedChunk2);
                nbOfUnclaimedChunk++;
                if (nbOfUnclaimedChunk >= minNbOfUnclaimedChunk) break;
            }
        }
    }
}
