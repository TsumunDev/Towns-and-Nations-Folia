package org.leralix.tan.dataclass.territory.tax;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TaxComponent class.
 * Tests tax calculation, normalization, and immutability.
 */
public class TaxComponentTest {

    @Test
    @DisplayName("Default constructor should create component with default values")
    void testDefaultConstructor() {
        // Act
        TaxComponent tax = new TaxComponent();

        // Assert
        assertEquals(1.0, tax.getBaseTax(), 0.001);
        assertEquals(0.1, tax.getPropertyRentTax(), 0.001);
        assertEquals(0.1, tax.getPropertyBuyTax(), 0.001);
        assertEquals(0.5, tax.getPropertyCreateTax(), 0.001);
    }

    @Test
    @DisplayName("Constructor with values should store them correctly")
    void testConstructorWithValues() {
        // Act
        TaxComponent tax = new TaxComponent(2.0, 0.15, 0.2, 0.3);

        // Assert
        assertEquals(2.0, tax.getBaseTax(), 0.001);
        assertEquals(0.15, tax.getPropertyRentTax(), 0.001);
        assertEquals(0.2, tax.getPropertyBuyTax(), 0.001);
        assertEquals(0.3, tax.getPropertyCreateTax(), 0.001);
    }

    @Test
    @DisplayName("Property rent tax above 1.0 should be normalized")
    void testPropertyRentTaxNormalization() {
        // Act
        TaxComponent tax = new TaxComponent(1.0, 1.5, 0.1, 0.1); // 1.5 = 150%

        // Assert
        assertEquals(1.0, tax.getPropertyRentTax(), 0.001, "Should be normalized to 100%");
    }

    @Test
    @DisplayName("Property buy tax above 1.0 should be normalized")
    void testPropertyBuyTaxNormalization() {
        // Act
        TaxComponent tax = new TaxComponent(1.0, 0.1, 2.0, 0.1); // 2.0 = 200%

        // Assert
        assertEquals(1.0, tax.getPropertyBuyTax(), 0.001, "Should be normalized to 100%");
    }

    @Test
    @DisplayName("Negative property tax should be normalized to 0")
    void testNegativePropertyTaxNormalization() {
        // Act
        TaxComponent tax = new TaxComponent(-0.5, -0.1, -0.2, 0.0);

        // Assert
        assertEquals(-0.5, tax.getBaseTax(), 0.001, "Base tax allows negative (debt)");
        assertEquals(0.0, tax.getPropertyRentTax(), 0.001, "Negative rent tax should be normalized to 0");
        assertEquals(0.0, tax.getPropertyBuyTax(), 0.001, "Negative buy tax should be normalized to 0");
    }

    @Test
    @DisplayName("setBaseTax should return new instance (immutability)")
    void testSetBaseTaxImmutability() {
        // Arrange
        TaxComponent original = new TaxComponent();

        // Act
        TaxComponent modified = original.setBaseTax(5.0);

        // Assert
        assertNotSame(original, modified, "Should return new instance");
        assertEquals(1.0, original.getBaseTax(), 0.001, "Original should be unchanged");
        assertEquals(5.0, modified.getBaseTax(), 0.001, "Modified should have new value");
    }

    @Test
    @DisplayName("addToBaseTax should return new instance with incremented value")
    void testAddToBaseTax() {
        // Arrange
        TaxComponent original = new TaxComponent(2.0, 0.1, 0.1, 0.1);

        // Act
        TaxComponent modified = original.addToBaseTax(1.5);

        // Assert
        assertEquals(3.5, modified.getBaseTax(), 0.001);
        assertEquals(2.0, original.getBaseTax(), 0.001, "Original should be unchanged");
    }

    @Test
    @DisplayName("subtractFromBaseTax should work with negative delta")
    void testSubtractFromBaseTax() {
        // Arrange
        TaxComponent original = new TaxComponent(5.0, 0.1, 0.1, 0.1);

        // Act
        TaxComponent modified = original.addToBaseTax(-2.0);

        // Assert
        assertEquals(3.0, modified.getBaseTax(), 0.001);
    }

    @Test
    @DisplayName("setTaxOnRentingProperty should return new instance")
    void testSetTaxOnRentingProperty() {
        // Arrange
        TaxComponent original = new TaxComponent();

        // Act
        TaxComponent modified = original.setTaxOnRentingProperty(0.25);

        // Assert
        assertEquals(0.1, original.getPropertyRentTax(), 0.001);
        assertEquals(0.25, modified.getPropertyRentTax(), 0.001);
    }

    @Test
    @DisplayName("setTaxOnBuyingProperty should return new instance")
    void testSetTaxOnBuyingProperty() {
        // Arrange
        TaxComponent original = new TaxComponent();

        // Act
        TaxComponent modified = original.setTaxOnBuyingProperty(0.15);

        // Assert
        assertEquals(0.1, original.getPropertyBuyTax(), 0.001);
        assertEquals(0.15, modified.getPropertyBuyTax(), 0.001);
    }

    @Test
    @DisplayName("setTaxOnCreatingProperty should return new instance")
    void testSetTaxOnCreatingProperty() {
        // Arrange
        TaxComponent original = new TaxComponent();

        // Act
        TaxComponent modified = original.setTaxOnCreatingProperty(1.0);

        // Assert
        assertEquals(0.5, original.getPropertyCreateTax(), 0.001);
        assertEquals(1.0, modified.getPropertyCreateTax(), 0.001);
    }

    @Test
    @DisplayName("withBaseTax should return new instance with specified base tax")
    void testWithBaseTax() {
        // Arrange
        TaxComponent original = new TaxComponent();

        // Act
        TaxComponent modified = original.withBaseTax(10.0);

        // Assert
        assertEquals(1.0, original.getBaseTax(), 0.001);
        assertEquals(10.0, modified.getBaseTax(), 0.001);
    }

    @Test
    @DisplayName("Builder should create component with specified values")
    void testBuilder() {
        // Act
        TaxComponent tax = TaxComponent.builder()
            .baseTax(3.0)
            .propertyRentTax(0.2)
            .propertyBuyTax(0.15)
            .propertyCreateTax(0.75)
            .build();

        // Assert
        assertEquals(3.0, tax.getBaseTax(), 0.001);
        assertEquals(0.2, tax.getPropertyRentTax(), 0.001);
        assertEquals(0.15, tax.getPropertyBuyTax(), 0.001);
        assertEquals(0.75, tax.getPropertyCreateTax(), 0.001);
    }

    @Test
    @DisplayName("Builder with partial values should use defaults for rest")
    void testBuilderPartialValues() {
        // Act
        TaxComponent tax = TaxComponent.builder()
            .baseTax(5.0)
            .build();

        // Assert
        assertEquals(5.0, tax.getBaseTax(), 0.001);
        assertEquals(0.1, tax.getPropertyRentTax(), 0.001, "Should use default");
        assertEquals(0.1, tax.getPropertyBuyTax(), 0.001, "Should use default");
        assertEquals(0.5, tax.getPropertyCreateTax(), 0.001, "Should use default");
    }

    @Test
    @DisplayName("calculateTax should correctly compute tax amount")
    void testCalculateTax() {
        // Act & Assert
        assertEquals(10.0, TaxComponent.calculateTax(100.0, 0.1), 0.001);
        assertEquals(20.0, TaxComponent.calculateTax(100.0, 0.2), 0.001);
        assertEquals(0.0, TaxComponent.calculateTax(100.0, 0.0), 0.001);
    }

    @Test
    @DisplayName("calculateTax with rate above 1.0 should normalize")
    void testCalculateTaxNormalization() {
        // Act
        double tax = TaxComponent.calculateTax(100.0, 1.5); // 150%

        // Assert
        assertEquals(100.0, tax, 0.001, "Should normalize to 100%");
    }

    @Test
    @DisplayName("equals should return true for identical components")
    void testEquals() {
        // Arrange
        TaxComponent tax1 = new TaxComponent(2.0, 0.1, 0.1, 0.5);
        TaxComponent tax2 = new TaxComponent(2.0, 0.1, 0.1, 0.5);

        // Assert
        assertEquals(tax1, tax2);
    }

    @Test
    @DisplayName("equals should return false for different components")
    void testNotEquals() {
        // Arrange
        TaxComponent tax1 = new TaxComponent(2.0, 0.1, 0.1, 0.5);
        TaxComponent tax2 = new TaxComponent(3.0, 0.1, 0.1, 0.5);

        // Assert
        assertNotEquals(tax1, tax2);
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void testHashCode() {
        // Arrange
        TaxComponent tax1 = new TaxComponent(2.0, 0.1, 0.1, 0.5);
        TaxComponent tax2 = new TaxComponent(2.0, 0.1, 0.1, 0.5);

        // Assert
        assertEquals(tax1.hashCode(), tax2.hashCode());
    }

    @Test
    @DisplayName("toString should contain all tax values")
    void testToString() {
        // Act
        TaxComponent tax = new TaxComponent(2.5, 0.15, 0.2, 0.3);
        String str = tax.toString();

        // Assert
        assertTrue(str.contains("2.5"), "Should contain base tax");
        assertTrue(str.contains("0.15"), "Should contain rent tax");
        assertTrue(str.contains("0.2"), "Should contain buy tax");
        assertTrue(str.contains("0.3"), "Should contain create tax");
    }

    @Test
    @DisplayName("Chained setters should work correctly")
    void testChainedSetters() {
        // Arrange
        TaxComponent original = new TaxComponent();

        // Act
        TaxComponent modified = original
            .setBaseTax(5.0)
            .setTaxOnRentingProperty(0.2)
            .setTaxOnBuyingProperty(0.15)
            .setTaxOnCreatingProperty(0.75);

        // Assert
        assertEquals(5.0, modified.getBaseTax(), 0.001);
        assertEquals(0.2, modified.getPropertyRentTax(), 0.001);
        assertEquals(0.15, modified.getPropertyBuyTax(), 0.001);
        assertEquals(0.75, modified.getPropertyCreateTax(), 0.001);
        assertEquals(1.0, original.getBaseTax(), 0.001, "Original unchanged");
    }
}
