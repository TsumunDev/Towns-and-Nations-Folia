package org.leralix.tan.domain.territory;

import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.newhistory.PlayerDonationHistory;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.lib.data.SoundEnum;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.text.TanChatUtils;

/**
 * Handles player donations to territories.
 */
public class TerritoryDonationService {

    public CompletableFuture<Void> addDonationAsync(TerritoryData territory, Player player, double amount) {
        if (amount <= 0) {
            return PlayerDataStorage.getInstance().get(player)
                .thenAccept(tanPlayer -> {
                    TanChatUtils.message(player, Lang.PAY_MINIMUM_REQUIRED.get(tanPlayer.getLang()));
                });
        }
        return PlayerDataStorage.getInstance()
            .get(player)
            .thenCompose(tanPlayer -> {
                LangType langType = tanPlayer.getLang();
                return org.leralix.tan.service.AsyncEconomyService.getBalance(player)
                    .thenCompose(balance -> {
                        if (balance < amount) {
                            TanChatUtils.message(player, Lang.PLAYER_NOT_ENOUGH_MONEY.get(langType));
                            return CompletableFuture.completedFuture(null);
                        }
                        return org.leralix.tan.service.AsyncEconomyService.withdraw(player, amount)
                            .thenRun(() -> {
                                territory.addToBalance(amount);
                                TownsAndNations.getPlugin()
                                    .getDatabaseHandler()
                                    .addTransactionHistory(new PlayerDonationHistory(territory, player, amount));
                                TanChatUtils.message(player,
                                    Lang.PLAYER_SEND_MONEY_SUCCESS.get(
                                        langType, Double.toString(amount), territory.getBaseColoredName()),
                                    SoundEnum.MINOR_GOOD);
                            });
                    });
            })
            .exceptionally(throwable -> {
                TownsAndNations.getPlugin()
                    .getLogger()
                    .warning("Failed to process donation from " + player.getName() + ": " + throwable.getMessage());
                TanChatUtils.message(player, Lang.SYNTAX_ERROR.get(player));
                return null;
            });
    }

    public void addDonation(TerritoryData territory, Player player, double amount) {
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
        LangType langType = tanPlayer.getLang();
        double playerBalance = EconomyUtil.getBalance(player);
        if (playerBalance < amount) {
            TanChatUtils.message(player, Lang.PLAYER_NOT_ENOUGH_MONEY.get(langType));
            return;
        }
        if (amount <= 0) {
            TanChatUtils.message(player, Lang.PAY_MINIMUM_REQUIRED.get(langType));
            return;
        }
        EconomyUtil.removeFromBalance(player, amount);
        territory.addToBalance(amount);
        TownsAndNations.getPlugin()
            .getDatabaseHandler()
            .addTransactionHistory(new PlayerDonationHistory(territory, player, amount));
        TanChatUtils.message(player,
            Lang.PLAYER_SEND_MONEY_SUCCESS.get(langType, Double.toString(amount), territory.getBaseColoredName()),
            SoundEnum.MINOR_GOOD);
    }
}
