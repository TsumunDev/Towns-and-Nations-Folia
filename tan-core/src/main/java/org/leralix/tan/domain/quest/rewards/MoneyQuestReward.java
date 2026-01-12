package org.leralix.tan.domain.quest.rewards;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.domain.quest.model.QuestReward;
import org.leralix.tan.economy.EconomyUtil;

/**
 * Quest reward that grants money to the player.
 *
 * @param amount The amount of money to grant
 */
public record MoneyQuestReward(double amount) implements QuestReward {

    public MoneyQuestReward {
        if (amount <= 0) {
            throw new IllegalArgumentException("Money amount must be positive");
        }
    }

    @Override
    public RewardType getType() {
        return RewardType.MONEY;
    }

    @Override
    public String getDescription() {
        return EconomyUtil.formatMoney(amount);
    }

    @Override
    public void grant(Player player) {
        EconomyUtil.addToBalance(player, amount);
    }
}
