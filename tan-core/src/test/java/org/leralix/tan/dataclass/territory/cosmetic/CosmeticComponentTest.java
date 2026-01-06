package org.leralix.tan.dataclass.territory.cosmetic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CosmeticComponent class.
 * Tests cosmetic management, immutability, and validation.
 */
public class CosmeticComponentTest {

    @Test
    @DisplayName("Default constructor should create component with default values")
    void testDefaultConstructor() {
        // Act
        CosmeticComponent cosmetic = new CosmeticComponent();

        // Assert
        assertEquals(CosmeticComponent.DEFAULT_DESCRIPTION, cosmetic.getDescription());
        assertNull(cosmetic.getIcon(), "Icon should be null by default");
        assertEquals(CosmeticComponent.DEFAULT_COLOR, cosmetic.getColor());
    }

    @Test
    @DisplayName("Constructor with values should store them correctly")
    void testConstructorWithValues() {
        // Arrange
        String description = "A powerful kingdom";
        ICustomIcon icon = mock(ICustomIcon.class);
        int color = 0xFF0000; // Red

        // Act
        CosmeticComponent cosmetic = new CosmeticComponent(description, icon, color);

        // Assert
        assertEquals(description, cosmetic.getDescription());
        assertEquals(icon, cosmetic.getIcon());
        assertEquals(color, cosmetic.getColor());
    }

    @Test
    @DisplayName("Constructor with null values should use defaults")
    void testConstructorWithNulls() {
        // Act
        CosmeticComponent cosmetic = new CosmeticComponent(null, null, null);

        // Assert
        assertEquals(CosmeticComponent.DEFAULT_DESCRIPTION, cosmetic.getDescription());
        assertNull(cosmetic.getIcon());
        assertEquals(CosmeticComponent.DEFAULT_COLOR, cosmetic.getColor());
    }

    @Test
    @DisplayName("Constructor with blank description should use default")
    void testConstructorWithBlankDescription() {
        // Act
        CosmeticComponent cosmetic = new CosmeticComponent("   ", null, 0x00FF00);

        // Assert
        assertEquals(CosmeticComponent.DEFAULT_DESCRIPTION, cosmetic.getDescription());
    }

    @Test
    @DisplayName("withDescription should return new instance with updated description")
    void testWithDescription() {
        // Arrange
        CosmeticComponent original = new CosmeticComponent("Original", null, 0x0000FF);

        // Act
        CosmeticComponent modified = original.withDescription("New Description");

        // Assert
        assertNotSame(original, modified, "Should return new instance");
        assertEquals("Original", original.getDescription(), "Original should be unchanged");
        assertEquals("New Description", modified.getDescription());
    }

    @Test
    @DisplayName("withIcon should return new instance with updated icon")
    void testWithIcon() {
        // Arrange
        CosmeticComponent original = new CosmeticComponent("Test", null, 0x0000FF);
        ICustomIcon newIcon = mock(ICustomIcon.class);

        // Act
        CosmeticComponent modified = original.withIcon(newIcon);

        // Assert
        assertNull(original.getIcon(), "Original should be unchanged");
        assertEquals(newIcon, modified.getIcon());
    }

    @Test
    @DisplayName("withColor should return new instance with updated color")
    void testWithColor() {
        // Arrange
        CosmeticComponent original = new CosmeticComponent("Test", null, 0x0000FF);

        // Act
        CosmeticComponent modified = original.withColor(0xFF00FF);

        // Assert
        assertEquals(0x0000FF, original.getColor(), "Original should be unchanged");
        assertEquals(0xFF00FF, modified.getColor());
    }

    @Test
    @DisplayName("hasIcon should return true when icon is present")
    void testHasIconTrue() {
        // Arrange
        ICustomIcon icon = mock(ICustomIcon.class);
        CosmeticComponent cosmetic = new CosmeticComponent("Test", icon, 0x0000FF);

        // Act
        boolean result = cosmetic.hasIcon();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("hasIcon should return false when icon is null")
    void testHasIconFalse() {
        // Arrange
        CosmeticComponent cosmetic = new CosmeticComponent("Test", null, 0x0000FF);

        // Act
        boolean result = cosmetic.hasIcon();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("getColorAsHex should return proper hex string")
    void testGetColorAsHex() {
        // Arrange
        CosmeticComponent cosmetic = new CosmeticComponent("Test", null, 0xFF0000);

        // Act
        String hex = cosmetic.getColorAsHex();

        // Assert
        assertEquals("#FF0000", hex);
    }

    @Test
    @DisplayName("getColorAsHex should handle full white color")
    void testGetColorAsHexWhite() {
        // Arrange
        CosmeticComponent cosmetic = new CosmeticComponent("Test", null, 0xFFFFFF);

        // Act
        String hex = cosmetic.getColorAsHex();

        // Assert
        assertEquals("#FFFFFF", hex);
    }

    @Test
    @DisplayName("getColorAsHex should handle default cyan color")
    void testGetColorAsHexDefault() {
        // Arrange
        CosmeticComponent cosmetic = new CosmeticComponent();

        // Act
        String hex = cosmetic.getColorAsHex();

        // Assert
        assertEquals("#00FFFF", hex);
    }

    @Test
    @DisplayName("equals should return true for identical components")
    void testEquals() {
        // Arrange
        ICustomIcon icon = mock(ICustomIcon.class);
        CosmeticComponent c1 = new CosmeticComponent("Test", icon, 0xFF0000);
        CosmeticComponent c2 = new CosmeticComponent("Test", icon, 0xFF0000);

        // Assert
        assertEquals(c1, c2);
    }

    @Test
    @DisplayName("equals should return false for different descriptions")
    void testNotEqualsDescription() {
        // Arrange
        CosmeticComponent c1 = new CosmeticComponent("Test1", null, 0xFF0000);
        CosmeticComponent c2 = new CosmeticComponent("Test2", null, 0xFF0000);

        // Assert
        assertNotEquals(c1, c2);
    }

    @Test
    @DisplayName("equals should return false for different colors")
    void testNotEqualsColor() {
        // Arrange
        CosmeticComponent c1 = new CosmeticComponent("Test", null, 0xFF0000);
        CosmeticComponent c2 = new CosmeticComponent("Test", null, 0x00FF00);

        // Assert
        assertNotEquals(c1, c2);
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void testHashCode() {
        // Arrange
        ICustomIcon icon = mock(ICustomIcon.class);
        CosmeticComponent c1 = new CosmeticComponent("Test", icon, 0xFF0000);
        CosmeticComponent c2 = new CosmeticComponent("Test", icon, 0xFF0000);

        // Assert
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    @DisplayName("toString should contain description and color")
    void testToString() {
        // Arrange
        CosmeticComponent cosmetic = new CosmeticComponent("My Kingdom", null, 0xFF0000);

        // Act
        String str = cosmetic.toString();

        // Assert
        assertTrue(str.contains("My Kingdom"), "Should contain description");
        assertTrue(str.contains("#FF0000"), "Should contain color");
    }

    @Test
    @DisplayName("Builder should create component with specified values")
    void testBuilder() {
        // Arrange
        ICustomIcon icon = mock(ICustomIcon.class);
        String expectedDescription = "Emerald City";
        int expectedColor = 0x00FF00;

        // Act
        CosmeticComponent cosmetic = CosmeticComponent.builder()
            .description(expectedDescription)
            .icon(icon)
            .color(expectedColor)
            .build();

        // Assert
        assertEquals(expectedDescription, cosmetic.getDescription());
        assertEquals(icon, cosmetic.getIcon());
        assertEquals(expectedColor, cosmetic.getColor());
    }

    @Test
    @DisplayName("Builder with partial values should use defaults for rest")
    void testBuilderPartialValues() {
        // Act
        CosmeticComponent cosmetic = CosmeticComponent.builder()
            .description("Partial Build")
            .build();

        // Assert
        assertEquals("Partial Build", cosmetic.getDescription());
        assertNull(cosmetic.getIcon(), "Should use default icon (null)");
        assertEquals(CosmeticComponent.DEFAULT_COLOR, cosmetic.getColor(), "Should use default color");
    }

    @Test
    @DisplayName("Builder with no values should use all defaults")
    void testBuilderDefault() {
        // Act
        CosmeticComponent cosmetic = CosmeticComponent.builder().build();

        // Assert
        assertEquals(CosmeticComponent.DEFAULT_DESCRIPTION, cosmetic.getDescription());
        assertNull(cosmetic.getIcon());
        assertEquals(CosmeticComponent.DEFAULT_COLOR, cosmetic.getColor());
    }

    @Test
    @DisplayName("Chained with methods should work correctly")
    void testChainedWithMethods() {
        // Arrange
        ICustomIcon icon = mock(ICustomIcon.class);
        CosmeticComponent original = new CosmeticComponent();

        // Act
        CosmeticComponent modified = original
            .withDescription("Beacon City")
            .withIcon(icon)
            .withColor(0xFFA500); // Orange

        // Assert
        assertEquals("Beacon City", modified.getDescription());
        assertEquals(icon, modified.getIcon());
        assertEquals(0xFFA500, modified.getColor());
        assertEquals(CosmeticComponent.DEFAULT_DESCRIPTION, original.getDescription());
    }

    @Test
    @DisplayName("Color value should be preserved exactly")
    void testColorPreservation() {
        // Arrange
        int testColor = 0x123456;

        // Act
        CosmeticComponent cosmetic = new CosmeticComponent("Test", null, testColor);

        // Assert
        assertEquals(testColor, cosmetic.getColor(), "Color should be preserved exactly");
    }

    @Test
    @DisplayName("Component with null icon should still be functional")
    void testNullIconFunctional() {
        // Arrange
        CosmeticComponent cosmetic = new CosmeticComponent("Test", null, 0x0000FF);

        // Act & Assert
        assertFalse(cosmetic.hasIcon());
        assertNull(cosmetic.getIcon());
        assertEquals("Test", cosmetic.getDescription());
        assertEquals(0x0000FF, cosmetic.getColor());
    }
}
