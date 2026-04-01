package org.leralix.tan.domain.property;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.lib.position.Vector3D;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PropertyService.
 *
 * <p>Tests the new property service that extracts property logic from TownData.</p>
 *
 * @since 2.0.0
 */
@DisplayName("PropertyService Tests")
class PropertyServiceTest {

    private PropertyService service;
    private TownDataStorage mockTownStorage;

    @BeforeEach
    void setUp() {
        // Create mock storage instance
        mockTownStorage = mock(TownDataStorage.class);

        // Create the service with mocked dependency
        service = new PropertyServiceImpl(mockTownStorage);
    }

    @AfterEach
    void tearDown() {
        // Reset holder to avoid polluting other tests
        PropertyHolder.clear();
    }

    @Test
    @DisplayName("getProperties should return all properties")
    void testGetProperties() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        PropertyData property1 = mock(PropertyData.class);
        PropertyData property2 = mock(PropertyData.class);
        Collection<PropertyData> properties = Set.of(property1, property2);

        when(mockTown.getProperties()).thenReturn(properties);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Collection<PropertyData> result = service.getProperties(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(property1));
        assertTrue(result.contains(property2));
    }

    @Test
    @DisplayName("getProperties should return empty collection for null town")
    void testGetPropertiesNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Collection<PropertyData> result = service.getProperties(townId).get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getPropertyDataMap should return correct map")
    void testGetPropertyDataMap() throws Exception {
        // Arrange
        String townId = "town_test";
        TownData mockTown = mock(TownData.class);

        Map<String, PropertyData> propertyMap = Map.of(
            "P0", mock(PropertyData.class),
            "P1", mock(PropertyData.class)
        );

        when(mockTown.getPropertyDataMap()).thenReturn(propertyMap);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        Map<String, PropertyData> result = service.getPropertyDataMap(townId).get();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("P0"));
        assertTrue(result.containsKey("P1"));
    }

    @Test
    @DisplayName("getPropertyDataMap should return empty map for null town")
    void testGetPropertyDataMapNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        Map<String, PropertyData> result = service.getPropertyDataMap(townId).get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getProperty should return correct property by ID")
    void testGetPropertyById() throws Exception {
        // Arrange
        String townId = "town_test";
        String propertyId = "P0";

        PropertyData mockProperty = mock(PropertyData.class);
        TownData mockTown = mock(TownData.class);

        when(mockTown.getProperty(propertyId)).thenReturn(mockProperty);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        PropertyData result = service.getProperty(townId, propertyId).get();

        // Assert
        assertNotNull(result);
        assertEquals(mockProperty, result);
    }

    @Test
    @DisplayName("getProperty should return null for non-existent property")
    void testGetPropertyByIdNotFound() throws Exception {
        // Arrange
        String townId = "town_test";
        String propertyId = "P999";

        TownData mockTown = mock(TownData.class);
        when(mockTown.getProperty(propertyId)).thenReturn(null);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        PropertyData result = service.getProperty(townId, propertyId).get();

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("getProperty should return null for null town")
    void testGetPropertyByIdNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        String propertyId = "P0";

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        PropertyData result = service.getProperty(townId, propertyId).get();

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("getPropertyAtLocation should return property at location")
    void testGetPropertyAtLocation() throws Exception {
        // Arrange
        String townId = "town_test";
        Location location = mock(Location.class);

        PropertyData mockProperty = mock(PropertyData.class);
        TownData mockTown = mock(TownData.class);

        when(mockTown.getProperty(location)).thenReturn(mockProperty);
        when(mockTown.getID()).thenReturn(townId);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        PropertyData result = service.getPropertyAtLocation(townId, location).get();

        // Assert
        assertNotNull(result);
        assertEquals(mockProperty, result);
    }

    @Test
    @DisplayName("getPropertyAtLocation should return null for null town")
    void testGetPropertyAtLocationNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        Location location = mock(Location.class);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        PropertyData result = service.getPropertyAtLocation(townId, location).get();

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("removeProperty should remove property and save")
    void testRemoveProperty() throws Exception {
        // Arrange
        String townId = "town_test";
        PropertyData mockProperty = mock(PropertyData.class);
        when(mockProperty.getPropertyID()).thenReturn("P0");

        TownData mockTown = mock(TownData.class);
        when(mockTown.getID()).thenReturn(townId);
        when(mockTownStorage.updateAsync(mockTown))
            .thenReturn(CompletableFuture.completedFuture(null));

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(mockTown));

        // Act
        service.removeProperty(townId, mockProperty).get();

        // Assert
        verify(mockTown).removeProperty(mockProperty);
        verify(mockTownStorage).updateAsync(mockTown);
    }

    @Test
    @DisplayName("removeProperty should handle null town gracefully")
    void testRemovePropertyNullTown() throws Exception {
        // Arrange
        String townId = "nonexistent_town";
        PropertyData mockProperty = mock(PropertyData.class);

        when(mockTownStorage.get(townId))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            service.removeProperty(townId, mockProperty).get();
        });
    }

    @Test
    @DisplayName("PropertyHolder should return service instance")
    void testPropertyHolder() {
        // Arrange
        PropertyHolder.setService(service);

        // Act
        PropertyService retrieved = PropertyHolder.getService();

        // Assert
        assertNotNull(retrieved);
        assertEquals(service, retrieved);
    }

    @Test
    @DisplayName("isEnabled should return false when plugin is null")
    void testIsEnabledDefault() {
        // Act - No plugin initialized during unit tests
        boolean result = service.isEnabled();

        // Assert
        assertFalse(result);
    }
}
