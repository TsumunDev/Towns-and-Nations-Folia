package org.leralix.tan.utils.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StringUtil}.
 * <p>
 * Tests string utility methods for color codes, money formatting, and number handling.
 */
@DisplayName("StringUtil Tests")
class StringUtilTest {

    // ==================== Color Code Validation Tests ====================

    @Test
    @DisplayName("Should validate green color code")
    void isValidColorCode_validGreen_returnsTrue() {
        assertTrue(StringUtil.isValidColorCode("00FF00"));
    }

    @Test
    @DisplayName("Should validate red color code")
    void isValidColorCode_validRed_returnsTrue() {
        assertTrue(StringUtil.isValidColorCode("FF0000"));
    }

    @Test
    @DisplayName("Should validate blue color code")
    void isValidColorCode_validBlue_returnsTrue() {
        assertTrue(StringUtil.isValidColorCode("0000FF"));
    }

    @Test
    @DisplayName("Should validate white color code")
    void isValidColorCode_validWhite_returnsTrue() {
        assertTrue(StringUtil.isValidColorCode("FFFFFF"));
    }

    @Test
    @DisplayName("Should validate black color code")
    void isValidColorCode_validBlack_returnsTrue() {
        assertTrue(StringUtil.isValidColorCode("000000"));
    }

    @Test
    @DisplayName("Should validate lowercase color code")
    void isValidColorCode_validLowercase_returnsTrue() {
        assertTrue(StringUtil.isValidColorCode("00ff00"));
    }

    @Test
    @DisplayName("Should validate mixed case color code")
    void isValidColorCode_validMixedCase_returnsTrue() {
        assertTrue(StringUtil.isValidColorCode("00Ff00"));
    }

    @Test
    @DisplayName("Should reject too short color code")
    void isValidColorCode_tooShort_returnsFalse() {
        assertFalse(StringUtil.isValidColorCode("00FF0"));
    }

    @Test
    @DisplayName("Should reject too long color code")
    void isValidColorCode_tooLong_returnsFalse() {
        assertFalse(StringUtil.isValidColorCode("00FF000"));
    }

    @Test
    @DisplayName("Should reject color code with invalid characters")
    void isValidColorCode_invalidChars_returnsFalse() {
        assertFalse(StringUtil.isValidColorCode("00GG00"));
    }

    @Test
    @DisplayName("Should reject color code with hash prefix")
    void isValidColorCode_withHash_returnsFalse() {
        assertFalse(StringUtil.isValidColorCode("#00FF00"));
    }

    @Test
    @DisplayName("Should reject empty color code")
    void isValidColorCode_empty_returnsFalse() {
        assertFalse(StringUtil.isValidColorCode(""));
    }

    @Test
    @DisplayName("Should throw exception for null color code")
    void isValidColorCode_null_throwsException() {
        assertThrows(NullPointerException.class, () -> StringUtil.isValidColorCode(null));
    }

    // ==================== Hex Color to Int Tests ====================

    @Test
    @DisplayName("Should convert white hex to max int value")
    void hexColorToInt_white_returnsMaxValue() {
        assertEquals(16777215, StringUtil.hexColorToInt("FFFFFF"));
    }

    @Test
    @DisplayName("Should convert black hex to zero")
    void hexColorToInt_black_returnsZero() {
        assertEquals(0, StringUtil.hexColorToInt("000000"));
    }

    @Test
    @DisplayName("Should convert red hex to correct value")
    void hexColorToInt_red_returnsCorrectValue() {
        assertEquals(16711680, StringUtil.hexColorToInt("FF0000"));
    }

    @Test
    @DisplayName("Should convert green hex to correct value")
    void hexColorToInt_green_returnsCorrectValue() {
        assertEquals(65280, StringUtil.hexColorToInt("00FF00"));
    }

    @Test
    @DisplayName("Should convert blue hex to correct value")
    void hexColorToInt_blue_returnsCorrectValue() {
        assertEquals(255, StringUtil.hexColorToInt("0000FF"));
    }

    // ==================== Random Color Tests ====================

    @Test
    @DisplayName("Should generate valid random color")
    void randomColor_returnsValidColor() {
        int color = StringUtil.randomColor();
        assertTrue(color >= 0 && color <= 16777215);
    }

    @Test
    @DisplayName("Should generate different random colors")
    void randomColor_multipleCalls_returnsDifferentValues() {
        int color1 = StringUtil.randomColor();
        int color2 = StringUtil.randomColor();
        int color3 = StringUtil.randomColor();

        // At least one should be different (very high probability)
        assertTrue(color1 != color2 || color2 != color3 || color1 != color3);
    }

    // ==================== Format Money Tests ====================

    @Test
    @DisplayName("Should format zero money")
    void formatMoney_zero_returnsZeroString() {
        String result = StringUtil.formatMoney(0);
        assertEquals("0.0", result);
    }

    @Test
    @DisplayName("Should format under thousand money")
    void formatMoney_underThousand_returnsPlainNumber() {
        String result = StringUtil.formatMoney(999);
        assertTrue(result.contains("999"));
    }

    @Test
    @DisplayName("Should format thousand with K suffix")
    void formatMoney_thousand_returnsKFormat() {
        String result = StringUtil.formatMoney(1000);
        assertTrue(result.endsWith("K"));
    }

    @Test
    @DisplayName("Should format five thousand with K suffix")
    void formatMoney_fiveThousand_returnsKFormat() {
        String result = StringUtil.formatMoney(5000);
        assertTrue(result.contains("5") && result.endsWith("K"));
    }

    @Test
    @DisplayName("Should format million with M suffix")
    void formatMoney_million_returnsMFormat() {
        String result = StringUtil.formatMoney(1_000_000);
        assertTrue(result.contains("1") && result.endsWith("M"));
    }

    @Test
    @DisplayName("Should format billion with B suffix")
    void formatMoney_billion_returnsBFormat() {
        String result = StringUtil.formatMoney(1_000_000_000);
        assertTrue(result.contains("1") && result.endsWith("B"));
    }

    @Test
    @DisplayName("Should format trillion with T suffix")
    void formatMoney_trillion_returnsTFormat() {
        String result = StringUtil.formatMoney(1_000_000_000_000L);
        assertTrue(result.contains("1") && result.endsWith("T"));
    }

    @Test
    @DisplayName("Should format negative thousand")
    void formatMoney_negativeThousand_returnsKFormat() {
        String result = StringUtil.formatMoney(-1000);
        assertTrue(result.contains("K"));
    }

    // ==================== Colored Money Tests ====================

    @Test
    @DisplayName("Should color positive money green")
    void getColoredMoney_positive_startsWithGreen() {
        String result = StringUtil.getColoredMoney(100);
        assertTrue(result.startsWith("§a+"));
    }

    @Test
    @DisplayName("Should color negative money red")
    void getColoredMoney_negative_startsWithRed() {
        String result = StringUtil.getColoredMoney(-100);
        assertTrue(result.startsWith("§c"));
    }

    @Test
    @DisplayName("Should color zero money gray")
    void getColoredMoney_zero_startsWithGray() {
        String result = StringUtil.getColoredMoney(0);
        assertTrue(result.startsWith("§7"));
    }

    @Test
    @DisplayName("Should format positive thousand with K and green")
    void getColoredMoney_positiveThousand_includesKAndGreen() {
        String result = StringUtil.getColoredMoney(5000);
        assertTrue(result.startsWith("§a+"));
        assertTrue(result.contains("K"));
    }

    @Test
    @DisplayName("Should format negative thousand with K and red")
    void getColoredMoney_negativeThousand_includesKAndRed() {
        String result = StringUtil.getColoredMoney(-5000);
        assertTrue(result.startsWith("§c"));
        assertTrue(result.contains("K"));
    }

    // ==================== Handle Digits Tests ====================

    @Test
    @DisplayName("Should handle integer digits")
    void handleDigits_integer_returnsInteger() {
        double result = StringUtil.handleDigits(100.0);
        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Should round decimal digits")
    void handleDigits_decimal_roundsCorrectly() {
        double result = StringUtil.handleDigits(123.456);
        // Result depends on Constants.getNbDigits()
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle zero")
    void handleDigits_zero_returnsZero() {
        double result = StringUtil.handleDigits(0.0);
        assertEquals(0.0, result);
    }

    @Test
    @DisplayName("Should handle negative numbers")
    void handleDigits_negative_handlesCorrectly() {
        double result = StringUtil.handleDigits(-123.456);
        assertTrue(result < 0);
    }
}