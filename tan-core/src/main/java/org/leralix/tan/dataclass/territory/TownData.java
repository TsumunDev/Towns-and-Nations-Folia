package org.leralix.tan.dataclass.territory;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.leralix.lib.data.SoundEnum;
import org.leralix.lib.position.Vector2D;
import org.leralix.lib.position.Vector3D;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.*;
import org.leralix.tan.dataclass.newhistory.PlayerTaxHistory;
import org.leralix.tan.dataclass.territory.economy.*;
import org.leralix.tan.dataclass.territory.progression.TownProgressionComponent;
import org.leralix.tan.dataclass.territory.progression.TownTier;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.events.EventManager;
import org.leralix.tan.events.events.PlayerJoinTownAcceptedInternalEvent;
import org.leralix.tan.events.events.PlayerJoinTownRequestInternalEvent;
import org.leralix.tan.gui.user.territory.TownMenu;
import org.leralix.tan.lang.FilledLang;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.*;
import org.leralix.tan.upgrade.rewards.numeric.TownPlayerCap;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.utils.graphic.PrefixUtil;
import org.leralix.tan.utils.graphic.TeamUtils;
import org.leralix.tan.utils.text.TanChatUtils;
import org.leralix.tan.domain.claim.ClaimHolder;
import org.leralix.tan.domain.gui.TownGuiHolder;
import org.leralix.tan.domain.economy.TownEconomyHolder;
import org.leralix.tan.domain.member.MemberHolder;
import org.leralix.tan.dataclass.territory.components.TownPropertyComponent;
import org.leralix.tan.dataclass.territory.components.TownRecruitmentComponent;
import org.leralix.tan.domain.property.PropertyHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a town within the Towns and Nations plugin.
 * <p>
 * A town is a player-controlled territory that can claim land, collect taxes,
 * manage members through ranks, and optionally join a {@link RegionData} (region/nation).
 * </p>
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li><b>Land Claims:</b> Towns can claim chunks in a defined area around their capital</li>
 *   <li><b>Membership:</b> Players join towns via application or invitation</li>
 *   <li><b>Ranks:</b> Customizable roles with permissions (build, claim, upgrade, etc.)</li>
 *   <li><b>Economy:</b> Treasury, taxes, property sales, and salaries</li>
 *   <li><b>Upgrades:</b> Level-based unlocking of features (chunk cap, mob ban, spawn, etc.)</li>
 *   <li><b>Properties:</b> Players can buy/rent land within town claims</li>
 *   <li><b>Diplomacy:</b> Can join regions, establish alliances, trade, or war</li>
 * </ul>
 * </p>
 * <p>
 * <b>Thread Safety:</b><br>
 * All operations should be performed asynchronously. Use {@link TownDataStorage#get(String)}
 * for non-blocking reads.
 * </p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * // Create a new town
 * TownData town = new TownData("town_123", "MyTown", leaderPlayer);
 * town.addPlayer(leaderPlayer);
 * TownDataStorage.getInstance().update(town);
 *
 * // Claim a chunk
 * town.claimChunk(player);
 * }</pre>
 *
 * @see TerritoryData
 * @see RegionData
 * @see RankData
 * @see PropertyData
 * @since 0.15.0
 */
public class TownData extends TerritoryData {
  private static final Logger LOGGER = LoggerFactory.getLogger(TownData.class);
  private String uuidLeader;
  private String townTag;
  // Gson-serialized data fields (must stay on TownData for deserialization compatibility)
  private Map<String, PropertyData> propertyDataMap = new ConcurrentHashMap<>();
  private boolean isRecruiting;
  private HashSet<String> playerJoinRequestSet = new HashSet<>();

  // Transient components — lazy-initialized, operate on TownData fields by reference
  private transient TownRecruitmentComponent recruitmentComponent;
  private transient TownPropertyComponent propertyComponent;

  private TeleportationPosition teleportationPosition;
  private final HashSet<String> townPlayerListId;
  private Vector2D capitalLocation;
  private TownProgressionComponent progression;
  private org.leralix.tan.domain.prestige.model.PrestigePoints prestigePoints;
  private final Set<String> purchasedUpgrades = ConcurrentHashMap.newKeySet();

  private TownRecruitmentComponent getRecruitmentComponent() {
    if (recruitmentComponent == null) {
      recruitmentComponent = new TownRecruitmentComponent(this, isRecruiting, playerJoinRequestSet);
    }
    return recruitmentComponent;
  }

  private TownPropertyComponent getPropertyComponent() {
    if (propertyComponent == null) {
      propertyComponent = new TownPropertyComponent(getID(), propertyDataMap);
    }
    return propertyComponent;
  }

  public TownData(String townId, String townName, ITanPlayer leader) {
    super(townId, townName, leader);
    this.isRecruiting = false;
    this.playerJoinRequestSet = new HashSet<>();
    this.townPlayerListId = new HashSet<>();
    this.propertyDataMap = new ConcurrentHashMap<>();
    if (leader != null) {
      this.uuidLeader = leader.getID();
      addPlayer(leader);
    }
    int prefixSize = Constants.getPrefixSize();
    this.townTag =
        townName.length() >= prefixSize
            ? townName.substring(0, prefixSize).toUpperCase()
            : townName.toUpperCase();
    this.progression = new TownProgressionComponent();
    this.prestigePoints = org.leralix.tan.domain.prestige.model.PrestigePoints.create();
  }
  @Override
  protected void initUpgradesStatus() {
    this.upgradesStatus =
        new org.leralix.tan.upgrade.TerritoryStats(org.leralix.tan.upgrade.rewards.StatsType.TOWN);
  }
  @Override
  public RankData getRank(ITanPlayer tanPlayer) {
    try {
      return MemberHolder.getService()
          .getRank(this.getID(), tanPlayer)
          .exceptionally(throwable -> {
            LOGGER.warn("MemberService.getRank failed", throwable);
            return null;
          })
          .join();
    } catch (Exception e) {
      LOGGER.warn("MemberService.getRank failed", e);
      return getRank(tanPlayer.getTownRankID());
    }
  }

  /**
   * Gets a player's rank asynchronously.
   *
   * <p>This is the non-blocking version of {@link #getRank(ITanPlayer)}. It returns a
   * {@link CompletableFuture} and never blocks the calling thread, making it safe
   * for Folia region threads.</p>
   *
   * @param tanPlayer The player to query
   * @return CompletableFuture containing the player's rank
   * @since 2.1.0
   */
  public CompletableFuture<RankData> getRankAsync(ITanPlayer tanPlayer) {
    return MemberHolder.getService()
        .getRank(this.getID(), tanPlayer)
        .exceptionally(throwable -> {
          LOGGER.warn("MemberService.getRank failed", throwable);
          return null;
        })
        .thenApply(rank -> rank != null ? rank : getRank(tanPlayer.getTownRankID()));
  }
  public void addPlayer(ITanPlayer tanNewPlayer) {
    townPlayerListId.add(tanNewPlayer.getID());
    getTownDefaultRank().addPlayer(tanNewPlayer);
    tanNewPlayer.joinTown(this);
    org.leralix.tan.utils.FoliaScheduler.runTask(
        org.leralix.tan.TownsAndNations.getPlugin(),
        () -> {
          // Entity validity guard — player may have disconnected during async load
          Player newPlayer = tanNewPlayer.getPlayer();
          if (newPlayer == null || !newPlayer.isOnline()) return;

          TanChatUtils.message(
              newPlayer,
              Lang.TOWN_INVITATION_ACCEPTED_MEMBER_SIDE.get(
                  tanNewPlayer.getLang(), getBaseColoredName()));
          tanNewPlayer.clearAllTownApplications();
          for (TerritoryData overlords : getOverlords()) {
            overlords.registerPlayer(tanNewPlayer);
          }
          EventManager.getInstance()
              .callEvent(new PlayerJoinTownAcceptedInternalEvent(tanNewPlayer, this));
          TeamUtils.updateAllScoreboardColor();
          PrefixUtil.updatePrefix(newPlayer);
        });
    TownDataStorage.getInstance().putSync(getID(), this);
  }
  /**
   * Removes a player from the town asynchronously.
   *
   * <p>This method loads the player data asynchronously and then removes them from the town.</p>
   *
   * @param tanPlayerID The UUID of the player to remove
   * @return CompletableFuture that completes when the player is removed
   */
  public CompletableFuture<Void> removePlayerAsync(String tanPlayerID) {
    return PlayerDataStorage.getInstance()
        .get(tanPlayerID)
        .thenAccept(this::removePlayer);
  }
  /**
   * Removes a player from the town (synchronous - player already loaded).
   *
   * <p><b>Deprecated:</b> Use {@link #removePlayerAsync(String)} instead to avoid blocking I/O.
   * This synchronous version is kept for backwards compatibility.</p>
   *
   * @param tanPlayerID The UUID of the player to remove
   * @deprecated Use removePlayerAsync instead
   */
  @Deprecated
  public void removePlayer(String tanPlayerID) {
    removePlayer(PlayerDataStorage.getInstance().getSync(tanPlayerID));
  }
  public void removePlayer(ITanPlayer tanPlayer) {
    for (TerritoryData overlords : getOverlords()) {
      overlords.unregisterPlayer(tanPlayer);
    }
    getRank(tanPlayer).removePlayer(tanPlayer);
    townPlayerListId.remove(tanPlayer.getID());
    tanPlayer.leaveTown();
    TownDataStorage.getInstance().putSync(getID(), this);
    PrefixUtil.updatePrefix(tanPlayer.getPlayer());
  }
  @Override
  public Collection<String> getPlayerIDList() {
    return townPlayerListId;
  }
  @Override
  public Collection<ITanPlayer> getITanPlayerList() {
    Map<String, ITanPlayer> playerMap =
        PlayerDataStorage.getInstance().getBatchSync(getPlayerIDList());
    return new ArrayList<>(playerMap.values());
  }
  /**
   * Gets the town icon with name asynchronously.
   *
   * <p>This is the non-blocking version of {@link #getIconWithName()}. It returns a
   * {@link CompletableFuture} and never blocks the calling thread, making it safe
   * for Folia region threads.</p>
   *
   * @return CompletableFuture containing the ItemStack with the town name
   * @since 2.1.0
   */
  public CompletableFuture<ItemStack> getIconWithNameAsync() {
    return TownGuiHolder.getService()
        .getIconWithName(getID())
        .exceptionally(e -> {
          LOGGER.warn("TownGuiService.getIconWithName failed: " + e.getMessage());
          return null;
        });
  }

  @Override
  @Deprecated
  public ItemStack getIconWithName() {
    return getIconWithNameAsync().join();
  }
  /**
   * Gets the town icon with information asynchronously.
   *
   * <p>This is the non-blocking version of {@link #getIconWithInformations(LangType)}.
   * It returns a {@link CompletableFuture} and never blocks the calling thread,
   * making it safe for Folia region threads.</p>
   *
   * @param langType The language type for localization
   * @return CompletableFuture containing the ItemStack with town information
   * @since 2.1.0
   */
  public CompletableFuture<ItemStack> getIconWithInformationsAsync(LangType langType) {
    return TownGuiHolder.getService()
        .getIconWithInformations(getID(), langType)
        .exceptionally(e -> {
          LOGGER.warn("TownGuiService.getIconWithInformations failed: " + e.getMessage());
          return null;
        });
  }

  @Override
  @Deprecated
  public ItemStack getIconWithInformations(LangType langType) {
    return TownGuiHolder.getService()
        .getIconWithInformations(getID(), langType)
        .exceptionally(e -> {
          LOGGER.warn("TownGuiService.getIconWithInformations failed: " + e.getMessage());
          return null;
        })
        .join();
  }
  @Override
  public int getHierarchyRank() {
    return 0;
  }
  @Override
  public String getBaseColoredName() {
    return "§9" + getName();
  }
  @Override
  public String getLeaderID() {
    if (this.uuidLeader == null) {
      if (townPlayerListId.isEmpty()) {
        return null;
      }
      return townPlayerListId
          .iterator()
          .next();
    }
    return this.uuidLeader;
  }
  @Override
  public ITanPlayer getLeaderData() {
    String leaderID = getLeaderID();
    if (leaderID == null) {
      return null;
    }
    return PlayerDataStorage.getInstance().getSync(leaderID);
  }
  /**
   * Gets the town leader's data asynchronously.
   *
   * <p>This method loads the leader player data without blocking the calling thread.</p>
   *
   * @return CompletableFuture containing the leader's ITanPlayer, or null if no leader
   */
  public CompletableFuture<ITanPlayer> getLeaderDataAsync() {
    String leaderID = getLeaderID();
    if (leaderID == null) {
      return CompletableFuture.completedFuture(null);
    }
    return PlayerDataStorage.getInstance().get(leaderID);
  }
  @Override
  public void setLeaderID(String leaderID) {
    this.uuidLeader = leaderID;
  }

  @Override
  public boolean isLeader(String leaderID) {
    return getLeaderID().equals(leaderID);
  }
  @Override
  protected Collection<TerritoryData> getOverlords() {
    List<TerritoryData> overlords = new ArrayList<>();
    if (haveOverlord()) {
      RegionData regionData = RegionDataStorage.getInstance().getSync(this.overlordID);
      if (regionData != null) {
        overlords.add(regionData);
        regionData.getOverlord().ifPresent(overlords::add);
      }
    }
    return overlords;
  }
  /**
   * Gets the town's overlord (region/nation) hierarchy asynchronously.
   *
   * <p>This method loads all overlords in the hierarchy without blocking the calling thread.</p>
   *
   * @return CompletableFuture containing a collection of overlords
   */
  protected CompletableFuture<List<TerritoryData>> getOverlordsAsync() {
    List<TerritoryData> overlords = new ArrayList<>();
    if (!haveOverlord()) {
      return CompletableFuture.completedFuture(overlords);
    }

    return RegionDataStorage.getInstance()
        .get(this.overlordID)
        .thenApply(regionData -> {
          if (regionData != null) {
            overlords.add(regionData);
            regionData.getOverlord().ifPresent(overlords::add);
          }
          return overlords;
        });
  }
  @Override
  public void broadCastMessage(FilledLang message) {
    for (String playerId : townPlayerListId) {
      Player player = Bukkit.getServer().getPlayer(UUID.fromString(playerId));
      if (player != null && player.isOnline()) {
        TanChatUtils.message(player, message.get(player));
      }
    }
  }
  @Override
  public void broadcastMessageWithSound(
      FilledLang message, SoundEnum soundEnum, boolean addPrefix) {
    for (String playerId : townPlayerListId) {
      org.leralix.tan.utils.FoliaScheduler.runTask(
          org.leralix.tan.TownsAndNations.getPlugin(),
          () -> {
            Player player = Bukkit.getPlayer(UUID.fromString(playerId));
            if (player != null && player.isOnline()) {
              TanChatUtils.message(player, message, soundEnum);
            }
          });
    }
  }
  @Override
  public void broadcastMessageWithSound(FilledLang message, SoundEnum soundEnum) {
    broadcastMessageWithSound(message, soundEnum, true);
  }
  public RankData getTownDefaultRank() {
    return getRank(getDefaultRankID());
  }
  public boolean isFull() {
    return !upgradesStatus.getStat(TownPlayerCap.class).canDoAction(this.townPlayerListId.size());
  }
  public void addPlayerJoinRequest(Player player) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    EventManager.getInstance().callEvent(new PlayerJoinTownRequestInternalEvent(tanPlayer, this));
    getRecruitmentComponent().addPlayerJoinRequest(tanPlayer.getID());
  }

  /**
   * Adds a player's join request asynchronously.
   *
   * <p>This method loads player data asynchronously to avoid blocking the calling thread.
   * Use this version for join requests in async contexts or on region threads.</p>
   *
   * @param player The player requesting to join
   * @return CompletableFuture that completes when the request is processed
   */
  public CompletableFuture<Void> addPlayerJoinRequestAsync(Player player) {
    return PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(tanPlayer -> {
          EventManager.getInstance().callEvent(new PlayerJoinTownRequestInternalEvent(tanPlayer, this));
          getRecruitmentComponent().addPlayerJoinRequest(tanPlayer.getID());
        });
  }
  public void addPlayerJoinRequest(String playerUUID) {
    getRecruitmentComponent().addPlayerJoinRequest(playerUUID);
  }
  public void removePlayerJoinRequest(String playerUUID) {
    getRecruitmentComponent().removePlayerJoinRequest(playerUUID);
  }
  public void removePlayerJoinRequest(Player player) {
    getRecruitmentComponent().removePlayerJoinRequest(player);
  }
  public boolean isPlayerAlreadyRequested(String playerUUID) {
    return getRecruitmentComponent().isPlayerAlreadyRequested(playerUUID);
  }
  public boolean isPlayerAlreadyRequested(Player player) {
    return getRecruitmentComponent().isPlayerAlreadyRequested(player);
  }
  public Set<String> getPlayerJoinRequestSet() {
    return this.playerJoinRequestSet;
  }
  public boolean isRecruiting() {
    return this.isRecruiting;
  }
  public void swapRecruiting() {
    this.isRecruiting = !this.isRecruiting;
  }
  protected CompletableFuture<Void> collectTaxesAsync() {
    Collection<ITanPlayer> tanPlayers = getITanPlayerList();

    // Collect taxes from all members asynchronously (in parallel)
    List<CompletableFuture<Void>> taxFutures = new ArrayList<>();

    for (ITanPlayer tanPlayer : tanPlayers) {
      OfflinePlayer offlinePlayer = tanPlayer.getOfflinePlayer();
      if (!getRank(tanPlayer).isPayingTaxes()) continue;

      double tax = getTax();

      // Create async tax collection task for each player
      CompletableFuture<Void> taxFuture = org.leralix.tan.service.AsyncEconomyService
          .getBalance(offlinePlayer)
          .thenAccept(balance -> {
            if (balance > tax) {
              // Sufficient funds - collect tax
              org.leralix.tan.service.AsyncEconomyService.withdraw(offlinePlayer, tax)
                  .thenRun(() -> {
                    addToBalance(tax);
                    TownsAndNations.getPlugin()
                        .getDatabaseHandler()
                        .addTransactionHistory(new PlayerTaxHistory(this, tanPlayer, tax));
                  })
                  .exceptionally(throwable -> {
                    TownsAndNations.getPlugin()
                        .getLogger()
                        .warning("Failed to collect tax from " + tanPlayer.getNameStored() + ": " + throwable.getMessage());
                    return null;
                  });
            } else {
              // Insufficient funds - record failed tax collection
              TownsAndNations.getPlugin()
                  .getDatabaseHandler()
                  .addTransactionHistory(new PlayerTaxHistory(this, tanPlayer, -1));
            }
          })
          .exceptionally(throwable -> {
            TownsAndNations.getPlugin()
                .getLogger()
                .warning("Failed to check balance for tax collection from " + tanPlayer.getNameStored() + ": " + throwable.getMessage());
            return null;
          });

      taxFutures.add(taxFuture);
    }

    // Wait for all tax collections to complete
    return CompletableFuture.allOf(taxFutures.toArray(new CompletableFuture[0]));
  }

  /**
   * Legacy synchronous tax collection method.
   *
   * <p><b>Deprecated:</b> Use {@link #collectTaxesAsync()} instead to avoid blocking I/O.</p>
   *
   * @deprecated Use collectTaxesAsync instead
   */
  @Override
  @Deprecated
  protected void collectTaxes() {
    Collection<ITanPlayer> tanPlayers = getITanPlayerList();
    for (ITanPlayer tanPlayer : tanPlayers) {
      OfflinePlayer offlinePlayer = tanPlayer.getOfflinePlayer();
      if (!getRank(tanPlayer).isPayingTaxes()) continue;
      double tax = getTax();
      if (EconomyUtil.getBalance(offlinePlayer) > tax) {
        EconomyUtil.removeFromBalance(offlinePlayer, tax);
        addToBalance(tax);
        TownsAndNations.getPlugin()
            .getDatabaseHandler()
            .addTransactionHistory(new PlayerTaxHistory(this, tanPlayer, tax));
      } else {
        TownsAndNations.getPlugin()
            .getDatabaseHandler()
            .addTransactionHistory(new PlayerTaxHistory(this, tanPlayer, -1));
      }
    }
  }
  public void setSpawn(Location location) {
    this.teleportationPosition = new TeleportationPosition(location);
  }
  public boolean isSpawnSet() {
    return this.teleportationPosition != null;
  }
  public TeleportationPosition getSpawn() {
    return this.teleportationPosition;
  }
  /**
   * Claims a chunk asynchronously.
   *
   * <p>This is the non-blocking version of {@link #abstractClaimChunk(Player, Chunk, boolean)}.
   * It returns a {@link CompletableFuture} and never blocks the calling thread,
   * making it safe for Folia region threads.</p>
   *
   * @param player The player claiming the chunk
   * @param chunk The chunk to claim
   * @param ignoreAdjacent Whether to ignore adjacency requirements
   * @return CompletableFuture that completes when the chunk is claimed
   * @since 2.1.0
   */
  public CompletableFuture<Void> abstractClaimChunkAsync(Player player, Chunk chunk, boolean ignoreAdjacent) {
    return ClaimHolder.getService()
        .claimChunk(getID(), chunk)
        .exceptionally(e -> {
          LOGGER.warn("ClaimService.claimChunk failed: " + e.getMessage());
          return null;
        })
        .thenRun(() -> {});
  }

  @Override
  @Deprecated
  public void abstractClaimChunk(Player player, Chunk chunk, boolean ignoreAdjacent) {
    ClaimHolder.getService()
        .claimChunk(getID(), chunk)
        .exceptionally(e -> {
          LOGGER.warn("ClaimService.claimChunk failed: " + e.getMessage());
          return null;
        })
        .join();
  }
  public void setCapitalLocation(Vector2D vector2D) {
    capitalLocation = vector2D;
  }
  public Optional<Vector2D> getCapitalLocation() {
    return Optional.ofNullable(capitalLocation);
  }
  public RegionData getRegionSync() {
    return RegionDataStorage.getInstance().getSync(this.overlordID);
  }

  /**
   * Gets the region (nation) this town belongs to asynchronously.
   *
   * <p>This method loads region data asynchronously to avoid blocking the calling thread.
   * Use this version for region lookups in async contexts or on region threads.</p>
   *
   * @return CompletableFuture containing the RegionData, or null if town has no region
   */
  public CompletableFuture<RegionData> getRegionAsync() {
    if (this.overlordID == null) {
      return CompletableFuture.completedFuture(null);
    }
    return RegionDataStorage.getInstance().get(this.overlordID)
        .thenApply(regionData -> (RegionData) regionData);
  }
  @Override
  public Collection<TerritoryData> getPotentialVassals() {
    return Collections.emptyList();
  }
  public void removeOverlordPrivate() {
    Collection<ITanPlayer> tanPlayers = getITanPlayerList();
    for (ITanPlayer tanPlayer : tanPlayers) {
      tanPlayer.setRegionRankID(null);
    }
  }
  @Override
  protected void addVassalPrivate(TerritoryData vassal) {
  }
  @Override
  protected void removeVassal(TerritoryData vassal) {
  }
  @Override
  public TerritoryData getCapital() {
    return null;
  }
  @Override
  @Deprecated
  public List<GuiItem> getOrderedMemberList(ITanPlayer tanPlayer) {
    return new ArrayList<>();
  }

  /**
   * Gets ordered member list asynchronously for GUI display.
   *
   * <p>This method loads all player data in batch using PlayerDataStorage.getBatchSync()
   * to minimize blocking I/O operations. Use this version for GUI rendering in async contexts.</p>
   *
   * @param tanPlayer The player viewing the member list
   * @return CompletableFuture containing the list of GUI items for each member
   */
  public CompletableFuture<List<GuiItem>> getOrderedMemberListAsync(ITanPlayer tanPlayer) {
    return TownGuiHolder.getService().getOrderedMemberList(getID(), tanPlayer)
        .exceptionally(e -> {
          LOGGER.warn("TownGuiService.getOrderedMemberList failed: " + e.getMessage());
          return new ArrayList<>();
        });
  }
  @Override
  protected void specificSetPlayerRank(ITanPlayer tanPlayer, int rankID) {
    tanPlayer.setTownRankID(rankID);
  }
  @Override
  protected void addSpecificTaxes(Budget budget) {
    budget.addProfitLine(new PlayerTaxLine(this));
    getOverlord().ifPresent(overlord -> budget.addProfitLine(new OverlordTaxLine(this, overlord)));
    budget.addProfitLine(new PropertyRentTaxLine(this));
    budget.addProfitLine(new PropertySellTaxLine(this));
    budget.addProfitLine(new PropertyCreationTaxLine(this));
  }
  public Map<String, PropertyData> getPropertyDataMap() {
    return getPropertyComponent().getPropertyDataMap();
  }
  public Collection<PropertyData> getProperties() {
    return getPropertyComponent().getProperties();
  }
  public String nextPropertyID() {
    return getPropertyComponent().nextPropertyID();
  }
  public PropertyData registerNewProperty(Vector3D p1, Vector3D p2, TerritoryData owner) {
    return getPropertyComponent().registerNewProperty(p1, p2, owner);
  }
  public PropertyData registerNewProperty(Vector3D p1, Vector3D p2, ITanPlayer owner) {
    return getPropertyComponent().registerNewProperty(p1, p2, owner);
  }
  /**
   * Gets a property by its ID asynchronously.
   *
   * <p>This is the non-blocking version of {@link #getProperty(String)}. It returns a
   * {@link CompletableFuture} and never blocks the calling thread, making it safe
   * for Folia region threads.</p>
   *
   * @param id The property ID (e.g., "P0", "P1")
   * @return CompletableFuture containing the property, or null if not found
   * @since 2.1.0
   */
  public CompletableFuture<PropertyData> getPropertyAsync(String id) {
    return PropertyHolder.getService()
        .getProperty(getID(), id)
        .exceptionally(e -> { LOGGER.warn("PropertyService.getProperty failed: {}", e.getMessage()); return null; });
  }

  /**
   * Gets a property by its ID.
   *
   * @param id The property ID (e.g., "P0", "P1")
   * @return The property, or null if not found
   * @deprecated Use {@link #getPropertyAsync(String)} instead to avoid blocking Folia region threads
   * @since 2.0.0
   */
  @Deprecated
  public PropertyData getProperty(String id) {
    return PropertyHolder.getService()
        .getProperty(getID(), id)
        .exceptionally(e -> { LOGGER.warn("PropertyService.getProperty failed: {}", e.getMessage()); return null; })
        .join();
  }

  /**
   * Gets a property at a specific location asynchronously.
   *
   * @param location The location to search
   * @return CompletableFuture containing the property at the location, or null if not found
   * @since 2.1.0
   */
  public CompletableFuture<PropertyData> getPropertyAsync(Location location) {
    return PropertyHolder.getService()
        .getPropertyAtLocation(getID(), location)
        .exceptionally(e -> {
          LOGGER.warn("PropertyService.getPropertyAtLocation failed: {}", e.getMessage());
          return null;
        });
  }

  /**
   * Gets a property at a specific location.
   *
   * @param location The location to search
   * @return The property at the location, or null if not found
   * @deprecated Use {@link #getPropertyAsync(Location)} instead to avoid blocking Folia region threads
   * @since 2.0.0
   */
  @Deprecated
  public PropertyData getProperty(Location location) {
    return PropertyHolder.getService()
        .getPropertyAtLocation(getID(), location)
        .exceptionally(e -> {
          LOGGER.warn("PropertyService.getPropertyAtLocation failed: {}", e.getMessage());
          return null;
        })
        .join();
  }
  /**
   * Removes a property from the town asynchronously.
   *
   * @param propertyData The property to remove
   * @return CompletableFuture that completes when the property is removed
   * @since 2.1.0
   */
  public CompletableFuture<Void> removePropertyAsync(PropertyData propertyData) {
    return PropertyHolder.getService()
        .removeProperty(getID(), propertyData)
        .exceptionally(e -> { LOGGER.warn("PropertyService.removeProperty failed: {}", e.getMessage()); return null; })
        .thenRun(() -> getPropertyComponent().removeProperty(propertyData));
  }

  /**
   * Removes a property from the town.
   *
   * @param propertyData The property to remove
   * @deprecated Use {@link #removePropertyAsync(PropertyData)} instead to avoid blocking Folia region threads
   * @since 2.0.0
   */
  @Deprecated
  public void removeProperty(PropertyData propertyData) {
    PropertyHolder.getService()
        .removeProperty(getID(), propertyData)
        .exceptionally(e -> {
          LOGGER.warn("PropertyService.removeProperty failed: {}", e.getMessage());
          return null;
        })
        .thenRun(() -> getPropertyComponent().removeProperty(propertyData))
        .join();
  }
  public String getTownTag() {
    if (this.townTag == null)
      setTownTag(name.substring(0, Constants.getPrefixSize()).toUpperCase());
    return this.townTag;
  }
  public void setTownTag(String townTag) {
    this.townTag = townTag;
    applyToAllOnlinePlayer(PrefixUtil::updatePrefix);
  }
  public String getColoredTag() {
    return getChunkColor() + "[" + getTownTag() + "]";
  }

  // ===== PROGRESSION SYSTEM =====

  /**
   * Gets the town's progression component.
   *
   * @return the progression component
   */
  public TownProgressionComponent getProgression() {
    // Thread-safe: Initialized in constructor, no lazy loading needed
    return progression;
  }

  /**
   * Sets the town's progression component (for deserialization).
   *
   * @param progression the new progression component
   */
  public void setProgression(TownProgressionComponent progression) {
    this.progression = progression;
  }

  /**
   * Creates a new instance with updated progression.
   *
   * @param progression the new progression component
   */
  public void withProgression(TownProgressionComponent progression) {
    this.progression = progression;
  }

  /**
   * Gets the current civilization tier of the town asynchronously.
   *
   * <p>This is the non-blocking version of {@link #getTownTier()}. It returns a
   * {@link CompletableFuture} and never blocks the calling thread, making it safe
   * for Folia region threads.</p>
   *
   * @return CompletableFuture containing the current tier
   * @since 2.1.0
   */
  public CompletableFuture<TownTier> getTownTierAsync() {
    return TownEconomyHolder.getService()
        .getTownTier(getID())
        .exceptionally(e -> { LOGGER.warn("TownEconomyService.getTownTier failed: " + e.getMessage()); return null; });
  }

  /**
   * Gets the current civilization tier of the town.
   *
   * @return the current tier
   * @deprecated Use {@link #getTownTierAsync()} instead to avoid blocking Folia region threads
   */
  @Deprecated
  public TownTier getTownTier() {
    return TownEconomyHolder.getService()
        .getTownTier(getID())
        .exceptionally(e -> { LOGGER.warn("TownEconomyService.getTownTier failed: " + e.getMessage()); return null; })
        .join();
  }

  /**
   * Gets the current level within the tier.
   *
   * @return the current level
   */
  public int getTownLevel() {
    return getProgression().getCurrentLevel();
  }

  /**
   * Gets the current XP in the current level.
   *
   * @return the current XP
   */
  public long getTownXp() {
    return getProgression().getCurrentXp();
  }

  // ===== PRESTIGE SYSTEM =====

  /**
   * Gets the town's prestige points component.
   *
   * @return the prestige points component
   */
  public org.leralix.tan.domain.prestige.model.PrestigePoints getPrestigePoints() {
    // Thread-safe: Initialized in constructor, no lazy loading needed
    return prestigePoints;
  }

  /**
   * Sets the town's prestige points component (for deserialization).
   *
   * @param prestigePoints the new prestige points component
   */
  public void setPrestigePoints(org.leralix.tan.domain.prestige.model.PrestigePoints prestigePoints) {
    this.prestigePoints = prestigePoints;
  }

  /**
   * Gets the current prestige balance.
   *
   * @return current prestige points
   */
  /**
   * Gets the current prestige balance asynchronously.
   *
   * <p>This is the non-blocking version of {@link #getPrestigeBalance()}. It returns a
   * {@link CompletableFuture} and never blocks the calling thread, making it safe
   * for Folia region threads.</p>
   *
   * @return CompletableFuture containing current prestige points
   * @since 2.1.0
   */
  public CompletableFuture<Long> getPrestigeBalanceAsync() {
    return TownEconomyHolder.getService()
        .getPrestigeBalance(getID())
        .exceptionally(e -> { LOGGER.warn("TownEconomyService.getPrestigeBalance failed: " + e.getMessage()); return null; });
  }

  /**
   * Gets the current prestige balance.
   *
   * @return current prestige points
   * @deprecated Use {@link #getPrestigeBalanceAsync()} instead to avoid blocking Folia region threads
   */
  @Deprecated
  public long getPrestigeBalance() {
    return TownEconomyHolder.getService()
        .getPrestigeBalance(getID())
        .exceptionally(e -> { LOGGER.warn("TownEconomyService.getPrestigeBalance failed: " + e.getMessage()); return null; })
        .join();
  }

  // ===== END PRESTIGE SYSTEM =====

  /**
   * Kicks a player from the town asynchronously.
   *
   * <p>This method loads the player data asynchronously and removes them from the town.</p>
   *
   * @param kickedPlayer The player to kick
   * @return CompletableFuture that completes when the player is kicked
   */
  public CompletableFuture<Void> kickPlayerAsync(OfflinePlayer kickedPlayer) {
    return PlayerDataStorage.getInstance()
        .get(kickedPlayer)
        .thenAccept(kickedITanPlayer -> {
          removePlayer(kickedITanPlayer);
          broadcastMessageWithSound(
              Lang.GUI_TOWN_MEMBER_KICKED_SUCCESS.get(kickedPlayer.getName()), SoundEnum.BAD);
          org.leralix.tan.utils.FoliaScheduler.runTask(
              org.leralix.tan.TownsAndNations.getPlugin(),
              () -> {
                // Entity validity guard — kicked player may have disconnected
                Player player = kickedPlayer.getPlayer();
                if (player != null && player.isOnline()) {
                  TanChatUtils.message(
                      player, Lang.GUI_TOWN_MEMBER_KICKED_SUCCESS_PLAYER.get(player), SoundEnum.BAD);
                }
              });
        });
  }

  /**
   * Kicks a player from the town (synchronous).
   *
   * <p><b>Deprecated:</b> Use {@link #kickPlayerAsync(OfflinePlayer)} instead to avoid blocking I/O.
   * This synchronous version is kept for backwards compatibility.</p>
   *
   * @param kickedPlayer The player to kick
   * @deprecated Use kickPlayerAsync instead
   */
  @Deprecated
  public void kickPlayer(OfflinePlayer kickedPlayer) {
    ITanPlayer kickedITanPlayer = PlayerDataStorage.getInstance().getSync(kickedPlayer);
    removePlayer(kickedITanPlayer);
    broadcastMessageWithSound(
        Lang.GUI_TOWN_MEMBER_KICKED_SUCCESS.get(kickedPlayer.getName()), SoundEnum.BAD);
    org.leralix.tan.utils.FoliaScheduler.runTask(
        org.leralix.tan.TownsAndNations.getPlugin(),
        () -> {
          // Entity validity guard — kicked player may have disconnected
          Player player = kickedPlayer.getPlayer();
          if (player != null && player.isOnline()) {
            TanChatUtils.message(
                player, Lang.GUI_TOWN_MEMBER_KICKED_SUCCESS_PLAYER.get(player), SoundEnum.BAD);
          }
        });
  }
  public boolean haveNoLeader() {
    return this.uuidLeader == null;
  }
  public void removeAllLandmark() {
    for (Landmark landmark : LandmarkStorage.getInstance().getLandmarkOf(this)) {
      landmark.removeOwnership();
    }
  }
  @Override
  public CompletableFuture<Void> delete() {
    // First execute parent's deletion logic (async)
    return super.delete().thenRun(() -> {
      // Remove from overlord region if applicable (synchronous DB call)
      if (haveOverlord()) {
        RegionData regionData = RegionDataStorage.getInstance().getSync(this.overlordID);
        if (regionData != null) {
          regionData.removeVassal(this);
        }
      }

      // Remove landmarks (synchronous)
      removeAllLandmark();

      // Remove all properties (synchronous)
      removeAllProperty();

      // Remove all players from town (synchronous in-memory)
      List<String> playersToRemove = new ArrayList<>(getPlayerIDList());
      for (String playerID : playersToRemove) {
        removePlayer(playerID);
      }

      // Update scoreboards (synchronous but fast)
      TeamUtils.updateAllScoreboardColor();

      // Delete from database storage (synchronous for now)
      TownDataStorage.getInstance().deleteTown(this);
    });
  }
  private void removeAllProperty() {
    getPropertyComponent().removeAllProperties();
  }
  @Override
  public void openMainMenu(Player player) {
    TownMenu.open(player, this);
  }

  public boolean hasPurchasedUpgrade(String upgradeId) {
    return purchasedUpgrades.contains(upgradeId);
  }

  public Set<String> getPurchasedUpgrades() {
    return Collections.unmodifiableSet(purchasedUpgrades);
  }

  public void addPurchasedUpgrade(String upgradeId) {
    purchasedUpgrades.add(upgradeId);
  }

  @Override
  public boolean canHaveVassals() {
    return false;
  }
  @Override
  public boolean canHaveOverlord() {
    return true;
  }
  @Override
  public List<String> getVassalsID() {
    return Collections.emptyList();
  }
  @Override
  public boolean isVassal(String territoryID) {
    return false;
  }

}