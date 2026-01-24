package org.leralix.tan.dataclass;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.leralix.lib.position.Vector3D;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.property.PlayerOwned;
import org.leralix.tan.dataclass.property.TerritoryOwned;
import org.leralix.tan.dataclass.property.AbstractOwner;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.permissions.ChunkPermissionType;
import org.leralix.tan.lang.FilledLang;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.PermissionManager;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive test suite for PropertyData class.
 * Tests core property functionality including ownership, sales, rentals, and permissions.
 */
@ExtendWith(MockitoExtension.class)
class PropertyDataTest {

  private static final String TEST_PROPERTY_ID = "town123_prop456";
  private static final String TEST_TOWN_ID = "town123";
  private static final String TEST_PROPERTY_NAME = "Test Property";

  private PropertyData propertyData;
  private ITanPlayer mockOwner;
  private TownData mockTown;
  private Vector3D p1;
  private Vector3D p2;
  private PlayerDataStorage mockPlayerStorage;
  private TownDataStorage mockTownStorage;
  private MockedStatic<PlayerDataStorage> mockedPlayerStorage;
  private MockedStatic<TownDataStorage> mockedTownStorage;
  private MockedStatic<TownsAndNations> mockedPlugin;

  @BeforeEach
  void setUp() {
    mockOwner = mock(ITanPlayer.class);
    when(mockOwner.getID()).thenReturn(UUID.randomUUID().toString());
    when(mockOwner.getNameStored()).thenReturn("TestOwner");

    mockTown = mock(TownData.class);
    when(mockTown.getID()).thenReturn(TEST_TOWN_ID);
    when(mockTown.getName()).thenReturn("TestTown");
    when(mockTown.getTaxOnRentingProperty()).thenReturn(0.1);
    when(mockTown.getTaxOnBuyingProperty()).thenReturn(0.05);

    mockPlayerStorage = mock(PlayerDataStorage.class);
    mockTownStorage = mock(TownDataStorage.class);

    when(mockTownStorage.getSync(TEST_TOWN_ID)).thenReturn(mockTown);
    when(mockPlayerStorage.getSync(anyString())).thenReturn(mockOwner);

    mockedPlayerStorage = Mockito.mockStatic(PlayerDataStorage.class);
    mockedPlayerStorage.when(() -> PlayerDataStorage.getInstance()).thenReturn(mockPlayerStorage);

    mockedTownStorage = Mockito.mockStatic(TownDataStorage.class);
    mockedTownStorage.when(() -> TownDataStorage.getInstance()).thenReturn(mockTownStorage);

    mockedPlugin = Mockito.mockStatic(TownsAndNations.class);

    p1 = new Vector3D(100, 64, 100, "world");
    p2 = new Vector3D(110, 70, 110, "world");

    propertyData = new PropertyData(TEST_PROPERTY_ID, p1, p2, mockOwner);
  }

  @AfterEach
  void tearDown() {
    mockedPlayerStorage.close();
    mockedTownStorage.close();
    mockedPlugin.close();
  }

  @Test
  void testConstructor_PlayerOwner() {
    assertNotNull(propertyData, "PropertyData should be created with player owner");
    assertEquals(TEST_PROPERTY_ID, propertyData.getTotalID());
    assertEquals(p1, propertyData.getFirstCorner());
    assertEquals(p2, propertyData.getSecondCorner());
    assertEquals("Unnamed Zone", propertyData.getName());
    assertEquals("No description", propertyData.getDescription());
  }

  @Test
  void testConstructor_TerritoryOwner() {
    PropertyData territoryProperty = new PropertyData(TEST_PROPERTY_ID, p1, p2, mockTown);

    assertNotNull(territoryProperty, "PropertyData should be created with territory owner");
    assertTrue(territoryProperty.getOwner() instanceof TerritoryOwned,
        "Owner should be TerritoryOwned");
  }

  @Test
  void testGetNameAndSetName() {
    assertEquals("Unnamed Zone", propertyData.getName());

    propertyData.setName(TEST_PROPERTY_NAME);
    assertEquals(TEST_PROPERTY_NAME, propertyData.getName());
  }

  @Test
  void testGetDescriptionAndSetDescription() {
    assertEquals("No description", propertyData.getDescription());

    propertyData.setDescription("A nice property");
    assertEquals("A nice property", propertyData.getDescription());
  }

  @Test
  void testGetOwner_PlayerOwner() {
    org.leralix.tan.dataclass.property.AbstractOwner owner = propertyData.getOwner();
    assertTrue(owner instanceof PlayerOwned, "Owner should be PlayerOwned");
    assertEquals(mockOwner.getNameStored(), owner.getName());
  }

  @Test
  void testGetOwner_TerritoryOwner() {
    PropertyData territoryProperty = new PropertyData(TEST_PROPERTY_ID, p1, p2, mockTown);
    org.leralix.tan.dataclass.property.AbstractOwner owner = territoryProperty.getOwner();

    assertTrue(owner instanceof TerritoryOwned, "Owner should be TerritoryOwned");
  }

  @Test
  void testGetTown() {
    TerritoryData town = propertyData.getTown();
    assertNotNull(town, "Town should not be null");
    assertEquals(TEST_TOWN_ID, town.getID());
  }

  @Test
  void testGetPropertyID() {
    String propertyID = propertyData.getPropertyID();
    assertEquals("prop456", propertyID, "Property ID should extract correctly from total ID");
  }

  @Test
  void testIsForSale_Default() {
    assertFalse(propertyData.isForSale(), "New property should not be for sale by default");
  }

  @Test
  void testSwapIsForSale() {
    assertFalse(propertyData.isForSale());
    propertyData.swapIsForSale();
    assertTrue(propertyData.isForSale(), "Property should be for sale after swap");

    propertyData.swapIsForSale();
    assertFalse(propertyData.isForSale(), "Property should not be for sale after second swap");
  }

  @Test
  void testSwapIsForSale_DisablesRent() {
    propertyData.swapIsRent();
    assertTrue(propertyData.isForRent());

    propertyData.swapIsForSale();
    assertTrue(propertyData.isForSale());
    assertFalse(propertyData.isForRent(), "Setting for sale should disable for rent");
  }

  @Test
  void testIsForRent_Default() {
    assertFalse(propertyData.isForRent(), "New property should not be for rent by default");
  }

  @Test
  void testSwapIsRent() {
    assertFalse(propertyData.isForRent());
    propertyData.swapIsRent();
    assertTrue(propertyData.isForRent(), "Property should be for rent after swap");

    propertyData.swapIsRent();
    assertFalse(propertyData.isForRent(), "Property should not be for rent after second swap");
  }

  @Test
  void testSwapIsRent_DisablesSale() {
    propertyData.swapIsForSale();
    assertTrue(propertyData.isForSale());

    propertyData.swapIsRent();
    assertTrue(propertyData.isForRent());
    assertFalse(propertyData.isForSale(), "Setting for rent should disable for sale");
  }

  @Test
  void testSetSalePriceAndGetBaseSalePrice() {
    propertyData.setSalePrice(1000.0);
    assertEquals(1000.0, propertyData.getBaseSalePrice(), 0.001);
  }

  @Test
  void testGetSalePrice_WithTax() {
    propertyData.setSalePrice(1000.0);
    when(mockTown.getTaxOnBuyingProperty()).thenReturn(0.1);

    double salePrice = propertyData.getSalePrice();
    assertEquals(1100.0, salePrice, 0.001, "Sale price should include tax");
  }

  @Test
  void testGetSalePrice_NoTown() {
    PropertyData orphanProperty = new PropertyData("orphan_prop", p1, p2, mockOwner);
    orphanProperty.setSalePrice(1000.0);

    when(mockTownStorage.getSync("orphan")).thenReturn(null);

    assertEquals(1000.0, orphanProperty.getSalePrice(), 0.001,
        "Orphan property should return base price");
  }

  @Test
  void testSetRentPriceAndGetBaseRentPrice() {
    propertyData.setRentPrice(100.0);
    assertEquals(100.0, propertyData.getBaseRentPrice(), 0.001);
  }

  @Test
  void testGetRentPrice_WithTax() {
    propertyData.setRentPrice(100.0);
    when(mockTown.getTaxOnRentingProperty()).thenReturn(0.2);

    double rentPrice = propertyData.getRentPrice();
    assertEquals(120.0, rentPrice, 0.001, "Rent price should include tax");
  }

  @Test
  void testGetRentPrice_NoTown() {
    PropertyData orphanProperty = new PropertyData("orphan_prop", p1, p2, mockOwner);
    orphanProperty.setRentPrice(100.0);

    when(mockTownStorage.getSync("orphan")).thenReturn(null);

    assertEquals(100.0, orphanProperty.getRentPrice(), 0.001,
        "Orphan property should return base rent price");
  }

  @Test
  void testIsRented_Default() {
    assertFalse(propertyData.isRented(), "New property should not be rented by default");
    assertNull(propertyData.getRenterID());
  }

  @Test
  void testAllocateRenter() {
    Player mockPlayer = mock(Player.class);
    UUID renterUUID = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(renterUUID);

    propertyData.allocateRenter(mockPlayer);

    assertTrue(propertyData.isRented(), "Property should be rented after allocation");
    assertEquals(renterUUID.toString(), propertyData.getRenterID());
    assertFalse(propertyData.isForRent(), "Property should not be for rent after renter allocated");
  }

  @Test
  void testExpelRenter() {
    Player mockPlayer = mock(Player.class);
    UUID renterUUID = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(renterUUID);

    ITanPlayer mockRenter = mock(ITanPlayer.class);
    when(mockPlayerStorage.getSync(renterUUID.toString())).thenReturn(mockRenter);

    propertyData.allocateRenter(mockPlayer);
    assertTrue(propertyData.isRented());

    propertyData.expelRenter(false);

    assertFalse(propertyData.isRented(), "Property should not be rented after expulsion");
    assertNull(propertyData.getRenterID());
    assertFalse(propertyData.isForRent(), "Property should not be for rent when rentBack is false");
  }

  @Test
  void testExpelRenter_WithRentBack() {
    Player mockPlayer = mock(Player.class);
    UUID renterUUID = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(renterUUID);

    ITanPlayer mockRenter = mock(ITanPlayer.class);
    when(mockPlayerStorage.getSync(renterUUID.toString())).thenReturn(mockRenter);

    propertyData.allocateRenter(mockPlayer);
    propertyData.expelRenter(true);

    assertTrue(propertyData.isForRent(), "Property should be for rent when rentBack is true");
  }

  @Test
  void testContainsLocation_Inside() {
    Location insideLocation = new Location(mock(World.class), 105, 66, 105);

    boolean result = propertyData.containsLocation(insideLocation);

    assertTrue(result, "Location inside property bounds should return true");
  }

  @Test
  void testContainsLocation_Outside() {
    Location outsideLocation = new Location(mock(World.class), 200, 66, 200);

    boolean result = propertyData.containsLocation(outsideLocation);

    assertFalse(result, "Location outside property bounds should return false");
  }

  @Test
  void testContainsLocation_OnBoundary() {
    Location boundaryLocation = new Location(mock(World.class), 100, 64, 100);

    boolean result = propertyData.containsLocation(boundaryLocation);

    assertTrue(result, "Location on boundary should return true");
  }

  @Test
  void testContainsLocation_ReversedCorners() {
    Vector3D reversedP1 = new Vector3D(110, 70, 110, "world");
    Vector3D reversedP2 = new Vector3D(100, 64, 100, "world");
    PropertyData reversedProperty = new PropertyData(TEST_PROPERTY_ID, reversedP1, reversedP2, mockOwner);

    Location insideLocation = new Location(mock(World.class), 105, 66, 105);

    assertTrue(reversedProperty.containsLocation(insideLocation),
        "Should handle reversed corner coordinates");
  }

  @Test
  void testGetIcon_Default() {
    ItemStack icon = propertyData.getIcon();

    assertNotNull(icon, "Icon should not be null");
    assertEquals(Material.OAK_SIGN, icon.getType(), "Default icon should be OAK_SIGN");
  }

  @Test
  void testSetIcon() {
    ItemStack customIcon = new ItemStack(Material.DIAMOND);
    org.leralix.tan.dataclass.territory.cosmetic.CustomIcon newIcon =
        new org.leralix.tan.dataclass.territory.cosmetic.CustomIcon(customIcon);

    propertyData.setIcon(newIcon);

    assertEquals(Material.DIAMOND, propertyData.getIcon().getType());
  }

  @Test
  void testGetPermissionManager() {
    PermissionManager pm = propertyData.getPermissionManager();

    assertNotNull(pm, "PermissionManager should not be null");
  }

  @Test
  void testIsInChunk_SingleChunk() {
    boolean result = propertyData.isInChunk(mock(org.leralix.tan.dataclass.chunk.ClaimedChunk2.class));

    assertTrue(result, "Property spanning single chunk should return true");
  }

  @Test
  void testIsInChunk_MultipleChunks() {
    Vector3D largeP1 = new Vector3D(0, 64, 0, "world");
    Vector3D largeP2 = new Vector3D(50, 70, 50, "world");
    PropertyData largeProperty = new PropertyData(TEST_PROPERTY_ID, largeP1, largeP2, mockOwner);

    var mockChunk = mock(org.leralix.tan.dataclass.chunk.ClaimedChunk2.class);
    when(mockChunk.getX()).thenReturn(1);
    when(mockChunk.getZ()).thenReturn(1);

    boolean result = largeProperty.isInChunk(mockChunk);

    assertTrue(result, "Property in chunk (1,1) should return true");
  }

  @Test
  void testIsInChunk_OutsideChunk() {
    var mockChunk = mock(org.leralix.tan.dataclass.chunk.ClaimedChunk2.class);
    when(mockChunk.getX()).thenReturn(20);
    when(mockChunk.getZ()).thenReturn(20);

    boolean result = propertyData.isInChunk(mockChunk);

    assertFalse(result, "Property should not be in outside chunk");
  }

  @Test
  void testGetBasicDescription_NotForSale() {
    List<FilledLang> description = propertyData.getBasicDescription();

    assertNotNull(description);
    assertFalse(description.isEmpty(), "Description should not be empty");
  }

  @Test
  void testGetBasicDescription_ForSale() {
    propertyData.swapIsForSale();
    propertyData.setSalePrice(1500.0);

    List<FilledLang> description = propertyData.getBasicDescription();

    assertNotNull(description);
    assertTrue(description.size() >= 3, "For sale description should include sale price info");
  }

  @Test
  void testGetBasicDescription_Rented() {
    Player mockPlayer = mock(Player.class);
    UUID renterUUID = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(renterUUID);

    ITanPlayer mockRenter = mock(ITanPlayer.class);
    when(mockRenter.getNameStored()).thenReturn("RenterName");
    when(mockPlayerStorage.getSync(renterUUID.toString())).thenReturn(mockRenter);

    propertyData.allocateRenter(mockPlayer);

    List<FilledLang> description = propertyData.getBasicDescription();

    assertNotNull(description);
    assertTrue(description.size() >= 3, "Rented description should include renter info");
  }

  @Test
  void testIsPlayerAllowed_OwnerCanAccess() {
    when(mockOwner.getID()).thenReturn("owner123");

    assertTrue(propertyData.isPlayerAllowed(ChunkPermissionType.PLACE_BLOCK, mockOwner),
        "Owner should be allowed to build");
  }

  @Test
  void testIsPlayerAllowed_RenterCanAccess() {
    Player mockPlayer = mock(Player.class);
    UUID renterUUID = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(renterUUID);

    ITanPlayer renter = mock(ITanPlayer.class);
    when(renter.getID()).thenReturn(renterUUID.toString());

    propertyData.allocateRenter(mockPlayer);
    when(mockPlayerStorage.getSync(renterUUID.toString())).thenReturn(renter);

    assertTrue(propertyData.isPlayerAllowed(ChunkPermissionType.PLACE_BLOCK, renter),
        "Renter should be allowed to access");
  }

  @Test
  void testGetDenyMessage_Rented() {
    Player mockPlayer = mock(Player.class);
    UUID renterUUID = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(renterUUID);

    ITanPlayer mockRenter = mock(ITanPlayer.class);
    when(mockRenter.getNameStored()).thenReturn("RenterName");
    when(mockPlayerStorage.getSync(renterUUID.toString())).thenReturn(mockRenter);

    propertyData.allocateRenter(mockPlayer);

    String denyMessage = propertyData.getDenyMessage(LangType.ENGLISH);
    assertNotNull(denyMessage);
    assertFalse(denyMessage.isEmpty(), "Deny message for rented property should not be empty");
  }

  @Test
  void testGetDenyMessage_Owner() {
    String denyMessage = propertyData.getDenyMessage(LangType.ENGLISH);

    assertNotNull(denyMessage);
    assertFalse(denyMessage.isEmpty(), "Deny message should not be empty");
  }

  @Test
  void testGetPosition_InitiallyNull() {
    Vector3D position = propertyData.getPosition();
    assertNull(position, "Position should be null before sign is set");
  }

  @Test
  void testMultiplePropertyCreation() {
    PropertyData prop1 = new PropertyData("town1_prop1", p1, p2, mockOwner);
    PropertyData prop2 = new PropertyData("town2_prop2", p1, p2, mockTown);

    assertEquals("town1_prop1", prop1.getTotalID());
    assertEquals("prop1", prop1.getPropertyID());
    // getOwningStructureID() is private - tested indirectly through getTotalID()

    assertEquals("town2_prop2", prop2.getTotalID());
    assertEquals("prop2", prop2.getPropertyID());
    // getOwningStructureID() is private - tested indirectly through getTotalID()
  }

  @Test
  void testPriceBoundaryConditions() {
    propertyData.setSalePrice(0.0);
    assertEquals(0.0, propertyData.getBaseSalePrice(), 0.001);

    propertyData.setSalePrice(Double.MAX_VALUE);
    assertEquals(Double.MAX_VALUE, propertyData.getBaseSalePrice(), 0.001);

    propertyData.setRentPrice(0.0);
    assertEquals(0.0, propertyData.getBaseRentPrice(), 0.001);
  }

  @Test
  void testEmptyNameAndDescription() {
    PropertyData newProperty = new PropertyData(TEST_PROPERTY_ID, p1, p2, mockOwner);

    assertNotNull(newProperty.getName());
    assertNotNull(newProperty.getDescription());
    assertFalse(newProperty.getName().isEmpty());
    assertFalse(newProperty.getDescription().isEmpty());
  }
}
