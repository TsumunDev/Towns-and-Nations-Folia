package org.leralix.tan.dataclass.territory;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.leralix.lib.data.SoundEnum;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.RankData;
import org.leralix.tan.dataclass.newhistory.SubjectTaxHistory;
import org.leralix.tan.dataclass.territory.economy.Budget;
import org.leralix.tan.dataclass.territory.economy.SubjectTaxLine;
import org.leralix.tan.events.EventManager;
import org.leralix.tan.events.events.TerritoryIndependanceInternalEvent;
import org.leralix.tan.lang.FilledLang;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.item.HeadUtils;
import org.leralix.tan.utils.gameplay.TerritoryUtil;
import org.leralix.tan.utils.graphic.TeamUtils;
import org.leralix.tan.upgrade.TerritoryStats;
import org.leralix.tan.upgrade.rewards.StatsType;

/**
 * Represents a nation — the highest level in the territory hierarchy.
 *
 * <p>A nation groups regions under a central government. It is the top of the
 * Town → Region → Nation hierarchy (hierarchyRank = 2). Nations cannot have an
 * overlord but can have vassals (regions).</p>
 *
 * <p><b>Thread Safety:</b><br>
 * All operations should be performed asynchronously. Use
 * {@code NationDataStorage.get(String)} for non-blocking reads.</p>
 *
 * @see TerritoryData
 * @see RegionData
 * @since 0.18.0
 */
public class NationData extends TerritoryData {
  private String leaderID;
  private String capitalID;
  private final List<String> regionsInNation;

  public NationData(String id, String name, ITanPlayer owner) {
    super(id, name, owner);
    RegionData ownerRegion = getOwnerRegion(owner);
    this.capitalID = ownerRegion != null ? ownerRegion.getID() : null;
    this.regionsInNation = new ArrayList<>();
  }

  private static RegionData getOwnerRegion(ITanPlayer owner) {
    if (owner == null) return null;
    try {
      var town = owner.getTownSync();
      if (town != null && town.haveOverlord()) {
        return town.getRegionSync();
      }
    } catch (Exception ignored) {}
    return null;
  }

  @Override
  protected void initUpgradesStatus() {
    this.upgradesStatus = new TerritoryStats(StatsType.NATION);
  }

  @Override
  public int getHierarchyRank() {
    return 2;
  }

  @Override
  public String getBaseColoredName() {
    return "§6" + getName();
  }

  @Override
  public String getLeaderID() {
    if (leaderID == null) leaderID = getCapital().getLeaderID();
    return leaderID;
  }

  @Override
  public ITanPlayer getLeaderData() {
    return PlayerDataStorage.getInstance().getSync(getLeaderID());
  }

  public CompletableFuture<ITanPlayer> getLeaderDataAsync() {
    return PlayerDataStorage.getInstance().get(getLeaderID());
  }

  @Override
  public void setLeaderID(String newLeaderID) {
    this.leaderID = newLeaderID;
  }

  @Override
  public boolean isLeader(String id) {
    return getLeaderID().equals(id);
  }

  @Override
  public Collection<String> getPlayerIDList() {
    ArrayList<String> playerList = new ArrayList<>();
    for (TerritoryData region : getSubjects()) {
      playerList.addAll(region.getPlayerIDList());
    }
    return playerList;
  }

  @Override
  public Collection<ITanPlayer> getITanPlayerList() {
    Map<String, ITanPlayer> playerMap =
        PlayerDataStorage.getInstance().getBatchSync(getPlayerIDList());
    return new ArrayList<>(playerMap.values());
  }

  @Override
  public ItemStack getIconWithName() {
    ItemStack icon = getIcon();
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      org.leralix.tan.utils.text.ComponentUtil.setDisplayName(meta, "§6" + getName());
      icon.setItemMeta(meta);
    }
    return icon;
  }

  @Override
  public ItemStack getIconWithInformations(LangType langType) {
    ItemStack icon = getIcon();
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      org.leralix.tan.utils.text.ComponentUtil.setDisplayName(meta, "§6" + getName());
      List<String> lore = new ArrayList<>();
      lore.add(Lang.GUI_REGION_INFO_DESC0.get(langType, getDescription()));
      lore.add(Lang.GUI_REGION_INFO_DESC1.get(langType, getCapital().getName()));
      lore.add(Lang.GUI_REGION_INFO_DESC2.get(langType, Integer.toString(getNumberOfRegionsIn())));
      lore.add(Lang.GUI_REGION_INFO_DESC3.get(langType, Integer.toString(getTotalPlayerCount())));
      lore.add(
          Lang.GUI_REGION_INFO_DESC5.get(langType, Integer.toString(getNumberOfClaimedChunk())));
      org.leralix.tan.utils.text.ComponentUtil.setLore(meta, lore);
      icon.setItemMeta(meta);
    }
    return icon;
  }

  public int getTotalPlayerCount() {
    int count = 0;
    for (TerritoryData region : getSubjects()) {
      count += region.getPlayerIDList().size();
    }
    return count;
  }

  @Override
  public boolean haveOverlord() {
    return false;
  }

  // ── Subjects (regions) ──

  public List<TerritoryData> getSubjects() {
    List<TerritoryData> regions = new ArrayList<>();
    for (String regionID : regionsInNation) {
      TerritoryData region = TerritoryUtil.getTerritory(regionID);
      if (region != null) regions.add(region);
    }
    return regions;
  }

  public int getNumberOfRegionsIn() {
    return regionsInNation.size();
  }

  public List<String> getRegionsInNation() {
    return regionsInNation;
  }

  // ── Capital ──

  public void setCapital(String regionID) {
    this.capitalID = regionID;
  }

  @Override
  public TerritoryData getCapital() {
    if (capitalID == null && !regionsInNation.isEmpty()) {
      capitalID = regionsInNation.getFirst();
    }
    if (capitalID == null) return null;
    return TerritoryUtil.getTerritory(capitalID);
  }

  // ── Vassalage ──

  @Override
  protected void addVassalPrivate(TerritoryData vassal) {
    regionsInNation.add(vassal.getID());
  }

  @Override
  protected void removeVassal(TerritoryData vassal) {
    EventManager.getInstance().callEvent(new TerritoryIndependanceInternalEvent(this, vassal));
    regionsInNation.remove(vassal.getID());
  }

  @Override
  public void removeOverlordPrivate() {
    // Nations cannot have an overlord — no-op
  }

  @Override
  protected Collection<TerritoryData> getOverlords() {
    return new ArrayList<>();
  }

  // ── Chunks ──

  @Override
  public void abstractClaimChunk(Player player, org.bukkit.Chunk chunk, boolean ignoreAdjacent) {
    removeFromBalance(getClaimCost());
    org.leralix.tan.storage.stored.NewClaimedChunkStorage.getInstance()
        .claimRegionChunk(chunk, getID());
  }

  // ── Tax ──

  @Override
  protected void collectTaxes() {
    for (TerritoryData region : getVassals()) {
      if (region == null) continue;
      double tax = getTax();
      if (region.getBalance() < tax) {
        TownsAndNations.getPlugin()
            .getDatabaseHandler()
            .addTransactionHistory(new SubjectTaxHistory(this, region, -1));
      } else {
        region.removeFromBalance(tax);
        addToBalance(tax);
        TownsAndNations.getPlugin()
            .getDatabaseHandler()
            .addTransactionHistory(new SubjectTaxHistory(this, region, tax));
      }
    }
  }

  // ── Broadcast ──

  @Override
  public void broadCastMessage(FilledLang message) {
    for (TerritoryData region : getSubjects()) region.broadCastMessage(message);
  }

  @Override
  public void broadcastMessageWithSound(FilledLang message, SoundEnum soundEnum, boolean addPrefix) {
    for (TerritoryData region : getSubjects()) {
      region.broadcastMessageWithSound(message, soundEnum, addPrefix);
    }
  }

  @Override
  public void broadcastMessageWithSound(FilledLang message, SoundEnum soundEnum) {
    broadcastMessageWithSound(message, soundEnum, true);
  }

  // ── Rank / Members ──

  @Override
  public RankData getRank(ITanPlayer tanPlayer) {
    if (!tanPlayer.hasRegion()) return null;
    Integer rankID = tanPlayer.getNationRankID();
    return rankID != null ? getRank(rankID) : null;
  }

  @Override
  public boolean haveNoLeader() {
    return false;
  }

  @Override
  public List<GuiItem> getOrderedMemberList(ITanPlayer tanPlayer) {
    return new ArrayList<>();
  }

  public CompletableFuture<List<GuiItem>> getOrderedMemberListAsync(ITanPlayer tanPlayer) {
    LangType langType = tanPlayer.getLang();
    Collection<String> playerUUIDs = getOrderedPlayerIDListSync();
    Map<String, ITanPlayer> playerMap = PlayerDataStorage.getInstance().getBatchSync(playerUUIDs);

    List<GuiItem> res = new ArrayList<>();
    for (String playerUUID : playerUUIDs) {
      ITanPlayer playerIterateData = playerMap.get(playerUUID);
      if (playerIterateData == null) continue;

      OfflinePlayer playerIterate = Bukkit.getOfflinePlayer(java.util.UUID.fromString(playerUUID));
      ItemStack playerHead =
          HeadUtils.getPlayerHead(
              playerIterate,
              Lang.GUI_TOWN_MEMBER_DESC1.get(langType, "Nation Member"));
      GuiItem playerButton =
          ItemBuilder.from(playerHead).asGuiItem(event -> event.setCancelled(true));
      res.add(playerButton);
    }
    return CompletableFuture.completedFuture(res);
  }

  @Override
  protected void specificSetPlayerRank(ITanPlayer playerStat, int rankID) {
    playerStat.setNationRankID(rankID);
  }

  // ── Budget ──

  @Override
  protected void addSpecificTaxes(Budget budget) {
    budget.addProfitLine(new SubjectTaxLine(this));
  }

  // ── Hierarchy ──

  @Override
  public boolean canHaveVassals() {
    return true;
  }

  @Override
  public boolean canHaveOverlord() {
    return false;
  }

  @Override
  public List<String> getVassalsID() {
    return regionsInNation;
  }

  @Override
  public boolean isVassal(String territoryID) {
    return regionsInNation.contains(territoryID);
  }

  @Override
  public Collection<TerritoryData> getPotentialVassals() {
    return new ArrayList<>();
  }

  // ── Lifecycle ──

  @Override
  public CompletableFuture<Void> delete() {
    return super.delete().thenRun(() -> {
      TeamUtils.updateAllScoreboardColor();
      // NationDataStorage deletion handled by caller (Phase 2)
    });
  }

  @Override
  public void openMainMenu(Player player) {
    // NationMenu will be implemented in Phase 4
    player.sendMessage("§cNation menu not yet implemented");
  }
}
