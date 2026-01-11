package org.leralix.tan.upgrade;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.leralix.lib.utils.config.ConfigTag;
import org.leralix.lib.utils.config.ConfigUtil;
import org.leralix.tan.upgrade.rewards.AggregatableStat;
import org.leralix.tan.upgrade.rewards.IndividualStat;
import org.leralix.tan.upgrade.rewards.StatsType;
import org.leralix.tan.upgrade.rewards.bool.EnableMobBan;
import org.leralix.tan.upgrade.rewards.bool.EnableTownSpawn;
import org.leralix.tan.upgrade.rewards.list.BiomeStat;
import org.leralix.tan.upgrade.rewards.list.PermissionList;
import org.leralix.tan.upgrade.rewards.numeric.*;
import org.leralix.tan.upgrade.rewards.percentage.LandmarkBonus;
import org.leralix.tan.utils.constants.Constants;
public class TerritoryStats {
  private int mainLevel;
  private Map<String, Integer> level;
  private final StatsType statsType;
  private final Map<Class<?>, Object> customStats; // Custom stats storage for direct modifications
  public TerritoryStats(StatsType statsType) {
    this.mainLevel = 1;
    this.level = new HashMap<>();
    this.level.put("CITY_HALL", 1);
    this.statsType = statsType;
    this.customStats = new HashMap<>();
  }
  public int getLevel(Upgrade upgrade) {
    if (level == null) {
      level = new HashMap<>();
    }
    if (!level.containsKey(upgrade.getID())) {
      return 0;
    }
    return level.get(upgrade.getID());
  }
  public void levelUp(Upgrade townUpgrade) {
    String key = townUpgrade.getID();
    if (!level.containsKey(key)) {
      level.put(key, 1);
      return;
    }
    level.put(key, level.get(key) + 1);
  }
  public int getMainLevel() {
    return mainLevel;
  }
  public void levelUpMain() {
    mainLevel++;
  }
  public <T extends IndividualStat & AggregatableStat<T>> T getStat(Class<T> rewardClass) {
    List<T> stats = new ArrayList<>();
    for (Upgrade upgrade : Constants.getUpgradeStorage().getUpgrades(statsType)) {
      int currentLevel = getLevel(upgrade);
      if (currentLevel == 0) continue;
      for (IndividualStat reward : upgrade.getRewards()) {
        if (rewardClass.isInstance(reward)) {
          stats.add(rewardClass.cast(reward).scale(currentLevel));
        }
      }
    }
    if (stats.isEmpty()) {
      try {
        return rewardClass.getDeclaredConstructor().newInstance();
      } catch (InstantiationException
          | IllegalAccessException
          | InvocationTargetException
          | NoSuchMethodException e) {
        throw new RuntimeException(
            "Failed to create default instance of " + rewardClass.getName(), e);
      }
    }
    return stats.getFirst().aggregate(stats);
  }
  public Collection<IndividualStat> getAllStats() {
    List<IndividualStat> allStats = new ArrayList<>();
    allStats.add(getStat(ChunkCap.class));
    allStats.add(getStat(ChunkCost.class));
    if (statsType == StatsType.TOWN) {
      allStats.add(getStat(LandmarkCap.class));
      allStats.add(getStat(PropertyCap.class));
      allStats.add(getStat(TownPlayerCap.class));
      allStats.add(getStat(LandmarkBonus.class));
      allStats.add(getStat(EnableTownSpawn.class));
      allStats.add(getStat(EnableMobBan.class));
    }
    allStats.add(getStat(PermissionList.class));
    allStats.add(getStat(BiomeStat.class));
    return allStats;
  }
  public int getMoneyRequiredForLevelUp() {
    return getRequiredMoney(mainLevel);
  }
  private int getRequiredMoney(int level) {
    FileConfiguration fg = ConfigUtil.getCustomConfig(ConfigTag.UPGRADE);
    ConfigurationSection section = fg.getConfigurationSection("townLevelExpression");
    String expressionString = section.getString("LevelExpression");
    String squareMultName = "squareMultiplier";
    String flatMultName = "flatMultiplier";
    double squareMultiplier = section.getDouble(squareMultName);
    double flatMultiplier = section.getDouble(flatMultName);
    double base = section.getDouble("base");
    Expression expression =
        new ExpressionBuilder(expressionString)
            .variable("level")
            .variable(squareMultName)
            .variable(flatMultName)
            .variable("base")
            .build()
            .setVariable("level", level)
            .setVariable(squareMultName, squareMultiplier)
            .setVariable(flatMultName, flatMultiplier)
            .setVariable("base", base);
    return (int) expression.evaluate();
  }

  /**
   * Adds a numeric value to a custom stat.
   * This is used for directly modifying stats without going through the upgrade system.
   *
   * @param statClass The class representing the stat type
   * @param value The value to add
   */
  public void addStat(Class<?> statClass, int value) {
    customStats.merge(statClass, value, (old, newVal) -> {
      if (old instanceof Integer oldInt) {
        return oldInt + (Integer) newVal;
      }
      return newVal;
    });
  }

  /**
   * Sets a boolean stat value.
   * This is used for directly modifying stats without going through the upgrade system.
   *
   * @param statClass The class representing the stat type
   * @param value The value to set
   */
  public void setStat(Class<?> statClass, boolean value) {
    customStats.put(statClass, value);
  }

  /**
   * Sets a numeric stat value.
   * This is used for directly modifying stats without going through the upgrade system.
   *
   * @param statClass The class representing the stat type
   * @param value The value to set
   */
  public void setStat(Class<?> statClass, int value) {
    customStats.put(statClass, value);
  }

  /**
   * Gets a custom stat value.
   *
   * @param statClass The class representing the stat type
   * @param defaultValue The default value if stat doesn't exist
   * @return The stat value
   */
  @SuppressWarnings("unchecked")
  public <T> T getCustomStat(Class<T> statClass, T defaultValue) {
    Object value = customStats.get(statClass);
    if (value == null) {
      return defaultValue;
    }
    return (T) value;
  }
}