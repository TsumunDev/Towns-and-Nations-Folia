package org.leralix.tan.domain.quest.rewards;

import org.bukkit.entity.Player;
import org.leralix.tan.domain.quest.model.QuestReward;
import org.leralix.tan.storage.stored.PlayerDataStorage;

/**
 * Quest reward that grants prestige points to the player's town.
 * Prestige points are used to purchase town upgrades.
 *
 * @param amount The amount of prestige points to grant
 */
public record PrestigeQuestReward(int amount) implements QuestReward {

    public PrestigeQuestReward {
        if (amount <= 0) {
            throw new IllegalArgumentException("Prestige amount must be positive");
        }
    }

    @Override
    public RewardType getType() {
        return RewardType.PRESTIGE;
    }

    @Override
    public String getDescription() {
        return String.format("%d Prestige Points", amount);
    }

    @Override
    public void grant(Player player) {
        PlayerDataStorage.getInstance()
                .get(player)
                .thenAccept(tanPlayer -> {
                    String townId = tanPlayer.getTownId();
                    if (townId != null) {
                        // Use PrestigeService to add prestige
                        org.leralix.tan.service.prestige.PrestigeService.getInstance()
                                .addPrestige(
                                        townId,
                                        amount,
                                        org.leralix.tan.domain.prestige.model.PrestigeSource.QUEST_COMPLETION,
                                        "Quest reward: " + amount + " prestige"
                                );
                    }
                });
    }
}
