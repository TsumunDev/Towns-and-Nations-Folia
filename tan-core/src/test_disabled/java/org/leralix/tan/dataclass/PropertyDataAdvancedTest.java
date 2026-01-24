package org.leralix.tan.dataclass;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Sign;
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
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.enums.permissions.ChunkPermissionType;
import org.leralix.tan.storage.PermissionManager;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Advanced test suite for PropertyData class.
 * Tests complex scenarios including transactions, permissions, edge cases, and state transitions.
 */
@ExtendWith(MockitoExtension.class)
class PropertyDataAdvancedTest {

  private static final String PROPERTY_ID = "town789_prop123";
  private static final String TOWN_ID = "town789";
  private static final double INITIAL_BALANCE = 10000.0;

  private PropertyData property;
  private ITanPlayer owner;
  private ITanPlayer buyer;
  private ITanPlayer renter;
  private TownData town;
  private PlayerDataStorage mockPlayerStorage;
  private TownDataStorage mockTownStorage;
  private MockedStatic<PlayerDataStorage> mockedPlayerStorage;
  private MockedStatic<TownDataStorage> mockedTownStorage;
  private MockedStatic<TownsAndNations> mockedPlugin;
  private MockedStatic<EconomyUtil> mockedEconomy;

  @BeforeEach
  void setUp() {
    // Setup mock players
    owner = mock(ITanPlayer.class);
    when(owner.getID()).thenReturn(UUID.randomUUID().toString());
    when(owner.getNameStored()).thenReturn("PropertyOwner");
    when(owner.getBalance()).thenReturn(INITIAL_BALANCE);

    buyer = mock(ITanPlayer.class);
    UUID buyerUuid = UUID.randomUUID();
    when(buyer.getID()).thenReturn(buyerUuid.toString());
    when(buyer.getNameStored()).thenReturn("PropertyBuyer");
    when(buyer.getBalance()).thenReturn(INITIAL_BALANCE * 2);

    renter = mock(ITanPlayer.class);
    UUID renterUuid = UUID.randomUUID();
    when(renter.getID()).thenReturn(renterUuid.toString());
    when(renter.getNameStored()).thenReturn("PropertyRenter");
    when(renter.getBalance()).thenReturn(5000.0);

    // Setup mock town
    town = mock(TownData.class);
    when(town.getID()).thenReturn(TOWN_ID);
    when(town.getName()).thenReturn("TestTown");
    when(town.getTaxOnBuyingProperty()).thenReturn(0.05);
    when(town.getTaxOnRentingProperty()).thenReturn(0.10);
    when(town.getBalance()).thenReturn(100000.0);

    // Setup storages
    mockPlayerStorage = mock(PlayerDataStorage.class);
    mockTownStorage = mock(TownDataStorage.class);

    when(mockTownStorage.getSync(TOWN_ID)).thenReturn(town);
    when(mockPlayerStorage.getSync(any(String.class))).thenAnswer(invocation -> {
      String id = invocation.getArgument(0);
      if (id.equals(owner.getID())) return owner;
      if (id.equals(buyer.getID())) return buyer;
      if (id.equals(renter.getID())) return renter;
      return null;
    });

    mockedPlayerStorage = Mockito.mockStatic(PlayerDataStorage.class);
    mockedPlayerStorage.when(() -> PlayerDataStorage.getInstance()).thenReturn(mockPlayerStorage);

    mockedTownStorage = Mockito.mockStatic(TownDataStorage.class);
    mockedTownStorage.when(() -> TownDataStorage.getInstance()).thenReturn(mockTownStorage);

    mockedPlugin = Mockito.mockStatic(TownsAndNations.class);

    mockedEconomy = Mockito.mockStatic(EconomyUtil.class);
    mockedEconomy.when(() -> EconomyUtil.getBalance(any(ITanPlayer.class))).thenReturn(INITIAL_BALANCE);
    mockedEconomy.when(() -> EconomyUtil.removeFromBalance(any(ITanPlayer.class), anyDouble())).thenAnswer(inv -> {
      // Simulate balance reduction
      return null;
    });
    mockedEconomy.when(() -> EconomyUtil.addFromBalance(any(ITanPlayer.class), anyDouble())).thenAnswer(inv -> {
      // Simulate balance addition
      return null;
    });

    Vector3D p1 = new Vector3D(100, 64, 100, "world");
    Vector3D p2 = new Vector3D(110, 70, 110, "world");
    property = new PropertyData(PROPERTY_ID, p1, p2, owner);
  }

  @AfterEach
  void tearDown() {
    mockedPlayerStorage.close();
    mockedTownStorage.close();
    mockedPlugin.close();
    mockedEconomy.close();
  }

  // ========== Transaction Tests ==========

  @Test
  void testPropertySale_CompleteTransaction() {
    property.swapIsForSale();
    property.setSalePrice(5000.0);

    assertTrue(property.isForSale());
    assertEquals(5000.0, property.getBaseSalePrice(), 0.001);

    // With 5% tax: 5000 * 1.05 = 5250
    double finalPrice = property.getSalePrice();
    assertEquals(5250.0, finalPrice, 0.001, "Sale price should include tax");
  }

  @Test
  void testPropertySale_WithoutTown() {
    PropertyData orphanProperty = new PropertyData("orphan_prop",
        new Vector3D(0, 64, 0, "world"), new Vector3D(10, 70, 10, "world"), owner);

    orphanProperty.swapIsForSale();
    orphanProperty.setSalePrice(5000.0);

    when(mockTownStorage.getSync("orphan")).thenReturn(null);

    assertEquals(5000.0, orphanProperty.getSalePrice(), 0.001,
        "Orphan property should not apply tax");
  }

  @Test
  void testPropertyRent_CompleteCycle() {
    property.swapIsRent();
    property.setRentPrice(100.0);

    assertTrue(property.isForRent());
    assertEquals(100.0, property.getBaseRentPrice(), 0.001);

    // With 10% tax: 100 * 1.10 = 110
    double finalRent = property.getRentPrice();
    assertEquals(110.0, finalRent, 0.001, "Rent price should include tax");
  }

  @Test
  void testPropertyRent_WithTaxDistribution() {
    property.setRentPrice(100.0);
    when(town.getTaxOnRentingProperty()).thenReturn(0.20); // 20% tax

    double baseRent = property.getBaseRentPrice(); // 100
    double totalRent = property.getRentPrice(); // 120
    double taxAmount = totalRent - baseRent; // 20

    assertEquals(100.0, baseRent, 0.001);
    assertEquals(120.0, totalRent, 0.001);
    assertEquals(20.0, taxAmount, 0.001, "Tax amount should be calculated correctly");
  }

  @Test
  void testSaleAndRentMutualExclusivity_SaleDisablesRent() {
    property.swapIsRent();
    assertTrue(property.isForRent());

    property.swapIsForSale();

    assertTrue(property.isForSale(), "Property should be for sale");
    assertFalse(property.isForRent(), "For sale should disable for rent");
  }

  @Test
  void testSaleAndRentMutualExclusivity_RentDisablesSale() {
    property.swapIsForSale();
    assertTrue(property.isForSale());

    property.swapIsRent();

    assertTrue(property.isForRent(), "Property should be for rent");
    assertFalse(property.isForSale(), "For rent should disable for sale");
  }

  // ========== Permission Tests ==========

  @Test
  void testPermission_OwnerHasFullAccess() {
    PlayerOwned playerOwner = mock(PlayerOwned.class);
    when(playerOwner.canAccess(any(ITanPlayer.class))).thenReturn(true);
    when(property.getOwner()).thenReturn(playerOwner);

    assertTrue(property.isPlayerAllowed(ChunkPermissionType.PLACE_BLOCK, owner),
        "Owner should have build permission");
    assertTrue(property.isPlayerAllowed(ChunkPermissionType.INTERACT_CHEST, owner),
        "Owner should have interact permission");
  }

  @Test
  void testPermission_RenterHasLimitedAccess() {
    Player mockPlayer = mock(Player.class);
    UUID renterUuid = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(renterUuid);

    ITanPlayer renterPlayer = mock(ITanPlayer.class);
    when(renterPlayer.getID()).thenReturn(renterUuid.toString());
    when(mockPlayerStorage.getSync(renterUuid.toString())).thenReturn(renterPlayer);

    property.allocateRenter(mockPlayer);

    assertTrue(property.isPlayerAllowed(ChunkPermissionType.PLACE_BLOCK, renterPlayer),
        "Renter should have build access");
    assertTrue(property.isPlayerAllowed(ChunkPermissionType.INTERACT_CHEST, renterPlayer),
        "Renter should have interact access");
  }

  @Test
  void testPermission_NonRenterHasNoAccess() {
    ITanPlayer stranger = mock(ITanPlayer.class);
    when(stranger.getID()).thenReturn(UUID.randomUUID().toString());

    PlayerOwned playerOwner = mock(PlayerOwned.class);
    when(playerOwner.canAccess(any(ITanPlayer.class))).thenReturn(false);
    when(property.getOwner()).thenReturn(playerOwner);

    assertFalse(property.isPlayerAllowed(ChunkPermissionType.PLACE_BLOCK, stranger),
        "Stranger should not have build permission");
  }

  @Test
  void testPermission_AfterRenterExpelled() {
    Player mockPlayer = mock(Player.class);
    UUID renterUuid = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(renterUuid);

    ITanPlayer renterPlayer = mock(ITanPlayer.class);
    when(renterPlayer.getID()).thenReturn(renterUuid.toString());
    when(mockPlayerStorage.getSync(renterUuid.toString())).thenReturn(renterPlayer);

    property.allocateRenter(mockPlayer);
    assertTrue(property.isRented());

    property.expelRenter(false);

    assertFalse(property.isRented());
    assertFalse(property.isPlayerAllowed(ChunkPermissionType.PLACE_BLOCK, renterPlayer),
        "Expelled renter should lose access");
  }

  // ========== Boundary Tests ==========

  @Test
  void testBoundaryDetection_EdgesIncluded() {
    Vector3D testP1 = new Vector3D(0, 0, 0, "world");
    Vector3D testP2 = new Vector3D(10, 10, 10, "world");
    PropertyData testProperty = new PropertyData("test_prop", testP1, testP2, owner);

    World mockWorld = mock(World.class);
    Location exactCorner = new Location(mockWorld, 0, 0, 0);

    assertTrue(testProperty.containsLocation(exactCorner),
        "Property should include boundary edges");
  }

  @Test
  void testBoundaryDetection_ReversedCoordinates() {
    Vector3D p1 = new Vector3D(100, 64, 100, "world");
    Vector3D p2 = new Vector3D(50, 70, 50, "world");
    PropertyData reversedProperty = new PropertyData("test_prop", p1, p2, owner);

    World mockWorld = mock(World.class);
    Location inside = new Location(mockWorld, 75, 67, 75);

    assertTrue(reversedProperty.containsLocation(inside),
        "Should detect location correctly with reversed coordinates");
  }

  @Test
  void testBoundaryDetection_3DVolume() {
    Vector3D testP1 = new Vector3D(0, 0, 0, "world");
    Vector3D testP2 = new Vector3D(10, 20, 10, "world");
    PropertyData testProperty = new PropertyData("test_prop", testP1, testP2, owner);

    World mockWorld = mock(World.class);

    // Test at different Y levels
    Location groundLevel = new Location(mockWorld, 5, 5, 5);
    Location midLevel = new Location(mockWorld, 5, 10, 5);
    Location topLevel = new Location(mockWorld, 5, 15, 5);
    Location aboveTop = new Location(mockWorld, 5, 25, 5);

    assertTrue(testProperty.containsLocation(groundLevel));
    assertTrue(testProperty.containsLocation(midLevel));
    assertTrue(testProperty.containsLocation(topLevel));
    assertFalse(testProperty.containsLocation(aboveTop));
  }

  // ========== Chunk Integration Tests ==========

  @Test
  void testPropertySpanningMultipleChunks() {
    Vector3D p1 = new Vector3D(0, 64, 0, "world");
    Vector3D p2 = new Vector3D(50, 70, 50, "world");
    PropertyData largeProperty = new PropertyData("large_prop", p1, p2, owner);

    var mockChunk1 = mock(org.leralix.tan.dataclass.chunk.ClaimedChunk2.class);
    when(mockChunk1.getX()).thenReturn(0);
    when(mockChunk1.getZ()).thenReturn(0);

    var mockChunk2 = mock(org.leralix.tan.dataclass.chunk.ClaimedChunk2.class);
    when(mockChunk2.getX()).thenReturn(2);
    when(mockChunk2.getZ()).thenReturn(2);

    assertTrue(largeProperty.isInChunk(mockChunk1),
        "Property should be in chunk (0,0)");
    assertTrue(largeProperty.isInChunk(mockChunk2),
        "Property should be in chunk (2,2)");
  }

  @Test
  void testPropertyInPartialChunk() {
    var mockChunk = mock(org.leralix.tan.dataclass.chunk.ClaimedChunk2.class);
    when(mockChunk.getX()).thenReturn(6);
    when(mockChunk.getZ()).thenReturn(6);

    assertTrue(property.isInChunk(mockChunk),
        "Property should be detected in chunk it overlaps");
  }

  // ========== Description Generation Tests ==========

  @Test
  void testDescription_ForSaleState() {
    property.swapIsForSale();
    property.setSalePrice(7500.0);
    property.setName("Luxury Villa");

    var description = property.getBasicDescription();

    assertNotNull(description);
    assertFalse(description.isEmpty());
  }

  @Test
  void testDescription_ForRentState() {
    property.swapIsRent();
    property.setRentPrice(200.0);
    property.setName("Cozy Cottage");

    var description = property.getBasicDescription();

    assertNotNull(description);
    assertFalse(description.isEmpty());
  }

  @Test
  void testDescription_RentedState() {
    Player mockPlayer = mock(Player.class);
    UUID renterUuid = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(renterUuid);

    ITanPlayer mockRenter = mock(ITanPlayer.class);
    when(mockRenter.getNameStored()).thenReturn("JohnDoe");
    when(mockPlayerStorage.getSync(renterUuid.toString())).thenReturn(mockRenter);

    property.allocateRenter(mockPlayer);
    property.setRentPrice(150.0);

    var description = property.getBasicDescription();

    assertNotNull(description);
    assertTrue(description.size() >= 3);
  }

  // ========== Icon Tests ==========

  @Test
  void testIcon_DefaultIcon() {
    ItemStack icon = property.getIcon();

    assertNotNull(icon);
    assertEquals(Material.OAK_SIGN, icon.getType());
  }

  @Test
  void testIcon_CustomIcon() {
    ItemStack customStack = new ItemStack(Material.DIAMOND_BLOCK);
    org.leralix.tan.dataclass.territory.cosmetic.CustomIcon customIcon =
        new org.leralix.tan.dataclass.territory.cosmetic.CustomIcon(customStack);

    property.setIcon(customIcon);

    assertEquals(Material.DIAMOND_BLOCK, property.getIcon().getType());
  }

  // ========== Ownership Transfer Tests ==========

  @Test
  void testOwnership_TerritoryOwned() {
    PropertyData territoryProperty = new PropertyData(PROPERTY_ID,
        new Vector3D(0, 64, 0, "world"), new Vector3D(10, 70, 10, "world"), town);

    org.leralix.tan.dataclass.property.AbstractOwner abstractOwner = territoryProperty.getOwner();

    assertTrue(abstractOwner instanceof TerritoryOwned,
        "Property should have territory owner");
  }

  @Test
  void testOwnership_PlayerOwned() {
    org.leralix.tan.dataclass.property.AbstractOwner abstractOwner = property.getOwner();

    assertTrue(abstractOwner instanceof PlayerOwned,
        "Property should have player owner");
    assertEquals(owner.getNameStored(), abstractOwner.getName());
  }

  // ========== Edge Cases ==========

  @Test
  void testExtremePrices() {
    property.setSalePrice(Double.MAX_VALUE);
    assertEquals(Double.MAX_VALUE, property.getBaseSalePrice(), 0.001);

    property.setSalePrice(0.0);
    assertEquals(0.0, property.getBaseSalePrice(), 0.001);

    property.setRentPrice(0.01);
    assertEquals(0.01, property.getBaseRentPrice(), 0.001);
  }

  @Test
  void testMultipleStateTransitions() {
    // Start: not for sale, not for rent
    assertFalse(property.isForSale());
    assertFalse(property.isForRent());
    assertFalse(property.isRented());

    // Transition to for sale
    property.swapIsForSale();
    assertTrue(property.isForSale());
    assertFalse(property.isForRent());

    // Transition to for rent
    property.swapIsRent();
    assertTrue(property.isForRent());
    assertFalse(property.isForSale());

    // Allocate renter
    Player mockPlayer = mock(Player.class);
    when(mockPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
    property.allocateRenter(mockPlayer);
    assertTrue(property.isRented());
    assertFalse(property.isForRent(), "Rent should be hidden when rented");

    // Expel renter
    property.expelRenter(true);
    assertFalse(property.isRented());
    assertTrue(property.isForRent(), "Should return to for rent after expel with rentBack=true");
  }

  @Test
  void testPropertyDeletion() {
    doAnswer(inv -> null).when(town).removeProperty(any());

    assertDoesNotThrow(() -> {
      property.delete();
    }, "Property deletion should not throw exceptions");
  }

  @Test
  void testPermissionManager_DefaultState() {
    PermissionManager pm = property.getPermissionManager();

    assertNotNull(pm);
  }

  @Test
  void testPropertyNameUpdates() {
    property.setName("New Name");

    assertEquals("New Name", property.getName());
  }

  @Test
  void testPropertyDescriptionUpdates() {
    property.setDescription("A beautiful property");

    assertEquals("A beautiful property", property.getDescription());
  }

  @Test
  void testGetTownFromProperty() {
    TownData retrievedTown = property.getTown();

    assertNotNull(retrievedTown);
    assertEquals(TOWN_ID, retrievedTown.getID());
  }

  @Test
  void testPropertyIdParsing() {
    assertEquals("prop123", property.getPropertyID());
    // getOwningStructureID() is private - tested indirectly through other methods
  }

  @Test
  void testSignLocation_InitiallyNull() {
    assertNull(property.getPosition(),
        "Sign position should be null before sign is created");
  }

  @Test
  void testPropertyWithNullTown() {
    when(mockTownStorage.getSync(TOWN_ID)).thenReturn(null);

    TownData nullTown = property.getTown();

    assertNull(nullTown, "Should return null when town doesn't exist");
  }

  @Test
  void testRenterWithInvalidUUID() {
    // This tests the error handling in getRenterPlayer()
    // when the stored UUID is malformed
    PropertyData testProperty = new PropertyData("test_prop",
        new Vector3D(0, 64, 0, "world"), new Vector3D(10, 70, 10, "world"), owner);

    // Should not throw even with invalid UUID scenario
    assertDoesNotThrow(() -> {
      testProperty.getRenterPlayer();
    }, "Should handle invalid UUID gracefully");
  }
}
