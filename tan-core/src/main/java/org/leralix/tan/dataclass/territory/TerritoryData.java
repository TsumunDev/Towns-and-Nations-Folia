package org.leralix.tan.dataclass.territory;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.leralix.lib.data.SoundEnum;
import org.leralix.lib.position.Vector2D;
import org.leralix.lib.position.Vector3D;
import org.leralix.lib.utils.RandomUtil;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.building.Building;
import org.leralix.tan.dataclass.*;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.dataclass.chunk.TerritoryChunk;
import org.leralix.tan.dataclass.newhistory.ChunkPaymentHistory;
import org.leralix.tan.dataclass.newhistory.MiscellaneousHistory;
import org.leralix.tan.dataclass.newhistory.PlayerDonationHistory;
import org.leralix.tan.dataclass.newhistory.SalaryPaymentHistory;
import org.leralix.tan.dataclass.territory.cosmetic.CustomIcon;
import org.leralix.tan.dataclass.territory.cosmetic.CosmeticComponent;
import org.leralix.tan.dataclass.territory.diplomacy.DiplomacyComponent;
import org.leralix.tan.dataclass.territory.war.WarComponent;
import org.leralix.tan.dataclass.territory.cosmetic.ICustomIcon;
import org.leralix.tan.dataclass.territory.cosmetic.PlayerHeadIcon;
import org.leralix.tan.dataclass.territory.economy.Budget;
import org.leralix.tan.dataclass.territory.economy.ChunkUpkeepLine;
import org.leralix.tan.dataclass.territory.economy.SalaryPaymentLine;
import org.leralix.tan.dataclass.territory.permission.ChunkPermission;
import org.leralix.tan.dataclass.territory.tax.TaxComponent;
import org.leralix.tan.dataclass.territory.treasury.TreasuryComponent;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.enums.RolePermission;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.enums.permissions.ChunkPermissionType;
import org.leralix.tan.events.EventManager;
import org.leralix.tan.events.events.DiplomacyProposalAcceptedInternalEvent;
import org.leralix.tan.events.events.DiplomacyProposalInternalEvent;
import org.leralix.tan.events.events.TerritoryVassalAcceptedInternalEvent;
import org.leralix.tan.events.events.TerritoryVassalProposalInternalEvent;
import org.leralix.tan.lang.FilledLang;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.ClaimBlacklistStorage;
import org.leralix.tan.storage.CurrentAttacksStorage;
import org.leralix.tan.storage.stored.FortStorage;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;
import org.leralix.tan.storage.stored.PlannedAttackStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.upgrade.TerritoryStats;
import org.leralix.tan.upgrade.Upgrade;
import org.leralix.tan.upgrade.rewards.StatsType;
import org.leralix.tan.upgrade.rewards.list.BiomeStat;
import org.leralix.tan.upgrade.rewards.numeric.ChunkCap;
import org.leralix.tan.upgrade.rewards.numeric.ChunkCost;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.utils.item.HeadUtils;
import org.leralix.tan.utils.file.FileUtil;
import org.leralix.tan.utils.gameplay.TerritoryUtil;
import org.leralix.tan.utils.graphic.PrefixUtil;
import org.leralix.tan.utils.graphic.TeamUtils;
import org.leralix.tan.utils.territory.ChunkUtil;
import org.leralix.tan.utils.text.StringUtil;
import org.leralix.tan.utils.text.TanChatUtils;
import org.leralix.tan.wars.PlannedAttack;
import org.leralix.tan.wars.fort.Fort;
import org.leralix.tan.wars.legacy.CurrentAttack;

/**
 * Abstract base class representing a territory in the Towns and Nations plugin.
 * <p>
 * A territory is a geopolitical entity that can be either a {@link TownData} (town)
 * or a {@link RegionData} (region/nation). Territories manage land claims, economy,
 * diplomacy, ranks, and upgrades.
 * </p>
 * <p>
 * <b>Component Architecture:</b><br>
 * This class uses composition with specialized components:
 * <ul>
 *   <li>{@link TreasuryComponent} - Manages balance, income, and expenses</li>
 *   <li>{@link TaxComponent} - Handles tax collection and rates</li>
 *   <li>{@link CosmeticComponent} - Icon, color, and visual customization</li>
 *   <li>{@link DiplomacyComponent} - Relations, alliances, vassals</li>
 *   <li>{@link WarComponent} - War goals and military actions</li>
 * </ul>
 * </p>
 * <p>
 * <b>Thread Safety:</b><br>
 * Territory data is accessed asynchronously via {@link CompletableFuture}.
 * All database operations are non-blocking. Use {@code getAsync()} methods for reads.
 * </p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Get territory asynchronously
 * TownDataStorage.getInstance().get(townId)
 *     .thenAccept(town -> {
 *         if (town != null) {
 *             double balance = town.getBalance();
 *             town.claimChunk(player);
 *         }
 *     });
 * }</pre>
 *
 * @see TownData
 * @see RegionData
 * @see TreasuryComponent
 * @see DiplomacyComponent
 * @since 0.15.0
 */
public abstract class TerritoryData {
  protected String id;
  protected String name;
  protected String overlordID;
  private TreasuryComponent treasury;
  private final Long dateTimeCreated;
  private CosmeticComponent cosmetics;
  private DiplomacyComponent diplomacy;
  private TaxComponent taxes;
  protected Integer defaultRankID;
  protected Map<Integer, RankData> ranks;
  private WarComponent war;
  private HashMap<String, Integer> availableClaims;
  private ClaimedChunkSettings chunkSettings;
  protected TerritoryStats upgradesStatus;
  protected TerritoryData(String id, String name, ITanPlayer owner) {
    this.id = id;
    this.name = name;
    this.dateTimeCreated = new Date().getTime();
    this.treasury = new TreasuryComponent();
    this.taxes = new TaxComponent();
    this.cosmetics = CosmeticComponent.builder()
        .description(Lang.DEFAULT_DESCRIPTION.getDefault())
        .icon(new PlayerHeadIcon(owner))
        .color(StringUtil.randomColor())
        .build();
    this.diplomacy = new DiplomacyComponent();
    this.war = new WarComponent();
    ranks = new HashMap<>();
    RankData defaultRank = registerNewRank("default");
    setDefaultRank(defaultRank);
    availableClaims = new HashMap<>();
    chunkSettings = new ClaimedChunkSettings();
    initUpgradesStatus();
  }
  protected abstract void initUpgradesStatus();

  /**
   * Gets the unique identifier of this territory.
   *
   * <p>The ID is generated automatically when the territory is created and remains
   * constant for the lifetime of the territory. It is used as a primary key in
   * database storage and for internal references.</p>
   *
   * @return the unique territory ID, never null
   * @see #getName()
   */
  public String getID() {
    return id;
  }

  /**
   * Gets the name of this territory.
   *
   * <p>The name can be changed using {@link #rename(Player, int, String)} or
   * {@link #rename(String)}. Names are displayed in UI elements and used for
   * player-facing references.</p>
   *
   * @return the territory name, never null or empty
   * @see #getID()
   * @see #rename(Player, int, String)
   */
  public String getName() {
    return name;
  }

  /**
   * Renames this territory with a cost check.
   *
   * <p>Checks if the territory has sufficient balance to pay the rename cost,
   * deducts the cost, updates the name, and logs the change. If insufficient
   * funds are available, an error message is sent to the player.</p>
   *
   * <p><b>Thread Safety:</b> This method performs synchronous balance checks and
   * database updates. For async operations, consider using {@code supplyAsync()}.</p>
   *
   * @param player the player requesting the rename (must be leader/admin)
   * @param cost the cost to rename the territory
   * @param newName the new name for the territory
   * @see #rename(String)
   */
  public void rename(Player player, int cost, String newName) {
    if (getBalance() < cost) {
      TanChatUtils.message(
          player,
          Lang.TERRITORY_NOT_ENOUGH_MONEY.get(
              player, getColoredName(), Double.toString(cost - getBalance())));
      return;
    }
    TownsAndNations.getPlugin()
        .getDatabaseHandler()
        .addTransactionHistory(new MiscellaneousHistory(this, cost));
    removeFromBalance(cost);
    FileUtil.addLineToHistory(Lang.HISTORY_TOWN_NAME_CHANGED.get(player.getName(), name, newName));
    TanChatUtils.message(
        player, Lang.CHANGE_MESSAGE_SUCCESS.get(player, name, newName), SoundEnum.GOOD);
    rename(newName);
  }

  /**
   * Renames this territory without cost check.
   *
   * <p>Directly updates the territory name. Use this for administrative operations
   * or when the cost check has already been performed.</p>
   *
   * @param newName the new name for the territory
   * @see #rename(Player, int, String)
   */
  public void rename(String newName) {
    this.name = newName;
  }
  public abstract int getHierarchyRank();
  public abstract String getBaseColoredName();

  /**
   * Gets the territory name as a colored Adventure component.
   *
   * <p>Returns the territory name with the territory's configured color applied.
   * Useful for displaying in modern UI components using Adventure API.</p>
   *
   * @return a colored Component with the territory name
   * @see #getBaseColoredName()
   * @see #getChunkColor()
   */
  public Component getCustomColoredName() {
    Component coloredName = Component.text(getName());
    coloredName = coloredName.color(getChunkColor());
    return coloredName;
  }

  /**
   * Gets the unique ID of the territory leader.
   *
   * <p>Returns the player UUID of the current leader. For towns, this is the mayor.
   * For regions, this is the nation leader.</p>
   *
   * @return the leader's player UUID as a string, or null if no leader
   * @see #isLeader(String)
   * @see #getLeaderData()
   */
  public abstract String getLeaderID();

  /**
   * Gets the ITanPlayer object for the territory leader.
   *
   * <p>Returns the full player data object for the current leader. This includes
   * the leader's balance, town membership, and other player-specific data.</p>
   *
   * @return the leader's ITanPlayer object, or null if leader not found
   * @see #getLeaderID()
   * @see #isLeader(ITanPlayer)
   */
  public abstract ITanPlayer getLeaderData();

  /**
   * Sets a new leader for this territory.
   *
   * <p>Updates the territory leader to the specified player. The previous leader
   * loses all leadership privileges and permissions.</p>
   *
   * @param leaderID the UUID of the new leader
   * @see #getLeaderID()
   * @see #isLeader(String)
   */
  public abstract void setLeaderID(String leaderID);

  /**
   * Checks if the given player is the leader of this territory.
   *
   * <p>This is a convenience overload that extracts the UUID from the ITanPlayer.</p>
   *
   * @param tanPlayer the player to check
   * @return true if the player is the leader, false otherwise
   * @see #isLeader(String)
   */
  public boolean isLeader(ITanPlayer tanPlayer) {
    return isLeader(tanPlayer.getID());
  }

  /**
   * Checks if the player with the given UUID is the leader of this territory.
   *
   * <p>This method must be implemented by concrete classes to determine
   * leadership status. Implementation may vary between towns and regions.</p>
   *
   * @param playerID the UUID of the player to check
   * @return true if the player is the leader, false otherwise
   * @see #getLeaderID()
   */
  public abstract boolean isLeader(String playerID);

  /**
   * Checks if the given Bukkit player is the leader of this territory.
   *
   * <p>This is a convenience overload that extracts the UUID from the Player.</p>
   *
   * @param player the Bukkit player to check
   * @return true if the player is the leader, false otherwise
   * @see #isLeader(String)
   */
  public boolean isLeader(Player player) {
    return isLeader(player.getUniqueId().toString());
  }
  /**
   * Gets the description of this territory.
   *
   * <p>The description is a custom text that can be set by the leader to provide
   * information about the territory. It is displayed in GUIs and territory info screens.</p>
   *
   * @return the territory description, may be empty
   * @see #setDescription(String)
   */
  public String getDescription() {
    return cosmetics.getDescription();
  }

  /**
   * Sets a new description for this territory.
   *
   * <p>Updates the cosmetic description. Changes are applied via the immutable
   * CosmeticComponent pattern.</p>
   *
   * @param newDescription the new description text
   * @see #getDescription()
   */
  public void setDescription(String newDescription) {
    this.cosmetics = cosmetics.withDescription(newDescription);
  }

  /**
   * Gets the icon ItemStack for this territory.
   *
   * <p>Returns a custom icon for use in GUIs. If no custom icon is set,
   * a player head icon or default barrier icon is returned.</p>
   *
   * @return the territory icon as an ItemStack
   * @see #setIcon(ICustomIcon)
   */
  public ItemStack getIcon() {
    ICustomIcon icon = cosmetics.getIcon();
    if (icon == null) {
      if (haveNoLeader()) {
        icon = new CustomIcon(new ItemStack(Material.BARRIER));
      } else {
        icon = new PlayerHeadIcon(getLeaderID());
      }
      this.cosmetics = cosmetics.withIcon(icon);
    }
    return icon.getIcon();
  }

  /**
   * Sets a custom icon for this territory.
   *
   * <p>Sets the icon displayed in GUIs and other UI elements. Accepts any
   * implementation of ICustomIcon (player heads, custom items, etc.).</p>
   *
   * @param icon the custom icon to set
   * @see #getIcon()
   */
  public void setIcon(ICustomIcon icon) {
    this.cosmetics = cosmetics.withIcon(icon);
  }
  public abstract Collection<String> getPlayerIDList();
  public boolean isPlayerIn(ITanPlayer tanPlayer) {
    return isPlayerIn(tanPlayer.getID());
  }
  public boolean isPlayerIn(Player player) {
    return isPlayerIn(player.getUniqueId().toString());
  }
  public boolean isPlayerIn(String playerID) {
    return getPlayerIDList().contains(playerID);
  }
  @Deprecated
  public CompletableFuture<Collection<String>> getOrderedPlayerIDList() {
    return CompletableFuture.supplyAsync(() -> getOrderedPlayerIDListSync());
  }
  public Collection<String> getOrderedPlayerIDListSync() {
    List<String> sortedList = new ArrayList<>();
    Collection<ITanPlayer> iTanPlayers = getITanPlayerList();
    List<ITanPlayer> playersSorted =
        iTanPlayers.stream()
            .sorted(
                Comparator.comparingInt(
                    tanPlayer -> -this.getRank(tanPlayer.getRankID(this)).getLevel()))
            .toList();
    for (ITanPlayer tanPlayer : playersSorted) {
      sortedList.add(tanPlayer.getID());
    }
    return sortedList;
  }
  public abstract Collection<ITanPlayer> getITanPlayerList();
  public ClaimedChunkSettings getChunkSettings() {
    if (chunkSettings == null) chunkSettings = new ClaimedChunkSettings();
    return chunkSettings;
  }
  public RelationData getRelations() {
    return diplomacy.getRelations();
  }
  public void setRelation(TerritoryData otherTerritory, TownRelation newRelation) {
    TownRelation oldRelation = getRelationWith(otherTerritory);
    EventManager.getInstance()
        .callEvent(
            new DiplomacyProposalAcceptedInternalEvent(
                otherTerritory, this, oldRelation, newRelation));
    this.getRelations().setRelation(newRelation, otherTerritory);
    otherTerritory.getRelations().setRelation(newRelation, this);
    TeamUtils.updateAllScoreboardColor();
  }
  private Map<String, DiplomacyProposal> getDiplomacyProposals() {
    return diplomacy.getDiplomacyProposals();
  }
  public void removeDiplomaticProposal(TerritoryData proposingTerritory) {
    removeDiplomaticProposal(proposingTerritory.getID());
  }
  public void removeDiplomaticProposal(String proposingTerritoryID) {
    getDiplomacyProposals().remove(proposingTerritoryID);
  }
  private void addDiplomaticProposal(
      TerritoryData proposingTerritory, TownRelation wantedRelation) {
    EventManager.getInstance()
        .callEvent(new DiplomacyProposalInternalEvent(this, proposingTerritory, wantedRelation));
    getDiplomacyProposals()
        .put(
            proposingTerritory.getID(),
            new DiplomacyProposal(proposingTerritory.getID(), getID(), wantedRelation));
  }
  public void receiveDiplomaticProposal(
      TerritoryData proposingTerritory, TownRelation wantedRelation) {
    removeDiplomaticProposal(proposingTerritory);
    addDiplomaticProposal(proposingTerritory, wantedRelation);
  }
  public Collection<DiplomacyProposal> getAllDiplomacyProposal() {
    return getDiplomacyProposals().values();
  }
  public CompletableFuture<TownRelation> getWorstRelationWith(ITanPlayer player) {
    return player
        .getAllTerritoriesPlayerIsIn()
        .thenApply(
            territoryDataList -> {
              TownRelation worstRelation = null;
              for (TerritoryData territoryData : territoryDataList) {
                TownRelation actualRelation = getRelationWith(territoryData);
                if (worstRelation == null || worstRelation.isSuperiorTo(actualRelation)) {
                  worstRelation = actualRelation;
                }
              }
              if (worstRelation == null) {
                return TownRelation.NEUTRAL;
              }
              return worstRelation;
            });
  }
  public TownRelation getWorstRelationWithSync(ITanPlayer player) {
    TownRelation worstRelation = null;
    List<TerritoryData> territoryDataList = player.getAllTerritoriesPlayerIsInSync();
    if (territoryDataList == null) return TownRelation.NEUTRAL;
    for (TerritoryData territoryData : territoryDataList) {
      TownRelation actualRelation = getRelationWith(territoryData);
      if (worstRelation == null || worstRelation.isSuperiorTo(actualRelation)) {
        worstRelation = actualRelation;
      }
    }
    if (worstRelation == null) {
      return TownRelation.NEUTRAL;
    }
    return worstRelation;
  }
  public TownRelation getRelationWith(TerritoryData territoryData) {
    return getRelationWith(territoryData.getID());
  }
  public TownRelation getRelationWith(String territoryID) {
    if (getID().equals(territoryID)) return TownRelation.SELF;
    Optional<TerritoryData> overlord = getOverlord();
    if (overlord.isPresent() && overlord.get().getID().equals(territoryID))
      return TownRelation.OVERLORD;
    if (getVassalsID().contains(territoryID)) return TownRelation.VASSAL;
    return getRelations().getRelationWith(territoryID);
  }
  @SuppressWarnings("unused")
  public long getCreationDate() {
    return dateTimeCreated;
  }
  public abstract void broadCastMessage(FilledLang message);
  public abstract void broadcastMessageWithSound(
      FilledLang message, SoundEnum soundEnum, boolean addPrefix);
  public abstract void broadcastMessageWithSound(FilledLang message, SoundEnum soundEnum);
  public abstract boolean haveNoLeader();
  protected abstract ItemStack getIconWithName();
  public abstract ItemStack getIconWithInformations(LangType langType);
  public ItemStack getIconWithInformationAndRelation(
      TerritoryData territoryData, LangType langType) {
    ItemStack icon = getIconWithInformations(langType);
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      List<String> lore =
          meta.hasLore()
              ? new ArrayList<>(
                  meta.lore().stream()
                      .map(LegacyComponentSerializer.legacySection()::serialize)
                      .toList())
              : new ArrayList<>();
      if (territoryData != null && lore != null) {
        TownRelation relation = getRelationWith(territoryData);
        lore.add(Lang.GUI_TOWN_INFO_TOWN_RELATION.get(langType, relation.getColoredName(langType)));
      }
      meta.lore(lore.stream().map(LegacyComponentSerializer.legacySection()::deserialize).toList());
      icon.setItemMeta(meta);
    }
    return icon;
  }
  public Collection<String> getAttacksInvolvedID() {
    return war.getAttackIncomingList();
  }
  public void addPlannedAttack(PlannedAttack plannedAttack) {
    this.war = war.withAttack(plannedAttack.getID());
  }
  public void removePlannedAttack(PlannedAttack plannedAttack) {
    this.war = war.withoutAttack(plannedAttack.getID());
  }
  public Collection<CurrentAttack> getCurrentAttacks() {
    Collection<CurrentAttack> res = new ArrayList<>();
    for (String attackID : getAttacksInvolvedID()) {
      CurrentAttack attackInvolved = CurrentAttacksStorage.get(attackID);
      if (attackInvolved != null) {
        res.add(attackInvolved);
      }
    }
    return res;
  }
  public void removeCurrentAttack(CurrentAttack currentAttacks) {
    getAttacksInvolvedID().remove(currentAttacks.getAttackData().getID());
  }
  public double getBalance() {
    return treasury.getBalance();
  }
  public void addToBalance(double balance) {
    this.treasury = treasury.addToBalance(balance);
  }
  public void removeFromBalance(double balance) {
    this.treasury = treasury.withdraw(balance);
  }
  public void setOverlord(TerritoryData overlord) {
    getOverlordsProposals().remove(overlord.getID());
    broadcastMessageWithSound(
        Lang.ACCEPTED_VASSALISATION_PROPOSAL_ALL.get(
            this.getBaseColoredName(), overlord.getBaseColoredName()),
        SoundEnum.GOOD);
    this.overlordID = overlord.getID();
    overlord.addVassal(this);
  }
  public Optional<TerritoryData> getOverlord() {
    if (overlordID == null) return Optional.empty();
    TerritoryData overlord = TerritoryUtil.getTerritory(overlordID);
    if (overlord == null) {
      overlordID = null;
      return Optional.empty();
    }
    return Optional.of(overlord);
  }
  protected abstract Collection<TerritoryData> getOverlords();
  public void removeOverlord() {
    getOverlord()
        .ifPresent(
            overlord -> {
              overlord.removeVassal(this);
              removeOverlordPrivate();
              this.overlordID = null;
            });
  }
  public abstract void removeOverlordPrivate();
  public void addVassal(TerritoryData vassal) {
    EventManager.getInstance().callEvent(new TerritoryVassalAcceptedInternalEvent(vassal, this));
    addVassalPrivate(vassal);
  }
  protected abstract void addVassalPrivate(TerritoryData vassal);
  protected abstract void removeVassal(TerritoryData vassalID);
  public boolean isCapital() {
    Optional<TerritoryData> capital = getOverlord();
    return capital
        .map(overlord -> Objects.equals(overlord.getCapital().getID(), getID()))
        .orElse(false);
  }
  public abstract TerritoryData getCapital();
  public int getChunkColorCode() {
    return cosmetics.getColor();
  }
  public String getChunkColorInHex() {
    return cosmetics.getColorAsHex();
  }
  public TextColor getChunkColor() {
    return TextColor.fromHexString(getChunkColorInHex());
  }
  public void setChunkColor(int color) {
    this.cosmetics = cosmetics.withColor(color);
    applyToAllOnlinePlayer(PrefixUtil::updatePrefix);
  }
  public boolean haveOverlord() {
    return getOverlord().isPresent();
  }
  public Map<String, Integer> getAvailableEnemyClaims() {
    if (availableClaims == null) availableClaims = new HashMap<>();
    return availableClaims;
  }
  public void addAvailableClaims(String territoryID, int amount) {
    getAvailableEnemyClaims().merge(territoryID, amount, Integer::sum);
  }
  public void consumeEnemyClaim(String territoryID) {
    getAvailableEnemyClaims().merge(territoryID, -1, Integer::sum);
    if (getAvailableEnemyClaims().get(territoryID) <= 0)
      getAvailableEnemyClaims().remove(territoryID);
  }
  public boolean claimChunk(Player player) {
    return claimChunk(player, player.getLocation().getChunk());
  }
  public boolean claimChunk(Player player, Chunk chunk) {
    return claimChunk(player, chunk, Constants.allowNonAdjacentChunksFor(this));
  }
  public boolean claimChunk(Player player, Chunk chunk, boolean ignoreAdjacent) {
    if (!canClaimChunkSync(player, chunk, ignoreAdjacent)) {
      return false;
    }
    abstractClaimChunk(player, chunk, ignoreAdjacent);
    ChunkCap chunkCap = getNewLevel().getStat(ChunkCap.class);
    FilledLang message;
    if (chunkCap.isUnlimited()) {
      message = Lang.CHUNK_CLAIMED_SUCCESS_UNLIMITED.get(getColoredName());
    } else {
      String currentAmountOfChunks = Integer.toString(getNumberOfClaimedChunk());
      String maxAmountOfChunks = Integer.toString(chunkCap.getMaxAmount());
      message =
          Lang.CHUNK_CLAIMED_SUCCESS_LIMITED.get(
              getColoredName(), currentAmountOfChunks, maxAmountOfChunks);
    }
    TanChatUtils.message(player, message);
    return true;
  }
  protected abstract void abstractClaimChunk(Player player, Chunk chunk, boolean ignoreAdjacent);
  public boolean canClaimChunkSync(Player player, Chunk chunk, boolean ignoreAdjacent) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    if (ClaimBlacklistStorage.cannotBeClaimed(chunk)) {
      TanChatUtils.message(player, Lang.CHUNK_IS_BLACKLISTED.get(player));
      return false;
    }
    if (!doesPlayerHavePermission(tanPlayer, RolePermission.CLAIM_CHUNK)) {
      TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(player));
      return false;
    }
    TerritoryStats territoryStats = getNewLevel();
    int nbOfClaimedChunks = getNumberOfClaimedChunk();
    if (!territoryStats.getStat(BiomeStat.class).canClaimBiome(chunk)) {
      TanChatUtils.message(player, Lang.CHUNK_BIOME_NOT_ALLOWED.get(player));
      return false;
    }
    if (!territoryStats.getStat(ChunkCap.class).canDoAction(nbOfClaimedChunks)) {
      TanChatUtils.message(player, Lang.MAX_CHUNK_LIMIT_REACHED.get(player));
      return false;
    }
    int cost = getClaimCost();
    if (getBalance() < cost) {
      TanChatUtils.message(
          player,
          Lang.TERRITORY_NOT_ENOUGH_MONEY.get(
              player, getColoredName(), Double.toString(cost - getBalance())));
      return false;
    }
    ClaimedChunk2 chunkData = NewClaimedChunkStorage.getInstance().get(chunk);
    if (!chunkData.canTerritoryClaim(player, this)) {
      return false;
    }
    if (ignoreAdjacent) {
      return true;
    }
    if (getNumberOfClaimedChunk() == 0) {
      if (ChunkUtil.isInBufferZone(chunkData, this)) {
        TanChatUtils.message(
            player,
            Lang.CHUNK_IN_BUFFER_ZONE.get(
                player, Integer.toString(Constants.territoryClaimBufferZone())));
        return false;
      }
      return true;
    }
    if (!NewClaimedChunkStorage.getInstance()
        .isOneAdjacentChunkClaimedBySameTerritoryAsync(chunk, getID())
        .join()) {
      TanChatUtils.message(player, Lang.CHUNK_NOT_ADJACENT.get(player));
      return false;
    }
    return true;
  }
  public int getClaimCost() {
    return getNewLevel().getStat(ChunkCost.class).getCost();
  }
  public synchronized void delete() {
    NewClaimedChunkStorage.getInstance()
        .unclaimAllChunksFromTerritory(this);
    applyToAllOnlinePlayer(Player::closeInventory);
    for (TerritoryData territory : getVassals()) {
      territory.removeOverlord();
    }
    for (Fort occupiedFort : getOccupiedForts()) {
      occupiedFort.liberate();
    }
    for (Fort ownedFort : getOwnedForts()) {
      FortStorage.getInstance().delete(ownedFort);
    }
    getRelations()
        .cleanAll(this);
    PlannedAttackStorage.getInstance().territoryDeleted(this);
  }
  public boolean canConquerChunk(ClaimedChunk2 chunk) {
    if (getAvailableEnemyClaims().containsKey(chunk.getOwnerID())) {
      consumeEnemyClaim(chunk.getOwnerID());
      return true;
    }
    return false;
  }
  /**
   * Adds a player donation to the territory asynchronously.
   *
   * <p>This method is now async to avoid blocking I/O calls. It checks the player's balance,
   * withdraws the donation amount, adds it to the territory balance, and records the transaction.</p>
   *
   * @param player The player making the donation
   * @param amount The amount to donate
   * @return CompletableFuture that completes when the donation is processed
   */
  public CompletableFuture<Void> addDonationAsync(Player player, double amount) {
    if (amount <= 0) {
      // Synchronous validation - fast operation
      return PlayerDataStorage.getInstance().get(player)
          .thenAccept(tanPlayer -> {
            TanChatUtils.message(
                player,
                Lang.PAY_MINIMUM_REQUIRED.get(tanPlayer.getLang()));
          });
    }

    // Load player data and check balance asynchronously
    return PlayerDataStorage.getInstance()
        .get(player)
        .thenCompose(tanPlayer -> {
          LangType langType = tanPlayer.getLang();

          // Check player's balance asynchronously
          return org.leralix.tan.service.AsyncEconomyService.getBalance(player)
              .thenCompose(balance -> {
                if (balance < amount) {
                  // Insufficient funds
                  TanChatUtils.message(
                      player,
                      Lang.PLAYER_NOT_ENOUGH_MONEY.get(langType));
                  return CompletableFuture.completedFuture(null);
                }

                // Sufficient funds - withdraw and add to territory balance
                return org.leralix.tan.service.AsyncEconomyService.withdraw(player, amount)
                    .thenRun(() -> {
                      addToBalance(amount);
                      TownsAndNations.getPlugin()
                          .getDatabaseHandler()
                          .addTransactionHistory(new PlayerDonationHistory(this, player, amount));
                      TanChatUtils.message(
                          player,
                          Lang.PLAYER_SEND_MONEY_SUCCESS.get(
                              langType, Double.toString(amount), getBaseColoredName()),
                          SoundEnum.MINOR_GOOD);
                    });
              });
        })
        .exceptionally(throwable -> {
          TownsAndNations.getPlugin()
              .getLogger()
              .warning("Failed to process donation from " + player.getName() + ": " + throwable.getMessage());
          TanChatUtils.message(
              player,
              Lang.SYNTAX_ERROR.get(player));
          return null;
        });
  }

  /**
   * Synchronous version of addDonation for backwards compatibility.
   *
   * <p><b>Deprecated:</b> Use {@link #addDonationAsync(Player, double)} instead to avoid blocking I/O.</p>
   *
   * @param player The player making the donation
   * @param amount The amount to donate
   * @deprecated Use addDonationAsync instead
   */
  @Deprecated
  public void addDonation(Player player, double amount) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    LangType langType = tanPlayer.getLang();
    double playerBalance = EconomyUtil.getBalance(player);
    if (playerBalance < amount) {
      TanChatUtils.message(player, Lang.PLAYER_NOT_ENOUGH_MONEY.get(langType));
      return;
    }
    if (amount <= 0) {
      TanChatUtils.message(player, Lang.PAY_MINIMUM_REQUIRED.get(langType));
      return;
    }
    EconomyUtil.removeFromBalance(player, amount);
    addToBalance(amount);
    TownsAndNations.getPlugin()
        .getDatabaseHandler()
        .addTransactionHistory(new PlayerDonationHistory(this, player, amount));
    TanChatUtils.message(
        player,
        Lang.PLAYER_SEND_MONEY_SUCCESS.get(langType, Double.toString(amount), getBaseColoredName()),
        SoundEnum.MINOR_GOOD);
  }
  public abstract void openMainMenu(Player player);
  public abstract boolean canHaveVassals();
  public abstract boolean canHaveOverlord();
  public abstract List<String> getVassalsID();
  public List<TerritoryData> getVassals() {
    List<TerritoryData> res = new ArrayList<>();
    for (String vassalID : getVassalsID()) {
      TerritoryData vassal = TerritoryUtil.getTerritory(vassalID);
      if (vassal != null) res.add(vassal);
    }
    return res;
  }
  public int getVassalCount() {
    return getVassalsID().size();
  }
  public boolean isVassal(TerritoryData territoryData) {
    return isVassal(territoryData.getID());
  }
  public abstract boolean isVassal(String territoryID);
  public abstract Collection<TerritoryData> getPotentialVassals();
  private List<String> getOverlordsProposals() {
    return diplomacy.getOverlordsProposals();
  }
  public void addVassalisationProposal(TerritoryData proposal) {
    this.diplomacy = diplomacy.withOverlordProposal(proposal.getID());
    broadcastMessageWithSound(
        Lang.REGION_DIPLOMATIC_INVITATION_RECEIVED_1.get(
            proposal.getBaseColoredName(), getBaseColoredName()),
        SoundEnum.MINOR_GOOD);
    EventManager.getInstance().callEvent(new TerritoryVassalProposalInternalEvent(proposal, this));
  }
  public void removeVassalisationProposal(TerritoryData proposal) {
    this.diplomacy = diplomacy.withoutOverlordProposal(proposal.getID());
  }
  public boolean containsVassalisationProposal(TerritoryData proposal) {
    return getOverlordsProposals().contains(proposal.getID());
  }
  public int getNumberOfVassalisationProposals() {
    return getOverlordsProposals().size();
  }
  public List<GuiItem> getAllSubjugationProposals(Player player, int page) {
    ArrayList<GuiItem> proposals = new ArrayList<>();
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    LangType langType = tanPlayer.getLang();
    for (String proposalID : getOverlordsProposals()) {
      TerritoryData proposalOverlord = TerritoryUtil.getTerritory(proposalID);
      if (proposalOverlord == null) continue;
      ItemStack territoryItem = proposalOverlord.getIconWithInformations(langType);
      HeadUtils.addLore(
          territoryItem,
          Lang.GUI_GENERIC_LEFT_CLICK_TO_ACCEPT.get(langType),
          Lang.RIGHT_CLICK_TO_REFUSE.get(langType));
      GuiItem acceptInvitation =
          ItemBuilder.from(territoryItem)
              .asGuiItem(
                  event -> {
                    event.setCancelled(true);
                    if (event.isLeftClick()) {
                      if (haveOverlord()) {
                        TanChatUtils.message(
                            player,
                            Lang.TOWN_ALREADY_HAVE_OVERLORD.get(langType),
                            SoundEnum.NOT_ALLOWED);
                        return;
                      }
                      setOverlord(proposalOverlord);
                      broadcastMessageWithSound(
                          Lang.ACCEPTED_VASSALISATION_PROPOSAL_ALL.get(
                              this.getBaseColoredName(), proposalOverlord.getName()),
                          SoundEnum.GOOD);
                    }
                    if (event.isRightClick()) {
                      getOverlordsProposals().remove(proposalID);
                    }
                  });
      proposals.add(acceptInvitation);
    }
    return proposals;
  }

  /**
   * Gets vassalization proposals asynchronously for GUI display.
   *
   * <p>This method loads player data asynchronously to avoid blocking the GUI rendering thread.
   * Use this version when building proposal GUIs in async contexts.</p>
   *
   * @param player The player viewing the proposals
   * @param page The page number (for pagination)
   * @return CompletableFuture containing the list of GUI items for each proposal
   */
  public CompletableFuture<List<GuiItem>> getAllSubjugationProposalsAsync(Player player, int page) {
    return PlayerDataStorage.getInstance()
        .get(player)
        .thenApply(tanPlayer -> {
          ArrayList<GuiItem> proposals = new ArrayList<>();
          LangType langType = tanPlayer.getLang();
          for (String proposalID : getOverlordsProposals()) {
            TerritoryData proposalOverlord = TerritoryUtil.getTerritory(proposalID);
            if (proposalOverlord == null) continue;
            ItemStack territoryItem = proposalOverlord.getIconWithInformations(langType);
            HeadUtils.addLore(
                territoryItem,
                Lang.GUI_GENERIC_LEFT_CLICK_TO_ACCEPT.get(langType),
                Lang.RIGHT_CLICK_TO_REFUSE.get(langType));
            GuiItem acceptInvitation =
                ItemBuilder.from(territoryItem)
                    .asGuiItem(
                        event -> {
                          event.setCancelled(true);
                          if (event.isLeftClick()) {
                            if (haveOverlord()) {
                              TanChatUtils.message(
                                  player,
                                  Lang.TOWN_ALREADY_HAVE_OVERLORD.get(langType),
                                  SoundEnum.NOT_ALLOWED);
                              return;
                            }
                            setOverlord(proposalOverlord);
                            broadcastMessageWithSound(
                                Lang.ACCEPTED_VASSALISATION_PROPOSAL_ALL.get(
                                    this.getBaseColoredName(), proposalOverlord.getName()),
                                SoundEnum.GOOD);
                          }
                          if (event.isRightClick()) {
                            getOverlordsProposals().remove(proposalID);
                          }
                        });
            proposals.add(acceptInvitation);
          }
          return proposals;
        });
  }
  protected Map<Integer, RankData> getRanks() {
    if (ranks == null) {
      ranks = new HashMap<>();
    }
    return ranks;
  }
  public Collection<RankData> getAllRanks() {
    return getRanks().values();
  }
  public Collection<RankData> getAllRanksSorted() {
    return getRanks().values().stream()
        .sorted(Comparator.comparingInt(p -> -p.getLevel()))
        .toList();
  }
  public RankData getRank(int rankID) {
    return getRanks().get(rankID);
  }
  public abstract RankData getRank(ITanPlayer tanPlayer);
  public RankData getRank(Player player) {
    return getRank(PlayerDataStorage.getInstance().getSync(player));
  }

  /**
   * Gets a player's rank in this territory asynchronously.
   *
   * <p>This method loads player data asynchronously to avoid blocking the calling thread.
   * Use this version for rank lookups in async contexts or on region threads.</p>
   *
   * @param player The player to query
   * @return CompletableFuture containing the player's rank, or null if player is not in territory
   */
  public CompletableFuture<RankData> getRankAsync(Player player) {
    return PlayerDataStorage.getInstance()
        .get(player)
        .thenApply(this::getRank);
  }
  public int getNumberOfRank() {
    return getRanks().size();
  }
  public boolean isRankNameUsed(String message) {
    for (RankData rank : getAllRanks()) {
      if (rank.getName().equals(message)) {
        return true;
      }
    }
    return false;
  }
  public RankData registerNewRank(String rankName) {
    int nextRankId = 0;
    for (RankData rank : getAllRanks()) {
      if (rank.getID() >= nextRankId) nextRankId = rank.getID() + 1;
    }
    RankData newRank = new RankData(nextRankId, rankName);
    getRanks().put(nextRankId, newRank);
    return newRank;
  }
  public void removeRank(int key) {
    getRanks().remove(key);
  }
  public int getDefaultRankID() {
    if (defaultRankID == null) {
      defaultRankID =
          getAllRanks()
              .iterator()
              .next()
              .getID();
    }
    return defaultRankID;
  }
  public void setDefaultRank(RankData rank) {
    setDefaultRank(rank.getID());
  }
  public void setDefaultRank(int rankID) {
    this.defaultRankID = rankID;
  }
  public abstract List<GuiItem> getOrderedMemberList(ITanPlayer tanPlayer);
  public boolean doesPlayerHavePermission(Player player, RolePermission townRolePermission) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    if (!this.isPlayerIn(tanPlayer)) {
      return false;
    }
    if (isLeader(tanPlayer)) return true;
    return getRank(tanPlayer).hasPermission(townRolePermission);
  }

  /**
   * Checks if a player has a specific permission asynchronously.
   *
   * <p>This method loads player data asynchronously to avoid blocking the calling thread.
   * Use this version for permission checks in async contexts or on region threads.</p>
   *
   * @param player The player to check
   * @param townRolePermission The permission to check for
   * @return CompletableFuture containing true if the player has the permission
   */
  public CompletableFuture<Boolean> doesPlayerHavePermissionAsync(Player player, RolePermission townRolePermission) {
    return PlayerDataStorage.getInstance()
        .get(player)
        .thenApply(tanPlayer -> {
          if (!this.isPlayerIn(tanPlayer)) {
            return false;
          }
          if (isLeader(tanPlayer)) return true;
          return getRank(tanPlayer).hasPermission(townRolePermission);
        });
  }
  public boolean doesPlayerHavePermission(ITanPlayer tanPlayer, RolePermission townRolePermission) {
    if (!this.isPlayerIn(tanPlayer)) {
      return false;
    }
    if (isLeader(tanPlayer)) return true;
    return getRank(tanPlayer).hasPermission(townRolePermission);
  }
  public void setPlayerRank(ITanPlayer playerStat, RankData rankData) {
    getRank(playerStat).removePlayer(playerStat);
    rankData.addPlayer(playerStat);
    specificSetPlayerRank(playerStat, rankData.getID());
  }
  protected abstract void specificSetPlayerRank(ITanPlayer playerStat, int rankID);
  public Budget getBudget() {
    Budget budget = new Budget();
    addCommonTaxes(budget);
    addSpecificTaxes(budget);
    return budget;
  }
  private void addCommonTaxes(Budget budget) {
    budget.addProfitLine(new SalaryPaymentLine(this));
    budget.addProfitLine(new ChunkUpkeepLine(this));
  }
  protected abstract void addSpecificTaxes(Budget budget);
  public int getNumberOfClaimedChunk() {
    return NewClaimedChunkStorage.getInstance().getAllChunkFrom(this).size();
  }
  public double getTax() {
    return taxes.getBaseTax();
  }
  public void setTax(double newTax) {
    taxes = taxes.setBaseTax(newTax);
  }
  public void addToTax(double i) {
    taxes = taxes.addToBaseTax(i);
  }
  public void executeTasks() {
    collectTaxes();
    paySalaries();
    payChunkUpkeep();
  }
  private void paySalaries() {
    // Use async version for non-blocking execution
    paySalariesAsync().exceptionally(throwable -> {
      TownsAndNations.getPlugin()
          .getLogger()
          .severe("Failed to pay salaries for territory '" + getName() + "': " + throwable.getMessage());
      return null;
    });
  }

  /**
   * Pays salaries to all players asynchronously.
   *
   * <p>This method loads player data in parallel and pays salaries using the non-blocking
   * AsyncEconomyService. This prevents blocking the territory thread during periodic salary payments.</p>
   *
   * @return CompletableFuture that completes when all salaries are paid
   */
  private CompletableFuture<Void> paySalariesAsync() {
    List<CompletableFuture<Void>> rankFutures = new ArrayList<>();

    for (RankData rank : getAllRanks()) {
      int rankSalary = rank.getSalary();
      List<String> playerIdList = rank.getPlayersID();
      double costOfSalary = (double) playerIdList.size() * rankSalary;

      if (rankSalary == 0 || costOfSalary > getBalance()) {
        continue;
      }

      // Withdraw total cost from territory balance (synchronous - fast operation)
      removeFromBalance(costOfSalary);

      // Create async payment tasks for all players in this rank
      for (String playerId : playerIdList) {
        CompletableFuture<Void> paymentFuture = PlayerDataStorage.getInstance()
            .get(playerId)
            .thenCompose(tanPlayer -> {
              // Use AsyncEconomyService for non-blocking balance update
              return org.leralix.tan.service.AsyncEconomyService.deposit(
                      tanPlayer.getOfflinePlayer(),
                      rankSalary)
                  .thenRun(() -> {
                    // Record transaction history
                    TownsAndNations.getPlugin()
                        .getDatabaseHandler()
                        .addTransactionHistory(
                            new SalaryPaymentHistory(this, String.valueOf(rank.getID()), costOfSalary));
                  });
            });
        rankFutures.add(paymentFuture);
      }
    }

    // Wait for all salary payments to complete
    return CompletableFuture.allOf(rankFutures.toArray(new CompletableFuture[0]));
  }
  private void payChunkUpkeep() {
    double upkeepCost = Constants.getUpkeepCost(this);
    int numberClaimedChunk = getNumberOfClaimedChunk();
    double totalUpkeep = numberClaimedChunk * upkeepCost;
    if (totalUpkeep > getBalance()) {
      deletePortionOfChunk();
      TownsAndNations.getPlugin()
          .getDatabaseHandler()
          .addTransactionHistory(new ChunkPaymentHistory(this, -1));
    } else {
      removeFromBalance(totalUpkeep);
      TownsAndNations.getPlugin()
          .getDatabaseHandler()
          .addTransactionHistory(new ChunkPaymentHistory(this, totalUpkeep));
    }
  }
  private void deletePortionOfChunk() {
    int minNbOfUnclaimedChunk = Constants.getMinimumNumberOfChunksUnclaimed();
    int nbOfUnclaimedChunk = 0;
    double percentageOfChunkToKeep = Constants.getPercentageOfChunksUnclaimed();
    List<ClaimedChunk2> borderChunks = ChunkUtil.getBorderChunks(this);
    for (ClaimedChunk2 claimedChunk2 : borderChunks) {
      if (RandomUtil.getRandom().nextDouble() < percentageOfChunkToKeep) {
        NewClaimedChunkStorage.getInstance().unclaimChunkAndUpdate(claimedChunk2);
        nbOfUnclaimedChunk++;
      }
    }
    if (nbOfUnclaimedChunk < minNbOfUnclaimedChunk) {
      for (ClaimedChunk2 claimedChunk2 : borderChunks) {
        NewClaimedChunkStorage.getInstance().unclaimChunkAndUpdate(claimedChunk2);
        nbOfUnclaimedChunk++;
        if (nbOfUnclaimedChunk >= minNbOfUnclaimedChunk) break;
      }
    }
  }
  protected abstract void collectTaxes();
  public double getTaxOnRentingProperty() {
    return taxes.getPropertyRentTax();
  }
  public void setTaxOnRentingProperty(double amount) {
    taxes = taxes.setTaxOnRentingProperty(amount);
  }
  public double getTaxOnBuyingProperty() {
    return taxes.getPropertyBuyTax();
  }
  public void setTaxOnBuyingProperty(double amount) {
    taxes = taxes.setTaxOnBuyingProperty(amount);
  }
  public double getTaxOnCreatingProperty() {
    return taxes.getPropertyCreateTax();
  }
  public void setTaxOnCreatingProperty(double amount) {
    taxes = taxes.setTaxOnCreatingProperty(amount);
  }
  public boolean isAtWar() {
    return !getCurrentAttacks().isEmpty();
  }
  public ChunkPermission getPermission(ChunkPermissionType type) {
    return getChunkSettings().getPermission(type);
  }
  public void nextPermission(ChunkPermissionType type) {
    getChunkSettings().nextPermission(type);
  }
  protected RankData getDefaultRank() {
    return getRank(getDefaultRankID());
  }
  protected void registerPlayer(ITanPlayer tanPlayer) {
    getDefaultRank().addPlayer(tanPlayer);
    tanPlayer.setRankID(this, getDefaultRankID());
  }
  protected void unregisterPlayer(ITanPlayer tanPlayer) {
    getRank(tanPlayer).removePlayer(tanPlayer);
    tanPlayer.setRankID(this, null);
  }
  public String getColoredName() {
    if (Constants.displayTerritoryColor()) {
      return LegacyComponentSerializer.legacySection().serialize(getCustomColoredName());
    } else {
      return getBaseColoredName();
    }
  }
  public CompletableFuture<String> getLeaderName() {
    if (this.haveNoLeader()) return CompletableFuture.completedFuture(Lang.NO_LEADER.getDefault());
    return CompletableFuture.supplyAsync(() -> getLeaderData().getNameStored());
  }
  public String getLeaderNameSync() {
    if (this.haveNoLeader()) return Lang.NO_LEADER.getDefault();
    return getLeaderData().getNameStored();
  }
  public void registerFort(Vector3D location) {
    Fort fort = FortStorage.getInstance().register(location, this);
    Vector2D flagPosition = fort.getPosition();
    flagPosition.getWorld().getChunkAt(flagPosition.getX(), flagPosition.getZ());
    addOwnedFort(fort);
  }
  public List<String> getOwnedFortIDs() {
    return war.getFortIds();
  }
  public List<String> getOccupiedFortIds() {
    return war.getOccupiedFortIds();
  }
  public void removeOccupiedFort(Fort fort) {
    removeOccupiedFortID(fort.getID());
  }
  public void removeOccupiedFortID(String fortID) {
    this.war = war.withoutOccupiedFort(fortID);
  }
  public void addOccupiedFort(Fort fort) {
    addOccupiedFortID(fort.getID());
  }
  public void addOccupiedFortID(String fortID) {
    this.war = war.withOccupiedFort(fortID);
  }
  public List<Fort> getOwnedForts() {
    return FortStorage.getInstance().getOwnedFort(this);
  }
  public List<Fort> getOccupiedForts() {
    return FortStorage.getInstance().getOccupiedFort(this);
  }
  public List<Fort> getAllControlledFort() {
    return FortStorage.getInstance().getAllControlledFort(this);
  }
  public void removeFort(String fortID) {
    this.war = war.withoutFort(fortID);
  }
  public Collection<Building> getBuildings() {
    List<Building> buildings = new ArrayList<>(getOwnedForts());
    if (this instanceof TownData townData) {
      buildings.addAll(townData.getProperties());
    }
    buildings.removeAll(Collections.singleton(null));
    return buildings;
  }
  public void addOwnedFort(Fort fortToCapture) {
    if (fortToCapture == null) {
      return;
    }
    this.war = war.withFort(fortToCapture.getID());
  }
  public void removeOwnedFort(Fort fortToCapture) {
    if (fortToCapture == null) {
      return;
    }
    this.war = war.withoutFort(fortToCapture.getID());
  }
  public void applyToAllOnlinePlayer(Consumer<Player> action) {
    for (Player player : getPlayers()) {
      action.accept(player);
    }
  }
  private List<Player> getPlayers() {
    List<Player> playerList = new ArrayList<>();
    for (String playerID : getPlayerIDList()) {
      Player player = Bukkit.getPlayer(UUID.fromString(playerID));
      if (player != null) {
        playerList.add(player);
      }
    }
    return playerList;
  }
  public boolean canAccessBufferZone(TerritoryChunk territoryChunk) {
    String ownerID = territoryChunk.getOwnerID();
    if (ownerID.equals(id)) {
      return true;
    }
    Optional<TerritoryData> optCapital = getOverlord();
    if (optCapital.isPresent()) {
      TerritoryData capital = optCapital.get();
      return ownerID.equals(capital.getID());
    }
    return false;
  }
  public TerritoryStats getNewLevel() {
    if (this.upgradesStatus == null) {
      if (this instanceof TownData) {
        this.upgradesStatus = new TerritoryStats(StatsType.TOWN);
      } else {
        this.upgradesStatus = new TerritoryStats(StatsType.REGION);
      }
    }
    return upgradesStatus;
  }

  /**
   * Alias for getNewLevel() - returns the upgrade status.
   * This method name is more semantically clear for upgrade-related operations.
   *
   * @return The territory stats/upgrades status
   */
  public TerritoryStats getUpgradesStatus() {
    return getNewLevel();
  }

  public void upgradeTown(Upgrade upgrade) {
    getNewLevel().levelUp(upgrade);
  }
}