package org.leralix.tan.gui.service.requirements;
import java.util.List;
import org.bukkit.entity.Player;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.upgrade.Upgrade;
import org.leralix.tan.utils.NumberUtils;
public class AmountForUpgradeRequirement extends IndividualRequirementWithCost {
  private final TerritoryData territoryData;
  private final Player player;
  private final Upgrade upgrade;
  private final List<Integer> costs;
  public AmountForUpgradeRequirement(
      TerritoryData territoryData, Player player, Upgrade upgrade, List<Integer> costs) {
    this.territoryData = territoryData;
    this.player = player;
    this.upgrade = upgrade;
    this.costs = costs;
  }
  @Override
  public String getLine(LangType langType) {
    double cost = getCost();
    if (isInvalid()) {
      return Lang.REQUIREMENT_COST_NEGATIVE.get(langType, Double.toString(cost));
    } else {
      return Lang.REQUIREMENT_COST_POSITIVE.get(langType, Double.toString(cost));
    }
  }
  @Override
  public boolean isInvalid() {
    return EconomyUtil.getBalance(player) < getCost();
  }
  public double getCost() {
    int level = territoryData.getNewLevel().getLevel(upgrade);
    if (costs.size() <= level) return NumberUtils.roundWithDigits(costs.getLast());
    return NumberUtils.roundWithDigits(costs.get(level));
  }
  @Override
  public void actionDone() {
    EconomyUtil.removeFromBalance(player, getCost());
  }
}
