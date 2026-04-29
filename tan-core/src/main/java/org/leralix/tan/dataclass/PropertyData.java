package org.leralix.tan.dataclass;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.leralix.lib.data.SoundEnum;
import org.leralix.lib.position.Vector3D;
import org.leralix.lib.utils.SoundUtil;
import org.leralix.lib.utils.particles.ParticleUtils;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.building.Building;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.dataclass.property.AbstractOwner;
import org.leralix.tan.dataclass.property.PlayerOwned;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.cosmetic.CustomIcon;
import org.leralix.tan.dataclass.territory.cosmetic.ICustomIcon;
import org.leralix.tan.dataclass.territory.permission.RelationPermission;
import org.leralix.tan.domain.property.PropertyRentalService;
import org.leralix.tan.domain.property.PropertySalesService;
import org.leralix.tan.domain.property.PropertySignService;
import org.leralix.tan.enums.permissions.ChunkPermissionType;
import org.leralix.tan.gui.BasicGui;
import org.leralix.tan.gui.cosmetic.IconManager;
import org.leralix.tan.gui.user.property.PlayerPropertyManager;
import org.leralix.tan.lang.FilledLang;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.PermissionManager;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.utils.text.TanChatUtils;
public class PropertyData extends Building {
  private final String ID;
  private AbstractOwner owner;
  private String rentingPlayerID;
  private PermissionManager permissionManager;
  private ICustomIcon icon;
  private String name;
  private String description;
  private boolean isForSale;
  private double salePrice;
  private boolean isForRent;
  private double rentPrice;
  private final Vector3D p1;
  private final Vector3D p2;
  private Vector3D signLocation;
  private Vector3D supportLocation;

  // Services are logic-only, state stays on PropertyData for Gson compatibility
  private transient PropertyRentalService rentalService;
  private transient PropertySalesService salesService;
  private transient PropertySignService signService;

  public PropertyData(String id, Vector3D p1, Vector3D p2, TerritoryData owner) {
    this(id, p1, p2, new org.leralix.tan.dataclass.property.TerritoryOwned(owner));
  }
  public PropertyData(String id, Vector3D p1, Vector3D p2, ITanPlayer owner) {
    this(id, p1, p2, new PlayerOwned(owner));
  }
  public PropertyData(String id, Vector3D p1, Vector3D p2, AbstractOwner owner) {
    this.ID = id;
    this.owner = owner;
    this.p1 = p1;
    this.p2 = p2;
    initServices();
    ItemStack itemStack = new ItemStack(Material.OAK_SIGN);
    this.icon = new CustomIcon(itemStack);
    this.name = "Unnamed Zone";
    this.description = "No description";
    this.isForSale = false;
    this.salePrice = 0;
    this.isForRent = false;
    this.rentingPlayerID = null;
    this.rentPrice = 0;
    this.permissionManager = new PermissionManager(RelationPermission.SELECTED_ONLY);
  }

  /** Lazily initializes services after Gson deserialization (Unsafe.allocateInstance bypasses constructors). */
  private void initServices() {
    if (rentalService == null) rentalService = new PropertyRentalService(this);
    if (salesService == null) salesService = new PropertySalesService(this);
    if (signService == null) signService = new PropertySignService(this);
  }

  // ---- Identity / Geometry ----

  public Vector3D getFirstCorner() { return this.p1; }
  public Vector3D getSecondCorner() { return this.p2; }
  public String getTotalID() { return ID; }
  private String getOwningStructureID() { return ID.split("_")[0]; }
  public String getPropertyID() { return ID.split("_")[1]; }
  public TownData getTown() {
    return TownDataStorage.getInstance().getSync(getOwningStructureID());
  }
  public boolean containsLocation(Location location) {
    return Math.max(p1.getX(), p2.getX()) >= location.getX()
        && Math.min(p1.getX(), p2.getX()) <= location.getX()
        && Math.max(p1.getY(), p2.getY()) >= location.getY()
        && Math.min(p1.getY(), p2.getY()) <= location.getY()
        && Math.max(p1.getZ(), p2.getZ()) >= location.getZ()
        && Math.min(p1.getZ(), p2.getZ()) <= location.getZ();
  }
  public boolean isInChunk(ClaimedChunk2 chunk) {
    int minX = Math.min(p1.getX() >> 4, p2.getX() >> 4);
    int maxX = Math.max(p1.getX() >> 4, p2.getX() >> 4);
    int minZ = Math.min(p1.getZ() >> 4, p2.getZ() >> 4);
    int maxZ = Math.max(p1.getZ() >> 4, p2.getZ() >> 4);
    int chunkX = chunk.getX();
    int chunkZ = chunk.getZ();
    return (chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ);
  }

  // ---- Ownership ----

  public AbstractOwner getOwner() { return owner; }
  /** Public so PropertySalesService (in domain.property package) can set new owner after purchase. */
  public void setOwner(AbstractOwner owner) { this.owner = owner; }
  public void delete() {
    TownData town = getTown();
    rentalService.expelRenter(false);
    signService.removeSign();
    town.removeProperty(this);
    if (owner instanceof PlayerOwned playerOwnedClass) {
      ITanPlayer playerOwner =
          PlayerDataStorage.getInstance().getSync(playerOwnedClass.getPlayerID());
      playerOwner.removeProperty(this);
      Player player = Bukkit.getPlayer(UUID.fromString(playerOwnedClass.getPlayerID()));
      if (player != null) {
        TanChatUtils.message(
            player, Lang.PROPERTY_DELETED.get(playerOwner.getLang()), SoundEnum.MINOR_GOOD);
      }
    }
  }

  // ---- Display ----

  public void setIcon(CustomIcon icon) { this.icon = icon; }
  public ItemStack getIcon() {
    if (icon == null) {
      icon = new CustomIcon(new ItemStack(Material.OAK_SIGN));
    }
    return icon.getIcon();
  }
  public String getName() { return name; }
  public void setName(String name) {
    this.name = name;
    org.leralix.tan.utils.FoliaScheduler.runTask(TownsAndNations.getPlugin(), this::updateSign);
  }
  public String getDescription() { return description; }
  public void setDescription(String description) {
    this.description = description;
    org.leralix.tan.utils.FoliaScheduler.runTask(TownsAndNations.getPlugin(), this::updateSign);
  }
  public List<FilledLang> getBasicDescription() {
    List<FilledLang> lore = new ArrayList<>();
    lore.add(Lang.GUI_PROPERTY_DESCRIPTION.get(getDescription()));
    TownData town = getTown();
    if (town != null) {
      lore.add(Lang.GUI_PROPERTY_STRUCTURE_OWNER.get(town.getName()));
    }
    lore.add(Lang.GUI_PROPERTY_OWNER.get(getOwner().getName()));
    if (isForSale()) lore.add(Lang.GUI_PROPERTY_FOR_SALE.get(String.valueOf(salePrice)));
    else if (isRented()) {
      ITanPlayer renter = getRenter();
      if (renter != null) {
        lore.add(Lang.GUI_PROPERTY_RENTED_BY.get(renter.getNameStored(), String.valueOf(rentPrice)));
      }
    } else if (isForRent()) lore.add(Lang.GUI_PROPERTY_FOR_RENT.get(String.valueOf(rentPrice)));
    else {
      lore.add(Lang.GUI_PROPERTY_NOT_FOR_SALE.get());
    }
    return lore;
  }
  public void showBox(Player player) {
    ParticleUtils.drawBox(
        TownsAndNations.getPlugin(), player, this.getFirstCorner(), this.getSecondCorner(),
        10, Constants.getPropertyBoundaryParticles());
  }

  // ---- Permissions ----

  public PermissionManager getPermissionManager() {
    if (permissionManager == null) {
      permissionManager = new PermissionManager(RelationPermission.SELECTED_ONLY);
    }
    return permissionManager;
  }
  public boolean isPlayerAllowed(ChunkPermissionType action, ITanPlayer tanPlayer) {
    if (getPermissionManager().canPlayerDo(getTown(), action, tanPlayer)) {
      return true;
    }
    if (isRented()) return tanPlayer.getID().equals(rentingPlayerID);
    return getOwner().canAccess(tanPlayer);
  }
  public String getDenyMessage(LangType langType) {
    if (isRented()) return Lang.PROPERTY_RENTED_BY.get(langType, getRenter().getNameStored());
    else return Lang.PROPERTY_BELONGS_TO.get(langType, getOwner().getName());
  }

  // ---- Rental delegation ----

  public void allocateRenter(Player renter) { initServices(); rentalService.allocateRenter(renter); }
  public boolean isRented() { return rentingPlayerID != null; }
  public boolean isForRent() { return isForRent; }
  public ITanPlayer getRenter() {
    return PlayerDataStorage.getInstance().getSync(rentingPlayerID);
  }
  public String getRenterID() { return rentingPlayerID; }
  public Player getRenterPlayer() { initServices(); return rentalService.getRenterPlayer(); }
  public OfflinePlayer getOfflineRenter() { initServices(); return rentalService.getOfflineRenter(); }
  public CompletableFuture<Void> payRent() { initServices(); return rentalService.payRent(); }
  public CompletableFuture<Void> expelRenterAsync(boolean rentBack) { initServices(); return rentalService.expelRenterAsync(rentBack); }
  @Deprecated
  public void expelRenter(boolean rentBack) { initServices(); rentalService.expelRenter(rentBack); }
  public double getBaseRentPrice() { return this.rentPrice; }
  public double getRentPrice() { initServices(); return rentalService.getRentPrice(); }
  public void setRentPrice(double i) {
    this.rentPrice = i;
    org.leralix.tan.utils.FoliaScheduler.runTask(TownsAndNations.getPlugin(), this::updateSign);
  }
  public void swapIsRent() {
    this.isForRent = !this.isForRent;
    if (this.isForRent) this.isForSale = false;
    org.leralix.tan.utils.FoliaScheduler.runTask(TownsAndNations.getPlugin(), this::updateSign);
  }

  // ---- Sales delegation ----

  public boolean isForSale() { return this.isForSale; }
  public double getBaseSalePrice() { return this.salePrice; }
  public double getSalePrice() { initServices(); return salesService.getSalePrice(); }
  public void setSalePrice(double i) {
    this.salePrice = i;
    org.leralix.tan.utils.FoliaScheduler.runTask(TownsAndNations.getPlugin(), this::updateSign);
  }
  public void swapIsForSale() {
    this.isForSale = !this.isForSale;
    if (this.isForSale) this.isForRent = false;
    org.leralix.tan.utils.FoliaScheduler.runTask(TownsAndNations.getPlugin(), this::updateSign);
  }
  public void buyProperty(Player buyer) { initServices(); salesService.buyProperty(buyer); }

  // ---- Sign delegation ----

  public void updateSign() { initServices(); signService.updateSign(); }
  public Optional<Block> getSign() { initServices(); return signService.getSign(); }
  public void setSignData() { initServices(); signService.setSignData(); }
  public void createPropertySign(Player player, Block block, BlockFace blockFace) {
    initServices();
    signService.createPropertySign(player, block, blockFace);
  }

  // ---- Field accessors for services (public for cross-package access from domain.property) ----

  public void setRentingPlayerID(String id) { this.rentingPlayerID = id; }
  public void setIsForRent(boolean val) { this.isForRent = val; }
  public void setIsForSale(boolean val) { this.isForSale = val; }
  public Vector3D getSignLocation() { return signLocation; }
  public void setSignLocation(Vector3D loc) { this.signLocation = loc; }
  public Vector3D getSupportLocation() { return supportLocation; }
  public void setSupportLocation(Vector3D loc) { this.supportLocation = loc; }

  // ---- GUI / Building ----

  @Override
  public GuiItem getGuiItem(
      IconManager iconManager, Player player, BasicGui basicGui, LangType langType) {
    ITanPlayer tanPlayer = PlayerDataStorage.getInstance().getSync(player);
    boolean canInteract = getOwner().canAccess(tanPlayer);
    return iconManager
        .get(getIcon())
        .setName(getName())
        .setDescription(getBasicDescription())
        .setAction(
            event -> {
              if (!canInteract) {
                SoundUtil.playSound(player, SoundEnum.NOT_ALLOWED);
                return;
              }
              PlayerPropertyManager.open(player, this, p -> basicGui.open());
            })
        .asGuiItem(player, langType);
  }
  @Override
  public Vector3D getPosition() { return signLocation; }
}
