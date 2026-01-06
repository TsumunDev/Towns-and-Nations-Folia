package org.leralix.tan.dataclass;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import org.leralix.tan.dataclass.territory.cosmetic.CustomIcon;
import org.leralix.tan.enums.RankEnum;
import org.leralix.tan.enums.RolePermission;
import org.leralix.tan.storage.stored.PlayerDataStorage;

/**
 * Represents a rank within a town or region in the Towns and Nations plugin.
 * <p>
 * Ranks define the hierarchy and permission system for territories. Each player
 * in a town is assigned exactly one rank, which determines their permissions and
 * position in the social structure.
 * </p>
 * <p>
 * <b>Rank Features:</b>
 * <ul>
 *   <li><b>Hierarchy Level:</b> Ranks have levels (1-5) determining authority</li>
 *   <li><b>Permissions:</b> Fine-grained control over actions (build, claim, upgrade, etc.)</li>
 *   <li><b>Salary:</b> Optional payment from town treasury to rank members</li>
 *   <li><b>Taxation:</b> Whether rank members pay taxes to the town</li>
 *   <li><b>Customization:</b> Name, icon, and color can be customized</li>
 * </ul>
 * </p>
 * <p>
 * <b>Permission System:</b><br>
 * Permissions are defined in {@link RolePermission} and include:
 * <ul>
 *   <li>CLAIM_CHUNK, UNCLAIM_CHUNK - Land management</li>
 *   <li>MANAGE_CLAIM_SETTINGS - Configure chunk permissions</li>
 *   <li>UPGRADE_TOWN - Spend town points on upgrades</li>
 *   <li>MANAGE_TOWN_RELATION - Diplomacy with other territories</li>
 *   <li>INVITE_PLAYER, KICK_PLAYER - Membership management</li>
 *   <li>MANAGE_RANKS - Create and modify ranks</li>
 *   <li>MANAGE_WAR - Military actions</li>
 * </ul>
 * </p>
 * <p>
 * <b>Default Ranks:</b><br>
 * Each town has a default rank that new members join. Only one rank can be
 * marked as default. The leader cannot have their rank changed.
 * </p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * // Create a custom rank
 * RankData merchantRank = new RankData(rankId, "Merchant");
 * merchantRank.addPermission(RolePermission.OPEN_GUI);
 * merchantRank.addPermission(RolePermission.CREATE_PROPERTY);
 * merchantRank.setSalary(100.0);
 *
 * // Assign player to rank
 * player.setTownRankID(merchantRank.getID());
 * }</pre>
 *
 * @see RolePermission
 * @see RankEnum
 * @see TownData
 * @see RegionData
 * @since 0.15.0
 */
public class RankData {
  private Integer ID;
  private String name;
  private RankEnum rankEnum;
  CustomIcon rankIcon;
  private final List<String> players;
  private int salary;
  private final Set<RolePermission> permissions = EnumSet.noneOf(RolePermission.class);
  private boolean isPayingTaxes;
  public RankData(int id, String name) {
    this.ID = id;
    this.name = name;
    this.rankEnum = RankEnum.FIVE;
    this.players = new ArrayList<>();
    this.isPayingTaxes = true;
    this.salary = 0;
  }
  public void swapPayingTaxes() {
    this.isPayingTaxes = !this.isPayingTaxes;
  }
  public String getName() {
    return this.name;
  }
  public String getColoredName() {
    return this.rankEnum.getColor() + this.name;
  }
  public void setName(String newName) {
    this.name = newName;
  }
  public RankEnum getRankEnum() {
    return this.rankEnum;
  }
  public int getLevel() {
    return this.rankEnum.getLevel();
  }
  public void incrementLevel() {
    this.rankEnum = rankEnum.nextRank();
  }
  public void decrementLevel() {
    this.rankEnum = rankEnum.previousRank();
  }
  public ItemStack getRankIcon() {
    if (this.rankIcon == null) return rankEnum.getBasicRankIcon();
    return rankIcon.getIcon();
  }
  public void addPlayer(String playerUUID) {
    this.players.add(playerUUID);
  }
  public void addPlayer(ITanPlayer tanPlayer) {
    addPlayer(tanPlayer.getID());
  }
  public void removePlayer(String playerUUID) {
    this.players.remove(playerUUID);
  }
  public void removePlayer(ITanPlayer player) {
    removePlayer(player.getID());
  }
  public List<String> getPlayersID() {
    return this.players;
  }
  public List<ITanPlayer> getPlayers() {
    List<ITanPlayer> playerList = new ArrayList<>();
    for (String playerID : this.players) {
      playerList.add(PlayerDataStorage.getInstance().getSync(playerID));
    }
    return playerList;
  }
  public boolean isPayingTaxes() {
    return this.isPayingTaxes;
  }
  public void setRankIcon(ItemStack rankItem) {
    this.rankIcon = new CustomIcon(rankItem);
  }
  public int getNumberOfPlayer() {
    return players.size();
  }
  public void addPermission(RolePermission permission) {
    permissions.add(permission);
  }
  public boolean hasPermission(RolePermission permission) {
    return permissions.contains(permission);
  }
  public void removePermission(RolePermission permission) {
    permissions.remove(permission);
  }
  public void switchPermission(RolePermission permission) {
    if (hasPermission(permission)) removePermission(permission);
    else addPermission(permission);
  }
  public void setSalary(int salary) {
    this.salary = salary;
  }
  public void addFromSalary(int amount) {
    this.salary += amount;
  }
  public void removeFromSalary(int amount) {
    this.salary -= amount;
  }
  public int getSalary() {
    return this.salary;
  }
  public Integer getID() {
    return ID;
  }
  public void setID(int id) {
    this.ID = id;
  }
  public boolean isSuperiorTo(RankData rank) {
    return this.getRankEnum().getLevel() > rank.getRankEnum().getLevel();
  }
}