package org.leralix.tan.dataclass;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.timezone.TimeZoneEnum;
import org.leralix.tan.wars.legacy.CurrentAttack;

/**
 * Interface representing a player in the Towns and Nations plugin system.
 * <p>
 * This interface abstracts player data including town membership, region affiliation,
 * economy balance, properties, and ranks. It provides both synchronous and asynchronous
 * methods for data access to support Folia's regionalized threading.
 * </p>
 * <p>
 * <b>Key Information:</b>
 * <ul>
 *   <li><b>Identity:</b> UUID, stored name, current display name</li>
 *   <li><b>Town:</b> Current town membership, rank within town</li>
 *   <li><b>Region:</b> Associated region through town membership</li>
 *   <li><b>Economy:</b> Personal balance, transactions</li>
 *   <li><b>Properties:</b> Owned land claims within towns</li>
 *   <li><b>Wars:</b> Military involvements and attack participations</li>
 *   <li><b>Settings:</b> Language, timezone preferences</li>
 * </ul>
 * </p>
 * <p>
 * <b>Thread Safety:</b><br>
 * Prefer async methods ({@code getTown()}, {@code getRegion()}) over sync variants.
 * Sync methods ({@code getTownSync()}, {@code getRegionSync()}) block and should only be
 * used when absolutely necessary.
 * </p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Async pattern (recommended)
 * PlayerDataStorage.getInstance().get(player)
 *     .thenAccept(tanPlayer -> {
 *         if (tanPlayer != null && tanPlayer.hasTown()) {
 *             tanPlayer.getTown().thenAccept(town -> {
 *                 // Process town data
 *             });
 *         }
 *     });
 *
 * // Sync pattern (use sparingly)
 * ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
 * if (tanPlayer != null) {
 *     double balance = tanPlayer.getBalance();
 * }
 * }</pre>
 *
 * @see PropertyData
 * @see TownData
 * @see RegionData
 * @see RankData
 * @see org.leralix.tan.storage.stored.PlayerDataStorage
 * @since 0.15.0
 */
public interface ITanPlayer {
  String getID();
  void setUuid(String uuid);
  String getNameStored();
  void setNameStored(String name);
  void clearName();
  double getBalance();
  void setBalance(double balance);
  String getTownId();
  String getTownName();
  CompletableFuture<TownData> getTown();
  default TownData getTownSync() {
    try {
      return getTown().join();
    } catch (Exception e) {
      return null;
    }
  }
  boolean hasTown();
  boolean isTownOverlord();
  RankData getTownRank();
  RankData getRegionRank();
  void addToBalance(double amount);
  void removeFromBalance(double amount);
  boolean hasRegion();
  CompletableFuture<RegionData> getRegion();
  default RegionData getRegionSync() {
    try {
      return getRegion().join();
    } catch (Exception e) {
      return null;
    }
  }
  String getNationName();
  UUID getUUID();
  void joinTown(TownData townData);
  void leaveTown();
  void setTownRankID(int townRankID);
  Integer getTownRankID();
  List<String> getPropertiesListID();
  void addProperty(PropertyData propertyData);
  List<PropertyData> getProperties();
  void removeProperty(PropertyData propertyData);
  Player getPlayer();
  List<String> getAttackInvolvedIn();
  void addWar(CurrentAttack currentAttacks);
  void updateCurrentAttack();
  boolean isAtWarWith(TerritoryData territoryData);
  void removeWar(@NotNull CurrentAttack currentAttacks);
  CompletableFuture<TownRelation> getRelationWithPlayer(ITanPlayer otherPlayer);
  CompletableFuture<TownRelation> getRelationWithPlayer(Player otherPlayer);
  TownRelation getRelationWithPlayerSync(ITanPlayer otherPlayer);
  Integer getRegionRankID();
  void setRegionRankID(Integer rankID);
  Integer getRankID(TerritoryData territoryData);
  RankData getRank(TerritoryData territoryData);
  CompletableFuture<List<TerritoryData>> getAllTerritoriesPlayerIsIn();
  default List<TerritoryData> getAllTerritoriesPlayerIsInSync() {
    try {
      return getAllTerritoriesPlayerIsIn().join();
    } catch (Exception e) {
      return null;
    }
  }
  OfflinePlayer getOfflinePlayer();
  LangType getLang();
  void setLang(LangType lang);
  void clearAllTownApplications();
  void setRankID(TerritoryData territoryData, Integer defaultRankID);
  TimeZoneEnum getTimeZone();
  void setTimeZone(TimeZoneEnum timeZone);
  CompletableFuture<List<CurrentAttack>> getCurrentAttacks();
}