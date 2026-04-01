package org.leralix.tan.service.economy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for financial calculations and utility functions.
 * <p>
 * These tests verify the correctness of:
 * <ul>
 *   <li>Rounding algorithms (banker's rounding vs standard rounding)</li>
 *   <li>Percentage calculations</li>
 *   <li>Clamping operations</li>
 *   <li>Number formatting and parsing</li>
 * </ul>
 * <p>
 * Financial accuracy is critical to prevent:
 * <ul>
 *   <li>Money creation/destruction through rounding</li>
 *   <li>Incorrect tax calculations</li>
 *   <li>Display inconsistencies</li>
 * </ul>
 */
@DisplayName("Financial Calculation Tests")
class FinancialCalculationTest {

    private int originalDigits;

    @BeforeEach
    void setUp() {
        originalDigits = org.leralix.tan.utils.NumberUtils.getDigits();
        setTestDigits(2);
    }

    @AfterEach
    void tearDown() {
        setTestDigits(originalDigits);
    }

    private void setTestDigits(int digits) {
        try {
            Field field = org.leralix.tan.utils.NumberUtils.class.getDeclaredField("nbDigits");
            field.setAccessible(true);
            field.set(null, digits);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set test digits", e);
        }
    }

    // ==================== Rounding Algorithm Tests ====================

    @Test
    @DisplayName("Should round half up for positive numbers")
    void roundHalfUp_positiveNumbers_correctRounding() {
        assertEquals(1.24, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.235), 0.001);
        assertEquals(1.23, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.234), 0.001);
        assertEquals(1.24, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.236), 0.001);
    }

    @Test
    @DisplayName("Should round half up for negative numbers")
    void roundHalfUp_negativeNumbers_correctRounding() {
        assertEquals(-1.24, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(-1.235), 0.001);
        assertEquals(-1.23, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(-1.234), 0.001);
        assertEquals(-1.24, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(-1.236), 0.001);
    }

    @Test
    @DisplayName("Should round exact halves correctly")
    void roundExactHalves_correctRounding() {
        assertEquals(1.00, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(0.999), 0.001);
        assertEquals(2.00, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.995), 0.001);
        assertEquals(3.00, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(2.995), 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "0.001, 0.0",
        "0.004, 0.0",
        "0.005, 0.01",
        "0.009, 0.01",
        "0.014, 0.01",
        "0.015, 0.02"
    })
    @DisplayName("Should handle edge case rounding values")
    void edgeCaseRounding_correctRounding(String input, String expected) {
        double result = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(Double.parseDouble(input));
        assertEquals(Double.parseDouble(expected), result, 0.0001,
            "Value " + input + " should round to " + expected);
    }

    // ==================== Precision with Different Digits ====================

    @Test
    @DisplayName("Should round correctly with 0 decimal places")
    void roundWith0Decimals_correctRounding() {
        setTestDigits(0);
        assertEquals(1.0, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.4), 0.001);
        assertEquals(2.0, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.5), 0.001);
        assertEquals(1.0, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.49), 0.001);
    }

    @Test
    @DisplayName("Should round correctly with 1 decimal place")
    void roundWith1Decimal_correctRounding() {
        setTestDigits(1);
        assertEquals(1.2, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.24), 0.001);
        assertEquals(1.3, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.25), 0.001);
        assertEquals(1.2, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.249), 0.001);
    }

    @Test
    @DisplayName("Should round correctly with 3 decimal places")
    void roundWith3Decimals_correctRounding() {
        setTestDigits(3);
        assertEquals(1.234, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.2344), 0.0001);
        assertEquals(1.235, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.2345), 0.0001);
        assertEquals(1.235, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.2346), 0.0001);
    }

    @Test
    @DisplayName("Should round correctly with 4 decimal places")
    void roundWith4Decimals_correctRounding() {
        setTestDigits(4);
        assertEquals(1.2345, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.23454), 0.00001);
        assertEquals(1.2346, org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(1.23455), 0.00001);
    }

    // ==================== Percentage Calculation Tests ====================

    @Test
    @DisplayName("Should calculate percentage correctly")
    void percentage_correctCalculation() {
        assertEquals(50.0, org.leralix.tan.utils.NumberUtilsKt.percentageOf(50.0, 100.0), 0.001);
        assertEquals(25.0, org.leralix.tan.utils.NumberUtilsKt.percentageOf(25.0, 100.0), 0.001);
        assertEquals(100.0, org.leralix.tan.utils.NumberUtilsKt.percentageOf(100.0, 100.0), 0.001);
        assertEquals(200.0, org.leralix.tan.utils.NumberUtilsKt.percentageOf(200.0, 100.0), 0.001);
    }

    @Test
    @DisplayName("Should handle zero total in percentage calculation")
    void percentage_zeroTotal_returnsZero() {
        assertEquals(0.0, org.leralix.tan.utils.NumberUtilsKt.percentageOf(50.0, 0.0), 0.001);
        assertEquals(0.0, org.leralix.tan.utils.NumberUtilsKt.percentageOf(0.0, 0.0), 0.001);
    }

    @Test
    @DisplayName("Should format percentage correctly")
    void percentageFormat_correctFormatting() {
        assertEquals("50,0%", org.leralix.tan.utils.NumberUtilsKt.asPercentageOf(50.0, 100.0));
        assertEquals("25,5%", org.leralix.tan.utils.NumberUtilsKt.asPercentageOf(25.5, 100.0));
        assertEquals("150,0%", org.leralix.tan.utils.NumberUtilsKt.asPercentageOf(150.0, 100.0));
    }

    @Test
    @DisplayName("Should handle fractional percentages")
    void fractionalPercentage_correctCalculation() {
        assertEquals(0.5, org.leralix.tan.utils.NumberUtilsKt.percentageOf(0.5, 100.0), 0.001);
        assertEquals(33.333, org.leralix.tan.utils.NumberUtilsKt.percentageOf(1.0, 3.0), 0.001);
        assertEquals(66.667, org.leralix.tan.utils.NumberUtilsKt.percentageOf(2.0, 3.0), 0.001);
    }

    // ==================== Clamp Operation Tests ====================

    @Test
    @DisplayName("Should clamp double values correctly")
    void clampDouble_correctClamping() {
        assertEquals(5.0, org.leralix.tan.utils.NumberUtilsKt.clamp(10.0, 0.0, 5.0), 0.001);
        assertEquals(0.0, org.leralix.tan.utils.NumberUtilsKt.clamp(-5.0, 0.0, 5.0), 0.001);
        assertEquals(2.5, org.leralix.tan.utils.NumberUtilsKt.clamp(2.5, 0.0, 5.0), 0.001);
        assertEquals(5.0, org.leralix.tan.utils.NumberUtilsKt.clamp(5.0, 0.0, 5.0), 0.001);
        assertEquals(0.0, org.leralix.tan.utils.NumberUtilsKt.clamp(0.0, 0.0, 5.0), 0.001);
    }

    @Test
    @DisplayName("Should clamp integer values correctly")
    void clampInt_correctClamping() {
        assertEquals(10, org.leralix.tan.utils.NumberUtilsKt.clamp(20, 0, 10));
        assertEquals(0, org.leralix.tan.utils.NumberUtilsKt.clamp(-5, 0, 10));
        assertEquals(5, org.leralix.tan.utils.NumberUtilsKt.clamp(5, 0, 10));
        assertEquals(10, org.leralix.tan.utils.NumberUtilsKt.clamp(10, 0, 10));
        assertEquals(0, org.leralix.tan.utils.NumberUtilsKt.clamp(0, 0, 10));
    }

    @Test
    @DisplayName("Should handle negative clamp ranges")
    void clampNegativeRange_correctClamping() {
        assertEquals(-5.0, org.leralix.tan.utils.NumberUtilsKt.clamp(10.0, -10.0, -5.0), 0.001);
        assertEquals(-10.0, org.leralix.tan.utils.NumberUtilsKt.clamp(-20.0, -10.0, -5.0), 0.001);
        assertEquals(-7.5, org.leralix.tan.utils.NumberUtilsKt.clamp(-7.5, -10.0, -5.0), 0.001);
    }

    // ==================== Number Property Tests ====================

    @Test
    @DisplayName("Should correctly identify positive numbers")
    void isPositive_correctIdentification() {
        assertTrue(org.leralix.tan.utils.NumberUtilsKt.isPositive(1.0));
        assertTrue(org.leralix.tan.utils.NumberUtilsKt.isPositive(0.001));
        assertTrue(org.leralix.tan.utils.NumberUtilsKt.isPositive(Double.MIN_VALUE));
        assertFalse(org.leralix.tan.utils.NumberUtilsKt.isPositive(0.0));
        assertFalse(org.leralix.tan.utils.NumberUtilsKt.isPositive(-0.001));
        assertFalse(org.leralix.tan.utils.NumberUtilsKt.isPositive(-1.0));
    }

    @Test
    @DisplayName("Should correctly identify negative numbers")
    void isNegative_correctIdentification() {
        assertTrue(org.leralix.tan.utils.NumberUtilsKt.isNegative(-1.0));
        assertTrue(org.leralix.tan.utils.NumberUtilsKt.isNegative(-0.001));
        assertTrue(org.leralix.tan.utils.NumberUtilsKt.isNegative(-Double.MIN_VALUE));
        assertFalse(org.leralix.tan.utils.NumberUtilsKt.isNegative(0.0));
        assertFalse(org.leralix.tan.utils.NumberUtilsKt.isNegative(0.001));
        assertFalse(org.leralix.tan.utils.NumberUtilsKt.isNegative(1.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.0001, -0.0001, 0.00001, -0.00001})
    @DisplayName("Should correctly identify zero with epsilon")
    void isZero_withEpsilon_correctIdentification(double value) {
        assertTrue(org.leralix.tan.utils.NumberUtilsKt.isZero(value, 0.0001));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.001, -0.001, 1.0, -1.0})
    @DisplayName("Should correctly identify non-zero values")
    void isZero_nonZero_correctIdentification(double value) {
        assertFalse(org.leralix.tan.utils.NumberUtilsKt.isZero(value, 0.0001));
    }

    // ==================== Parsing Tests ====================

    @Test
    @DisplayName("Should parse valid integers correctly")
    void parseInt_validInput_correctParsing() {
        assertEquals(Integer.valueOf(123), org.leralix.tan.utils.NumberUtils.parseIntOrNull("123"));
        assertEquals(Integer.valueOf(0), org.leralix.tan.utils.NumberUtils.parseIntOrNull("0"));
        assertEquals(Integer.valueOf(-456), org.leralix.tan.utils.NumberUtils.parseIntOrNull("-456"));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), org.leralix.tan.utils.NumberUtils.parseIntOrNull("2147483647"));
    }

    @Test
    @DisplayName("Should return null for invalid integers")
    void parseInt_invalidInput_returnsNull() {
        assertNull(org.leralix.tan.utils.NumberUtils.parseIntOrNull("abc"));
        assertNull(org.leralix.tan.utils.NumberUtils.parseIntOrNull("12.34"));
        assertNull(org.leralix.tan.utils.NumberUtils.parseIntOrNull(""));
        assertNull(org.leralix.tan.utils.NumberUtils.parseIntOrNull(" 123 "));
        assertNull(org.leralix.tan.utils.NumberUtils.parseIntOrNull("9999999999"));
    }

    @Test
    @DisplayName("Should parse valid doubles correctly")
    void parseDouble_validInput_correctParsing() {
        assertEquals(123.45, org.leralix.tan.utils.NumberUtils.parseDoubleOrNull("123.45"), 0.001);
        assertEquals(0.0, org.leralix.tan.utils.NumberUtils.parseDoubleOrNull("0.0"), 0.001);
        assertEquals(-789.12, org.leralix.tan.utils.NumberUtils.parseDoubleOrNull("-789.12"), 0.001);
        assertEquals(1.0, org.leralix.tan.utils.NumberUtils.parseDoubleOrNull("1"), 0.001);
    }

    @Test
    @DisplayName("Should return null for invalid doubles")
    void parseDouble_invalidInput_returnsNull() {
        assertNull(org.leralix.tan.utils.NumberUtils.parseDoubleOrNull("abc"));
        assertNull(org.leralix.tan.utils.NumberUtils.parseDoubleOrNull(""));
        assertNull(org.leralix.tan.utils.NumberUtils.parseDoubleOrNull("12.34.56"));
    }

    @Test
    @DisplayName("Should parse valid longs correctly")
    void parseLong_validInput_correctParsing() {
        assertEquals(Long.valueOf(123456789L), org.leralix.tan.utils.NumberUtils.parseLongOrNull("123456789"));
        assertEquals(Long.valueOf(0L), org.leralix.tan.utils.NumberUtils.parseLongOrNull("0"));
        assertEquals(Long.valueOf(-987654321L), org.leralix.tan.utils.NumberUtils.parseLongOrNull("-987654321"));
        assertEquals(Long.valueOf(Long.MAX_VALUE), org.leralix.tan.utils.NumberUtils.parseLongOrNull("9223372036854775807"));
    }

    @Test
    @DisplayName("Should return null for invalid longs")
    void parseLong_invalidInput_returnsNull() {
        assertNull(org.leralix.tan.utils.NumberUtils.parseLongOrNull("abc"));
        assertNull(org.leralix.tan.utils.NumberUtils.parseLongOrNull("12.34"));
        assertNull(org.leralix.tan.utils.NumberUtils.parseLongOrNull(""));
    }

    // ==================== Formatting Tests ====================

    @Test
    @DisplayName("Should format long with separators correctly")
    void formatLongWithSeparators_correctFormatting() {
        assertEquals("1\u202F000", org.leralix.tan.utils.NumberUtilsKt.formatWithSeparators(1000L));
        assertEquals("1\u202F234\u202F567", org.leralix.tan.utils.NumberUtilsKt.formatWithSeparators(1234567L));
        assertEquals("1\u202F234\u202F567\u202F890", org.leralix.tan.utils.NumberUtilsKt.formatWithSeparators(1234567890L));
        assertEquals("-1\u202F000", org.leralix.tan.utils.NumberUtilsKt.formatWithSeparators(-1000L));
        assertEquals("0", org.leralix.tan.utils.NumberUtilsKt.formatWithSeparators(0L));
    }

    @Test
    @DisplayName("Should format int with separators correctly")
    void formatIntWithSeparators_correctFormatting() {
        assertEquals("1\u202F000", org.leralix.tan.utils.NumberUtilsKt.formatWithSeparators(1000));
        assertEquals("1\u202F234\u202F567", org.leralix.tan.utils.NumberUtilsKt.formatWithSeparators(1234567));
        assertEquals("-999", org.leralix.tan.utils.NumberUtilsKt.formatWithSeparators(-999));
        assertEquals("0", org.leralix.tan.utils.NumberUtilsKt.formatWithSeparators(0));
    }

    // ==================== Financial Edge Cases ====================

    @Test
    @DisplayName("Should handle money addition without floating point errors")
    void moneyAddition_noFloatingPointError() {
        setTestDigits(2);

        double sum = 0.0;
        for (int i = 0; i < 100; i++) {
            sum = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(sum + 0.1);
        }

        assertEquals(10.0, sum, 0.001,
            "100 * 0.1 should equal 10.0");
    }

    @Test
    @DisplayName("Should handle cumulative rounding correctly")
    void cumulativeRounding_correctResult() {
        setTestDigits(2);

        double balance = 100.0;
        for (int i = 0; i < 10; i++) {
            balance = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(balance + 0.01);
            balance = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(balance - 0.01);
        }

        assertEquals(100.0, balance, 0.001,
            "Cumulative add/remove should return to original");
    }

    @Test
    @DisplayName("Should calculate tax correctly")
    void taxCalculation_correctResult() {
        setTestDigits(2);

        double amount = 100.0;
        double taxRate = 0.15; // 15%

        double tax = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(amount * taxRate);
        double afterTax = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(amount - tax);

        assertEquals(15.0, tax, 0.001, "Tax should be 15.0");
        assertEquals(85.0, afterTax, 0.001, "After tax should be 85.0");
    }

    @Test
    @DisplayName("Should calculate compound interest correctly")
    void compoundInterest_correctResult() {
        setTestDigits(2);

        double principal = 1000.0;
        double rate = 0.05; // 5%
        int periods = 12;

        double amount = principal;
        for (int i = 0; i < periods; i++) {
            double interest = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(amount * rate);
            amount = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(amount + interest);
        }

        // Should be approximately 1000 * (1.05)^12 = 1795.86
        assertTrue(amount >= 1790.0 && amount <= 1800.0,
            "Compound interest calculation should be reasonable: " + amount);
    }

    // ==================== Comparison Tests ====================

    @Test
    @DisplayName("Should compare monetary values correctly")
    void monetaryComparison_correctResult() {
        setTestDigits(2);

        double a = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(100.0);
        double b = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(100.001);
        double c = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(100.01);
        double d = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(99.99);

        assertTrue(a == b, "100.0 and 100.001 should be equal after rounding");
        assertTrue(a < c, "100.0 should be less than 100.01");
        assertTrue(a > d, "100.0 should be greater than 99.99");
    }

    @Test
    @DisplayName("Should handle very large monetary values")
    void largeMonetaryValues_correctRounding() {
        setTestDigits(2);

        double large = 1_000_000_000.0;
        double fraction = 0.005;

        double result = org.leralix.tan.utils.NumberUtilsKt.roundToConfiguredDigits(large + fraction);

        assertEquals(1_000_000_000.01, result, 0.001,
            "Large values should round correctly");
    }
}
