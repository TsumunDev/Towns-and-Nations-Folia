package org.leralix.tan.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link NumberUtils}.
 */
@DisplayName("NumberUtils Tests")
class NumberUtilsTest {

    private int originalDigits;

    @BeforeEach
    void setUp() {
        originalDigits = NumberUtils.getDigits();
        // Ensure 2 digits for tests
        setTestDigits(2);
    }

    @AfterEach
    void tearDown() {
        // Restore original digits
        setTestDigits(originalDigits);
    }

    private void setTestDigits(int digits) {
        try {
            Field field = NumberUtils.class.getDeclaredField("nbDigits");
            field.setAccessible(true);
            field.set(null, digits);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set test digits", e);
        }
    }

    // ==================== Rounding Tests ====================

    @Test
    @DisplayName("Should round values with 2 digits")
    void roundWithDigits_twoDigits_returnsRoundedValue() {
        assertEquals(123.46, NumberUtils.roundWithDigits(123.456), 0.001);
        assertEquals(100.00, NumberUtils.roundWithDigits(100.001), 0.001);
        assertEquals(99.99, NumberUtils.roundWithDigits(99.994), 0.001);
    }

    @Test
    @DisplayName("Should round negative values")
    void roundWithDigits_negativeValue_returnsRoundedValue() {
        assertEquals(-123.46, NumberUtils.roundWithDigits(-123.456), 0.001);
        assertEquals(-100.00, NumberUtils.roundWithDigits(-100.001), 0.001);
    }

    @Test
    @DisplayName("Should round zero")
    void roundWithDigits_zero_returnsZero() {
        assertEquals(0.0, NumberUtils.roundWithDigits(0.0), 0.001);
        assertEquals(0.0, NumberUtils.roundWithDigits(0.0001), 0.001);
    }

    @Test
    @DisplayName("Should round with different digit precision")
    void roundWithDigits_differentPrecision_returnsCorrectValue() {
        setTestDigits(0);
        assertEquals(123.0, NumberUtils.roundWithDigits(123.456), 0.001);

        setTestDigits(1);
        assertEquals(123.5, NumberUtils.roundWithDigits(123.456), 0.001);

        setTestDigits(3);
        assertEquals(123.456, NumberUtils.roundWithDigits(123.4564), 0.001);
    }

    // ==================== Parsing Tests ====================

    @Test
    @DisplayName("Should parse valid integers")
    void parseIntOrNull_validInt_returnsInt() {
        assertEquals(123, NumberUtils.parseIntOrNull("123"));
        assertEquals(0, NumberUtils.parseIntOrNull("0"));
        assertEquals(-456, NumberUtils.parseIntOrNull("-456"));
    }

    @Test
    @DisplayName("Should return null for invalid integers")
    void parseIntOrNull_invalidInt_returnsNull() {
        assertNull(NumberUtils.parseIntOrNull("abc"));
        assertNull(NumberUtils.parseIntOrNull("12.34"));
        assertNull(NumberUtils.parseIntOrNull(""));
        assertNull(NumberUtils.parseIntOrNull(" 123 "));
    }

    @Test
    @DisplayName("Should parse valid doubles")
    void parseDoubleOrNull_validDouble_returnsDouble() {
        assertEquals(123.45, NumberUtils.parseDoubleOrNull("123.45"), 0.001);
        assertEquals(0.0, NumberUtils.parseDoubleOrNull("0.0"), 0.001);
        assertEquals(-789.12, NumberUtils.parseDoubleOrNull("-789.12"), 0.001);
    }

    @Test
    @DisplayName("Should return null for invalid doubles")
    void parseDoubleOrNull_invalidDouble_returnsNull() {
        assertNull(NumberUtils.parseDoubleOrNull("abc"));
        assertNull(NumberUtils.parseDoubleOrNull(""));
        assertNull(NumberUtils.parseDoubleOrNull("12.34.56"));
    }

    @Test
    @DisplayName("Should parse valid longs")
    void parseLongOrNull_validLong_returnsLong() {
        assertEquals(123456789L, NumberUtils.parseLongOrNull("123456789"));
        assertEquals(0L, NumberUtils.parseLongOrNull("0"));
        assertEquals(-987654321L, NumberUtils.parseLongOrNull("-987654321"));
    }

    @Test
    @DisplayName("Should return null for invalid longs")
    void parseLongOrNull_invalidLong_returnsNull() {
        assertNull(NumberUtils.parseLongOrNull("abc"));
        assertNull(NumberUtils.parseLongOrNull("12.34"));
        assertNull(NumberUtils.parseLongOrNull(""));
    }

    // ==================== Clamp Tests ====================

    @Test
    @DisplayName("Should clamp double values")
    void clamp_double_returnsClampedValue() {
        assertEquals(5.0, NumberUtils.clamp(10.0, 0.0, 5.0), 0.001);
        assertEquals(0.0, NumberUtils.clamp(-5.0, 0.0, 5.0), 0.001);
        assertEquals(2.5, NumberUtils.clamp(2.5, 0.0, 5.0), 0.001);
    }

    @Test
    @DisplayName("Should clamp integer values")
    void clamp_int_returnsClampedValue() {
        assertEquals(10, NumberUtils.clamp(20, 0, 10));
        assertEquals(0, NumberUtils.clamp(-5, 0, 10));
        assertEquals(5, NumberUtils.clamp(5, 0, 10));
    }

    // ==================== Percentage Tests ====================

    @Test
    @DisplayName("Should calculate percentage")
    void percentage_validNumbers_returnsPercentage() {
        assertEquals(50.0, NumberUtils.percentage(50.0, 100.0), 0.001);
        assertEquals(25.0, NumberUtils.percentage(25.0, 100.0), 0.001);
        assertEquals(100.0, NumberUtils.percentage(100.0, 100.0), 0.001);
    }

    @Test
    @DisplayName("Should handle zero total")
    void percentage_zeroTotal_returnsZero() {
        assertEquals(0.0, NumberUtils.percentage(50.0, 0.0), 0.001);
        assertEquals(0.0, NumberUtils.percentage(0.0, 0.0), 0.001);
    }

    @Test
    @DisplayName("Should format percentage")
    void formatPercentage_validNumbers_returnsFormattedString() {
        assertEquals("50,0%", NumberUtils.formatPercentage(50.0, 100.0));
        assertEquals("25,5%", NumberUtils.formatPercentage(25.5, 100.0));
        assertEquals("0,0%", NumberUtils.formatPercentage(0.0, 100.0));
    }

    // ==================== Extension Function Tests ====================

    @Test
    @DisplayName("Should format long with separators")
    void formatWithSeparators_long_returnsFormattedString() {
        assertEquals("1\u202F000", NumberUtilsKt.formatWithSeparators(1000L));
        assertEquals("1\u202F234\u202F567", NumberUtilsKt.formatWithSeparators(1234567L));
        assertEquals("1\u202F234\u202F567\u202F890", NumberUtilsKt.formatWithSeparators(1234567890L));
        assertEquals("-1\u202F000", NumberUtilsKt.formatWithSeparators(-1000L));
    }

    @Test
    @DisplayName("Should format int with separators")
    void formatWithSeparators_int_returnsFormattedString() {
        assertEquals("1\u202F000", NumberUtilsKt.formatWithSeparators(1000));
        assertEquals("1\u202F234\u202F567", NumberUtilsKt.formatWithSeparators(1234567));
        assertEquals("-999", NumberUtilsKt.formatWithSeparators(-999));
    }

    // ==================== Double Extension Tests ====================

    @Test
    @DisplayName("Should round double to configured digits via extension")
    void roundToConfiguredDigits_validDouble_returnsRoundedValue() {
        assertEquals(123.46, NumberUtilsKt.roundToConfiguredDigits(123.456), 0.001);
        assertEquals(100.0, NumberUtilsKt.roundToConfiguredDigits(100.001), 0.001);
    }

    @Test
    @DisplayName("Should clamp double via extension")
    void clamp_extension_returnsClampedValue() {
        assertEquals(5.0, NumberUtilsKt.clamp(10.0, 0.0, 5.0), 0.001);
        assertEquals(0.0, NumberUtilsKt.clamp(-5.0, 0.0, 5.0), 0.001);
        assertEquals(2.5, NumberUtilsKt.clamp(2.5, 0.0, 5.0), 0.001);
    }

    @Test
    @DisplayName("Should clamp int via extension")
    void clamp_intExtension_returnsClampedValue() {
        assertEquals(10, NumberUtilsKt.clamp(20, 0, 10));
        assertEquals(0, NumberUtilsKt.clamp(-5, 0, 10));
        assertEquals(5, NumberUtilsKt.clamp(5, 0, 10));
    }

    @Test
    @DisplayName("Should calculate percentage via extension")
    void percentageOf_validNumbers_returnsPercentage() {
        assertEquals(50.0, NumberUtilsKt.percentageOf(50.0, 100.0), 0.001);
        assertEquals(25.0, NumberUtilsKt.percentageOf(25.0, 100.0), 0.001);
    }

    @Test
    @DisplayName("Should format percentage via extension")
    void asPercentageOf_validNumbers_returnsFormattedString() {
        assertEquals("50,0%", NumberUtilsKt.asPercentageOf(50.0, 100.0));
        assertEquals("25,5%", NumberUtilsKt.asPercentageOf(25.5, 100.0));
    }

    // ==================== Double Property Tests ====================

    @Test
    @DisplayName("Should check if double is positive")
    void isPositive_positive_returnsTrue() {
        assertTrue(NumberUtilsKt.isPositive(1.0));
        assertTrue(NumberUtilsKt.isPositive(0.001));
    }

    @Test
    @DisplayName("Should check if double is negative")
    void isNegative_negative_returnsTrue() {
        assertTrue(NumberUtilsKt.isNegative(-1.0));
        assertTrue(NumberUtilsKt.isNegative(-0.001));
    }

    @Test
    @DisplayName("Should check if double is zero")
    void isZero_zero_returnsTrue() {
        assertTrue(NumberUtilsKt.isZero(0.0, 0.0001));
        assertTrue(NumberUtilsKt.isZero(0.00001, 0.0001));
        assertTrue(NumberUtilsKt.isZero(-0.00001, 0.0001));
    }

    @Test
    @DisplayName("Should check if non-zero is not zero")
    void isZero_nonZero_returnsFalse() {
        assertFalse(NumberUtilsKt.isZero(1.0, 0.0001));
        assertFalse(NumberUtilsKt.isZero(-1.0, 0.0001));
        assertFalse(NumberUtilsKt.isZero(0.001, 0.0001));
    }
}
