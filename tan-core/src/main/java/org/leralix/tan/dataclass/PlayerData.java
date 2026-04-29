package org.leralix.tan.dataclass;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.dataclass.player.PlayerPropertyComponent;
import org.leralix.tan.dataclass.player.PlayerTerritoryComponent;
import org.leralix.tan.dataclass.player.PlayerWarComponent;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.timezone.TimeZoneEnum;
import org.leralix.tan.timezone.TimeZoneManager;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.wars.legacy.CurrentAttack;
public class PlayerData implements ITanPlayer {
  // ── Serialized fields (Gson) ──
  private String uuid;
  private String storedName;
  private Double Balance;
  private String TownId;
  private Integer townRankID;
  private Integer regionRankID;
  private Integer nationRankID;
  private List<String> propertiesListID;
  private List<String> attackInvolvedIn;
  private LangType lang;
  private TimeZoneEnum timeZone;

  // Database tracking fields (v2.0)
  private String ipAddress;
  private Long firstSeen;
  private boolean isOnline;

  // ── Transient components ──
  private transient PlayerWarComponent warComponent;
  private transient PlayerTerritoryComponent territoryComponent;
  private transient PlayerPropertyComponent propertyComponent;

  private PlayerWarComponent warComponent() {
    if (warComponent == null) {
      warComponent = new PlayerWarComponent(this, getAttackInvolvedIn());
    }
    return warComponent;
  }

  private PlayerTerritoryComponent territoryComponent() {
    if (territoryComponent == null) {
      territoryComponent = new PlayerTerritoryComponent(this);
    }
    return territoryComponent;
  }

  private PlayerPropertyComponent propertyComponent() {
    if (propertyComponent == null) {
      if (this.propertiesListID == null) this.propertiesListID = new ArrayList<>();
      propertyComponent = new PlayerPropertyComponent(this.propertiesListID);
    }
    return propertyComponent;
  }

  // ── Constructor ──

  public PlayerData(Player player) {
    this.uuid = player.getUniqueId().toString();
    this.storedName = player.getName();
    this.Balance = Constants.getStartingBalance();
    this.TownId = null;
    this.townRankID = null;
    this.regionRankID = null;
    this.nationRankID = null;
    this.propertiesListID = new ArrayList<>();
    this.attackInvolvedIn = new ArrayList<>();
    this.ipAddress = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
    this.firstSeen = System.currentTimeMillis();
    this.isOnline = true;
  }

  // ── Identity ──

  public String getID() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getNameStored() {
    if (storedName == null) {
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
      storedName = offlinePlayer.getName();
      if (storedName == null) {
        storedName = "Unknown name";
      }
    }
    return storedName;
  }

  public void setNameStored(String name) {
    this.storedName = name;
  }

  public void clearName() {
    this.storedName = null;
  }

  public UUID getUUID() {
    return UUID.fromString(uuid);
  }

  public OfflinePlayer getOfflinePlayer() {
    return Bukkit.getServer().getOfflinePlayer(getUUID());
  }

  public Player getPlayer() {
    return Bukkit.getPlayer(getUUID());
  }

  // ── Economy ──

  public double getBalance() {
    return this.Balance != null ? this.Balance : 0.0;
  }

  public void setBalance(double balance) {
    org.leralix.tan.economy.EconomyUtil.setBalance(this, balance);
    this.Balance = balance;
  }

  public void addToBalance(double amount) {
    this.Balance = (this.Balance != null ? this.Balance : 0.0) + amount;
  }

  public void removeFromBalance(double amount) {
    this.Balance = (this.Balance != null ? this.Balance : 0.0) - amount;
  }

  // ── Preferences ──

  public LangType getLang() {
    if (lang == null) return Lang.getServerLang();
    return lang;
  }

  public void setLang(LangType lang) {
    this.lang = lang;
  }

  public TimeZoneEnum getTimeZone() {
    if (timeZone == null) {
      return TimeZoneManager.getInstance().getTimezoneEnum();
    }
    return timeZone;
  }

  public void setTimeZone(TimeZoneEnum timeZone) {
    this.timeZone = timeZone;
  }

  // ── Session / Database tracking (v2.0) ──

  public String getLastKnownIP() {
    return ipAddress;
  }

  public void setLastKnownIP(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public Long getFirstSeen() {
    return firstSeen;
  }

  public void setFirstSeen(Long firstSeen) {
    this.firstSeen = firstSeen;
  }

  public boolean isOnline() {
    return isOnline;
  }

  public void setOnline(boolean isOnline) {
    this.isOnline = isOnline;
  }

  // ── Town membership (simple field access) ──

  public String getTownId() {
    return this.TownId;
  }

  public void joinTown(TownData townData) {
    this.TownId = townData.getID();
    setTownRankID(townData.getDefaultRankID());
  }

  public void leaveTown() {
    this.TownId = null;
    this.townRankID = null;
  }

  public void setTownRankID(int townRankID) {
    this.townRankID = townRankID;
  }

  public Integer getTownRankID() {
    return this.townRankID;
  }

  public void setRegionRankID(Integer rankID) {
    this.regionRankID = rankID;
  }

  public Integer getNationRankID() {
    return this.nationRankID;
  }

  public void setNationRankID(Integer rankID) {
    this.nationRankID = rankID;
  }

  // ── Territory delegation ──

  @Override
  public String getTownName() { return territoryComponent().getTownName(); }

  public CompletableFuture<String> getTownNameAsync() { return territoryComponent().getTownNameAsync(); }

  public CompletableFuture<TownData> getTown() { return territoryComponent().getTown(); }

  public boolean hasTown() { return territoryComponent().hasTown(); }

  public boolean isTownOverlord() { return territoryComponent().isTownOverlord(); }

  public CompletableFuture<Boolean> isTownOverlordAsync() { return territoryComponent().isTownOverlordAsync(); }

  public RankData getTownRank() { return territoryComponent().getTownRank(); }

  public CompletableFuture<RankData> getTownRankAsync() { return territoryComponent().getTownRankAsync(); }

  @Deprecated
  public RankData getRegionRank() { return null; }

  public CompletableFuture<RankData> getRegionRankAsync() { return territoryComponent().getRegionRankAsync(); }

  public boolean hasRegion() { return territoryComponent().hasRegion(); }

  public CompletableFuture<Boolean> hasRegionAsync() { return territoryComponent().hasRegionAsync(); }

  public CompletableFuture<RegionData> getRegion() { return territoryComponent().getRegion(); }

  @Override
  public String getNationName() { return territoryComponent().getNationName(); }

  public CompletableFuture<String> getNationNameAsync() { return territoryComponent().getNationNameAsync(); }

  public Integer getRegionRankID() { return territoryComponent().getRegionRankID(); }

  public CompletableFuture<Integer> getRegionRankIDAsync() { return territoryComponent().getRegionRankIDAsync(); }

  public Integer getRankID(TerritoryData territoryData) { return territoryComponent().getRankID(territoryData); }

  @Override
  public RankData getRank(TerritoryData territoryData) { return territoryComponent().getRank(territoryData); }

  public CompletableFuture<List<TerritoryData>> getAllTerritoriesPlayerIsIn() { return territoryComponent().getAllTerritoriesPlayerIsIn(); }

  public void setRankID(TerritoryData territoryData, Integer defaultRankID) { territoryComponent().setRankID(territoryData, defaultRankID); }

  public void clearAllTownApplications() { territoryComponent().clearAllTownApplications(); }

  @Override
  public CompletableFuture<TownRelation> getRelationWithPlayer(Player otherPlayer) { return territoryComponent().getRelationWithPlayer(otherPlayer); }

  public CompletableFuture<TownRelation> getRelationWithPlayer(ITanPlayer otherPlayer) { return territoryComponent().getRelationWithPlayer(otherPlayer); }

  public TownRelation getRelationWithPlayerSync(ITanPlayer otherPlayer) { return territoryComponent().getRelationWithPlayerSync(otherPlayer); }

  // ── Property delegation ──

  public List<String> getPropertiesListID() { return propertyComponent().getPropertiesListID(); }

  public void addProperty(PropertyData propertyData) { propertyComponent().addProperty(propertyData); }

  public List<PropertyData> getProperties() { return propertyComponent().getProperties(); }

  public void removeProperty(PropertyData propertyData) { propertyComponent().removeProperty(propertyData); }

  // ── War delegation ──

  public List<String> getAttackInvolvedIn() {
    if (attackInvolvedIn == null) attackInvolvedIn = new ArrayList<>();
    return attackInvolvedIn;
  }

  public void addWar(CurrentAttack currentAttacks) { warComponent().addWar(currentAttacks); }

  public void updateCurrentAttack() { warComponent().updateCurrentAttack(); }

  public boolean isAtWarWith(TerritoryData territoryData) { return warComponent().isAtWarWith(territoryData); }

  public void removeWar(@NotNull CurrentAttack currentAttacks) { warComponent().removeWar(currentAttacks); }

  @Override
  public CompletableFuture<List<CurrentAttack>> getCurrentAttacks() {
    return getAllTerritoriesPlayerIsIn()
        .thenApply(territories -> {
          List<CurrentAttack> res = new ArrayList<>();
          for (TerritoryData territoryData : territories) {
            res.addAll(territoryData.getCurrentAttacks());
          }
          return res;
        });
  }
}
