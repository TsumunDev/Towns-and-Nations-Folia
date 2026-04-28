package org.leralix.tan.dataclass;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.CurrentAttacksStorage;
import org.leralix.tan.storage.invitation.TownInviteDataStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.timezone.TimeZoneEnum;
import org.leralix.tan.timezone.TimeZoneManager;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.wars.legacy.CurrentAttack;
public class PlayerData implements ITanPlayer {
  private String uuid;
  private String storedName;
  private Double Balance;
  private String TownId;
  private Integer townRankID;
  private Integer regionRankID;
  private List<String> propertiesListID;
  private List<String> attackInvolvedIn;
  private LangType lang;
  private TimeZoneEnum timeZone;

  // Database tracking fields (v2.0)
  private String ipAddress;
  private Long firstSeen;
  private boolean isOnline;

  // Performance optimization: Cache for town/region names to avoid blocking .join() calls
  private transient String cachedTownName;
  private transient String cachedNationName;
  private transient Boolean cachedIsOverlord;
  private transient long cacheTime;
  private static final long CACHE_TTL_MS = 5000; // 5 seconds cache

  public PlayerData(Player player) {
    this.uuid = player.getUniqueId().toString();
    this.storedName = player.getName();
    this.Balance = Constants.getStartingBalance();
    this.TownId = null;
    this.townRankID = null;
    this.regionRankID = null;
    this.propertiesListID = new ArrayList<>();
    this.attackInvolvedIn = new ArrayList<>();

    // Initialize tracking fields
    this.ipAddress = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
    this.firstSeen = System.currentTimeMillis();
    this.isOnline = true;
  }
  public String getID() {
    return uuid;
  }
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
  public String getNameStored() {
    if (storedName == null) {
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
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
  public double getBalance() {
    // Return the local Balance field directly — this IS the source of truth in standalone mode.
    // In external mode (Vault/ZEssentials), callers should use EconomyUtil.getBalance() which
    // routes to TanEconomyExternal and reads from the external provider.
    // NOTE: Must NOT delegate to EconomyUtil.getBalance(this) — that causes infinite recursion:
    //   PlayerData.getBalance() → EconomyUtil → TanEconomyStandalone.getBalance() → tanPlayer.getBalance() → loop
    return this.Balance != null ? this.Balance : 0.0;
  }
  public void setBalance(double balance) {
    // Use EconomyUtil to set balance in Vault/ZEssentials
    org.leralix.tan.economy.EconomyUtil.setBalance(this, balance);
    // Also update local cache for compatibility
    this.Balance = balance;
  }
  public String getTownId() {
    return this.TownId;
  }
  @Override
  public String getTownName() {
    if (!hasTown()) {
      return null;
    }
    // Use cached value if available and fresh
    long now = System.currentTimeMillis();
    if (cachedTownName != null && (now - cacheTime) < CACHE_TTL_MS) {
      return cachedTownName;
    }
    // Fetch async and cache for next call
    getTown().thenAccept(town -> {
      if (town != null) {
        cachedTownName = town.getName();
        cacheTime = now;
      }
    });
    // Return cached value (might be slightly stale but non-blocking)
    return cachedTownName;
  }

  /**
   * Gets the town name asynchronously.
   * @return CompletableFuture with the town name, or null if not in a town
   */
  public CompletableFuture<String> getTownNameAsync() {
    if (!hasTown()) {
      return CompletableFuture.completedFuture(null);
    }
    return getTown().thenApply(town -> town != null ? town.getName() : null);
  }
  public CompletableFuture<TownData> getTown() {
    if (this.TownId == null) {
      return CompletableFuture.completedFuture(null);
    }
    return TownDataStorage.getInstance().get(this.TownId);
  }
  public boolean hasTown() {
    return this.TownId != null;
  }
  public boolean isTownOverlord() {
    if (!hasTown()) return false;
    // Use cached value if available and fresh
    long now = System.currentTimeMillis();
    if (cachedIsOverlord != null && (now - cacheTime) < CACHE_TTL_MS) {
      return cachedIsOverlord;
    }
    // Fetch async and cache for next call
    getTown().thenAccept(town -> {
      if (town != null) {
        cachedIsOverlord = town.isLeader(this.uuid);
        cacheTime = now;
      }
    });
    // Return cached value (might be slightly stale but non-blocking)
    return cachedIsOverlord != null && cachedIsOverlord;
  }

  /**
   * Checks if the player is town overlord asynchronously.
   * @return CompletableFuture with true if player is town leader, false otherwise
   */
  public CompletableFuture<Boolean> isTownOverlordAsync() {
    if (!hasTown()) {
      return CompletableFuture.completedFuture(false);
    }
    return getTown().thenApply(town -> town != null && town.isLeader(this.uuid));
  }
  public RankData getTownRank() {
    if (!hasTown()) return null;
    // Try to get from cache first without blocking
    TownData cachedTown = TownDataStorage.getInstance().getSync(this.TownId);
    if (cachedTown != null) {
      return cachedTown.getRank(getTownRankID());
    }
    // If not in cache, return null (non-blocking)
    // Caller should use getTownRankAsync() for guaranteed result
    return null;
  }

  /**
   * Gets the player's town rank asynchronously.
   * @return CompletableFuture with the rank data, or null if not in a town
   */
  public CompletableFuture<RankData> getTownRankAsync() {
    if (!hasTown()) {
      return CompletableFuture.completedFuture(null);
    }
    return getTown().thenApply(town -> town != null ? town.getRank(getTownRankID()) : null);
  }
  @Deprecated
  public RankData getRegionRank() {
    return null;
  }

  /**
   * Gets the player's region rank asynchronously.
   * @return CompletableFuture with the rank data, or null if not in a region
   */
  public CompletableFuture<RankData> getRegionRankAsync() {
    if (!hasRegion()) return CompletableFuture.completedFuture(null);
    return getRegion().thenCombine(
        CompletableFuture.completedFuture(getRegionRankID()),
        (region, rankID) -> region != null ? region.getRank(rankID) : null
    );
  }
  public void addToBalance(double amount) {
    this.Balance = (this.Balance != null ? this.Balance : 0.0) + amount;
  }
  public void removeFromBalance(double amount) {
    this.Balance = (this.Balance != null ? this.Balance : 0.0) - amount;
  }
  public boolean hasRegion() {
    if (!this.hasTown()) {
      return false;
    }
    // Try cache first without blocking
    TownData cachedTown = TownDataStorage.getInstance().getSync(this.TownId);
    if (cachedTown != null) {
      return cachedTown.haveOverlord();
    }
    // If not in cache, assume false for non-blocking call
    // Use hasRegionAsync() for guaranteed result
    return false;
  }

  /**
   * Checks if player is in a region asynchronously.
   * @return CompletableFuture with true if player is in a nation
   */
  public CompletableFuture<Boolean> hasRegionAsync() {
    if (!this.hasTown()) {
      return CompletableFuture.completedFuture(false);
    }
    return getTown().thenApply(town -> town != null && town.haveOverlord());
  }
  public CompletableFuture<RegionData> getRegion() {
    if (!hasRegion()) return CompletableFuture.completedFuture(null);
    return getTown()
        .thenApply(
            town -> {
              if (town == null) return null;
              Optional<TerritoryData> overlord = town.getOverlord();
              return overlord.map(territoryData -> (RegionData) territoryData).orElse(null);
            });
  }
  @Override
  public String getNationName() {
    if (!hasRegion()) {
      return null;
    }
    // Use cached value if available and fresh
    long now = System.currentTimeMillis();
    if (cachedNationName != null && (now - cacheTime) < CACHE_TTL_MS) {
      return cachedNationName;
    }
    // Fetch async and cache for next call
    getRegion().thenAccept(region -> {
      if (region != null) {
        cachedNationName = region.getName();
        cacheTime = now;
      }
    });
    // Return cached value (might be slightly stale but non-blocking)
    return cachedNationName;
  }

  /**
   * Gets the nation name asynchronously.
   * @return CompletableFuture with the nation name, or null if not in a nation
   */
  public CompletableFuture<String> getNationNameAsync() {
    if (!hasRegion()) {
      return CompletableFuture.completedFuture(null);
    }
    return getRegion().thenApply(region -> region != null ? region.getName() : null);
  }
  public UUID getUUID() {
    return java.util.UUID.fromString(uuid);
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
  public List<String> getPropertiesListID() {
    if (this.propertiesListID == null) this.propertiesListID = new ArrayList<>();
    return propertiesListID;
  }
  public void addProperty(PropertyData propertyData) {
    getPropertiesListID().add(propertyData.getTotalID());
  }
  public List<PropertyData> getProperties() {
    return new ArrayList<>();
  }
  public void removeProperty(PropertyData propertyData) {
    this.propertiesListID.remove(propertyData.getTotalID());
  }
  public Player getPlayer() {
    return Bukkit.getPlayer(getUUID());
  }
  public List<String> getAttackInvolvedIn() {
    if (attackInvolvedIn == null) attackInvolvedIn = new ArrayList<>();
    return attackInvolvedIn;
  }
  public void addWar(CurrentAttack currentAttacks) {
    if (getAttackInvolvedIn().contains(currentAttacks.getAttackData().getID())) {
      return;
    }
    getAttackInvolvedIn().add(currentAttacks.getAttackData().getID());
  }
  public void updateCurrentAttack() {
    Iterator<String> iterator = getAttackInvolvedIn().iterator();
    while (iterator.hasNext()) {
      String attackID = iterator.next();
      CurrentAttack currentAttack = CurrentAttacksStorage.get(attackID);
      if (currentAttack == null || !currentAttack.containsPlayer(this)) {
        iterator.remove();
      } else {
        currentAttack.addPlayer(this);
      }
    }
  }
  public boolean isAtWarWith(TerritoryData territoryData) {
    if (territoryData == null) {
      return false;
    }
    for (String attackID : getAttackInvolvedIn()) {
      CurrentAttack currentAttack = CurrentAttacksStorage.get(attackID);
      if (currentAttack == null) {
        getAttackInvolvedIn().remove(attackID);
        continue;
      }
      if (currentAttack.getAttackData().getDefendingTerritories().contains(territoryData)) {
        return true;
      }
    }
    return false;
  }
  public void removeWar(@NotNull CurrentAttack currentAttacks) {
    getAttackInvolvedIn().remove(currentAttacks.getAttackData().getID());
  }
  @Override
  public CompletableFuture<TownRelation> getRelationWithPlayer(Player otherPlayer) {
    return PlayerDataStorage.getInstance()
        .get(otherPlayer)
        .thenCompose(otherPlayerData -> getRelationWithPlayer(otherPlayerData));
  }
  public CompletableFuture<TownRelation> getRelationWithPlayer(ITanPlayer otherPlayer) {
    if (!hasTown() || !otherPlayer.hasTown())
      return CompletableFuture.completedFuture(TownRelation.NEUTRAL);
    return getTown()
        .thenCombine(
            otherPlayer.getTown(),
            (playerTown, otherPlayerTown) -> {
              if (playerTown == null || otherPlayerTown == null) {
                return TownRelation.NEUTRAL;
              }
              return playerTown.getRelationWith(otherPlayerTown);
            });
  }
  public TownRelation getRelationWithPlayerSync(ITanPlayer otherPlayer) {
    if (!hasTown() || !otherPlayer.hasTown()) return TownRelation.NEUTRAL;
    return TownRelation.NEUTRAL;
  }
  public Integer getRegionRankID() {
    if (!hasRegion()) {
      return null;
    }
    // Return cached rankID if already set
    if (regionRankID != null) {
      return regionRankID;
    }
    // Return null for non-blocking call; async fetch will cache for next call
    return null;
  }

  /**
   * Gets the region rank ID asynchronously.
   * @return CompletableFuture with the rank ID, or null if not in a nation
   */
  public CompletableFuture<Integer> getRegionRankIDAsync() {
    if (!hasRegion()) {
      return CompletableFuture.completedFuture(null);
    }
    if (regionRankID != null) {
      return CompletableFuture.completedFuture(regionRankID);
    }
    return getRegion().thenApply(region -> {
      if (region != null) {
        regionRankID = region.getDefaultRankID();
        return regionRankID;
      }
      return null;
    });
  }
  public void setRegionRankID(Integer rankID) {
    this.regionRankID = rankID;
  }
  public Integer getRankID(TerritoryData territoryData) {
    if (territoryData instanceof TownData) {
      return getTownRankID();
    } else if (territoryData instanceof RegionData) {
      return getRegionRankID();
    }
    return null;
  }
  @Override
  public RankData getRank(TerritoryData territoryData) {
    return territoryData.getRank(getRankID(territoryData));
  }
  public CompletableFuture<List<TerritoryData>> getAllTerritoriesPlayerIsIn() {
    CompletableFuture<TownData> townFuture = getTown();
    CompletableFuture<RegionData> regionFuture = getRegion();
    return CompletableFuture.allOf(townFuture, regionFuture)
        .thenApply(
            v -> {
              List<TerritoryData> territories = new ArrayList<>();
              TownData town = townFuture.join();
              RegionData region = regionFuture.join();
              if (town != null) {
                territories.add(town);
              }
              if (region != null) {
                territories.add(region);
              }
              return territories;
            });
  }
  public OfflinePlayer getOfflinePlayer() {
    return Bukkit.getServer().getOfflinePlayer(getUUID());
  }
  public LangType getLang() {
    if (lang == null) return Lang.getServerLang();
    return lang;
  }
  public void setLang(LangType lang) {
    this.lang = lang;
  }
  public void clearAllTownApplications() {
    TownInviteDataStorage.removeInvitation(uuid);
    for (TownData allTown : TownDataStorage.getInstance().getAllSync().values()) {
      allTown.removePlayerJoinRequest(uuid);
    }
  }
  public void setRankID(TerritoryData territoryData, Integer defaultRankID) {
    if (territoryData instanceof TownData) {
      setTownRankID(defaultRankID);
    }
    if (territoryData instanceof RegionData) {
      setRegionRankID(defaultRankID);
    }
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
  @Override
  public CompletableFuture<List<CurrentAttack>> getCurrentAttacks() {
    return getAllTerritoriesPlayerIsIn()
        .thenApply(
            territories -> {
              List<CurrentAttack> res = new ArrayList<>();
              for (TerritoryData territoryData : territories) {
                res.addAll(territoryData.getCurrentAttacks());
              }
              return res;
            });
  }

  // Database tracking fields getters/setters (v2.0)

  /**
   * Gets the player's last known IP address.
   * @return The IP address, or null if not tracked
   */
  public String getLastKnownIP() {
    return ipAddress;
  }

  /**
   * Sets the player's IP address.
   * Called automatically on player join.
   * @param ipAddress The IP address
   */
  public void setLastKnownIP(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  /**
   * Gets the timestamp when the player first joined.
   * @return The first seen timestamp in milliseconds, or null if not tracked
   */
  public Long getFirstSeen() {
    return firstSeen;
  }

  /**
   * Sets the first seen timestamp.
   * Only set once during player registration.
   * @param firstSeen The timestamp in milliseconds
   */
  public void setFirstSeen(Long firstSeen) {
    this.firstSeen = firstSeen;
  }

  /**
   * Checks if the player is currently online.
   * @return true if online, false otherwise
   */
  public boolean isOnline() {
    return isOnline;
  }

  /**
   * Sets the player's online status.
   * Called automatically on join/quit.
   * @param isOnline true if player is online
   */
  public void setOnline(boolean isOnline) {
    this.isOnline = isOnline;
  }
}