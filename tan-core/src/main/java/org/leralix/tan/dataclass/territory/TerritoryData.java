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
import org.leralix.tan.utils.deprecated.HeadUtils;
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
  public String getID() {
    return id;
  }
  public String getName() {
    return name;
  }
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
  public void rename(String newName) {
    this.name = newName;
  }
  public abstract int getHierarchyRank();
  public abstract String getBaseColoredName();
  public Component getCustomColoredName() {
    Component coloredName = Component.text(getName());
    coloredName = coloredName.color(getChunkColor());
    return coloredName;
  }
  public abstract String getLeaderID();
  public abstract ITanPlayer getLeaderData();
  public abstract void setLeaderID(String leaderID);
  public boolean isLeader(ITanPlayer tanPlayer) {
    return isLeader(tanPlayer.getID());
  }
  public abstract boolean isLeader(String playerID);
  public boolean isLeader(Player player) {
    return isLeader(player.getUniqueId().toString());
  }
  public String getDescription() {
    return cosmetics.getDescription();
  }
  public void setDescription(String newDescription) {
    this.cosmetics = cosmetics.withDescription(newDescription);
  }
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
    for (RankData rank : getAllRanks()) {
      int rankSalary = rank.getSalary();
      List<String> playerIdList = rank.getPlayersID();
      double costOfSalary = (double) playerIdList.size() * rankSalary;
      if (rankSalary == 0 || costOfSalary > getBalance()) {
        continue;
      }
      removeFromBalance(costOfSalary);
      for (String playerId : playerIdList) {
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(playerId);
        EconomyUtil.addFromBalance(tanPlayer, rankSalary);
        TownsAndNations.getPlugin()
            .getDatabaseHandler()
            .addTransactionHistory(
                new SalaryPaymentHistory(this, String.valueOf(rank.getID()), costOfSalary));
      }
    }
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
  public void upgradeTown(Upgrade upgrade) {
    getNewLevel().levelUp(upgrade);
  }
}