package org.leralix.tan.domain.quest.rewards;

import org.bukkit.entity.Player;
import org.leralix.tan.domain.quest.model.QuestReward;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.concurrent.CompletableFuture;

/**
 * Quest reward that grants XP to the player's town.
 *
 * @param amount The amount of XP to grant
 */
public record XpQuestReward(long amount) implements QuestReward {

    public XpQuestReward {
        if (amount <= 0) {
            throw new IllegalArgumentException("XP amount must be positive");
        }
    }

    @Override
    public RewardType getType() {
        return RewardType.XP;
    }

    @Override
    public String getDescription() {
        return String.format("%d Town XP", amount);
    }

    @Override
    public void grant(Player player) {
        PlayerDataStorage.getInstance()
                .get(player)
                .thenAccept(tanPlayer -> {
                    String townId = tanPlayer.getTownId();
                    if (townId != null) {
                        TownDataStorage.getInstance()
                                .get(townId)
                                .thenAccept(town -> {
                                    if (town != null) {
                                        // Add XP using the progression service
                                        town.getProgression().addXp(amount,
                                                org.leralix.tan.dataclass.territory.progression.TownProgressionService
                                                        .getInstance().getConfig(town.getTownTier()));

                                        // Save updated town
                                        TownDataStorage.getInstance().update(town);
                                    }
                                });
                    }
                });
    }
}
