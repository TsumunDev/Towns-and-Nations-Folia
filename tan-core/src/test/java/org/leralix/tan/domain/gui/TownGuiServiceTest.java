package org.leralix.tan.domain.gui;

import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for TownGuiService.
 *
 * <p>Tests the new GUI service that extracts GUI logic from TownData.</p>
 *
 * @since 2.0.0
 */
@DisplayName("TownGuiService Tests")
class TownGuiServiceTest {

    private TownGuiService service;
    private TownDataStorage mockTownStorage;
    private PlayerDataStorage mockPlayerStorage;

    @BeforeEach
    void setUp() {
        // Create mock storage instances
        mockTownStorage = mock(TownDataStorage.class);
        mockPlayerStorage = mock(PlayerDataStorage.class);

        // Create the service with mocked dependencies
        service = new TownGuiServiceImpl(mockTownStorage, mockPlayerStorage);
    }

    @AfterEach
    void tearDown() {
        // Reset holder to avoid polluting other tests
        TownGuiHolder.clear();
    }

    @Test
    @DisplayName("getIconWithName should return ItemStack with town name")
    void testGetIconWithName() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);
        when(mockTown.getName()).thenReturn("TestTown");
        when(mockTown.getID()).thenReturn(townId);
        when(mockTown.getIcon()).thenReturn(new ItemStack(Material.PLAYER_HEAD));

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        ItemStack result = service.getIconWithName(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());

        ItemMeta meta = result.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.hasDisplayName());
        assertTrue(meta.getDisplayName().contains("TestTown"));
    }

    @Test
    @DisplayName("getIconWithName should handle null town gracefully")
    void testGetIconWithNameNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        ItemStack result = service.getIconWithName(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(Material.BARRIER, result.getType());
    }

    @Test
    @DisplayName("getIconWithInformations should return ItemStack with lore")
    void testGetIconWithInformations() throws Exception {
        // Arrange
        String townId = "town_test";
        LangType langType = LangType.ENGLISH;

        TownData mockTown = mock(TownData.class);
        when(mockTown.getName()).thenReturn("TestTown");
        when(mockTown.getID()).thenReturn(townId);
        when(mockTown.getDescription()).thenReturn("A test town");
        when(mockTown.getLeaderNameSync()).thenReturn("LeaderPlayer");
        when(mockTown.getPlayerIDList()).thenReturn(List.of("uuid1", "uuid2", "uuid3"));
        when(mockTown.getNumberOfClaimedChunk()).thenReturn(5);
        when(mockTown.getOverlord()).thenReturn(java.util.Optional.empty());
        when(mockTown.getIcon()).thenReturn(new ItemStack(Material.PLAYER_HEAD));

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        ItemStack result = service.getIconWithInformations(townId, langType).get();

        // Assert
        assertNotNull(result);
        assertEquals(Material.PLAYER_HEAD, result.getType());

        ItemMeta meta = result.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.hasLore());

        List<String> lore = meta.getLore();
        assertNotNull(lore);
        assertTrue(lore.stream().anyMatch(line -> line.contains("TestTown") || line.contains("A test town")));
    }

    @Test
    @DisplayName("getIconWithInformations should handle null town gracefully")
    void testGetIconWithInformationsNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        LangType langType = LangType.ENGLISH;

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        ItemStack result = service.getIconWithInformations(townId, langType).get();

        // Assert
        assertNotNull(result);
        assertEquals(Material.BARRIER, result.getType());

        ItemMeta meta = result.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.getDisplayName().contains("Unknown") || meta.getDisplayName().contains("§c"));
    }

    @Test
    @DisplayName("getOrderedMemberList should return empty list for null town")
    void testGetOrderedMemberListNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        ITanPlayer viewer = mock(ITanPlayer.class);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        List<GuiItem> result = service.getOrderedMemberList(townId, viewer).get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getOrderedMemberList should return member items")
    void testGetOrderedMemberList() throws Exception {
        // Arrange
        String townId = "town_test";
        ITanPlayer viewer = mock(ITanPlayer.class);
        when(viewer.getLang()).thenReturn(LangType.ENGLISH);

        TownData mockTown = mock(TownData.class);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTown.getOrderedPlayerIDListSync())
            .thenReturn(List.of("uuid1", "uuid2"));

        ITanPlayer player1 = mock(ITanPlayer.class);
        when(player1.getUUID()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(player1.getTownRank()).thenReturn(mock(org.leralix.tan.dataclass.RankData.class));

        ITanPlayer player2 = mock(ITanPlayer.class);
        when(player2.getUUID()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        when(player2.getTownRank()).thenReturn(mock(org.leralix.tan.dataclass.RankData.class));

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        when(mockPlayerStorage.get("uuid1"))
            .thenReturn(CompletableFuture.completedFuture(player1));
        when(mockPlayerStorage.get("uuid2"))
            .thenReturn(CompletableFuture.completedFuture(player2));

        // Act
        List<GuiItem> result = service.getOrderedMemberList(townId, viewer).get();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getOrderedMemberList should handle null players gracefully")
    void testGetOrderedMemberListNullPlayers() throws Exception {
        // Arrange
        String townId = "town_test";
        ITanPlayer viewer = mock(ITanPlayer.class);
        when(viewer.getLang()).thenReturn(LangType.ENGLISH);

        TownData mockTown = mock(TownData.class);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTown.getOrderedPlayerIDListSync())
            .thenReturn(List.of("uuid1", "uuid2"));

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        when(mockPlayerStorage.get("uuid1"))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(mockPlayerStorage.get("uuid2"))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        List<GuiItem> result = service.getOrderedMemberList(townId, viewer).get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Null players are filtered out
    }

    @Test
    @DisplayName("isEnabled should return false by default")
    void testIsEnabledDefault() {
        // Act
        boolean result = service.isEnabled();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("TownGuiHolder should return service instance")
    void testTownGuiHolder() {
        // Arrange
        TownGuiHolder.setService(service);

        // Act
        TownGuiService retrieved = TownGuiHolder.getService();

        // Assert
        assertNotNull(retrieved);
        assertEquals(service, retrieved);
    }

    @Test
    @DisplayName("TownGuiHolder should create default instance if not set")
    void testTownGuiHolderDefaultInstance() {
        // Arrange
        TownGuiHolder.clear(); // Ensure clear state

        // Act
        TownGuiService retrieved = TownGuiHolder.getService();

        // Assert
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof TownGuiServiceImpl);
    }

    @Test
    @DisplayName("getIconWithName should handle exceptions gracefully")
    void testGetIconWithNameException() throws Exception {
        // Arrange
        String townId = "town_error";
        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Storage error")));

        // Act & Assert
        assertThrows(ExecutionException.class, () -> service.getIconWithName(townId).get());
    }

    @Test
    @DisplayName("getOrderedMemberList should be async")
    void testGetOrderedMemberListAsync() {
        // Arrange
        String townId = "town_test";
        ITanPlayer viewer = mock(ITanPlayer.class);
        when(viewer.getLang()).thenReturn(LangType.ENGLISH);

        TownData mockTown = mock(TownData.class);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTown.getOrderedPlayerIDListSync())
            .thenReturn(List.of("uuid1"));

        ITanPlayer player1 = mock(ITanPlayer.class);
        when(player1.getUUID()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(player1.getTownRank()).thenReturn(mock(org.leralix.tan.dataclass.RankData.class));

        // Return a future that completes after a delay
        CompletableFuture<ITanPlayer> playerFuture = new CompletableFuture<>();
        playerFuture.complete(player1);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));
        when(mockPlayerStorage.get("uuid1")).thenReturn(playerFuture);

        // Act
        CompletableFuture<List<GuiItem>> result = service.getOrderedMemberList(townId, viewer);

        // Assert - should return a CompletableFuture that isn't immediately complete
        assertNotNull(result);
        assertFalse(result.isDone()); // Might not be done immediately due to async nature
    }
}
